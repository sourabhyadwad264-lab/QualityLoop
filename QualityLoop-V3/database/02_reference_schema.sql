-- FOR UNDERSTANDING ONLY. Hibernate creates these tables by itself when the backend starts.
-- Do not run this before the first start. Read it to see what the Java entities become in MySQL.

CREATE TABLE app_user (                         -- entity AppUser
  id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username            VARCHAR(255) NOT NULL UNIQUE,
  full_name           VARCHAR(255) NOT NULL,
  email               VARCHAR(255),
  password            VARCHAR(255) NOT NULL,    -- BCrypt hash, never the real password
  role                ENUM('ADMIN','QUALITY_HEAD','QUALITY_ENGINEER') NOT NULL,
  active              BIT(1)       NOT NULL,
  manager_id          BIGINT,                   -- who this person reports to (points to another row of this table)
  failed_attempts     INT          NOT NULL,    -- wrong passwords in a row
  locked_until        DATETIME(6),              -- account locked until this time
  password_changed_at DATETIME(6),
  CONSTRAINT fk_user_manager FOREIGN KEY (manager_id) REFERENCES app_user (id)
);

CREATE TABLE defect (                           -- entity Defect
  id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title              VARCHAR(255)  NOT NULL,
  description        VARCHAR(1000),
  part_number        VARCHAR(255),
  part_name          VARCHAR(255),
  source             ENUM('CUSTOMER_COMPLAINT','INTERNAL_INSPECTION','PPAP_SAMPLE_REJECTION'),
  quantity_affected  INT,
  detected_on        DATE,
  detected_by        VARCHAR(255),
  severity           ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  status             ENUM('OPEN','IN_PROGRESS','CLOSED')    NOT NULL,
  reported_by        VARCHAR(255),
  containment_action VARCHAR(1000),
  root_cause         VARCHAR(1000),
  remarks            VARCHAR(1000),
  created_at         DATETIME(6),
  closed_at          DATETIME(6),
  share_token        VARCHAR(255),
  share_expires_at   DATETIME(6),
  INDEX idx_defect_part (part_number),          -- makes "find by part number" fast
  INDEX idx_defect_status (status)              -- makes "find by status" fast
);

CREATE TABLE corrective_action (                -- entity CorrectiveAction (many actions belong to one defect)
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  description VARCHAR(255) NOT NULL,
  owner       VARCHAR(255),
  due_date    DATE,
  status      ENUM('PENDING','COMPLETED') NOT NULL,
  defect_id   BIGINT       NOT NULL,            -- the foreign key (the "many" side holds it)
  CONSTRAINT fk_action_defect FOREIGN KEY (defect_id) REFERENCES defect (id)
);

CREATE TABLE audit_entry (                      -- entity AuditEntry (who did what, when)
  id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  defect_id    BIGINT,                          -- plain number on purpose: history survives even if the defect is deleted
  event        VARCHAR(255),
  detail       VARCHAR(500),
  performed_by VARCHAR(255),
  at           DATETIME(6)
);

CREATE TABLE login_event (                      -- entity LoginEvent (every login attempt)
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(255),
  success    BIT(1)       NOT NULL,
  ip_address VARCHAR(255),
  reason     VARCHAR(255),
  at         DATETIME(6)
);
