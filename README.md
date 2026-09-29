# 🚗 AutoCare — Car Detailing & Service Booking

### A smart marketplace for car servicing, detailing and maintenance

**AutoCare** is a mobile application that connects **car owners with garages and automobile workshops** through a single platform.

Instead of calling multiple garages, visiting different workshops, or manually comparing prices and services, customers can discover nearby garages, compare available services and prices, select a suitable date and time slot, book a service, and maintain their complete vehicle service history in one place.

Garages and workshops can also use the application to create their business listing, add services, manage prices and time slots, and view customer bookings.

---

## 📌 Problem Statement

Car owners often face difficulties when looking for reliable automobile services.

Some common problems include:

* Difficulty finding suitable garages or detailing centres.
* No single platform to compare different garages and their services.
* Different garages offer different prices for similar services.
* Appointment booking is often handled through calls or messaging.
* Customers may not have an organized record of previous services.
* Garage owners have limited digital visibility and may depend mainly on existing customers.

**AutoCare aims to solve these problems by providing a single digital platform where customers can discover, compare and book automobile services while garages can manage their services and bookings.**

---

## 🎯 Objectives

The main objectives of AutoCare are:

1. Provide customers with a centralized platform to discover garages and workshops.
2. Allow customers to compare garages, services and prices.
3. Provide service booking with selectable dates and time slots.
4. Maintain customer vehicle and service history.
5. Allow customers to view their bookings and total spending.
6. Allow customers to rate and review completed services.
7. Allow garages to create and manage their listings.
8. Allow garages to add services, prices and available slots.
9. Allow garages to view and manage customer bookings.
10. Demonstrate a complete database-driven Android application.

---

## 👥 Target Users

### 1. Customers

Car owners who want to:

* Find garages and detailing centres.
* View available services.
* Compare prices.
* Check ratings and reviews.
* Book a service.
* Track upcoming and previous bookings.
* Maintain vehicle service history.
* View total amount spent.

### 2. Garages / Workshops

Automobile businesses that want to:

* Create their garage profile.
* List available services.
* Set service prices.
* Manage available booking slots.
* View customer bookings.
* Update booking status.
* Build their customer reviews and ratings.

---

## ✨ Core Features

### 🔐 Authentication

* Customer and Garage account registration.
* Login and logout.
* Role-based application flow.
* Firebase Authentication.

### 🏪 Garage Discovery

Customers can browse garages and workshops through:

* Garage name
* Location
* Rating
* Reviews
* Available services
* Service prices

### 🔧 Service Listings

Each garage can list services such as:

* Car Washing
* Interior Cleaning
* Exterior Detailing
* Full Car Detailing
* Ceramic Coating
* Periodic Maintenance
* AC Service
* Oil Change
* General Inspection

Each service contains:

* Service name
* Description
* Price
* Estimated duration

### ⚖️ Garage & Service Comparison

Customers can compare different garages based on:

* Service price
* Rating
* Reviews
* Service availability
* Estimated service duration

### 📅 Service Booking

Customers can:

1. Select a garage.
2. Select a service.
3. Select a date.
4. Select an available time slot.
5. Review booking details.
6. Confirm the booking.

### 📋 Booking Management

Customers can view:

* Upcoming bookings
* Completed bookings
* Cancelled bookings
* Booking details

Garages can:

* View incoming bookings.
* Accept/update booking status.
* Mark services as completed.

### 🚘 Vehicle & Service History

Customers can maintain:

* Vehicle information.
* Previous services.
* Service dates.
* Garage used.
* Amount spent.
* Service performed.

### ⭐ Reviews & Ratings

After completing a service, customers can:

* Give a rating.
* Write a review.

Garage profiles display their customer ratings and reviews.

### 📊 Customer Dashboard

The customer dashboard displays:

* Upcoming appointment
* Previous services
* Total services completed
* Total money spent
* Registered vehicles
* Recent bookings

### 🏢 Garage Dashboard

Garages can view:

* Garage profile
* Listed services
* Incoming bookings
* Completed services
* Available slots
* Customer reviews

---

# 🧑‍💻 Technology Stack

| Technology              | Purpose                                |
| ----------------------- | -------------------------------------- |
| Kotlin                  | Application programming language       |
| Jetpack Compose         | Android UI development                 |
| Material 3              | Application design system              |
| Android Studio          | Development environment                |
| Firebase Authentication | User authentication                    |
| Cloud Firestore         | Application database                   |
| Firebase Storage        | Storage for garage/service images      |
| Git & GitHub            | Version control and team collaboration |

---

# 🗄️ Database

The application uses **Cloud Firestore** as its main database.

The database stores information such as:

### Users

* User ID
* Name
* Email
* Phone
* Role
* Vehicle information

### Garages

* Garage ID
* Garage name
* Address
* Description
* Contact information
* Rating
* Owner ID

### Services

* Service ID
* Garage ID
* Service name
* Description
* Price
* Duration

### Bookings

* Booking ID
* Customer ID
* Garage ID
* Service ID
* Date
* Time slot
* Price
* Booking status

### Reviews

* Review ID
* Customer ID
* Garage ID
* Booking ID
* Rating
* Review text

### Service History

* Customer ID
* Vehicle ID
* Garage ID
* Service
* Date
* Amount paid

---

# 🧠 New Concepts / Self-Learning

To satisfy the self-learning component of the project, the team will implement and demonstrate new concepts beyond basic Android UI development.

### 1. Firebase Authentication

Used for account creation, login and user identification.

### 2. Cloud Firestore

Used as a cloud-based NoSQL database for storing users, garages, services, bookings and reviews.

### 3. Firebase Storage

Used to store and retrieve garage/service images.

