# AirNova - DB API

Flight Booking Platform - Database Microservice

## Features
- User management (Customer, Airline, Admin)
- Airport management  
- Aircraft management
- Seat management with pricing
- Flight management (Direct + Connecting)
- Booking system with PNR generation
- Payment processing
- Review system

## Tech Stack
- Java 21
- Spring Boot 4.0.3
- MySQL
- JPA/Hibernate
- Maven
- Lombok

## API Endpoints

### Users
- `POST /api/db/users` - Create user
- `GET /api/db/users` - Get all users
- `GET /api/db/users/{id}` - Get user by ID
- `GET /api/db/users/email/{email}` - Get user by email
- `GET /api/db/users/role/{role}` - Get users by role

### Airports
- `POST /api/db/airports` - Create airport
- `GET /api/db/airports` - Get all airports
- `GET /api/db/airports/{id}` - Get airport by ID
- `GET /api/db/airports/code/{code}` - Get airport by code

### Aircraft
- `POST /api/db/aircrafts` - Create aircraft
- `GET /api/db/aircrafts` - Get all aircrafts
- `GET /api/db/aircrafts/airline/{airlineId}` - Get by airline

### Seats
- `POST /api/db/seats/bulk` - Create multiple seats
- `GET /api/db/seats/aircraft/{aircraftId}` - Get seats by aircraft

### Flights
- `POST /api/db/flights` - Create flight
- `POST /api/db/flights/search` - Search flights
- `GET /api/db/flights/{id}/seatmap` - Get seat map with pricing

### Bookings
- `POST /api/db/bookings` - Create booking
- `GET /api/db/bookings/pnr/{pnr}` - Get booking by PNR
- `GET /api/db/bookings/user/{userId}` - Get user bookings
- `PUT /api/db/bookings/{id}/cancel` - Cancel booking

## Setup
1. Create MySQL database (auto-created)
2. Update `application.properties` with your MySQL credentials
3. Run `mvn spring-boot:run`

## Port
- Application runs on `http://localhost:8082`
