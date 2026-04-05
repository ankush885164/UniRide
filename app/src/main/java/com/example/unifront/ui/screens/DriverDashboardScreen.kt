package com.example.unifront.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.unifront.data.BusData
import com.example.unifront.data.BusStop
import com.example.unifront.data.StudentRequest
import com.example.unifront.ui.components.MapboxMapView
import com.example.unifront.ui.theme.*
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.math.*

private const val TAG = "TRACKING_DRIVER"
private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(busId: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val database = remember { try { FirebaseDatabase.getInstance(DB_URL).reference } catch (e: Exception) { null } }
    val routeKey = remember(busId) { busId.trim().uppercase().replace(" ", "") }
    
    var isSharing by remember { mutableStateOf(false) }
    var isAddStopMode by remember { mutableStateOf(false) }
    
    var dynamicStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    val selectedRoute = remember(busId, dynamicStops) { 
        val baseRoute = BusData.ROUTES.find { it.id == busId } ?: BusData.ROUTES[0]
        baseRoute.copy(stops = baseRoute.stops + dynamicStops)
    }

    var speed by remember { mutableFloatStateOf(0f) }
    var lastLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var optimalRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    
    var busOccupancy by remember { mutableIntStateOf(0) }
    val totalSeats = 60
    
    var stopPassengerCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var waitingStudentsDetail by remember { mutableStateOf<List<StudentRequest>>(emptyList()) }
    var stopSpecificUsers by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selectedStopDetail by remember { mutableStateOf<String?>(null) }
    
    var clickedMapLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showAddStopDialog by remember { mutableStateOf(false) }
    var showManageStopsDialog by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) isSharing = true
        else Toast.makeText(context, "Location required", Toast.LENGTH_SHORT).show()
    }

    // SYNC SEATS
    DisposableEffect(routeKey, database) {
        if (database == null) return@DisposableEffect onDispose {}
        val busRef = database.child("active_buses").child(routeKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    busOccupancy = snapshot.child("occupancy").getValue(Int::class.java) ?: 0
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        busRef.addValueEventListener(listener)
        onDispose { busRef.removeEventListener(listener) }
    }

    // Load Approved Students/Staff
    LaunchedEffect(database) {
        if (database == null) return@LaunchedEffect
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StudentRequest>()
                snapshot.children.forEach { child ->
                    try {
                        val s = child.getValue(StudentRequest::class.java)
                        if (s != null && s.status == "approved") {
                            list.add(s.copy(id = child.key ?: ""))
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error parsing passenger: ${e.message}") }
                }
                waitingStudentsDetail = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Fetch Dynamic Stops
    LaunchedEffect(routeKey, database) {
        if (database == null) return@LaunchedEffect
        database.child("dynamic_stops").child(routeKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stops = mutableListOf<BusStop>()
                snapshot.children.forEach { child ->
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val lat = child.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("lng").getValue(Double::class.java) ?: 0.0
                    val time = child.child("scheduledTime").getValue(String::class.java) ?: ""
                    stops.add(BusStop(name, GeoPoint(lat, lng), time))
                }
                dynamicStops = stops
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Listen for Passengers at stops
    DisposableEffect(routeKey, database, selectedRoute) {
        if (database == null) return@DisposableEffect onDispose {}
        val stopsRef = database.child("stops").child(routeKey)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()
                val userMap = mutableMapOf<String, List<String>>()
                
                selectedRoute.stops.forEach { stop ->
                    val sanitized = stop.name.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "")
                    val stopNode = snapshot.child(sanitized)
                    val count = stopNode.child("passenger_count").getValue(Int::class.java) ?: 0
                    counts[stop.name] = count
                    
                    val userIds = mutableListOf<String>()
                    stopNode.child("waiting_users").children.forEach { u ->
                        userIds.add(u.key ?: "")
                    }
                    userMap[stop.name] = userIds
                }
                stopPassengerCounts = counts
                stopSpecificUsers = userMap
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        stopsRef.addValueEventListener(listener)
        onDispose { stopsRef.removeEventListener(listener) }
    }

    LaunchedEffect(lastLocation, selectedRoute) {
        if (lastLocation != null) {
            val destination = BusData.UNIVERSITY_LOCATION
            val stopsToVisit = selectedRoute.stops.map { it.location }
            optimalRoutePoints = calculateOptimalRoute(lastLocation!!, emptyList(), stopsToVisit, destination)
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val point = GeoPoint(location.latitude, location.longitude)
                    lastLocation = point
                    speed = location.speed * 3.6f
                    if (isSharing && database != null) {
                        database.child("active_buses").child(routeKey).updateChildren(mapOf(
                            "lat" to location.latitude,
                            "lng" to location.longitude,
                            "speed" to speed.toDouble(),
                            "status" to "active",
                            "lastUpdated" to System.currentTimeMillis()
                        ))
                    }
                }
            }
        }
    }

    DisposableEffect(isSharing, routeKey, database) {
        if (isSharing) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(5f)
                .build()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                database?.child("active_buses")?.child(routeKey)?.updateChildren(mapOf(
                    "status" to "active",
                    "serviceStart" to System.currentTimeMillis()
                ))
            } else {
                isSharing = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            database?.child("active_buses")?.child(routeKey)?.updateChildren(mapOf(
                "status" to "offline",
                "serviceEnd" to System.currentTimeMillis()
            ))
        }
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Driver Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(UberWhite)) {
            MapboxMapView(
                modifier = Modifier.fillMaxSize(),
                busLocation = lastLocation,
                stops = selectedRoute.stops,
                stopPassengerCounts = stopPassengerCounts,
                busOccupancy = busOccupancy,
                totalSeats = totalSeats,
                routePoints = optimalRoutePoints,
                recenterTrigger = recenterTrigger,
                onStopClick = { stopName ->
                    selectedStopDetail = stopName
                },
                onMapClick = { location ->
                    if (isAddStopMode) {
                        clickedMapLocation = location
                        showAddStopDialog = true
                    }
                }
            )

            // Info Card at top
            Surface(
                modifier = Modifier.padding(16.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                color = UberWhite, shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBus, null, tint = UberBlack, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Bus ID: $busId", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text(if (isSharing) "ON SERVICE" else "OFFLINE", color = if (isSharing) UberGreen else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Right side buttons
            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { isAddStopMode = !isAddStopMode }, 
                    containerColor = if (isAddStopMode) UberGreen else UberWhite
                ) { Icon(Icons.Default.AddLocation, "Add Stop Mode", tint = if(isAddStopMode) Color.White else UberBlack) }
                SmallFloatingActionButton(onClick = { showManageStopsDialog = true }, containerColor = UberWhite) { Icon(Icons.Default.EditLocation, "Manage Stops") }
                SmallFloatingActionButton(onClick = { recenterTrigger++ }, containerColor = UberWhite) { Icon(Icons.Default.MyLocation, "Recenter") }
            }

            if (isAddStopMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                    color = UberGreen, shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp
                ) {
                    Text("Select a location on map to add stop", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // COLLAPSIBLE BOTTOM SHEET
            var isSheetExpanded by remember { mutableStateOf(false) }
            
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().animateContentSize()
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)), 
                color = UberWhite, 
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    // Handle Bar / Toggle
                    Box(
                        modifier = Modifier.fillMaxWidth().height(32.dp).clickable { isSheetExpanded = !isSheetExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(40.dp, 4.dp).background(UberGray, CircleShape))
                    }

                    if (isSheetExpanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = if (isSharing) "In Transit" else "Ready to Start", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                                Text(text = "Target: KRM University", color = UberDarkGray)
                            }
                            if (isSharing) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${speed.toInt()} km/h", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                                    Text("Speed", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val activeWaiters = stopPassengerCounts.filter { it.value > 0 }
                        if (activeWaiters.isNotEmpty()) {
                            Text("Waiting Passengers (Tap to view details)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(activeWaiters.keys.toList()) { stopName ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedStopDetail = stopName },
                                        color = UberGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.People, null, tint = UberGreen)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("$stopName: ${activeWaiters[stopName]} Passengers", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("No passengers waiting currently", style = MaterialTheme.typography.bodyMedium, color = UberDarkGray, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), color = UberBlack.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EventSeat, null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Seats: $busOccupancy / $totalSeats Occupied", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { 
                            if (!isSharing) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else isSharing = true
                            } else isSharing = false
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSharing) Color(0xFFE11900) else UberBlack)
                    ) {
                        Text(if (isSharing) "End Service" else "Start Service", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (isSheetExpanded) { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (selectedStopDetail != null) {
        val stopName = selectedStopDetail!!
        AlertDialog(
            onDismissRequest = { selectedStopDetail = null },
            title = { Text("Waiting at $stopName") },
            text = {
                val userIdsAtStop = stopSpecificUsers[stopName] ?: emptyList()
                val passengersAtStop = waitingStudentsDetail.filter { it.id in userIdsAtStop }
                
                if (passengersAtStop.isEmpty()) {
                    Text("No specific records found for this stop yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(passengersAtStop) { passenger ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (passenger.profilePhotoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = passenger.profilePhotoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, UberGray, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(passenger.name, fontWeight = FontWeight.Bold)
                                    if (passenger.userType == "Student") {
                                        Text("Roll: ${passenger.rollNo}", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        Text("${passenger.userType} - ${passenger.department}", fontSize = 12.sp, color = Color.Gray)
                                        Text("Code: ${passenger.employeeCode}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text("Phone: ${passenger.phone}", fontSize = 12.sp, color = Color.Blue)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedStopDetail = null }) { Text("Close") } }
        )
    }

    if (showAddStopDialog && clickedMapLocation != null) {
        AddStopDialog(
            onDismiss = { showAddStopDialog = false; isAddStopMode = false },
            onSave = { name, time ->
                if (database != null) {
                    val stopData = mapOf(
                        "name" to name,
                        "lat" to clickedMapLocation!!.latitude,
                        "lng" to clickedMapLocation!!.longitude,
                        "scheduledTime" to time
                    )
                    database.child("dynamic_stops").child(routeKey).push().setValue(stopData)
                    Toast.makeText(context, "New Stop Added!", Toast.LENGTH_SHORT).show()
                }
                showAddStopDialog = false
                isAddStopMode = false
            }
        )
    }

    if (showManageStopsDialog) {
        ManageStopsDialog(stops = selectedRoute.stops, onDismiss = { showManageStopsDialog = false }, onRemoveRequest = { stop ->
            database?.child("stop_removal_requests")?.push()?.setValue(mapOf("busId" to busId, "routeKey" to routeKey, "stopName" to stop.name))
            showManageStopsDialog = false
        })
    }
}

@Composable
fun AddStopDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var stopName by remember { mutableStateOf("") }
    var stopTime by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Add New Stop") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter details for the selected location.")
                OutlinedTextField(value = stopName, onValueChange = { stopName = it }, label = { Text("Stop Name") })
                OutlinedTextField(value = stopTime, onValueChange = { stopTime = it }, label = { Text("Scheduled Time (e.g. 7:15 AM)") })
            }
        }, 
        confirmButton = { 
            Button(onClick = { if(stopName.isNotEmpty()) onSave(stopName, stopTime) }) { Text("Save Stop") } 
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManageStopsDialog(stops: List<BusStop>, onDismiss: () -> Unit, onRemoveRequest: (BusStop) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Manage Stops") }, text = {
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { 
            items(stops) { stop -> 
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stop.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveRequest(stop) }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            } 
        }
    }, confirmButton = { TextButton(onClick = { onDismiss() }) { Text("Close") } })
}

private suspend fun calculateOptimalRoute(start: GeoPoint, passengers: List<GeoPoint>, stops: List<GeoPoint>, destination: GeoPoint): List<Point> = withContext(Dispatchers.IO) {
    val allPoints = mutableListOf(start) + stops + destination
    val coordinates = allPoints.joinToString(";") { "${it.longitude},${it.latitude}" }
    val points = mutableListOf<Point>()
    try {
        val url = URL("https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=geojson")
        val connection = url.openConnection() as HttpURLConnection
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val routes = json.getJSONArray("routes")
        if (routes.length() > 0) {
            val route = routes.getJSONObject(0)
            val geometry = route.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            for (i in 0 until coords.length()) {
                val coordArray = coords.getJSONArray(i)
                points.add(Point.fromLngLat(coordArray.getDouble(0), coordArray.getDouble(1)))
            }
        }
    } catch (e: Exception) { Log.e(TAG, "Route Error: ${e.message}") }
    points
}

private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
