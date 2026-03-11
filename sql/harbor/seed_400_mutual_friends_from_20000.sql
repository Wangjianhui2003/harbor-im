-- Seed 400 users with ids 20000..20399 and make them all mutual friends.
-- Default login password for generated users: ChangeMe123!
-- This script refreshes only the friend relations inside the seeded 400-user set.

DROP PROCEDURE IF EXISTS seed_400_mutual_friends_from_20000;
DELIMITER $$

CREATE PROCEDURE seed_400_mutual_friends_from_20000()
BEGIN
    DECLARE v_user_count INT DEFAULT 400;
    DECLARE v_start_user_id BIGINT DEFAULT 20000;
    DECLARE v_end_user_id BIGINT DEFAULT 20399;

    DECLARE v_seq INT DEFAULT 0;
    DECLARE v_friend_seq INT DEFAULT 0;
    DECLARE v_user_id BIGINT DEFAULT 0;
    DECLARE v_friend_user_id BIGINT DEFAULT 0;
    DECLARE v_next_friend_id BIGINT DEFAULT 0;

    DECLARE v_username VARCHAR(255);
    DECLARE v_nickname VARCHAR(255);
    DECLARE v_email VARCHAR(50);
    DECLARE v_phone VARCHAR(20);
    DECLARE v_signature VARCHAR(255);
    DECLARE v_friend_nickname VARCHAR(255);

    DECLARE v_password_hash VARCHAR(255) DEFAULT '$2b$10$snH08q6UKAq/.lZI2JhpO.31OxsHZviMk9oPJMxzZOsxbsoSXXpky';
    DECLARE v_now DATETIME DEFAULT NOW();

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    WHILE v_seq < v_user_count DO
        SET v_user_id = v_start_user_id + v_seq;
        SET v_username = CONCAT('load_user_', v_user_id);

        IF EXISTS (
            SELECT 1
            FROM t_user
            WHERE id = v_user_id
              AND BINARY username <> BINARY v_username
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Existing non-seed user occupies one of the requested ids 20000..20399';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM t_user
            WHERE BINARY username = BINARY v_username
              AND id <> v_user_id
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Existing username load_user_<id> is bound to a different user id';
        END IF;

        SET v_seq = v_seq + 1;
    END WHILE;

    SET v_seq = 0;
    WHILE v_seq < v_user_count DO
        SET v_user_id = v_start_user_id + v_seq;
        SET v_username = CONCAT('load_user_', v_user_id);
        SET v_nickname = CONCAT('Load User ', LPAD(v_seq + 1, 4, '0'));
        SET v_email = CONCAT(v_username, '@harbor.local');
        SET v_phone = CONCAT('138', LPAD(v_user_id, 8, '0'));
        SET v_signature = CONCAT('Seeded mutual-friend user ', LPAD(v_seq + 1, 4, '0'));

        INSERT INTO t_user (
            id,
            username,
            nickname,
            head_image,
            head_image_thumb,
            password,
            email,
            phone_number,
            sex,
            is_banned,
            reason,
            type,
            add_type,
            signature,
            last_login_time,
            region,
            update_time,
            created_time
        ) VALUES (
            v_user_id,
            v_username,
            v_nickname,
            '',
            '',
            v_password_hash,
            v_email,
            v_phone,
            0,
            0,
            '',
            1,
            0,
            v_signature,
            NULL,
            '',
            v_now,
            v_now
        )
        ON DUPLICATE KEY UPDATE
            username = VALUES(username),
            nickname = VALUES(nickname),
            head_image = VALUES(head_image),
            head_image_thumb = VALUES(head_image_thumb),
            password = VALUES(password),
            email = VALUES(email),
            phone_number = VALUES(phone_number),
            sex = VALUES(sex),
            is_banned = VALUES(is_banned),
            reason = VALUES(reason),
            type = VALUES(type),
            add_type = VALUES(add_type),
            signature = VALUES(signature),
            last_login_time = VALUES(last_login_time),
            region = VALUES(region),
            update_time = VALUES(update_time);

        SET v_seq = v_seq + 1;
    END WHILE;

    DELETE FROM t_friend
    WHERE user_id BETWEEN v_start_user_id AND v_end_user_id
      AND friend_id BETWEEN v_start_user_id AND v_end_user_id;

    SET v_next_friend_id = COALESCE((SELECT MAX(id) FROM t_friend), 0) + 1;

    SET v_seq = 0;
    WHILE v_seq < v_user_count DO
        SET v_user_id = v_start_user_id + v_seq;

        SET v_friend_seq = 0;
        WHILE v_friend_seq < v_user_count DO
            SET v_friend_user_id = v_start_user_id + v_friend_seq;

            IF v_user_id <> v_friend_user_id THEN
                SET v_friend_nickname = CONCAT('Load User ', LPAD(v_friend_seq + 1, 4, '0'));

                INSERT INTO t_friend (
                    id,
                    user_id,
                    friend_id,
                    friend_nickname,
                    remark,
                    friend_head_image,
                    deleted,
                    update_time,
                    created_time
                ) VALUES (
                    v_next_friend_id,
                    v_user_id,
                    v_friend_user_id,
                    v_friend_nickname,
                    NULL,
                    '',
                    0,
                    v_now,
                    v_now
                );

                SET v_next_friend_id = v_next_friend_id + 1;
            END IF;

            SET v_friend_seq = v_friend_seq + 1;
        END WHILE;

        SET v_seq = v_seq + 1;
    END WHILE;

    COMMIT;

    SELECT
        v_user_count AS seeded_users,
        v_start_user_id AS first_user_id,
        v_end_user_id AS last_user_id,
        v_user_count * (v_user_count - 1) AS seeded_friend_rows,
        'ChangeMe123!' AS generated_user_password;
END $$

DELIMITER ;

CALL seed_400_mutual_friends_from_20000();
DROP PROCEDURE IF EXISTS seed_400_mutual_friends_from_20000;
