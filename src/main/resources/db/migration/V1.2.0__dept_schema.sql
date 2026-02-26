-- Create Department Table
CREATE TABLE sys_dept (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT UNSIGNED DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    deleted_millis BIGINT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add dept_id to User Table
ALTER TABLE sys_user ADD COLUMN dept_id BIGINT UNSIGNED;
ALTER TABLE sys_user ADD CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_dept(id);

-- Remove sort_order from Menu Table
ALTER TABLE sys_menu DROP COLUMN sort_order;

-- Add data_scope to Role Table
ALTER TABLE sys_role ADD COLUMN data_scope VARCHAR(50) DEFAULT 'SELF';
