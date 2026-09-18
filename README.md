# Enterprise Order & Inventory Management Platform

An enterprise-grade, high-performance Order and Inventory Management Platform engineered using **Java 21**, **Spring Boot 3.3.x**, **React**, **Vite**, **Spring Data JPA**, **Spring Security (JWT + RBAC)**, and **MySQL 8**.

---

## 1. Project Overview
The platform simulates a production-grade multi-tier enterprise architecture supporting high-concurrency e-commerce and logistics workflows. It acts as a full-stack SaaS application for managing orders, products, multi-warehouse inventory, and user roles efficiently.

## 2. Features
* **Multi-Role User Management** (`ADMIN`, `CUSTOMER`, `WAREHOUSE_MANAGER`, `SUPPORT_AGENT`)
* **Category & Product Catalog Management** with pagination, sorting, and dynamic search
* **Multi-Warehouse Management** & Active Status Lifecycle
* **Multi-Warehouse Stock & Inventory Tracking** with Optimistic Locking (`@Version`)
* **High-Concurrency Atomic Stock Operations**: Receiving, issuing, adjustment, reservation, and release
* **Customer Order Lifecycle & State Machine** (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`)
* **React Frontend Dashboard** for intuitive management of the entire system

## 3. Technology Stack
**Backend**:
* **Language**: Java 21 (LTS)
* **Framework**: Spring Boot 3.3.4
* **Security**: Spring Security + JWT
* **Data Layer**: Spring Data JPA / Hibernate 6.x
* **Database**: MySQL Server 8.x
* **Testing**: JUnit 5, Mockito, H2 Database

**Frontend**:
* **Library**: React 18
* **Build Tool**: Vite
* **Routing**: React Router DOM
* **HTTP Client**: Axios
* **Styling**: Vanilla CSS (Responsive UI)

## 4. Architecture
The backend follows a standard multi-tier Layered Architecture:
* `controller/`: REST API endpoints
* `service/`: Core business logic and transactions
* `repository/`: Spring Data JPA interfaces
* `entity/`: Database domain models
* `dto/`: Data Transfer Objects for API requests and responses
* `security/`: JWT and RBAC configurations
* `exception/`: Global Exception Handler

The frontend utilizes a modular architecture:
* `src/api/`: Axios client wrappers for backend communication
* `src/components/`: Reusable UI components
* `src/pages/`: Main route views (Dashboard, Products, Orders, etc.)
* `src/context/`: React context for Auth and Toast state

## 5. Database
Powered by **MySQL 8**. 
Entities are connected via structured relationships:
- `User` ↔ `Role`
- `Category` ↔ `Product`
- `Warehouse` ↔ `Inventory` 
- `Order` ↔ `OrderItem`
- `Order` ↔ `OrderStatusHistory`

## 6. Authentication
Stateless authentication implemented via **JWT (JSON Web Tokens)**.
Users register or login (`POST /api/v1/auth/login`) to receive a token. 
The React frontend stores the JWT and attaches it to the `Authorization: Bearer <token>` header via Axios interceptors.

## 7. Authorization & RBAC
Access is restricted via `@PreAuthorize` backend annotations and frontend role-based route guards.
- **CUSTOMER**: Can place orders, view own orders, cancel eligible orders.
- **ADMIN**: Full access to all resources, order state changes, product updates.
- **WAREHOUSE_MANAGER**: Access to inventory, warehouses, and fulfilling orders.

## 8. Product Management
A comprehensive catalog system allowing:
- Dynamic search by SKU and product name
- Filtering by category and active status
- Full CRUD capabilities for Admins

## 9. Inventory Management
Tracks stock per product across multiple warehouses.
- **Stock Receiving**: Adding new inventory.
- **Stock Reservation**: Holding stock when a customer places a PENDING order.
- **Stock Out**: Deducting reserved stock when an order is SHIPPED.

## 10. Order Management
State Machine transitions handle the complete fulfillment process.
Customers build orders which server-side calculates tax and shipping. Upon creation, inventory is synchronously reserved. Orders can be advanced by warehouse managers or cancelled by customers (releasing the reserved stock).

## 11. API Endpoints
### Authentication
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`

### Orders
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders/my-orders` - View own orders
- `PATCH /api/v1/orders/{id}/status` - Update status

### Products & Inventory
- `GET /api/v1/products` - List products
### Analytics & Reports
- `GET /api/v1/analytics/dashboard` - Enterprise metrics summary & order state counts
- `GET /api/v1/analytics/export/low-stock-csv` - Low stock CSV report download
- `GET /api/v1/analytics/export/orders-csv` - Customer orders CSV report download
- `GET /api/v1/analytics/export/inventory-csv` - Full multi-warehouse stock ledger CSV download

## 12. How to run backend
Prerequisites: Java 21, Oracle Database (19c / 21c / 23c / XE), Maven.

1. Ensure Oracle Database is running (e.g. `localhost:1521/FREEPDB1`).
2. Run database schema initialization using `database/oracle_schema.sql`.
3. Set environment variables (see section 14).
4. From the `backend` directory, run:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## 13. How to run frontend
Prerequisites: Node.js (18+).

1. Navigate to the `frontend` directory.
2. Install dependencies:
```bash
npm install
```
3. Set environment variables (see section 14).
4. Run the development server:
```bash
npm run dev
```
Access the application at `http://localhost:5173`.

## 14. Environment Variables
### Backend Production (`application-prod.yml` / system variables)
```properties
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=prod
DB_HOST=oracle-db-host.example.com
DB_PORT=1521
DB_SERVICE_NAME=FREEPDB1
DB_USERNAME=ENTERPRISE_DB
DB_PASSWORD=your_oracle_password
JWT_SECRET=your_super_secret_jwt_signing_key_here
FRONTEND_URL=https://your-deployed-frontend.example.com
```

### Frontend (`frontend/.env`)
Create a `.env` file from `.env.example`:
```properties
VITE_API_BASE_URL=https://your-deployed-backend.example.com/api/v1
```

## 15. Testing
The backend is covered by comprehensive JUnit 5 and Mockito tests utilizing an in-memory H2 database.
To execute tests:
```bash
cd backend
./mvnw clean test
```

## 16. Project Structure
```text
ENTERPRISE ORDER & INVENTORY MANAGEMENT PLATFORM/
├── backend/
│   ├── src/main/java/com/dinesh/enterprise/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── security/
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   └── context/
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## 17. Business Rules
- **Tax**: Fixed at 8%.
- **Free Shipping**: Applied to orders with a subtotal >= $500.00.
- **Stock Protection**: Inventory operations use Optimistic Locking (`@Version`). 
- **Order Cancellation**: Only allowed when order is PENDING or CONFIRMED. Triggers inventory reservation release.
- **Immutability**: `OrderStatusHistory` and `InventoryTransaction` are append-only.

## 18. Example API Flow (Order Creation)
1. Frontend makes a `POST /api/v1/orders` request with product IDs and quantities.
2. Backend validates the JWT and extracts user ID.
3. Service layer retrieves the current `Product` price and validates availability.
4. Service calculates Subtotal, Tax, Shipping, and Total.
5. System synchronously reserves inventory in the `Warehouse`.
6. Order is saved as `PENDING`.
7. Client is returned the newly created Order ID with calculated totals.
