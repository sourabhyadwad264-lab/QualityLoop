# QualityLoop explained (for presenting to mentors)

Every topic: the concept first, then an everyday analogy, then where it lives in the project.

## 1. The big picture: three layers

**Concept.** The browser (React) talks to the Spring Boot REST API, and the API talks to MySQL. Inside Spring Boot the code is split in layers: controller (receives the request), service (business rules), repository (database access).

**Analogy.** A restaurant. The waiter (controller) takes the order, the chef (service) follows the recipe and rules, the storeroom keeper (repository) fetches the ingredients (MySQL).

**Where.** `controller/`, `service/`, `repository/` under `com.qualityloop`. The rule "a defect can only be closed when everything is done" lives in `DefectService`, so it is enforced no matter who calls the API (React, Postman, anything).

## 2. Dependency Injection (Spring's core idea)

**Concept.** Classes do not create the objects they need. They ask for them in the constructor, and Spring supplies them.

**Analogy.** A chef does not grow vegetables. The kitchen manager delivers them.

**Where.** `public DefectService(DefectRepository defects, AuditEntryRepository audits)`: Spring creates the repositories and passes them in.

## 3. JPA, entities and MySQL

**Concept.** JPA (Hibernate) maps a Java class to a table. `@Entity class Defect` becomes table `defect`, each field a column. You write Java, Hibernate writes the SQL.

**Analogy.** A translator between Java and the database language.

**Where.** `entity/`. See `database/02_reference_schema.sql` for the tables that result.

- **`@OneToMany` / `@ManyToOne`:** one defect has many corrective actions. The "many" side (`corrective_action.defect_id`) holds the foreign key.
- **`cascade = ALL` and `orphanRemoval`:** saving or deleting a defect also saves or deletes its actions. Removing an action from the list deletes its row.
- **`EAGER` vs `LAZY`:** EAGER loads the actions together with the defect. LAZY loads them only when asked. With LAZY and the connection already closed, you get `LazyInitializationException`. We hit exactly that error earlier.
- **`ddl-auto=update`:** Hibernate creates or adds tables and columns at startup. Good for learning. In production use `validate` and apply SQL scripts yourself.
- **Index (`@Index`):** like the index of a book. MySQL jumps straight to rows with a given `part_number` instead of reading the whole table. Prove it with `EXPLAIN` in `03_useful_queries.sql`.
- **JPQL vs SQL:** JPQL queries entities (`select d.status, count(d) from Defect d group by d.status`), SQL queries tables. The dashboard uses JPQL `GROUP BY`, so MySQL does the counting, not Java.
- **SQL injection:** Spring Data passes values as parameters, never glued into the SQL text, so input like `' OR 1=1` is harmless.

## 4. Validation and error handling

**Concept.** `@NotBlank`, `@Pattern`, `@Positive` on the request objects reject bad input before any logic runs (HTTP 400). `@RestControllerAdvice` turns Java exceptions into clean JSON answers with the right status code.

**Analogy.** Reception checks your form is filled in before you see the doctor. One person handles all complaints in the same polite format.

**Where.** `dto/`, `exception/GlobalExceptionHandler`. Codes: 400 bad input, 401 not logged in, 403 not allowed, 404 not found, 409 rule broken, 502 email failed.

## 5. Transactions (`@Transactional`)

**Concept.** A group of database steps that succeed together or fail together.

**Analogy.** Moving money between two accounts: both the withdrawal and the deposit happen, or neither.

**Where.** Closing a defect also writes an audit row. If one fails, both are undone. Special case: `login()` uses `noRollbackFor = AuthException.class`, because a failed login throws an exception, yet the failed-attempt counter and the log row must still be saved.

## 6. Security

### Authentication and authorization
**Concept.** Authentication is "who are you?" (login). Authorization is "what may you do?" (roles).
**Analogy.** Showing your ID at the gate, then the badge decides which rooms open.

### Passwords and BCrypt
**Concept.** Passwords are never stored. A one-way hash is stored. BCrypt is slow on purpose and adds a random salt, so two equal passwords give different hashes and guessing is expensive.
**Analogy.** A meat grinder: you can make mince from meat, never meat from mince.
**Where.** `SecurityConfig.passwordEncoder()`. Check it yourself with query 13 in the SQL file.

### JWT (JSON Web Token)
**Concept.** After login the server gives a signed token. The browser sends it in the `Authorization: Bearer ...` header on every request. The server checks the signature, so it does not need to remember sessions (stateless).
**Analogy.** A festival wristband: staff only check the band, not a guest list.
**Where.** `JwtService` (creates and checks), `JwtAuthFilter` (runs before every request and reads the token).

