# PhillDesk Database Setup

## Overview
This document outlines the database schema and setup for the PhillDesk Online Pharmacy Management System.

## Database Schema

### Core Entities

1. **roles** - User role definitions (Admin, Pharmacist, Customer)
2. **users** - System users with role-based access
3. **medicines** - Medicine inventory with stock tracking
4. **prescriptions** - Customer prescription uploads
5. **prescription_items** - Individual medicines in prescriptions
6. **bills** - Billing information for prescriptions
7. **bill_items** - Individual items in bills
8. **notifications** - System notifications for users

### Entity Relationships

```
roles (1) -----> (many) users
users (1) -----> (many) prescriptions (as customer)
users (1) -----> (many) prescriptions (as pharmacist)
users (1) -----> (many) notifications
prescriptions (1) -----> (many) prescription_items
prescriptions (1) -----> (1) bills
medicines (1) -----> (many) prescription_items
medicines (1) -----> (many) bill_items
bills (1) -----> (many) bill_items
```

## Setup Instructions

### Prerequisites
- PostgreSQL 12+ installed and running
- Database user with create database privileges

### Database Creation

1. Create the database:
```sql
CREATE DATABASE philldesk_db;
```

2. Update application.properties with your database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/philldesk_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Automatic Setup
The application will automatically:
- Create tables using JPA entity definitions
- Insert initial data from `data.sql`
- Create necessary indexes

### Manual Setup (Optional)
If you prefer manual setup, run the SQL scripts in this order:
1. `src/main/resources/schema.sql` - Creates tables and indexes
2. `src/main/resources/data.sql` - Inserts initial data

## Key Features

### Security
- User roles with appropriate access control
- Password encryption (implement BCrypt)
- JWT token-based authentication

### Business Logic
- Automatic stock level monitoring
- Expiry date tracking
- Prescription approval workflow
- Bill generation with items

### Audit Trail
- Created/updated timestamps on all entities
- User tracking for modifications
- Notification system for important events

## Sample Data
The system includes sample data for testing:
- Default roles (Admin, Pharmacist, Customer)
- Sample medicines with various categories
- Proper relationship examples

## Performance Considerations
- Indexes on frequently queried columns
- Proper foreign key relationships
- Optimized queries for reporting

## Future Enhancements
- Audit log table for complete change tracking
- Multi-pharmacy support
- Advanced reporting tables
- Integration tables for external APIs
