-- Practice queries for MySQL Workbench. Run them one at a time (select the query, press Ctrl+Enter).
USE qualityloop;

-- 1. Every defect with its number of corrective actions (JOIN + GROUP BY)
SELECT d.id, d.title, d.part_number, d.status, COUNT(a.id) AS actions
FROM defect d
LEFT JOIN corrective_action a ON a.defect_id = d.id
GROUP BY d.id, d.title, d.part_number, d.status
ORDER BY d.id;

-- 2. Which parts cause the most defects (the dashboard's "Parts with most defects")
SELECT part_number, COUNT(*) AS defects, MAX(detected_on) AS last_detected
FROM defect
WHERE part_number IS NOT NULL
GROUP BY part_number
ORDER BY defects DESC
LIMIT 5;

-- 3. Defects per month (DATE_FORMAT turns a date into 'YYYY-MM')
SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS defects
FROM defect
GROUP BY month
ORDER BY month;

-- 4. Average days from reporting to closing (DATEDIFF)
SELECT ROUND(AVG(DATEDIFF(closed_at, created_at)), 1) AS avg_days_to_close
FROM defect
WHERE closed_at IS NOT NULL;

-- 5. Overdue corrective actions on defects that are not closed
SELECT d.id AS defect_id, d.title, a.description, a.owner, a.due_date,
       DATEDIFF(CURDATE(), a.due_date) AS days_late
FROM corrective_action a
JOIN defect d ON d.id = a.defect_id
WHERE a.status = 'PENDING' AND a.due_date < CURDATE() AND d.status <> 'CLOSED'
ORDER BY a.due_date;

-- 6. Defects that have NO corrective action yet (LEFT JOIN ... IS NULL)
SELECT d.id, d.title, d.status
FROM defect d
LEFT JOIN corrective_action a ON a.defect_id = d.id
WHERE a.id IS NULL;

-- 7. Owners with more than one pending action (GROUP BY + HAVING)
SELECT owner, COUNT(*) AS pending_actions
FROM corrective_action
WHERE status = 'PENDING' AND owner IS NOT NULL
GROUP BY owner
HAVING COUNT(*) > 1
ORDER BY pending_actions DESC;

-- 8. Open defects older than 7 days
SELECT id, title, created_at
FROM defect
WHERE status <> 'CLOSED' AND created_at < NOW() - INTERVAL 7 DAY;

-- 9. The full history of one defect (change 1 to the defect id)
SELECT at, event, detail, performed_by
FROM audit_entry
WHERE defect_id = 1
ORDER BY at DESC;

-- 10. Who reports to whom (a SELF JOIN: the table is joined to itself)
SELECT u.full_name AS person, u.role, m.full_name AS reports_to
FROM app_user u
LEFT JOIN app_user m ON u.manager_id = m.id;

-- 11. Failed logins in the last 24 hours, grouped by user (security check)
SELECT username, COUNT(*) AS failed, MAX(at) AS last_try
FROM login_event
WHERE success = 0 AND at > NOW() - INTERVAL 1 DAY
GROUP BY username
ORDER BY failed DESC;

-- 12. Does MySQL use the index? Look at the "key" column of the result: it should show idx_defect_part
EXPLAIN SELECT * FROM defect WHERE part_number = 'BRK-1042';

-- 13. Check that passwords are stored as hashes (they must look like $2a$10$... and never be readable)
SELECT username, LEFT(password, 7) AS hash_start, LENGTH(password) AS length FROM app_user;
