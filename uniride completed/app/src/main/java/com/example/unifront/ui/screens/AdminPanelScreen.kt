package com.example.unifront.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unifront.data.*
import com.example.unifront.ui.components.MapboxMapView
import com.example.unifront.ui.components.FleetBus
import com.example.unifront.ui.theme.*
import com.google.firebase.database.*
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ADMIN_PANEL"
private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val database = remember { 
        try { FirebaseDatabase.getInstance(DB_URL).reference } catch (e: Exception) { null }
    }
    
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var conductors by remember { mutableStateOf<List<Conductor>>(emptyList()) }
    var busInfos by remember { mutableStateOf<Map<String, BusLiveInfo>>(emptyMap()) }
    var studentRequests by remember { mutableStateOf<List<StudentRequest>>(emptyList()) }
    var allPassengers by remember { mutableStateOf<List<StudentRequest>>(emptyList()) }
    
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddConductorDialog by remember { mutableStateOf(false) }
    var editingDriver by remember { mutableStateOf<Driver?>(null) }
    var editingConductor by remember { mutableStateOf<Conductor?>(null) }
    var editingStudent by remember { mutableStateOf<StudentRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFabMenu by remember { mutableStateOf(false) }

    LaunchedEffect(database) {
        if (database == null) {
            isLoading = false
            return@LaunchedEffect
        }
        
        database.child("drivers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Driver>()
                snapshot.children.forEach { child ->
                    val d = child.getValue(Driver::class.java)
                    if (d != null) list.add(d.copy(id = child.key ?: ""))
                }
                drivers = list
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("conductors").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Conductor>()
                snapshot.children.forEach { child ->
                    val c = child.getValue(Conductor::class.java)
                    if (c != null) list.add(c.copy(id = child.key ?: ""))
                }
                conductors = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("active_buses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val infos = mutableMapOf<String, BusLiveInfo>()
                snapshot.children.forEach { child ->
                    val info = child.getValue(BusLiveInfo::class.java)
                    if (info != null) infos[child.key ?: ""] = info
                }
                busInfos = infos
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pending = mutableListOf<StudentRequest>()
                val approved = mutableListOf<StudentRequest>()
                snapshot.children.forEach { child ->
                    try {
                        val s = child.getValue(StudentRequest::class.java)
                        if (s != null) {
                            if (s.status == "pending") pending.add(s.copy(id = child.key ?: ""))
                            else if (s.status == "approved") approved.add(s.copy(id = child.key ?: ""))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user: ${e.message}")
                    }
                }
                studentRequests = pending
                allPassengers = approved
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("System Administration", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = UberBlack, titleContentColor = UberWhite),
                    actions = {
                        IconButton(onClick = onLogout) { 
                            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = UberWhite) 
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab, containerColor = UberBlack, contentColor = UberWhite) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Fleet") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Live Map") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Passengers") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { 
                        BadgedBox(badge = { if(studentRequests.isNotEmpty()) Badge { Text(studentRequests.size.toString()) } }) {
                            Text("Requests")
                        }
                    })
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showFabMenu) {
                        SmallFloatingActionButton(
                            onClick = { showAddConductorDialog = true; showFabMenu = false },
                            containerColor = UberWhite,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) { Row(Modifier.padding(horizontal = 12.dp)) { Icon(Icons.Default.SupportAgent, null); Spacer(Modifier.width(8.dp)); Text("Conductor") } }
                        
                        SmallFloatingActionButton(
                            onClick = { showAddDriverDialog = true; showFabMenu = false },
                            containerColor = UberWhite,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) { Row(Modifier.padding(horizontal = 12.dp)) { Icon(Icons.Default.Badge, null); Spacer(Modifier.width(8.dp)); Text("Driver") } }
                    }
                    FloatingActionButton(onClick = { showFabMenu = !showFabMenu }, containerColor = UberBlack, contentColor = UberWhite) {
                        Icon(if(showFabMenu) Icons.Default.Close else Icons.Default.Add, "Add")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(UberGray)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UberBlack)
                }
            } else if (database == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Database Connection Error", color = Color.Red)
                }
            } else {
                when (selectedTab) {
                    0 -> MergedFleetList(drivers, conductors, busInfos, 
                            onEditDriver = { editingDriver = it }, 
                            onEditConductor = { editingConductor = it },
                            onDeleteDriver = { database.child("drivers").child(it.id).removeValue() },
                            onDeleteConductor = { database.child("conductors").child(it.id).removeValue() }
                        )
                    1 -> ActiveFleetMapTab(drivers, conductors, busInfos, allPassengers)
                    2 -> RouteBasedPassengerList(allPassengers, onEdit = { editingStudent = it }, onDelete = { database.child("users").child(it.id).removeValue() })
                    3 -> ApprovalList(studentRequests, 
                        onApprove = { database.child("users").child(it.id).child("status").setValue("approved") },
                        onReject = { database.child("users").child(it.id).removeValue() }
                    )
                }
            }
        }

        if (showAddDriverDialog || editingDriver != null) {
            DriverEditDialog(
                driver = editingDriver,
                onDismiss = { showAddDriverDialog = false; editingDriver = null },
                onSave = { name, phone, busId, manualRoute, password, plate, photo ->
                    val driversRef = database?.child("drivers") ?: return@DriverEditDialog
                    val id = editingDriver?.id ?: driversRef.push().key ?: ""
                    driversRef.child(id).setValue(Driver(id, name, phone, busId, manualRoute, password, plate, photo))
                    showAddDriverDialog = false
                    editingDriver = null
                }
            )
        }

        if (showAddConductorDialog || editingConductor != null) {
            ConductorEditDialog(
                conductor = editingConductor,
                onDismiss = { showAddConductorDialog = false; editingConductor = null },
                onSave = { name, phone, busId, photo ->
                    val ref = database?.child("conductors") ?: return@ConductorEditDialog
                    val id = editingConductor?.id ?: ref.push().key ?: ""
                    ref.child(id).setValue(Conductor(id, name, phone, busId, photo))
                    showAddConductorDialog = false
                    editingConductor = null
                }
            )
        }

        if (editingStudent != null) {
            PassengerEditDialog(
                passenger = editingStudent!!,
                onDismiss = { editingStudent = null },
                onSave = { updatedPassenger ->
                    database?.child("users")?.child(updatedPassenger.id)?.setValue(updatedPassenger)
                    editingStudent = null
                }
            )
        }
    }
}