### The filter chain
**Concept.** Every request passes through a chain of filters. Ours reads the JWT, loads the user from the database, and tells Spring Security who is calling.
**Analogy.** Airport security lanes before the gate.
**Where.** `JwtAuthFilter`. It reads role and active status from MySQL each time, so an Admin's change applies immediately.

### Roles, twice
**Concept.** Role rules are checked in `SecurityConfig` (URL rules) and again with `@PreAuthorize` on controllers (method rules). Two layers is called defense in depth. The closing rule is also checked in the service.
**Analogy.** A locked building gate, and a locked office door inside.

### CORS (why we need it)
**Concept.** CORS = Cross-Origin Resource Sharing. A browser blocks a web page from one address (`localhost:5173`, React) from reading answers from another address (`localhost:8080`, Spring Boot). The server must say, in response headers, "this origin is allowed". Without it, the browser refuses the response even though the server worked. We allow only our own frontend address.
**Analogy.** A building security guard who only lets in visitors named on the list, even if the visitor is polite.
**Key points.** It is a browser rule, so Postman is not affected. `allowedOrigins` is set from `CORS_ORIGIN`, never `*`. Preflight: before PATCH or DELETE the browser sends an `OPTIONS` request first, which is why `OPTIONS` is permitted.
**Where.** `SecurityConfig.corsConfigurationSource()`.

### CSRF (why it is switched off)
**Concept.** CSRF attacks abuse browsers that send cookies automatically. We use no cookies and no sessions, only a header the browser does not add by itself, so CSRF protection is not needed here.
**Analogy.** A lock for a door that does not exist.

### Brute-force protection
**Concept.** After 5 wrong passwords the account is locked for 15 minutes. The Admin can unlock it. Every attempt is logged with time and IP address.
**Analogy.** A phone that locks after too many wrong PINs.
**Where.** `UserService.login`, `LoginEvent` table, "Security log" screen.

### Strong passwords and token invalidation
**Concept.** Passwords need 8+ characters with upper case, lower case and a number (`@Pattern`). After a password change, all older tokens stop working because `JwtAuthFilter` compares the token's issue time with `password_changed_at`.
**Analogy.** Changing the lock makes every old key useless.

### Security headers and hidden errors
**Concept.** The server sends headers that tell browsers to be strict (no framing, no content sniffing, no referrer, a locked-down content policy). Error messages and stack traces are never sent to the client.
**Analogy.** Telling visitors the rules at the door, and not showing them the building plans.

### Share links
**Concept.** A report link contains a random UUID token that expires in 7 days. The public page shows a limited view, with internal remarks and audit history left out.
**Analogy.** A one-week visitor pass that opens one room only.

## 7. Email

**Concept.** Spring's `JavaMailSender` sends mail through an SMTP server (Gmail here). Text typed by users is HTML-escaped (`HtmlUtils.htmlEscape`) so nobody can inject HTML into the email.
**Where.** `ReportService.emailReport`.

## 8. Frontend in one minute

- **State (`useState`):** React remembers what is on screen (selected defect, form values).
- **`fetch` in `api.js`:** one file that talks to the backend and adds the token to every request.
- **Theme toggle:** sets `data-bs-theme` on the page, Bootstrap 5.3 and our CSS variables switch colours, and the choice is saved in `localStorage`.
- **Hash links (`#/report/...`):** the public report opens without login and without a router library.

## 9. Questions mentors may ask

1. **Why layers?** Each part has one job, so changes and tests stay simple.
2. **Why is the closing rule in the service?** So every client must obey it, not only the React screen.
3. **Why DTOs (`DefectRequest`) and not the entity?** The client cannot set fields it must not touch (id, status, reportedBy, share token).
4. **What is the difference between 401 and 403?** 401: not logged in. 403: logged in but not allowed.
5. **Why JWT and not sessions?** Stateless, simple for a separate frontend. The cost is that a token lives until it expires, which is why ours expires in 2 hours and dies on password change.
6. **Where could a JWT be stolen?** It is kept in `localStorage`, which JavaScript can read, so an XSS bug could expose it. React escapes output by default. A stricter design uses an HttpOnly cookie, which needs CSRF protection again.
7. **How do you stop SQL injection?** Parameterized queries through Spring Data JPA.
8. **Why an index?** Faster searches on part number and status. The cost is slightly slower writes.
9. **Why LAZY problems?** The connection is closed after the service method, so lazy data cannot load later.
10. **What does `@Transactional` do here?** The status change and its audit row are saved together or not at all.
11. **How is brute force handled?** Lockout, login log, and BCrypt's slowness.
12. **What would you add for production?** See `SECURITY.md`.
