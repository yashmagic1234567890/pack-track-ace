# FreshTrack — Inbound Fruit & Vegetable Receiving System

Full-stack receiving platform: **Java Spring Boot + MySQL** backend and **React (Vite)** frontend with JWT auth, RBAC, CSV/Excel invoice ingestion, scan-to-receive, reconciliation reporting, and an append-only audit trail.

## Tech Stack
- Backend: Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Apache POI, Commons CSV, Maven, Java 21
- Frontend: React 18, Vite, React Router, Axios, html5-qrcode
- Database: MySQL 8 (H2 profile available for quick local runs)

## Roles
- **Central Admin** — global access: manage warehouses, users, user-to-warehouse mapping, upload invoices, view all reports & audit trail.
- **Hub User** — access limited to assigned warehouses: select warehouse/invoice, scan-to-receive, view reconciliation for their hubs.

## Project Structure
```
freshtrack/
├── backend/          # Spring Boot app (controllers, services, repos, entities, security)
├── frontend/         # React + Vite SPA
├── database/         # schema.sql + data.sql
├── sample-data/      # sample-invoices.csv
└── docker-compose.yml
```

## Backend — Run Locally
```bash
cd freshtrack/backend
# Option A: MySQL (configure freshtrack/backend/src/main/resources/application.yml)
mvn spring-boot:run
# Option B: In-memory H2 (no DB setup)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
API runs at `http://localhost:8080`. Seed data is created on first start.

### Default credentials
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Central Admin |
| hubdel | hub123 | Hub User |

## Frontend — Run Locally
```bash
cd freshtrack/frontend
npm install
npm run dev
```
App runs at `http://localhost:5173` and proxies `/api` to the backend.

## Key REST Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/login | Authenticate, returns JWT |
| GET | /api/dashboard/stats | KPI analytics (role scoped) |
| GET | /api/dashboard/my-warehouses | Warehouses visible to user |
| POST | /api/invoices/upload | Upload CSV/Excel invoices (admin) |
| GET | /api/invoices/by-warehouse/{code} | Invoices for a warehouse |
| POST | /api/receiving/scan | +1 receive by SKU |
| POST | /api/receiving/adjust | Manual quantity adjustment |
| GET | /api/reports/reconciliation | Expected vs received variance |
| GET | /api/reports/reconciliation/export/{csv\|excel} | Export report |
| GET | /api/audit | Audit trail (admin) |

## Invoice File Format
CSV/Excel columns: `Invoice_ID, Vendor_Name, Target_Warehouse_ID, Item_SKU, Item_Name, Expected_Quantity`.
See `sample-data/sample-invoices.csv`.

## Docker
```bash
cd freshtrack
docker compose up --build
```
Brings up MySQL, backend (8080), and frontend (5173).

## Notes / Assumptions
- Scan increments are processed with pessimistic row locking for high-frequency input integrity.
- Audit log is append-only (`REQUIRES_NEW` propagation) so it persists even if a transaction rolls back.
- UI uses a dark, high-contrast theme optimized for low-light warehouse environments.