@Composable
fun MergedFleetList(drivers: List<Driver>, conductors: List<Conductor>, busInfos: Map<String, BusLiveInfo>, 
                    onEditDriver: (Driver) -> Unit, onEditConductor: (Conductor) -> Unit,
                    onDeleteDriver: (Driver) -> Unit, onDeleteConductor: (Conductor) -> Unit) {
    val allRouteIds = (drivers.map { it.assignedBusId } + conductors.map { it.assignedBusId }).distinct().filter { it != "None" }
    
    if (allRouteIds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No fleet data found", color = Color.Gray) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(allRouteIds) { routeId ->
                val routeDrivers = drivers.filter { it.assignedBusId == routeId }
                val routeConductors = conductors.filter { it.assignedBusId == routeId }
                val liveInfo = busInfos[routeId.trim().uppercase().replace(" ", "")]
                
                MergedRouteCard(routeId, routeDrivers, routeConductors, liveInfo, onEditDriver, onEditConductor, onDeleteDriver, onDeleteConductor)
            }
        }
    }
}

@Composable
fun MergedRouteCard(routeId: String, drivers: List<Driver>, conductors: List<Conductor>, liveInfo: BusLiveInfo?,
                    onEditDriver: (Driver) -> Unit, onEditConductor: (Conductor) -> Unit,
                    onDeleteDriver: (Driver) -> Unit, onDeleteConductor: (Conductor) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = UberWhite), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsBus, null, tint = UberBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Route: $routeId", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                if (liveInfo?.status == "active") {
                    Surface(color = UberGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("LIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = UberGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Drivers Section
            drivers.forEach { driver ->
                StaffRow("Driver", driver.name, driver.phone, driver.photoUrl, { onEditDriver(driver) }, { onDeleteDriver(driver) })
                if (drivers.indexOf(driver) < drivers.size - 1 || conductors.isNotEmpty()) HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            }
            
            // Conductors Section
            conductors.forEach { conductor ->
                StaffRow("Conductor", conductor.name, conductor.phone, conductor.photoUrl, { onEditConductor(conductor) }, { onDeleteConductor(conductor) })
                if (conductors.indexOf(conductor) < conductors.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun StaffRow(role: String, name: String, phone: String, photo: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (photo.isNotEmpty()) {
            AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, UberGray, CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp), tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(color = if(role == "Driver") UberBlack else UberGreen, shape = RoundedCornerShape(4.dp)) {
                Text(role.uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = UberWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(phone, color = Color.Blue, fontSize = 14.sp, modifier = Modifier.clickable {
                if (phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    try { context.startActivity(intent) } catch(e: Exception) {}
                }
            })
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun RouteBasedPassengerList(passengers: List<StudentRequest>, onEdit: (StudentRequest) -> Unit, onDelete: (StudentRequest) -> Unit) {
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    val routes = BusData.ROUTES.map { it.id }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedRouteId == null) {
            Text("Select Route to View Passengers", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(routes) { routeId ->
                    val count = passengers.count { it.preferredRoute == routeId }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedRouteId = routeId },
                        colors = CardDefaults.cardColors(containerColor = UberWhite)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, null, tint = UberBlack)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Route: $routeId", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$count Passengers Enrolled", color = Color.Gray, fontSize = 14.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedRouteId = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text("Passengers for $selectedRouteId", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            val filtered = passengers.filter { it.preferredRoute == selectedRouteId }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No passengers on this route") }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered) { passenger ->
                        PassengerCard(passenger, onEdit, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerCard(passenger: StudentRequest, onEdit: (StudentRequest) -> Unit, onDelete: (StudentRequest) -> Unit) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = UberWhite), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Text(passenger.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val detail = if(passenger.userType == "Student") "Roll: ${passenger.rollNo}" else "Code: ${passenger.employeeCode} (${passenger.department})"
                Text(detail, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = UberBlack.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                        Text(passenger.userType, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (passenger.isBoarded) {
                        Surface(color = UberGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("IN BUS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = UberGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Phone: ${passenger.phone}", fontSize = 13.sp, color = Color.Blue, modifier = Modifier.clickable {
                    if (passenger.phone.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${passenger.phone}"))
                        try { context.startActivity(intent) } catch(e: Exception) {}
                    }
                })
                if (passenger.isBoarded && passenger.boardedAt > 0) {
                    Text("Boarded at: ${timeFormat.format(Date(passenger.boardedAt))}", fontSize = 13.sp, color = UberGreen, fontWeight = FontWeight.Medium)
                }
            }
            Column {
                IconButton(onClick = { onEdit(passenger) }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { onDelete(passenger) }) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun PassengerEditDialog(passenger: StudentRequest, onDismiss: () -> Unit, onSave: (StudentRequest) -> Unit) {
    var name by remember { mutableStateOf(passenger.name) }
    var rollNo by remember { mutableStateOf(passenger.rollNo) }
    var employeeCode by remember { mutableStateOf(passenger.employeeCode) }
    var department by remember { mutableStateOf(passenger.department) }
    var phone by remember { mutableStateOf(passenger.phone) }
    var route by remember { mutableStateOf(passenger.preferredRoute) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Passenger Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
                if (passenger.userType == "Student") {
                    OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") })
                } else {
                    OutlinedTextField(value = employeeCode, onValueChange = { employeeCode = it }, label = { Text("Employee Code") })
                    OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("Department") })
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                
                Box {
                    OutlinedTextField(
                        value = route,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Route") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BusData.ROUTES.forEach { r ->
                            DropdownMenuItem(text = { Text(r.id) }, onClick = { route = r.id; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(passenger.copy(name = name, rollNo = rollNo, phone = phone, preferredRoute = route, employeeCode = employeeCode, department = department)) }) {
                Text("Save Changes")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ActiveFleetMapTab(drivers: List<Driver>, conductors: List<Conductor>, busInfos: Map<String, BusLiveInfo>, allPassengers: List<StudentRequest>) {
    val activeBuses = drivers.filter { 
        busInfos[it.assignedBusId.trim().uppercase().replace(" ", "")]?.status == "active"
    }
    
    var selectedDriver by remember { mutableStateOf<Driver?>(null) }

    val fleetList = activeBuses.map { driver ->
        val info = busInfos[driver.assignedBusId.trim().uppercase().replace(" ", "")]!!
        FleetBus(
            id = driver.id,
            routeId = driver.assignedBusId,
            location = GeoPoint(info.lat, info.lng),
            occupancy = info.occupancy
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMapView(
            modifier = Modifier.fillMaxSize(),
            fleetBuses = fleetList,
            onBusClick = { busId ->
                selectedDriver = drivers.find { it.id == busId }
            }
        )
        
        if (selectedDriver != null) {
            val driver = selectedDriver!!
            val info = busInfos[driver.assignedBusId.trim().uppercase().replace(" ", "")]!!
            val conductor = conductors.find { it.assignedBusId == driver.assignedBusId }
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val passengersOnThisBus = allPassengers.filter { it.preferredRoute == driver.assignedBusId && it.isBoarded }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 550.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = UberWhite),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fleet Details: ${driver.assignedBusId}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { selectedDriver = null }) { Icon(Icons.Default.Close, null) }
                    }
                    
                    Spacer(Modifier.height(12.dp))

                    // DRIVER & CONDUCTOR INFO
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StaffLiveCard("Driver", driver.name, driver.phone, driver.photoUrl, Modifier.weight(1f))
                        if (conductor != null) {
                            StaffLiveCard("Conductor", conductor.name, conductor.phone, conductor.photoUrl, Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Boarded Passengers (${passengersOnThisBus.size})", fontWeight = FontWeight.Bold, color = UberBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (passengersOnThisBus.isEmpty()) {
                        Text("No passengers currently on board.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(passengersOnThisBus) { passenger ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (passenger.profilePhotoUrl.isNotEmpty()) {
                                        AsyncImage(model = passenger.profilePhotoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape).border(1.dp, UberGray, CircleShape), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(50.dp), tint = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(passenger.name, fontWeight = FontWeight.Bold)
                                        val detail = if(passenger.userType == "Student") "Roll: ${passenger.rollNo}" else "${passenger.userType} - ${passenger.department}"
                                        Text(detail, fontSize = 12.sp, color = Color.Gray)
                                        Text("Phone: ${passenger.phone}", fontSize = 12.sp, color = Color.Blue)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                    
                    Surface(modifier = Modifier.fillMaxWidth(), color = UberGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Live Occupancy: ${info.occupancy}/60 Seats",
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = UberGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StaffLiveCard(role: String, name: String, phone: String, photo: String, modifier: Modifier) {
    val context = LocalContext.current
    Surface(modifier = modifier.clickable {
        if (phone.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            try { context.startActivity(intent) } catch(e: Exception) {}
        }
    }, color = UberGray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (photo.isNotEmpty()) {
                AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, UberBlack, CircleShape), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(role, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if(role == "Driver") UberBlack else UberGreen)
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(phone, fontSize = 12.sp, color = Color.Blue)
        }
    }
}

@Composable
fun ApprovalList(requests: List<StudentRequest>, onApprove: (StudentRequest) -> Unit, onReject: (StudentRequest) -> Unit) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pending requests", color = Color.Gray) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requests) { request ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = UberWhite), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             if (request.profilePhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = request.profilePhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, UberGray, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(request.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                val detail = if(request.userType == "Student") "Roll: ${request.rollNo}" else "Code: ${request.employeeCode}"
                                Text("$detail | Route: ${request.preferredRoute}", fontSize = 13.sp)
                                Text("Type: ${request.userType}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onReject(request) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Reject") }
                            Button(onClick = { onApprove(request) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = UberBlack)) { Text("Approve") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverEditDialog(driver: Driver?, onDismiss: () -> Unit, onSave: (String, String, String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(driver?.name ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var busId by remember { mutableStateOf(driver?.assignedBusId ?: "None") }
    var password by remember { mutableStateOf(driver?.password ?: "") }
    var plate by remember { mutableStateOf(driver?.plateNumber ?: "") }
    var photo by remember { mutableStateOf(driver?.photoUrl ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { photo = it.toString() } }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (driver == null) "Add New Driver" else "Edit Driver") }, text = {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (photo.isEmpty()) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(80.dp).clickable { 
                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    })
                } else {
                    AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, UberBlack, CircleShape).clickable { 
                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    }, contentScale = ContentScale.Crop)
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Bus Plate Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedTextField(value = busId, onValueChange = {}, readOnly = true, label = { Text("Assign Route") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } } )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    BusData.ROUTES.forEach { route -> DropdownMenuItem(text = { Text(route.id) }, onClick = { busId = route.id; expanded = false }) }
                }
            }
        }
    }, confirmButton = { Button(onClick = { onSave(name, phone, busId, "", password, plate, photo) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorEditDialog(conductor: Conductor?, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(conductor?.name ?: "") }
    var phone by remember { mutableStateOf(conductor?.phone ?: "") }
    var busId by remember { mutableStateOf(conductor?.assignedBusId ?: "None") }
    var photo by remember { mutableStateOf(conductor?.photoUrl ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { photo = it.toString() } }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (conductor == null) "Add New Conductor" else "Edit Conductor") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (photo.isEmpty()) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(80.dp).clickable { 
                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    })
                } else {
                    AsyncImage(model = photo, contentDescription = null, modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, UberGreen, CircleShape).clickable { 
                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    }, contentScale = ContentScale.Crop)
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedTextField(value = busId, onValueChange = {}, readOnly = true, label = { Text("Assign Route") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } } )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    BusData.ROUTES.forEach { route -> DropdownMenuItem(text = { Text(route.id) }, onClick = { busId = route.id; expanded = false }) }
                }
            }
        }
    }, confirmButton = { Button(onClick = { onSave(name, phone, busId, photo) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
