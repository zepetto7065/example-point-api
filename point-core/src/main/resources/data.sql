INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('MAX_EARN_AMOUNT_PER_ONCE', '100000', '1회 최대 적립 금액', NOW(), NOW());
INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('MAX_BALANCE_PER_MEMBER', '5000000', '최대 보유 한도', NOW(), NOW());
INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('DEFAULT_EXPIRE_DAYS', '365', '기본 만료일수', NOW(), NOW());
INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('MIN_EXPIRE_DAYS', '1', '최소 만료일수', NOW(), NOW());
INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('MAX_EXPIRE_DAYS', '1824', '최대 만료일수 (5년 미만)', NOW(), NOW());
INSERT INTO config (config_key, config_value, description, created_at, updated_at) VALUES ('USE_CANCEL_RESTORE_EXPIRE_DAYS', '365', '사용취소 신규적립 만료일수', NOW(), NOW());
