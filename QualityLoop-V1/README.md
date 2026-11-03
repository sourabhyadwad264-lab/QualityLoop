# QualityLoop

Quality and corrective action management. A defect is reported, contained, investigated, fixed with corrective actions, and only then closed. Every step is recorded in an audit history.

Stack: HTML, CSS, JavaScript, Bootstrap, React.js | Java, Spring Boot | MySQL | Postman for testing.

## Run it

1. MySQL: make sure it is running. The database `qualityloop` is created automatically.
2. Backend (needs Java 21 and Maven):
   ```
   cd backend
   set DB_USERNAME=root
   set DB_PASSWORD=your_mysql_password
   mvn spring-boot:run
   ```
   (On Mac/Linux use `export` instead of `set`.) It starts on http://localhost:8080.
3. Frontend (needs Node.js):
   ```
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173.

## Business rules (enforced in DefectService)

- A defect can only be CLOSED if it has a containment action, a root cause, at least one corrective action, and every corrective action is COMPLETED. Otherwise the API returns 409 with the reason.
- A closed defect cannot be edited, get new actions, or change status (409).
- Every create, edit, status change, action added and action completed writes an AuditEntry.

## API (test these in Postman)

| Method | URL | Body |
|---|---|---|
| POST | /api/defects | `{"title":"Cracked housing","description":"Batch 42","severity":"HIGH","reportedBy":"Asha"}` |
| GET | /api/defects?status=OPEN | none (status is optional) |
| GET | /api/defects/{id} | none |
| PUT | /api/defects/{id} | same shape as POST, plus `containmentAction`, `rootCause` |
| PATCH | /api/defects/{id}/status | `{"status":"IN_PROGRESS"}` or `{"status":"CLOSED"}` |
| POST | /api/defects/{id}/actions | `{"description":"Replace mould","owner":"Ravi","dueDate":"2026-12-01"}` |
| PATCH | /api/defects/{id}/actions/{actionId}/complete | none |
| GET | /api/defects/{id}/audit | none |

Status codes: 200 OK, 201 created, 400 validation error, 404 not found, 409 business rule broken.

### Postman test order (shows every rule)

1. POST a defect (expect 201).
2. PATCH status to CLOSED right away (expect 409, containment missing).
3. PUT the defect with `containmentAction` and `rootCause` filled.
4. PATCH to CLOSED again (expect 409, no corrective action).
5. POST an action, then PATCH to CLOSED (expect 409, action pending).
6. PATCH the action complete, then PATCH to CLOSED (expect 200).
7. GET /audit to see the full history, then try a PUT (expect 409, closed).
8. POST with an empty title (expect 400).

## Structure

```
backend/src/main/java/com/qualityloop
  controller/  HTTP only: receives requests, returns responses
  service/     business rules and transactions
  repository/  database access (Spring Data JPA)
  entity/      Defect, CorrectiveAction, AuditEntry and enums
  dto/         request objects with validation
  exception/   NotFound, BusinessRule, global handler
  config/      CORS for the React app
frontend/src
  api.js       every backend call (built-in fetch)
  App.jsx      page layout and state
  components/  DefectList, DefectForm, DefectDetail
```
