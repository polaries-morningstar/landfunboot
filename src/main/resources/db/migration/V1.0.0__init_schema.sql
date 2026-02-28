SET FOREIGN_KEY_CHECKS = 0;

-- 1. Department Table
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT UNSIGNED DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dept_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Role Table
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    data_scope VARCHAR(50) DEFAULT 'SELF',
    delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (code, delete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Menu Table
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT UNSIGNED DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(255),
    path VARCHAR(255),
    permission VARCHAR(100),
    type VARCHAR(20) NOT NULL,
    delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_menu_permission (permission, delete_time),
    KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. User Table
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password VARCHAR(128) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0:禁用, 1:启用)',
    is_superuser TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为超级管理员(1-是, 0-否)',
    role_id BIGINT UNSIGNED,
    dept_id BIGINT UNSIGNED,
    delete_time DATETIME(6) NULL DEFAULT NULL COMMENT '逻辑删除时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username, delete_time),
    UNIQUE KEY uk_user_email (email, delete_time),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Role-Menu Mapping
DROP TABLE IF EXISTS sys_role_menu_mapping;
CREATE TABLE sys_role_menu_mapping (
    role_id BIGINT UNSIGNED NOT NULL,
    menu_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Role-Dept Mapping
DROP TABLE IF EXISTS sys_role_dept_mapping;
CREATE TABLE sys_role_dept_mapping (
    role_id BIGINT UNSIGNED NOT NULL,
    dept_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, dept_id),
    CONSTRAINT fk_role_dept_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_dept_dept FOREIGN KEY (dept_id) REFERENCES sys_dept (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
