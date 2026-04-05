package com.example.unifront.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifront.ui.theme.UberBlack
import com.example.unifront.ui.theme.UberWhite
import com.google.firebase.database.FirebaseDatabase

private const val DB_URL = "https://uniride-75ff0-default-rtdb.asia-southeast1.firebasedatabase.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerLoginScreen(
    onLoginSuccess: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(UberWhite)) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f)
                .background(Brush.verticalGradient(listOf(UberBlack, Color(0xFF1A1A1A), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).padding(top = 16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = UberWhite)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text("Passenger Login", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, color = UberWhite))
            Text("Professional campus commute", style = MaterialTheme.typography.bodyMedium.copy(color = UberWhite.copy(alpha = 0.7f)))

            Spacer(modifier = Modifier.height(60.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = UberWhite,
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("University Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

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

                    Button(
                        onClick = {
                            val inputEmail = email.trim().lowercase()
                            val inputPassword = password.trim()

                            if (inputEmail.isBlank() || inputPassword.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isLoading = true
                            val db = FirebaseDatabase.getInstance(DB_URL).reference
                            db.child("users").get().addOnSuccessListener { snapshot ->
                                isLoading = false
                                var studentNameFound = ""
                                var userIdFound = ""
                                var routeFound = ""
                                var isAccountFound = false
                                var isAnyApproved = false
                                
                                for (child in snapshot.children) {
                                    val dbEmail = child.child("email").getValue(String::class.java)?.trim()?.lowercase()
                                    if (dbEmail == inputEmail) {
                                        isAccountFound = true
                                        val dbPass = child.child("password").getValue(String::class.java)?.trim()
                                        if (dbPass == inputPassword) {
                                            val status = child.child("status").getValue(String::class.java)
                                            if (status == "approved") {
                                                isAnyApproved = true
                                                studentNameFound = child.child("name").getValue(String::class.java) ?: "Student"
                                                userIdFound = child.key ?: ""
                                                routeFound = child.child("preferredRoute").getValue(String::class.java) ?: "BH1"
                                                break
                                            }
                                        }
                                    }
                                }

                                if (isAnyApproved) {
                                    val prefs = context.getSharedPreferences("UniRidePrefs", Context.MODE_PRIVATE)
                                    prefs.edit()
                                        .putBoolean("isLoggedIn", true)
                                        .putString("role", "passenger")
                                        .putString("name", studentNameFound)
                                        .putString("userId", userIdFound)
                                        .putString("routeId", routeFound)
                                        .apply()
                                    onLoginSuccess(studentNameFound)
                                } else if (isAccountFound) {
                                    Toast.makeText(context, "Account pending admin approval or incorrect password.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Account not found. Please sign up.", Toast.LENGTH_LONG).show()
                                }
                            }.addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UberBlack),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = UberWhite, modifier = Modifier.size(24.dp))
                        else Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New to UniRide? ", color = Color.Gray)
                Text(
                    "Create Account",
                    color = UberBlack,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }
        }
    }
}
