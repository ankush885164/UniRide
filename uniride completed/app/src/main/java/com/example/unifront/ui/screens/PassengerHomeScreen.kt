package com.example.unifront.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.unifront.data.*
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

private const val TAG = "TRACKING_PASSENGER"
private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(studentName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val database = remember {
        try { FirebaseDatabase.getInstance(DB_URL).reference } catch (e: Exception) { null }
    }

    val prefs = remember { context.getSharedPreferences("UniRidePrefs", Context.MODE_PRIVATE) }
    val userId = remember { prefs.getString("userId", "") ?: "" }
    val userAssignedRoute = remember { prefs.getString("routeId", "") ?: "" }

    val selectedRouteId = remember { userAssignedRoute.ifEmpty { BusData.ROUTES[0].id } }
    
    var dynamicStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    var driverInfo by remember { mutableStateOf<Driver?>(null) }
    var conductorInfo by remember { mutableStateOf<Conductor?>(null) }
    
    val selectedRoute = remember(selectedRouteId, dynamicStops) { 
        val baseRoute = BusData.ROUTES.find { it.id == selectedRouteId } ?: BusData.ROUTES[0]
        baseRoute.copy(stops = baseRoute.stops + dynamicStops)
    }
    
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var busLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var busSpeed by remember { mutableFloatStateOf(0f) }
    var isBusActive by remember { mutableStateOf(false) }
    
    var stopPassengerCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var busOccupancy by remember { mutableIntStateOf(0) }
    val totalSeats = 60

    var isDriverNotified by remember { mutableStateOf(false) }
    var isSeatReserved by remember { mutableStateOf(false) }

    var recenterTrigger by remember { mutableIntStateOf(0) }
    var optimalRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    val nearestStop = remember(userLocation, selectedRouteId) {
        userLocation?.let { findNearestStop(it, selectedRoute.stops) }
    }

    val isUserBoarded = remember(busLocation, userLocation) {
        if (busLocation != null && userLocation != null) {
            calculateDistance(busLocation!!, userLocation!!) <= 0.05 // 50 meters
        } else false
    }

    var hasGeofenceNotified by remember { mutableStateOf(false) }
    LaunchedEffect(busLocation, userLocation) {
        if (busLocation != null && userLocation != null && !isUserBoarded && !hasGeofenceNotified) {
            val dist = calculateDistance(busLocation!!, userLocation!!)
            if (dist <= 0.2) {
                Toast.makeText(context, "Bus is near! Get ready at the stop.", Toast.LENGTH_LONG).show()
                hasGeofenceNotified = true
            }
        }
    }

    LaunchedEffect(isUserBoarded, userId) {
        if (isUserBoarded && userId.isNotEmpty()) {
            database?.child("users")?.child(userId)?.updateChildren(mapOf(
                "isBoarded" to true,
                "boardedAt" to System.currentTimeMillis()
            ))
        }
    }

    val etaMinutes = remember(busLocation, nearestStop, isBusActive, isUserBoarded) {
        if (busLocation != null && isBusActive) {
            val destination = if (isUserBoarded) BusData.UNIVERSITY_LOCATION else (nearestStop?.location ?: BusData.UNIVERSITY_LOCATION)
            val dist = calculateDistance(busLocation!!, destination)
            val speed = if (busSpeed > 10f) busSpeed else 25f 
            (dist / (speed / 60.0)).toInt().coerceAtLeast(1)
        } else null
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startUserLocationUpdates(fusedLocationClient) { userLocation = it }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startUserLocationUpdates(fusedLocationClient) { userLocation = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(selectedRouteId, database) {
        if (database == null) return@DisposableEffect onDispose {}
        
        val driversRef = database.child("drivers")
        val driversListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val d = child.getValue(Driver::class.java)
                    if (d?.assignedBusId == selectedRouteId) {
                        driverInfo = d
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        driversRef.addValueEventListener(driversListener)

        val conductorsRef = database.child("conductors")
        val conductorsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val c = child.getValue(Conductor::class.java)
                    if (c?.assignedBusId == selectedRouteId) {
                        conductorInfo = c
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        conductorsRef.addValueEventListener(conductorsListener)

        val routeKey = selectedRouteId.trim().uppercase().replace(" ", "")
        
        val dynamicStopsRef = database.child("dynamic_stops").child(routeKey)
        val dynamicStopsListener = object : ValueEventListener {
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
        }
        dynamicStopsRef.addValueEventListener(dynamicStopsListener)

        val busRef = database.child("active_buses").child(routeKey)
        val busListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.child("status").getValue(String::class.java) == "active") {
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                    busLocation = GeoPoint(lat, lng)
                    busSpeed = snapshot.child("speed").getValue(Double::class.java)?.toFloat() ?: 0f
                    busOccupancy = snapshot.child("occupancy").getValue(Int::class.java) ?: 0
                    isBusActive = true
                } else {
                    isBusActive = false
                    busLocation = null
                    busOccupancy = 0
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        val stopsRef = database.child("stops").child(routeKey)
        val stopsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newCounts = mutableMapOf<String, Int>()
                snapshot.children.forEach { stopSnap ->
                    val stopName = stopSnap.key ?: ""
                    val count = stopSnap.child("passenger_count").getValue(Int::class.java) ?: 0
                    newCounts[stopName] = count
                }
                stopPassengerCounts = newCounts
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        busRef.addValueEventListener(busListener)
        stopsRef.addValueEventListener(stopsListener)

        onDispose { 
            driversRef.removeEventListener(driversListener)
            conductorsRef.removeEventListener(conductorsListener)
            dynamicStopsRef.removeEventListener(dynamicStopsListener)
            busRef.removeEventListener(busListener)
            stopsRef.removeEventListener(stopsListener)
        }
    }

    LaunchedEffect(busLocation, nearestStop, isUserBoarded) {
        if (busLocation != null && (nearestStop != null || isUserBoarded)) {
            val destination = if (isUserBoarded) BusData.UNIVERSITY_LOCATION else nearestStop!!.location
            val points = calculateOptimalRoute(busLocation!!, emptyList(), listOf(destination), BusData.UNIVERSITY_LOCATION)
            optimalRoutePoints = points
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(UberWhite)) {
        MapboxMapView(
            modifier = Modifier.fillMaxSize(),
            busLocation = busLocation,
            userLocation = userLocation,
            stops = selectedRoute.stops,
            stopPassengerCounts = stopPassengerCounts,
            busOccupancy = busOccupancy,
            totalSeats = totalSeats,
            routePoints = optimalRoutePoints,
            recenterTrigger = recenterTrigger
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
                color = UberWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Hello $studentName", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                        Text(if (isUserBoarded) "Heading to K.R. Mangalam..." else "Waiting for commute...", fontSize = 12.sp, color = UberDarkGray)
                    }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
                color = UberWhite, shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsBus, null, tint = UberBlack)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Assigned Bus: $selectedRouteId", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = UberGray)
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (driverInfo != null) {
                            StaffSmallCard("Driver", driverInfo!!.name, driverInfo!!.phone, driverInfo!!.photoUrl, Modifier.weight(1f))
                        }
                        if (conductorInfo != null) {
                            StaffSmallCard("Conductor", conductorInfo!!.name, conductorInfo!!.phone, conductorInfo!!.photoUrl, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (isBusActive && busLocation != null) {
            SmallFloatingActionButton(
                onClick = { recenterTrigger++ },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 100.dp),
                containerColor = UberWhite, shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, null) }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = UberWhite, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.size(40.dp, 4.dp).background(UberGray, CircleShape).align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (isBusActive && etaMinutes != null) "$etaMinutes min" else "Wait for bus", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = if (isUserBoarded) "Arrival: KRM University" else "Arrival: ${nearestStop?.name ?: "Nearby Stop"}", style = MaterialTheme.typography.bodyMedium, color = UberDarkGray)
                    }
                    if (isBusActive) {
                        Column(horizontalAlignment = Alignment.End) {
                            val remaining = (totalSeats - busOccupancy).coerceAtLeast(0)
                            Text(text = "$remaining", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = if (remaining < 5) ComposeColor.Red else UberGreen))
                            Text("Seats left", style = MaterialTheme.typography.labelSmall, color = UberDarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { 
                            if (database == null || nearestStop == null || userId.isEmpty()) return@Button
                            val routeKey = selectedRouteId.trim().uppercase().replace(" ", "")
                            val stopKey = nearestStop.name.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "")
                            val stopRef = database.child("stops").child(routeKey).child(stopKey)
                            
                            stopRef.child("passenger_count").get().addOnSuccessListener { snapshot ->
                                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                                stopRef.child("passenger_count").setValue(currentCount + 1)
                                stopRef.child("waiting_users").child(userId).setValue(true)
                                isDriverNotified = true
                                Toast.makeText(context, "Waiting at ${nearestStop.name}!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isDriverNotified && isBusActive && !isUserBoarded,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDriverNotified) UberGreen else UberBlack)
                    ) {
                        Icon(if (isDriverNotified) Icons.Default.Check else Icons.Default.FrontHand, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDriverNotified) "Notified" else "I'm at stop")
                    }

                    Button(
                        onClick = { 
                            if (database == null) return@Button
                            val routeKey = selectedRouteId.trim().uppercase().replace(" ", "")
                            val busRef = database.child("active_buses").child(routeKey).child("occupancy")
                            busRef.get().addOnSuccessListener { snapshot ->
                                val currentOccupancy = snapshot.getValue(Int::class.java) ?: 0
                                if (isSeatReserved) {
                                    if (currentOccupancy > 0) busRef.setValue(currentOccupancy - 1)
                                    isSeatReserved = false
                                } else {
                                    if (currentOccupancy < totalSeats) {
                                        busRef.setValue(currentOccupancy + 1)
                                        isSeatReserved = true
                                    } else Toast.makeText(context, "Full", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isBusActive && (isSeatReserved || (totalSeats - busOccupancy > 0)),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSeatReserved) UberGreen else UberBlack)
                    ) {
                        Icon(if (isSeatReserved) Icons.Default.EventSeat else Icons.Default.AirlineSeatReclineExtra, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSeatReserved) "Reserved" else "Reserve Seat")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StaffSmallCard(role: String, name: String, phone: String, photo: String, modifier: Modifier) {
    val context = LocalContext.current
    Row(modifier = modifier.clickable {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    }, verticalAlignment = Alignment.CenterVertically) {
        if (photo.isNotEmpty()) {
            AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, UberGray, CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(role, style = MaterialTheme.typography.labelSmall, color = UberDarkGray)
            Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

private fun startUserLocationUpdates(client: FusedLocationProviderClient, onUpdate: (GeoPoint) -> Unit) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
    try {
        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onUpdate(GeoPoint(it.latitude, it.longitude)) }
            }
        }, Looper.getMainLooper())
    } catch (e: SecurityException) { }
}

private fun findNearestStop(userLoc: GeoPoint, stops: List<BusStop>): BusStop {
    return stops.minByOrNull { calculateDistance(userLoc, it.location) } ?: stops[0]
}

private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val r = 6371.0
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
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