### 4. Role-Based Application Flow

The application provides different functionality depending on whether the logged-in user is a **Customer** or **Garage Owner**.

These concepts are directly related to the requirements of the application rather than being added only for demonstration purposes.

---

# 🏗️ Application Flow

```text
                    ┌─────────────────┐
                    │   AutoCare App  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Login / Register│
                    └────────┬────────┘
                             │
                 ┌───────────┴───────────┐
                 │                       │
          ┌──────▼──────┐        ┌──────▼──────┐
          │   Customer  │        │    Garage   │
          └──────┬──────┘        └──────┬──────┘
                 │                       │
        ┌────────▼────────┐      ┌───────▼────────┐
        │ Browse Garages  │      │ Garage Profile │
        └────────┬────────┘      └───────┬────────┘
                 │                       │
        ┌────────▼────────┐      ┌───────▼────────┐
        │ Compare Services│      │ Manage Services│
        └────────┬────────┘      └───────┬────────┘
                 │                       │
        ┌────────▼────────┐      ┌───────▼────────┐
        │ Select Service  │      │ View Bookings  │
        └────────┬────────┘      └───────┬────────┘
                 │                       │
        ┌────────▼────────┐      ┌───────▼────────┐
        │ Select Date/Time│      │ Update Status  │
        └────────┬────────┘      └────────────────┘
                 │
        ┌────────▼────────┐
        │ Confirm Booking │
        └────────┬────────┘
                 │
        ┌────────▼────────┐
        │ Service History │
        │ & Reviews       │
        └─────────────────┘
```

---

# 🎨 UI / Design

The application follows **Material 3 design principles** throughout the application.

The interface will focus on:

* Simple navigation.
* Consistent typography.
* Reusable Compose components.
* Cards for garages and services.
* Clear booking flow.
* Proper form validation.
* Consistent buttons and input fields.
* Responsive layouts where required.

---

# 📱 Main Screens

### Customer Side

1. Splash Screen
2. Login
3. Registration
4. Customer Home
5. Garage Listing
6. Garage Details
7. Service Details
8. Compare Garages
9. Booking Screen
10. Booking Confirmation
11. My Bookings
12. Vehicle / Service History
13. Reviews
14. Customer Profile / Dashboard

### Garage Side

1. Garage Login / Registration
2. Garage Dashboard
3. Garage Profile
4. Manage Services
5. Add / Edit Service
6. Manage Booking Slots
7. Customer Bookings
8. Booking Details
9. Reviews

---

# 💰 Business Model

AutoCare follows a marketplace-based business model.

### Customer Service Fee

A small platform fee can be added to each completed customer booking.

```text
Service Price + Platform Fee = Total Booking Amount
```

### Garage Listing Fee

Garages can pay a listing/subscription fee to maintain their presence on the platform.

For the academic prototype, these charges can be demonstrated through the application's booking and garage listing logic without implementing a real payment gateway.

---

# 🔄 GitHub Development Workflow

The project is developed collaboratively using Git and GitHub.

Each team member will:

* Work on an assigned module.
* Create commits showing individual contribution.
* Pull the latest changes before starting work.
* Build upon the previous member's implementation.
* Test their module before handing it over.
* Push changes to GitHub.
* Maintain meaningful commit messages.

Suggested workflow:

```text
Member 1
   ↓
Project Foundation + Authentication
   ↓
Member 2
   ↓
Garage & Service Marketplace
   ↓
Member 3
   ↓
Booking + Service History
   ↓
Member 4
   ↓
Reviews + Dashboards + Garage Management
   ↓
Final Integration & Testing
```

---

# 👨‍👩‍👧‍👦 Team Contribution

| Member   | Major Responsibility                                |
| -------- | --------------------------------------------------- |
| Member 1 | Project Foundation, Firebase Setup & Authentication |
| Member 2 | Garage Discovery, Services & Comparison             |
| Member 3 | Booking System, Time Slots & Service History        |
| Member 4 | Reviews, Customer Dashboard & Garage Dashboard      |

Every member contributes to the same integrated application rather than developing independent modules.

---

# 📂 Suggested Project Structure

```text
app/
 └── src/
     └── main/
         └── java/
             └── com.autocare/
                 ├── data/
                 │   ├── model/
                 │   ├── repository/
                 │   └── firebase/
                 │
                 ├── ui/
                 │   ├── auth/
                 │   ├── customer/
                 │   ├── garage/
                 │   ├── booking/
                 │   ├── reviews/
                 │   └── components/
                 │
                 ├── navigation/
                 │
                 └── MainActivity.kt
```

---

# 🚀 Future Scope

The application can be expanded with:

* Online payment gateway.
* Google Maps integration.
* Live garage location.
* Push notifications.
* Real-time booking updates.
* Garage verification.
* Subscription plans for workshops.
* AI-based service recommendations.
* Pickup and drop service.
* Loyalty and rewards system.

---

# 🎓 Academic Purpose

This project demonstrates the development of a complete database-driven Android application using modern Android development technologies.

The project focuses on:

* Kotlin programming.
* Jetpack Compose.
* Material 3.
* Firebase Authentication.
* Cloud Firestore.
* Firebase Storage.
* CRUD operations.
* Navigation.
* Database integration.
* Role-based functionality.
* GitHub collaboration.
* Team-based software development.

---

## 👨‍💻 Team

**Project:** AutoCare — Car Detailing & Service Booking

**Members:**

* Akshay Sathaye 
* Abhinav Mandook 
* Tejas Mannur
* Shaurya Punamiya

**Technology:** Kotlin + Jetpack Compose + Firebase

---

## 📄 License

This project is developed as an academic project for educational purposes.
