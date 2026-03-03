-- Fix: the old composite unique keys (email, delete_time) / (username, delete_time)
-- do NOT enforce uniqueness for active users because MySQL treats multiple NULLs
-- as distinct in UNIQUE indexes.
--
-- email must be unique among active users (login credential).
-- username does not need to be unique.

ALTER TABLE sys_user DROP INDEX uk_user_email;
ALTER TABLE sys_user DROP INDEX uk_user_username;

-- Functional index: evaluates to the email for active users (delete_time IS NULL),
-- and NULL for deleted users. MySQL allows multiple NULLs in a UNIQUE index,
-- so deleted users with the same email won't conflict.
CREATE UNIQUE INDEX uk_user_email_active
    ON sys_user ((IF(delete_time IS NULL, email, NULL)));
