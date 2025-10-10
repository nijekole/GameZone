package com.example.gamezone

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gamezone.ui.screen.RegistrationScreen
import com.example.gamezone.ui.theme.GameZoneTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gamezone.ui.screen.LoginScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamezone.ui.screen.AuthViewModel
import com.example.gamezone.ui.screen.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "App pokrenut")

        setContent {
            GameZoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") { backStackEntry ->
                            val authViewModel: AuthViewModel = viewModel(backStackEntry)

                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = {
                                    Log.d("MainActivity", "Login uspešan, prelazim na home")
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegistration = {
                                    Log.d("MainActivity", "Prelazim na registraciju")
                                    navController.navigate("registration")
                                }
                            )
                        }

                        composable("registration") { backStackEntry ->
                            val authViewModel: AuthViewModel = viewModel(backStackEntry)

                            RegistrationScreen(
                                authViewModel = authViewModel,
                                onRegistrationSuccess = {
                                    Log.d("MainActivity", "Registracija uspešna, vraćam na login")
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("home") { backStackEntry ->
                            val authViewModel: AuthViewModel = viewModel(backStackEntry)
                            HomeScreen(
                                authViewModel = authViewModel,
                                onLogout = {
                                    Log.d("MainActivity", "Logout, vraćam na login")
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}