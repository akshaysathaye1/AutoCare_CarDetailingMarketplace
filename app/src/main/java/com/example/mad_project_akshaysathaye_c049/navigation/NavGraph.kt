package com.example.mad_project_akshaysathaye_c049.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mad_project_akshaysathaye_c049.ui.auth.LoginScreen
import com.example.mad_project_akshaysathaye_c049.ui.auth.RegisterScreen
import com.example.mad_project_akshaysathaye_c049.ui.auth.RoleSelectionScreen
import com.example.mad_project_akshaysathaye_c049.ui.auth.SplashScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.CustomerDashboardScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.CustomerHomeScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.GarageDetailScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.GarageListingScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.ServiceDetailScreen
import com.example.mad_project_akshaysathaye_c049.ui.customer.SubmitReviewScreen
import com.example.mad_project_akshaysathaye_c049.ui.garage.GarageBookingsScreen
import com.example.mad_project_akshaysathaye_c049.ui.garage.GarageDashboardScreen
import com.example.mad_project_akshaysathaye_c049.ui.garage.GarageProfileScreen
import com.example.mad_project_akshaysathaye_c049.ui.garage.ManageServicesScreen

@Composable
fun AutoCareNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Authentication Flow ---
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "CUSTOMER") {
                        navController.navigate(Screen.CustomerHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.GarageDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistrationSuccess = { role ->
                    if (role == "CUSTOMER") {
                        navController.navigate(Screen.CustomerHome.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.GarageDashboard.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    if (role == "CUSTOMER") {
                        navController.navigate(Screen.CustomerHome.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.GarageDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // --- Customer Flow ---
        composable(Screen.CustomerHome.route) {
            CustomerHomeScreen(
                onNavigateToGarages = {
                    navController.navigate(Screen.GarageListing.route)
                },
                onNavigateToGarageDetail = { garageId ->
                    navController.navigate(Screen.GarageDetail.createRoute(garageId))
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onLogout = {
                    com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository().logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Member 4: Customer Dashboard
        composable(Screen.CustomerDashboard.route) {
            CustomerDashboardScreen(
                onNavigateToBookings = {
                    navController.navigate(Screen.CustomerBookings.route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateToVehicles = {
                    navController.navigate(Screen.MyVehicles.route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateToGarages = {
                    navController.navigate(Screen.GarageListing.route)
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onLogout = {
                    com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository().logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.GarageListing.route) {
            GarageListingScreen(
                onNavigateToGarageDetail = { garageId ->
                    navController.navigate(Screen.GarageDetail.createRoute(garageId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                }
            )
        }

        composable(
            route = Screen.GarageDetail.route,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            GarageDetailScreen(
                garageId = garageId,
                onNavigateToServiceDetail = { serviceId ->
                    navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ServiceDetail.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            ServiceDetailScreen(
                serviceId = serviceId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBookService = { garageId, sId ->
                    navController.navigate(Screen.BookService.createRoute(garageId, sId))
                }
            )
        }

        // --- Member 3 Booking Engine & Vehicle Routes ---
        composable(Screen.CustomerBookings.route) {
            com.example.mad_project_akshaysathaye_c049.ui.customer.MyBookingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGarages = {
                    navController.navigate(Screen.GarageListing.route)
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateToReview = { bookingId ->
                    navController.navigate(Screen.SubmitReview.createRoute(bookingId))
                }
            )
        }

        composable(
            route = Screen.BookService.route,
            arguments = listOf(
                navArgument("garageId") { type = NavType.StringType },
                navArgument("serviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            com.example.mad_project_akshaysathaye_c049.ui.customer.BookServiceScreen(
                garageId = garageId,
                serviceId = serviceId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookingSuccess = { _ ->
                    navController.navigate(Screen.CustomerBookings.route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateToAddVehicle = {
                    navController.navigate(Screen.AddEditVehicle.createRoute(null))
                }
            )
        }

        composable(Screen.MyVehicles.route) {
            com.example.mad_project_akshaysathaye_c049.ui.customer.MyVehiclesScreen(
                onNavigateToAddEditVehicle = { vehicleId ->
                    navController.navigate(Screen.AddEditVehicle.createRoute(vehicleId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                }
            )
        }

        composable(
            route = Screen.AddEditVehicle.route,
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")
            com.example.mad_project_akshaysathaye_c049.ui.customer.AddEditVehicleScreen(
                vehicleId = vehicleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Member 4: Submit Review
        composable(
            route = Screen.SubmitReview.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            SubmitReviewScreen(
                bookingId = bookingId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReviewSubmitted = {
                    navController.navigate(Screen.CustomerBookings.route) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                }
            )
        }

        // --- Garage Flow ---
        composable(Screen.GarageDashboard.route) {
            GarageDashboardScreen(
                onNavigateToServices = {
                    navController.navigate(Screen.ManageServices.route)
                },
                onNavigateToBookings = {
                    navController.navigate(Screen.GarageBookings.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.GarageProfile.route)
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.GarageDashboard.route)
                    }
                },
                onLogout = {
                    com.example.mad_project_akshaysathaye_c049.data.repository.AuthRepository().logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.GarageProfile.route) {
            GarageProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.GarageDashboard.route)
                    }
                }
            )
        }

        composable(Screen.ManageServices.route) {
            ManageServicesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.GarageDashboard.route)
                    }
                }
            )
        }

        composable(Screen.GarageBookings.route) {
            GarageBookingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.GarageDashboard.route)
                    }
                }
            )
        }
    }
}
