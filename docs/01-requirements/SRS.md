# Software Requirements Specification (SRS)
## Leave Management System

**Version:** 1.0 (Auth Module only)  
**Date:** 2026-06-15  
**Status:** Draft – Agile iteration

---

## 1. Project Overview

[Write 3-4 sentences in your own words. Example below – you can modify.]

The Leave Management System is a web‑based application that allows employees to request leave, track their attendance, and view leave balances. Managers can approve leave requests (level 1), and HR provides final approval (level 2). The system exists to automate leave tracking, reduce manual paperwork, and provide transparency for teams and HR.

---

## 2. Actors & Permissions

| Actor                        | Can do |
|-------|--------------|--------------------------
| **Employee**         | Apply for leave, view leave balance, clock in/out, view own attendance history |
| **Manager**          | Approve/reject leave (first level), view team calendar, view team attendance |
| **HR**               | Final approval (second level), manage leave types, view organisation‑wide reports |
| **Admin**            | Manage users (create, deactivate), manage departments, manage holidays, manage               blackout dates, system configuration |

---

## 3. Functional Requirements (Auth & User Management – Sprint 1)

*Only the module for the current sprint. Other modules (Leave, Attendance, Reports) will be added at the start of their respective sprints.*

| ID             |                     | Requirement |
|----|-----------|----------------------------------------|
| FR‑01          | The system shall allow an Admin to create an employee account with name, email, role                 |                   Employee/Manager/HR/Admin), department, and joining date. |
| FR‑02          | The system shall send a welcome email with a temporary password to the new employee’s |  ||                 email address. |
| FR‑03          | The system shall allow any user to log in using email and password, returning a JWT token |                  valid for 8 hours on success. |
| FR‑04          | The system shall reject login with an error message “Invalid credentials” if email or |                    |                   password is incorrect (without revealing which field is wrong). |
| FR‑05          | The system shall assign a role (Employee, Manager, HR, Admin) to each user; role |    ||||   |                   determines endpoint access via RBAC. |
| FR‑06           | The system shall map an employee to exactly one department and optionally to a manager (                  |                 self‑reference). |
| FR‑07           | The system shall allow a user to view their own profile (name, email, role, department, ||                  manager). |
| FR‑08           | The system shall allow a user to update their profile (except email and role) after ||||||             providing current password. |
| FR‑09           | The system shall enforce a password policy: minimum 8 characters, at least one digit, one  |                 uppercase, one special character. |
| FR‑10           | The system shall lock an account after 5 failed login attempts; an Admin must unlock it. |

*Note: FR‑01 to FR‑10 cover Sprint 1. Later sprints will add leave, attendance, reporting requirements.*

---

## 4. Non‑Functional Requirements (Global)

| ID | Requirement |
|----|-------------|
| NFR‑01 | All API responses shall be in JSON format with a consistent structure: `{ "status": "success/error", "data": {}, "message": "" }`. |
| NFR‑02 | Passwords shall be hashed using BCrypt with strength factor 10 before storage. |
| NFR‑03 | JWT token expiry shall be 8 hours; refresh tokens (if implemented) shall expire in 7 days. |
| NFR‑04 | All list endpoints (e.g., users, leaves) shall support pagination with default page size 20. |
| NFR‑05 | The system shall log all leave status changes to an `audit_logs` table with actor, timestamp, old value, new value. |
| NFR‑06 | The system shall return HTTP 400 for validation errors with a list of fields and reasons; HTTP 401 for unauthenticated; HTTP 403 for forbidden. |
| NFR‑07 | Database queries on frequently filtered columns (`user_id`, `status`, `date`) shall be indexed. |

---

## 5. Edge Cases (for Auth module – quick notes)

- **FR‑01 duplicate email:** Admin should see error “Email already registered”.
- **FR‑03 wrong password:** Error message must not distinguish between “user not found” and “wrong password” (security best practice).
- **FR‑05 role change:** If a manager’s role is changed to Employee, they should no longer see team approvals.
- **FR‑09 weak password:** Return specific rule violations (“must contain a digit”).
- **FR‑10 lockout:** The counter resets after successful login; lockout message: “Account locked – contact Admin”.
