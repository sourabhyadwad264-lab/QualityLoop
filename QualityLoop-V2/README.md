# QualityLoop v2

Quality and corrective action management for a supplier. A defect (customer complaint, internal inspection or PPAP sample rejection) is recorded against a part number, contained, investigated, fixed with corrective actions and closed. Reports can be sent to the customer by email or WhatsApp.

Stack: HTML, CSS, JavaScript, Bootstrap, React.js | Java, Spring Boot | MySQL | Postman.
New in v2: Spring Security, JWT (jjwt), JavaMail (spring-boot-starter-mail).

## Run it

Backend (Eclipse run configuration, Environment tab, or terminal):

| Variable | Meaning | Needed? |
|---|---|---|
| DB_USERNAME / DB_PASSWORD | MySQL login | yes |
| ADMIN_PASSWORD | password of the first Admin (username `admin`). Default Admin@123 | change it |
| JWT_SECRET | long random text, 32+ characters, signs login tokens | change it |
| MAIL_USERNAME / MAIL_PASSWORD | sender email and its App Password (Gmail: turn on 2-step verification, create an App Password) | only for email |
| FRONTEND_URL | address used inside shared links. Default http://localhost:5173 | when hosted online |

Frontend: `cd frontend`, `npm install`, `npm run dev`, open http://localhost:5173.

First login: username `admin`, password from ADMIN_PASSWORD (default Admin@123). Other people click "Create an account" and start as Quality Engineer. The Admin changes roles under the Users tab.

## Roles

| Action | Engineer | Quality Head | Admin |
|---|---|---|---|
| Report, edit, add and complete actions | yes | yes | yes |
| Close a defect | no | yes | yes |
| Delete an OPEN defect, remove an action | no | yes | yes |
| Send reports, view dashboard | yes | yes | yes |
| Manage users and roles | no | no | yes |

## Rules

- Closing needs containment action, root cause, at least one corrective action, all actions completed (409 otherwise).
- Closed defects are locked. Only OPEN defects can be deleted.
- Customers see the report through a secret link valid for 7 days. Internal remarks and audit history are never included.
- Every change is recorded in the audit history with the user's name.

## Postman

1. POST http://localhost:8081/api/auth/login with `{"username":"admin","password":"Admin@123"}`. Copy `token` from the response.
2. In every other request: Authorization tab, Type "Bearer Token", paste the token.

| Method | URL | Notes |
|---|---|---|
| POST | /api/auth/register | `{"username","password","fullName","email"}` |
| GET | /api/auth/me | who am I |
| GET | /api/defects?status=OPEN&q=bracket | list, search |
| POST | /api/defects | see body below |
| PUT | /api/defects/{id} | same body |
| DELETE | /api/defects/{id} | OPEN only, Head/Admin |
| PATCH | /api/defects/{id}/status | `{"status":"CLOSED"}` Head/Admin |
| POST | /api/defects/{id}/actions | `{"description","owner","dueDate"}` |
| PATCH | /api/defects/{id}/actions/{aid}/complete | |
| DELETE | /api/defects/{id}/actions/{aid} | Head/Admin |
| GET | /api/defects/{id}/audit | |
| GET | /api/reports/summary | dashboard numbers |
| POST | /api/defects/{id}/share | creates 7 day link |
| POST | /api/defects/{id}/email | `{"to":"x@y.com","message":"Hello"}` |
| GET | /api/public/reports/{token} | no login needed |
| GET / PATCH | /api/users, /api/users/{id} | Admin: `{"role","active","managerId"}` |

Defect body:
```json
{"title":"Burr on bracket","description":"Sharp edge found","partNumber":"BRK-1042","partName":"Mounting bracket",
 "source":"CUSTOMER_COMPLAINT","quantityAffected":3,"detectedOn":"2026-11-02","detectedBy":"Customer QA",
 "severity":"HIGH","containmentAction":"","rootCause":"","remarks":"Internal note"}
```
Status codes: 400 invalid input, 401 not logged in or wrong password, 403 role not allowed, 404 not found, 409 business rule broken, 502 email failed.

Sharing: email carries the full report text. The WhatsApp button opens WhatsApp with a ready message (summary plus link); you press Send. WhatsApp cannot attach files this way. The link only opens for others when FRONTEND_URL points to a hosted address.
