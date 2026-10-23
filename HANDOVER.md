# AutoCare - Member 1 Handover Document

## 📌 What Member 1 Completed
The core foundation of the **AutoCare** application has been built and is ready for feature development.
- **Project Structure:** Setup Jetpack Compose, Material 3, Navigation, and MVVM-ready architecture.
- **UI Components:** Reusable buttons, cards, text fields, and themes inside `ui/components/`.
- **Firebase Authentication:** Integrated Email/Password login and registration.
- **Firestore Integration:** Created data models, repositories, and initial mock-data injection.
- **Role System:** Defined basic app routing for two distinct roles (`CUSTOMER` and `GARAGE`).
- **Core Screens:** Built the basic layouts for Authentication, Customer Home, Garage Detail, Garage Dashboard, and Manage Services.

## 🏗️ Project Structure
```
com.example.mad_project_akshaysathaye_c049
│
├── data/
│   ├── mock/        # MockDataInjector.kt
│   ├── model/       # Data classes (User, Garage, Service, Vehicle, Booking, Review)
│   └── repository/  # Firestore data-access classes
│
├── navigation/      # NavGraph.kt & Screen definitions
│
├── ui/              # Jetpack Compose Screens
│   ├── auth/        # Login, Register, Splash
│   ├── customer/    # Customer Home, Garage Detail
│   ├── garage/      # Garage Dashboard
│   ├── components/  # Reusable UI widgets
│   └── theme/       # Material 3 typography and colors
│
└── MainActivity.kt  # Entry point & NavHost
```

## 🔐 Firebase Authentication & Roles
- **Setup:** The app uses Firebase Email/Password auth.
- **How Roles Work:** During registration, users select a role (`CUSTOMER` or `GARAGE`). This role is saved in the `users` Firestore collection. When logging in, `AuthRepository.kt` fetches this role and the `NavGraph` uses it to determine whether to navigate to the `customer_home` or `garage_home`.

## 🗄️ Firestore Database Structure & Models
The database is structured into 6 primary collections. Models are in `data/model/`:
1. `users` (Managed by `AuthRepository`)
2. `garages` (`GarageRepository`)
3. `services` (`ServiceRepository`)
4. `vehicles` (`VehicleRepository`)
5. `bookings` (`BookingRepository`)
6. `reviews` (`ReviewRepository`)

*A `firestore.rules` file has been provided in the root directory. It enforces basic security:*
- *Users can only edit their own profiles.*
- *Only Garage owners can create/edit Garages and Services.*
- *Customers can only book for themselves, and only view their own bookings.*

## 🚀 How to Run the Project
1. Open the project in Android Studio.
2. Build the project using Gradle.
3. The app connects to Firebase automatically (ensure `google-services.json` is properly configured in the `app/` directory).
4. Run on an emulator or physical device. 
*Note: `MockDataInjector` runs on launch to populate the Customer Home screen if the DB is empty.*

---

## 🛠️ What Next Members Should Build

### **Member 2: Garage Marketplace**
- Implement search and filtering on the Customer Home Screen.
- Complete the Garage Detail screen UI (e.g. tabs for services, reviews, info).
- Allow Garage owners to actively add/edit their own services via the UI.

### **Member 3: Booking Engine**
- Build the "Select Date & Time" flow for services.
- Integrate the `BookingRepository` to create bookings.
- Implement the Customer "My Bookings" screen and Garage "Manage Bookings" screen.
- Manage vehicle profiles (Add/edit vehicles).

### **Member 4: Dashboards & Reviews (COMPLETED ✅)**
- **Reviews & Ratings System:**
  - `Review.kt`: Core data model and `ReviewValidator` enforcing rating range [1.0, 5.0], comment length [10, 500] chars, and non-blank ownership fields.
  - `ReviewRepository.kt`: Full Firestore data-access layer for reviews, duplicate submission prevention (`checkReviewExistsForBooking`), rating aggregation (`getGarageRatingStats`), and updating denormalized rating on garage records.
  - `RatingInputComponent.kt`: Interactive star-rating component with hover/touch states and rating labels.
  - `SubmitReviewScreen.kt`: Comprehensive review submission flow with booking validation, error states, and automatic return to Bookings.
  - `MyBookingsScreen.kt`: Wired "⭐ Review Service" action on completed bookings with navigation to review form.
  - `GarageDetailScreen.kt`: Dynamic reviews list, average rating score display, and total review count.
- **Customer Dashboard:**
  - `CustomerDashboardRepository.kt`: Aggregates customer data from `BookingRepository` and `VehicleRepository` without hardcoded figures. Excludes cancelled bookings from service count and spending calculations. Computes service history frequency and spending by workshop.
  - `CustomerDashboardScreen.kt`: Full Jetpack Compose dashboard with profile greeting, account overview cards, upcoming appointments, recent activity, registered vehicles with service counts, top requested services breakdown, and quick actions.
  - Wired in `NavGraph.kt` (`Screen.CustomerDashboard`) and added to `CustomerBottomBar`.
- **Garage Dashboard:**
  - `GarageDashboardRepository.kt`: Scoped strictly to authenticated `ownerId` with ownership verification. Computes booking queue status (pending, confirmed, in-progress, completed), total revenue, today's earnings, and average rating.
  - `GarageDashboardScreen.kt`: Partner workshop dashboard with profile summary, listed detailing services, live operations metrics, workshop bay/slot availability load indicator, incoming bookings queue, customer feedback review cards, and quick actions.
- **Verification & Tests:**
  - `ReviewValidatorTest.kt`: Unit tests verifying review bounds, comment length, and authentication constraints.
  - `DashboardStatsUnitTest.kt`: Unit tests verifying customer spending calculations, vehicle service mappings, service history sorting, and garage queue statistics.
  - All unit tests verified and passing via `./gradlew testDebugUnitTest`.

Good luck! AutoCare is fully equipped with complete end-to-end marketplace flows. 🚗✨

