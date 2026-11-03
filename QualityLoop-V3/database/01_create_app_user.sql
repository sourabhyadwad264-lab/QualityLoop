-- Run in MySQL Workbench as root.
-- Idea: the application should not log in to MySQL as root. Give it its own user with only the rights it needs
-- (the principle of least privilege). If the app is hacked, the damage stays small.

CREATE DATABASE IF NOT EXISTS qualityloop;

CREATE USER IF NOT EXISTS 'qualityloop_app'@'localhost' IDENTIFIED BY 'ChangeThisPassword#1';

-- Day-to-day rights: read and write rows only.
GRANT SELECT, INSERT, UPDATE, DELETE ON qualityloop.* TO 'qualityloop_app'@'localhost';

-- Only while ddl-auto=update is used (it creates and changes tables at startup) the user also needs these.
-- Remove them with the REVOKE line below once the tables exist and ddl-auto is set to 'validate'.
GRANT CREATE, ALTER, INDEX, REFERENCES ON qualityloop.* TO 'qualityloop_app'@'localhost';
-- REVOKE CREATE, ALTER, INDEX, REFERENCES ON qualityloop.* FROM 'qualityloop_app'@'localhost';

FLUSH PRIVILEGES;

-- Then run the backend with  DB_USERNAME=qualityloop_app  and  DB_PASSWORD=ChangeThisPassword#1
-- Check what the user may do:
SHOW GRANTS FOR 'qualityloop_app'@'localhost';
