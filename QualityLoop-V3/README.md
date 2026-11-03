# QualityLoop v3

Quality and corrective action management for a supplier. A defect (customer complaint, internal inspection or PPAP sample rejection) is recorded against a part number, contained, investigated, fixed with corrective actions and closed. Reports go to the customer by email, WhatsApp or printable page.

Stack: HTML, CSS, JavaScript, Bootstrap, React.js | Java, Spring Boot, Spring Security, JWT, JavaMail | MySQL | Postman.

Read these next: `docs/EXPLAINED.md` (every feature explained, with mentor questions), `docs/SECURITY.md`, `database/` (SQL files).

## Run it

Backend (Eclipse: Run Configurations, Environment tab):

| Variable | Meaning | Needed? |
|---|---|---|
| DB_USERNAME / DB_PASSWORD | MySQL login | yes |
| ADMIN_PASSWORD | password of the first Admin (username `admin`), default Admin@123 | change it |
| JWT_SECRET | long random text, 32+ characters | change it |
| CORS_ORIGIN | frontend address allowed to call the API, default http://localhost:5173 | when hosted |
| FRONTEND_URL | address used inside shared links, default http://localhost:5173 | when hosted |
| MAIL_USERNAME / MAIL_PASSWORD | sender email and its App Password | only for email |

Frontend: `cd frontend`, `npm install`, `npm run dev`, open http://localhost:5173.

First login: `admin` with the ADMIN_PASSWORD. Other people use "Create an account" and start as Quality Engineer. Passwords need 8+ characters with upper case, lower case and a number.

## Features

- Dashboard: status donut, defects per month, severity, worst parts, average days to close, overdue actions.
- Defects: part number, source, quantity, detected on and by, containment, root cause, corrective actions, remarks, search, delete (OPEN only).
- Day and night theme switch (top right, remembered).
- Login, registration, change password, roles, user management, account lock after 5 wrong passwords, security log.
- Report sharing: printable page (Save as PDF), email, WhatsApp message.

## Roles

| Action | Engineer | Quality Head | Admin |
|---|---|---|---|
| Report, edit, add and complete actions | yes | yes | yes |
| Close a defect, delete OPEN defect, remove an action | no | yes | yes |
| Dashboard and sending reports | yes | yes | yes |
| Users, unlock accounts, security log | no | no | yes |

## Postman

1. POST http://localhost:8080/api/auth/login with `{"username":"admin","password":"Admin@123"}`. Copy `token`.
2. Other requests: Authorization tab, Bearer Token, paste the token.

| Method | URL | Notes |
|---|---|---|
| POST | /api/auth/register | `{"username","password","fullName","email"}` |
| GET | /api/auth/me | who am I |
| POST | /api/auth/change-password | `{"currentPassword","newPassword"}` |
| GET | /api/defects?status=OPEN&q=bracket | list, search |
| POST / PUT | /api/defects, /api/defects/{id} | defect body below |
| DELETE | /api/defects/{id} | OPEN only, Head/Admin |
| PATCH | /api/defects/{id}/status | `{"status":"CLOSED"}` Head/Admin |
| POST | /api/defects/{id}/actions | `{"description","owner","dueDate"}` |
| PATCH | /api/defects/{id}/actions/{aid}/complete | |
| DELETE | /api/defects/{id}/actions/{aid} | Head/Admin |
| GET | /api/defects/{id}/audit | |
| GET | /api/reports/summary | dashboard numbers |
| POST | /api/defects/{id}/share | 7 day link |
| POST | /api/defects/{id}/email | `{"to":"x@y.com","message":"Hello"}` |
| GET | /api/public/reports/{token} | no login needed |
| GET | /api/users | Admin |
| PATCH | /api/users/{id} | Admin: `{"role","active","managerId"}` |
| PATCH | /api/users/{id}/unlock | Admin |
| GET | /api/users/login-events | Admin |

Defect body:
```json
{"title":"Burr on bracket","description":"Sharp edge found","partNumber":"BRK-1042","partName":"Mounting bracket",
 "source":"CUSTOMER_COMPLAINT","quantityAffected":3,"detectedOn":"2026-11-02","detectedBy":"Customer QA",
 "severity":"HIGH","containmentAction":"","rootCause":"","remarks":"Internal note"}
```

Test the lockout: POST /api/auth/login with a wrong password 5 times, then once with the right one. You get 401 with the lock message. Open the Security log as Admin to see all attempts.

Sharing: email carries the full report. The WhatsApp button opens WhatsApp with a ready message (summary plus link); you press Send. The link only opens for others when FRONTEND_URL points to a hosted address.
