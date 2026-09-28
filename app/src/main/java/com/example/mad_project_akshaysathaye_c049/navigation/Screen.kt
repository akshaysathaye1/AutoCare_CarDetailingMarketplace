package com.example.mad_project_akshaysathaye_c049.navigation

sealed class Screen(val route: String) {
    // Auth Routes
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoleSelection : Screen("role_selection")

    // Customer Routes
    object CustomerHome : Screen("customer_home")
    object GarageListing : Screen("garage_listing")
    object GarageDetail : Screen("garage_detail/{garageId}") {
        fun createRoute(garageId: String) = "garage_detail/$garageId"
    }
    object ServiceDetail : Screen("service_detail/{serviceId}") {
        fun createRoute(serviceId: String) = "service_detail/$serviceId"
    }

    // Garage Routes
    object GarageDashboard : Screen("garage_dashboard")
    object GarageProfile : Screen("garage_profile")
    object ManageServices : Screen("manage_services")
    object GarageBookings : Screen("garage_bookings")
}
