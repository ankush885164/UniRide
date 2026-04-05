package com.example.unifront.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifront.ui.theme.UberBlack
import com.example.unifront.ui.theme.UberWhite
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverLoginScreen(onLoginSuccess: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { FirebaseDatabase.getInstance(DB_URL).reference }

    var name by remember { mutableStateOf("") }
    var busId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.DirectionsBus,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = UberBlack
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Driver Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = busId,
                onValueChange = { busId = it },
                label = { Text("Bus ID") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.DirectionsBus, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val inputName = name.trim()
                    val inputBusId = busId.trim()
                    val inputPassword = password.trim()

                    if (inputName.isEmpty() || inputBusId.isEmpty() || inputPassword.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    database.child("drivers").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var success = false
                            var exactBusId = ""
                            snapshot.children.forEach { child ->
                                val dName = child.child("name").getValue(String::class.java)?.trim()
                                val dBusId = child.child("assignedBusId").getValue(String::class.java)?.trim()
                                val dPassword = child.child("password").getValue(String::class.java)?.trim()
                                
                                if (dName?.equals(inputName, ignoreCase = true) == true &&
                                    dBusId?.equals(inputBusId, ignoreCase = true) == true &&
                                    dPassword == inputPassword) {
                                    success = true
                                    exactBusId = dBusId ?: ""
                                }
                            }
                            isLoading = false
                            if (success) {
                                // Persist Session for Driver
                                val prefs = context.getSharedPreferences("UniRidePrefs", Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putBoolean("isLoggedIn", true)
                                    putString("role", "driver")
                                    putString("busId", exactBusId)
                                    apply()
                                }
                                onLoginSuccess(exactBusId)
                            } else {
                                Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            isLoading = false
                            Toast.makeText(context, "Database Error", Toast.LENGTH_SHORT).show()
                        }
                    })
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UberBlack),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = UberWhite, modifier = Modifier.size(24.dp))
                else Text("Login & Start Service", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
