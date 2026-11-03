# Security in QualityLoop

## What is built in

| Area | Measure |
|---|---|
| Passwords | BCrypt hash with salt, rule: 8+ characters, upper, lower, number |
| Login | JWT, expires in 2 hours, dies when the password changes |
| Brute force | 5 wrong passwords lock the account for 15 minutes, Admin can unlock |
| Audit | Every login attempt (time, user, address, result) and every defect change (with user name) |
| Roles | Engineer, Quality Head, Admin. Checked by URL rule, by `@PreAuthorize`, and in the service |
| Accounts | Admin can switch accounts off. Users cannot change their own role |
| API | CORS limited to one origin, CSRF off on purpose (no cookies), stateless, security headers |
| Data | JPA parameterized queries (no SQL injection), validation on every request, HTML escaping in emails |
| Errors | No stack traces or Java messages sent to the client |
| Sharing | Random token links that expire after 7 days, limited view, no internal remarks |
| Database | `database/01_create_app_user.sql` creates a least-privilege MySQL user |

## Before using it for real

1. Run over HTTPS (a reverse proxy such as Nginx, or a hosting platform). Without HTTPS, passwords and tokens travel readable.
2. Set `JWT_SECRET` (long and random), `ADMIN_PASSWORD`, and a database password through environment variables. Never commit them.
3. Log in as `admin` and change the password on the first day.
4. Use the `qualityloop_app` MySQL user, set `spring.jpa.hibernate.ddl-auto=validate`, and revoke `CREATE/ALTER`.
5. Set `CORS_ORIGIN` and `FRONTEND_URL` to the real site address.
6. Back up the database regularly.
7. Keep libraries updated (`npm audit`, and check Spring Boot and jjwt versions).

## Honest limits (good to say in a presentation)

- The token is stored in `localStorage`. An XSS bug could expose it. An HttpOnly cookie is stronger but needs CSRF protection.
- Lockout is per account, and there is no limit per IP address. A reverse proxy or gateway can add that.
- There is no email verification, password reset by email, or two-factor login yet.
- Self-registration is open, but new accounts have the lowest role and an Admin controls promotion.
- The test suite is manual (Postman). Automated tests would be a good next step.
