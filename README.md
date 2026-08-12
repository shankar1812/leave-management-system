# Leave Management System

![CI](https://github.com/shankar1812/leave-management-system/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)
![Railway](https://img.shields.io/badge/Deployed-Railway-purple?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

A production-grade, multi-role enterprise HR platform built with Java 17 and Spring Boot 3. Covers the complete employee leave lifecycle — application, multi-level approval, balance management, attendance tracking, WFH, and Comp-off — with JWT security, AOP audit logging, caching, rate limiting, and cloud deployment.

---

## 🌐 Live Demo

| Resource | URL |
|---|---|
| **Base URL** | https://leave-management-system-production-5147.up.railway.app |
| **Swagger UI** | https://leave-management-system-production-5147.up.railway.app/swagger-ui/index.html |
| **Health Check** | https://leave-management-system-production-5147.up.railway.app/actuator/health |

**Demo credentials:**
```
Admin → email: admin@example.com | password: admin123
```

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Design Patterns](#-design-patterns)
- [API Overview](#-api-overview)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Running Tests](#-running-tests)
- [CI/CD](#-cicd)
- [Project Structure](#-project-structure)

---

## ✨ Features

### Authentication & Authorization
- JWT-based stateless authentication
- 4 user roles — Employee, Manager, HR, Admin
- Role-based access control on every endpoint via `@PreAuthorize`
- Account seeding on startup via `DataSeeder`

### Leave Management
- 7 leave types — Casual, Sick, Earned, Maternity, Paternity, Comp-off, Unpaid
- Leave application with date range, reason, and half-day support
- Leave balance engine — auto-deduct on apply, restore on rejection/cancellation
- Yearly carry-forward credit for eligible leave types
- Overlap validation — prevents duplicate leave on same dates
- Leave state machine — enforces valid transitions (PENDING → APPROVED / REJECTED / CANCELLED)
- Multi-level approval workflow — Manager (level 1) → HR (level 2)
- Blackout dates — Admin blocks specific date ranges

### Attendance
- Clock-in / clock-out with timestamp capture
- Department shift-based late detection with 15-minute grace period
- Work hours auto-calculation with `BigDecimal` precision
- `@Scheduled` absent-detection cron job (10:30 AM weekdays) — skips employees on approved leave
- Monthly attendance summary — present, absent, late, half-day, total hours
- PDF attendance report generation using iText7

### Additional Modules
- WFH request — submit, manager approve/reject, cancel
- Comp-off — claim for worked holiday, balance credited on approval
- Holiday master calendar — Admin managed, auto-excluded from leave calculation
- In-app notification system — stored in DB with read/unread tracking
- Async email notifications via `JavaMailSender` + `@TransactionalEventListener`

### Performance & Security
- Spring Cache (Caffeine) — O(1) leave balance reads with `@Cacheable` + `@CacheEvict`
- Bucket4j rate limiting — token bucket, 3 leave applications per hour per user
- AOP audit trail — `@Aspect/@Around` logs all write operations without touching business logic
- Database indexing on all high-query columns — verified with `EXPLAIN SELECT`
- Pagination on all list endpoints via Spring Data `Pageable`

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| Security | Spring Security + JWT (JJWT 0.12.x) |
| ORM | Spring Data JPA + Hibernate 6 |
| Database | MySQL 8 |
| Cache | Spring Cache + Caffeine |
| Rate Limiting | Bucket4j 8.7 |
| Email | Spring Boot Mail + JavaMailSender |
| PDF Reports | iText7 |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Testing | JUnit 5 + Mockito |
| Build | Maven |
| CI/CD | GitHub Actions |
| Deployment | Railway.app |
| Monitoring | Spring Boot Actuator |

---

## 🏗 Architecture

The project follows a strict **4-layer Controller–Service–Repository architecture** with no cross-layer leakage:

```
Client Request
      │
      ▼
┌─────────────────────────────┐
│     Security Filter         │  JWT validation, role extraction
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Controller Layer        │  HTTP mapping, @Valid, @PreAuthorize
└─────────────┬───────────────┘
              │  DTO in
              ▼
┌─────────────────────────────┐
│     Service Layer           │  All business logic, @Transactional
│     (interface + impl)      │  @Cacheable, @Auditable
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     Repository Layer        │  JPA interfaces, custom @Query methods
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│     MySQL Database          │  13 tables, indexed, 3NF normalized
└─────────────────────────────┘

Cross-cutting concerns (via AOP):
├── Audit logging    → @Aspect / @Around on all @Auditable methods
├── Exec time log    → @Around on all service methods
└── Exception log    → @AfterThrowing on all service methods
```

---

## 🎨 Design Patterns

| Pattern | Where Used |
|---|---|
| **Repository** | `UserRepository`, `LeaveApplicationRepository`, all 12 JPA repos |
| **DTO** | `LeaveApplicationRequest/Response`, `UserResponse` — entities never exposed |
| **Builder** | All DTOs and entities via Lombok `@Builder` |
| **State Machine** | `LeaveStateMachine` — enforces valid leave status transitions |
| **Chain of Responsibility** | `ManagerApprovalHandler → HRApprovalHandler` multi-level approval |
| **Observer** | `ApplicationEventPublisher` + `@TransactionalEventListener` for async email |
| **Strategy** | `LeaveBalanceStrategy` — FullDay, HalfDay leave deduction strategies |
| **Factory** | `NotificationFactory` — creates email or in-app notifications |
| **Proxy (AOP)** | `ApplicationAspect` — Spring proxy intercepts all service method calls |
| **Singleton** | Every `@Service`, `@Repository`, `@Component` — Spring IoC container |
| **Decorator** | Spring Security filter chain — each filter decorates the HTTP request |
| **Template Method** | `AbstractApprovalHandler.approve()` — defines algorithm skeleton |

---

## 📡 API Overview

| Module | Method | Endpoint | Role Required |
|---|---|---|---|
| Auth | POST | `/api/v1/auth/login` | Public |
| Users | POST | `/api/v1/users` | Admin |
| Users | GET | `/api/v1/users/{id}` | Admin / HR |
| Users | PUT | `/api/v1/users/{id}` | Admin / HR |
| Departments | POST/GET/PUT/DELETE | `/api/v1/departments` | Admin |
| Leave Types | POST/GET/PUT/DELETE | `/api/v1/leave-types` | Admin / All |
| Leave Balances | GET | `/api/v1/leave-balances/user/{id}` | Self / Admin / HR |
| Leave Applications | POST | `/api/v1/leaves` | Employee |
| Leave Applications | PATCH | `/api/v1/leaves/{id}/cancel` | Employee |
| Leave Applications | GET | `/api/v1/leaves/my` | Employee |
| Leave Applications | GET | `/api/v1/leaves/team` | Manager |
| Leave Approvals | POST | `/api/v1/leaves/{id}/approvals/manager` | Manager |
| Leave Approvals | POST | `/api/v1/leaves/{id}/approvals/hr` | HR |
| Attendance | POST | `/api/v1/attendance/clock-in` | Employee |
| Attendance | POST | `/api/v1/attendance/clock-out` | Employee |
| Attendance | GET | `/api/v1/attendance/summary/{userId}` | Self / Manager |
| PDF Reports | GET | `/api/v1/attendance/report/{userId}` | Self / Manager |
| Holidays | POST/GET/PUT/DELETE | `/api/v1/holidays` | Admin / All |
| WFH Requests | POST/GET | `/api/v1/wfh` | Employee / Manager |
| Comp-off | POST/GET | `/api/v1/comp-off` | Employee / Manager |
| Audit Logs | GET | `/api/v1/audit-logs` | Admin / HR |

Full interactive documentation: [Swagger UI](https://leave-management-system-production-5147.up.railway.app/swagger-ui/index.html)

---

## 🗄 Database Schema

13 tables, all in 3NF:

```
users               — id, name, email, password, role, department_id, manager_id
departments         — id, name, shift_start_time
leave_types         — id, name, max_days_per_year, is_carry_forward_allowed
leave_balances      — id, user_id, leave_type_id, year, total_days, used_days, remaining_days
leave_applications  — id, user_id, leave_type_id, start_date, end_date, status, total_days
leave_approvals     — id, leave_application_id, approver_id, approval_level, status
attendance_records  — id, user_id, date, clock_in, clock_out, work_hours, is_late, status
holidays            — id, name, date, type
blackout_dates      — id, start_date, end_date, reason, created_by
notifications       — id, user_id, title, message, is_read
audit_logs          — id, entity_type, action, performed_by, old_value, new_value, created_at
wfh_requests        — id, user_id, date, reason, status, manager_id
comp_off_requests   — id, user_id, worked_on_date, reason, status, approved_by
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- MySQL 8.0+
- Maven 3.8+
- Git

### Clone and setup

```bash
git clone https://github.com/shankar1812/leave-management-system.git
cd leave-management-system
```

### Create MySQL database

```sql
CREATE DATABASE leave_management_db;
```

### Configure environment variables

Set these in your IDE run configuration (IntelliJ / STS):

```
DB_URL=jdbc:mysql://localhost:3306/leave_management_db
DB_USERNAME=root
DB_PASSWORD=yourpassword
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Run locally

```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Default admin credentials (auto-seeded on startup)

```
email:    admin@example.com
password: admin123
```

---

## 🔑 Environment Variables

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | JDBC connection string | `jdbc:mysql://localhost:3306/lms_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `yourpassword` |
| `JWT_SECRET` | 256-bit hex secret for signing JWTs | `404E635266...` |
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | Sender email address | `you@gmail.com` |
| `MAIL_PASSWORD` | Gmail App Password (not your real password) | `abcdefghijklmnop` |

---

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=LeaveApplicationServiceImplTest

# Run with coverage report
mvn verify
```

Tests use H2 in-memory database via `@DataJpaTest` and Mockito mocks via `@ExtendWith(MockitoExtension.class)`. No real database required to run the test suite.

---

## ⚙️ CI/CD

Every push to `develop` and every PR to `main` triggers the GitHub Actions pipeline:

1. Spins up MySQL 8.0 in a container
2. Sets up Java 17 (Temurin) with Maven cache
3. Runs `mvn test` — full JUnit + Mockito suite
4. Uploads test results as build artifacts
5. Blocks PR merge if any test fails

Railway auto-deploys from `main` branch on every successful merge.

---

## 📁 Project Structure

```
src/main/java/com/app/leaveManagement/
├── audit/          → @Auditable annotation + ApplicationAspect (AOP)
├── config/         → SecurityConfig, CacheConfig, AsyncConfig, SwaggerConfig, DataSeeder
├── controller/     → REST controllers (10 modules)
├── dto/            → Request and response DTOs
├── entity/         → JPA entities (13 tables)
├── enums/          → Role, LeaveStatus, AttendanceStatus, HalfDayType, etc.
├── event/          → LeaveStatusChangedEvent (Observer pattern)
├── exception/      → Custom exceptions + GlobalExceptionHandler
├── repository/     → JPA repositories with custom @Query methods
├── scheduler/      → AttendanceScheduler (@Scheduled cron jobs)
├── security/       → JwtService, JwtAuthFilter, UserDetailsServiceImpl
├── service/        → Service interfaces (contracts)
│   └── impl/       → Service implementations (business logic)
└── util/           → LeaveDayCalculator (weekend + holiday exclusion)

src/main/resources/
├── application.properties          → Local dev config
└── application-prod.properties     → Production config for Railway

src/test/java/
└── service/impl/   → Unit tests for all service implementations
```

---

## 📊 Key Design Decisions

**Why `BigDecimal` for leave days?**
Half-day leaves require `0.5` precision. `int` can't represent this. `double` has floating-point drift. `BigDecimal` with `scale=1` is the correct choice for balance calculations.

**Why balance deduction happens before save in `applyLeave()`?**
If save fails after deduction, `@Transactional` rolls back the deduction too. Either both succeed or neither does — atomicity guaranteed.

**Why `@TransactionalEventListener(phase = AFTER_COMMIT)` for email?**
Email fires only after the database transaction commits successfully. Without this, a transaction rollback would still send the email — an employee would get an approval notification for a leave that doesn't exist.

**Why in-memory Caffeine over Redis for caching?**
Single-instance deployment. Caffeine is zero-config and sufficient. Redis-backed cache would be needed for horizontal scaling — documented as a known limitation.

**Why interface + impl pattern for all services?**
Controller depends on the interface contract, not the concrete class. Mockito mocks the interface in tests. Consistent across all 10 services — matches enterprise coding standards used at TCS, Wipro, Infosys.

---

## 👨‍💻 Author

**Shankar Sahu**
- GitHub: [@shankar1812](https://github.com/shankar1812)
- LinkedIn: [shankar-sahu-839074348](https://www.linkedin.com/in/shankar-sahu-839074348/)
- LeetCode: [SHANKAR_4043](https://leetcode.com/u/SHANKAR_4043/)
- Email: sankarsahu4043@gmail.com

---

## 📄 License

This project is licensed under the MIT License.
