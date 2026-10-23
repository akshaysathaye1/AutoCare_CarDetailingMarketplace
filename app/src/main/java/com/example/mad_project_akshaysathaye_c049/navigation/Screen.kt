package com.example.mad_project_akshaysathaye_c049.navigation

sealed class Screen(val route: String) {
    // Auth Routes
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoleSelection : Screen("role_selection")

    // Customer Routes
    object CustomerHome : Screen("customer_home")
    object CustomerDashboard : Screen("customer_dashboard")
    object GarageListing : Screen("garage_listing")
    object GarageDetail : Screen("garage_detail/{garageId}") {
        fun createRoute(garageId: String) = "garage_detail/$garageId"
    }
    object ServiceDetail : Screen("service_detail/{serviceId}") {
        fun createRoute(serviceId: String) = "service_detail/$serviceId"
    }
    object CustomerBookings : Screen("customer_bookings")
    object BookService : Screen("book_service/{garageId}/{serviceId}") {
        fun createRoute(garageId: String, serviceId: String) = "book_service/$garageId/$serviceId"
    }
    object MyVehicles : Screen("my_vehicles")
    object AddEditVehicle : Screen("add_edit_vehicle?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: String? = null) =
            if (!vehicleId.isNullOrBlank()) "add_edit_vehicle?vehicleId=$vehicleId" else "add_edit_vehicle"
    }
    object SubmitReview : Screen("submit_review/{bookingId}") {
        fun createRoute(bookingId: String) = "submit_review/$bookingId"
    }

    // Garage Routes
    object GarageDashboard : Screen("garage_dashboard")
    object GarageProfile : Screen("garage_profile")
    object ManageServices : Screen("manage_services")
    object GarageBookings : Screen("garage_bookings")
}
