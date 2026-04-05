package com.example.unifront.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unifront.data.BusData
import com.example.unifront.ui.theme.UberBlack
import com.example.unifront.ui.theme.UberWhite
import com.google.firebase.database.FirebaseDatabase

private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerSignupScreen(
    onSignUpRequested: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRouteId by remember { mutableStateOf(BusData.ROUTES[0].id) }
    
    // ROLE-BASED FIELDS
    var userType by remember { mutableStateOf("Student") }
    var rollNo by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    var expandedType by remember { mutableStateOf(false) }
    var expandedDept by remember { mutableStateOf(false) }
    var expandedRoute by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> profilePhotoUri = uri }

    val roles = listOf("Student", "Staff", "Faculty")
    val departments = listOf("Engineering", "Management", "Law", "Medical", "Arts", "Basic Sciences", "Administration")

    Box(modifier = Modifier.fillMaxSize().background(UberWhite)) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f)
                .background(Brush.verticalGradient(listOf(UberBlack, Color(0xFF1A1A1A), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).padding(top = 16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = UberWhite)
            }

            Text("Join UniRide", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, color = UberWhite))
            Text("Professional transit registration", style = MaterialTheme.typography.bodyMedium.copy(color = UberWhite.copy(alpha = 0.7f)))

            Spacer(modifier = Modifier.height(30.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                color = UberWhite, shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Profile Photo Upload
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(100.dp).clickable { photoLauncher.launch("image/jpeg") },
                                shape = CircleShape, color = Color.LightGray.copy(alpha = 0.3f),
                                border = if(profilePhotoUri == null) null else BorderStroke(2.dp, UberBlack)
                            ) {
                                if (profilePhotoUri != null) {
                                    AsyncImage(
                                        model = profilePhotoUri, contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.padding(30.dp), tint = Color.Gray)
                                }
                            }
                            Text("Upload Photo (JPG/Required)*", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = it }) {
                        OutlinedTextField(
                            value = userType, onValueChange = {}, readOnly = true, label = { Text("I am a...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Group, null) }, shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            roles.forEach { role ->
                                DropdownMenuItem(text = { Text(role) }, onClick = { userType = role; expandedType = false })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) },
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    if (userType == "Student") {
                        OutlinedTextField(
                            value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") },
                            modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Badge, null) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = employeeCode, onValueChange = { employeeCode = it }, label = { Text("Employee Code") },
                            modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Badge, null) },
                            shape = RoundedCornerShape(12.dp), singleLine = true
                        )
                        
                        ExposedDropdownMenuBox(expanded = expandedDept, onExpandedChange = { expandedDept = it }) {
                            OutlinedTextField(
                                value = department, onValueChange = {}, readOnly = true, label = { Text("Department") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDept) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Apartment, null) }, shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedDept, onDismissRequest = { expandedDept = false }) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(text = { Text(dept) }, onClick = { department = dept; expandedDept = false })
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, label = { Text("University Email") },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, null) },
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Phone, null) },
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )

                    ExposedDropdownMenuBox(expanded = expandedRoute, onExpandedChange = { expandedRoute = it }) {
                        OutlinedTextField(
                            value = selectedRouteId, onValueChange = {}, readOnly = true, label = { Text("Route") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoute) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Route, null) }, shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedRoute, onDismissRequest = { expandedRoute = false }) {
                            BusData.ROUTES.forEach { route ->
                                DropdownMenuItem(text = { Text(route.id) }, onClick = { selectedRouteId = route.id; expandedRoute = false })
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (profilePhotoUri == null) {
                                Toast.makeText(context, "Please upload a profile photo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
                                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            val db = FirebaseDatabase.getInstance(DB_URL).reference
                            val userId = db.child("users").push().key ?: ""
                            val userMap = mutableMapOf(
                                "id" to userId, "name" to name.trim(), "email" to email.trim().lowercase(),
                                "phone" to phone.trim(), "password" to password.trim(),
                                "preferredRoute" to selectedRouteId, "status" to "pending",
                                "role" to "passenger", "userType" to userType,
                                "profilePhotoUrl" to profilePhotoUri.toString()
                            )
                            
                            if (userType == "Student") {
                                userMap["rollNo"] = rollNo.trim()
                            } else {
                                userMap["employeeCode"] = employeeCode.trim()
                                userMap["department"] = department
                            }

                            db.child("users").child(userId).setValue(userMap).addOnSuccessListener {
                                isLoading = false
                                onSignUpRequested()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UberBlack),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = UberWhite, modifier = Modifier.size(24.dp))
                        else Text("Request Access", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
