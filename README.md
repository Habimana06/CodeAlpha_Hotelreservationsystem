# Hotel Reservation System - Herbanet Grand Oasis

A comprehensive Java-based Hotel Reservation System built with Hibernate ORM, MySQL database, and Swing GUI. This system supports multiple user roles with role-based access control and privilege management.

## Features

### User Roles
1. **Customer** - Search and book rooms, view/cancel reservations, make payments
2. **Receptionist** - Manage reservations, check-in/check-out guests, view all bookings
3. **Manager** - View analytics, manage rooms, generate reports
4. **Admin** - Full system access including user management, role assignment, and privilege management

### Core Functionality
- **Room Management**: Categorization (Standard, Deluxe, Suite) with different rates
- **Reservation System**: Search available rooms by date, book reservations, cancel bookings
- **Payment Processing**: Simulated payment system with transaction references
- **User Management**: Create, update, and manage users with role-based privileges
- **Analytics Dashboard**: Revenue tracking, occupancy rates, reservation statistics
- **Modern UI**: Beautiful, responsive interface using FlatLaf theme

## Technology Stack

- **Java 17**
- **Hibernate 6.4.4** (ORM)
- **MySQL 8.0** (Database)
- **Swing** (GUI Framework)
- **FlatLaf** (Modern Look and Feel)
- **Maven** (Build Tool)

## Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6+

## Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE hotel_reservation_system;
```

2. Update database credentials in `src/main/resources/application.properties` or set environment variables:
   - `APP_DB_URL` (default: `jdbc:mysql://localhost:3306/hotel_reservation_system`)
   - `APP_DB_USER` (default: `root`)
   - `APP_DB_PASSWORD` (default: `changeme`)

## Installation & Running

1. Clone the repository:
```bash
git clone <repository-url>
cd HotelReservationSystem
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.hotel.HotelReservationApplication"
```

Or run the JAR file:
```bash
java -jar target/hotel-reservation-system-1.0.0.jar
```

## Default Login Credentials

The system automatically initializes with sample users:

| Username | Password | Role |
|----------|----------|------|
| customer | password | Customer |
| receptionist | password | Receptionist |
| manager | password | Manager |
| admin | password | Admin |

## Project Structure

```
src/
├── main/
│   ├── java/com/hotel/
│   │   ├── config/          # Database and Hibernate configuration
│   │   ├── model/           # JPA entities (User, Room, Reservation, etc.)
│   │   ├── repository/      # Hibernate repositories
│   │   ├── service/         # Business logic services
│   │   ├── ui/              # GUI components and dashboards
│   │   └── HotelReservationApplication.java
│   └── resources/
│       ├── sql/             # Database schema and sample data
│       └── assets/          # Images and resources
└── test/
```

## Key Components

### Models (JPA Entities)
- `User` - System users with roles and privileges
- `Role` - User roles (CUSTOMER, RECEPTIONIST, MANAGER, ADMIN)
- `PrivilegeEntity` - System privileges
- `Room` - Hotel rooms with categories
- `RoomCategory` - Room types (Standard, Deluxe, Suite)
- `Reservation` - Booking records
- `Payment` - Payment transactions

### Services
- `AuthService` - User authentication
- `HibernateReservationService` - Reservation management
- `HibernateRoomService` - Room search and management
- `HibernateAdminService` - User and privilege management
- `PaymentService` - Payment processing

### Dashboards
- `CustomerDashboard` - Room search, booking, reservation management
- `ReceptionistDashboard` - Check-in/out, reservation management
- `ManagerDashboard` - Analytics, reports, room management
- `AdminDashboard` - User management, role/privilege assignment

## Features by Role

### Customer
- Search available rooms by date range
- Book rooms with guest count selection
- View personal reservations
- Cancel reservations
- Automatic payment processing

### Receptionist
- View all reservations
- Check-in guests
- Check-out guests
- Search reservations
- View reservation details

### Manager
- View revenue analytics
- Monitor occupancy rates
- View reservation statistics
- Manage room inventory
- Generate reports

### Admin
- Create new users
- Update user information
- Assign roles to users
- Manage user privileges
- View all roles and privileges
- Activate/deactivate users

## Database Schema

The system uses the following main tables:
- `users` - User accounts
- `roles` - User roles
- `privileges` - System privileges
- `user_privileges` - User-specific privilege assignments
- `role_privileges` - Role-based privilege assignments
- `rooms` - Hotel rooms
- `room_categories` - Room types
- `reservations` - Booking records
- `payments` - Payment transactions

## Development

### Adding New Features
1. Create/update JPA entities in `model/` package
2. Create repositories in `repository/` package
3. Implement business logic in `service/` package
4. Create UI components in `ui/` package

### Database Changes
- Update entities with JPA annotations
- Hibernate will auto-update schema (hbm2ddl.auto=update)
- For production, use migration scripts

## License

This project is developed for educational purposes.

## Author

Developed as a Final Year Project - Hotel Reservation System

"# CodeAlpha_Hotelreservationsystem" 
