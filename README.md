# Silverline ERP - Backend Service

This is the Spring Boot backend service for Silverline ERP (Retail Operation Control System). It provides high-performance REST APIs, real-time sync via WebSockets, and background scheduling.

---

## 🏛️ Architecture & Tech Stack
- **Spring Boot 4.x** with **Java 25 (Project Loom)**
- **Lightweight Concurrency**: Requests are processed on virtual threads (`spring.threads.virtual.enabled: true`)
- **PostgreSQL Database**
- **Caffeine second-level caching** for high-volume reads (Products, Categories, Brands, Units, Branches, Permissions)
- **Response compression**: GZIP payload compression for JSON responses >= 1KB
- **JasperReports** integration for invoice generation

---

## 🚀 Key Performance Optimizations

1. **Tomcat Virtual Threads**: Tomcat handles incoming client request mappings on Project Loom Virtual Threads instead of heavy OS threads, maximizing request handling capacity under high concurrency.
2. **Metadata Caching**: All read-heavy, low-frequency write collections (like product details, category lists, unit definitions, branch metadata) are cached with Caffeine.
3. **N+1 Prevention**:
   - `@EntityGraph` annotation rules enforce JOIN query fetches for lazy entity mappings.
   - Database-level pagination via JPA query parameters.
   - Bulk aggregates and mappings to avoid nested database loops.
4. **Database Indexes**: Added composite indexes on primary transaction fields:
   - `sales(branch_id, sale_date)`
   - `sale_items(sale_id, product_id)`
   - `cash_shifts(branch_id, status)`
   - `payments(sale_id, payment_type)`
   - `batches(product_id, expiry_date)`
   - `item_dispatches(branch_id, status)`
   - `user_activity_log(user_id, created_at)`
5. **Async Processing**: Eaudit logging, email dispatch, and WebSocket broadcasts run asynchronously (`@Async`) without blocking request execution.

---

## 🛠️ Setup and Execution

### Prerequisites
- **Java 25 (Loom early access build)**
- **PostgreSQL** instance running locally

### Local Run
1. Copy `.env.example` to `.env` and configure your database parameters:
   ```bash
   cp .env.example .env
   ```
2. Build and run using the Loom JDK:
   ```powershell
   # PowerShell
   $env:JAVA_HOME="C:\Users\justc\.jdks\loom-ea-25-loom+1-11"
   .\mvnw.cmd spring-boot:run
   ```

### Tests
Run the test suite:
```powershell
.\mvnw.cmd test
```
