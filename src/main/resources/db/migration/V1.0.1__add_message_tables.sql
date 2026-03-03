-- Message table: one row per sent message
CREATE TABLE IF NOT EXISTS sys_message (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Message receiver: one row per recipient (supports read_at for "my messages")
CREATE TABLE IF NOT EXISTS sys_message_receiver (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    read_at DATETIME(6) NULL DEFAULT NULL,
    CONSTRAINT fk_msg_receiver_message FOREIGN KEY (message_id) REFERENCES sys_message (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_receiver_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    KEY idx_msg_receiver_message (message_id),
    KEY idx_msg_receiver_user (user_id),
    KEY idx_msg_receiver_read_at (read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
