-- Add message target type: USER, DEPT, DEPT_WITH_CHILDREN, ALL, ROLE, USER_IDS
ALTER TABLE sys_message ADD COLUMN target_type VARCHAR(32) NULL;
