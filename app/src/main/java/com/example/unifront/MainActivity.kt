package com.example.unifront

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unifront.ui.screens.*
import com.example.unifront.ui.theme.UniFrontTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniFrontTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    val prefs = context.getSharedPreferences("UniRidePrefs", Context.MODE_PRIVATE)
    val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
    val userRole = prefs.getString("role", "")
    val studentName = prefs.getString("name", "Student")
    val busId = prefs.getString("busId", "")

    val startDestination = if (isLoggedIn) {
        when (userRole) {
            "passenger" -> "passenger_home/$studentName"
            "driver" -> "driver_dashboard/$busId"
            "admin" -> "admin_panel"
            else -> "splash"
        }
    } else {
        "splash"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") { 
            SplashScreen(onAnimationFinished = { 
                navController.navigate("role_selection") {
                    popUpTo("splash") { inclusive = true }
                }
            }) 
        }
        composable("role_selection") { 
            LoginRoleScreen(
                onDriverSelected = { navController.navigate("driver_login") },
                onPassengerSelected = { navController.navigate("passenger_login") },
                onAdminSelected = { navController.navigate("admin_login") }
            ) 
        }
        composable("passenger_login") {
            PassengerLoginScreen(
                onLoginSuccess = { name ->
                    navController.navigate("passenger_home/$name") {
                        popUpTo("passenger_login") { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate("passenger_signup") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("passenger_signup") {
            PassengerSignupScreen(
                onSignUpRequested = {
                    navController.navigate("passenger_login") {
                        popUpTo("passenger_signup") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("driver_login") {
            DriverLoginScreen(
                onLoginSuccess = { id ->
                    navController.navigate("driver_dashboard/$id") {
                        popUpTo("driver_login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "passenger_home/{studentName}",
            arguments = listOf(navArgument("studentName") { defaultValue = "Student" })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("studentName") ?: "Student"
            PassengerHomeScreen(
                studentName = name,
                onLogout = {
                    prefs.edit().clear().apply()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "driver_dashboard/{busId}",
            arguments = listOf(navArgument("busId") { defaultValue = "" })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("busId") ?: ""
            DriverDashboardScreen(
                busId = id,
                onLogout = {
                    prefs.edit().clear().apply()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("admin_login") {
            AdminLoginScreen(
                onLoginSuccess = {
                    prefs.edit().apply {
                        putBoolean("isLoggedIn", true)
                        putString("role", "admin")
                        apply()
                    }
                    navController.navigate("admin_panel") {
                        popUpTo("admin_login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("admin_panel") {
            AdminPanelScreen(
                onLogout = {
                    prefs.edit().clear().apply()
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
