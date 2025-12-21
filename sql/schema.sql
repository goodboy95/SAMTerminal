-- MySQL 8.0 schema for S.A.M. Terminal
CREATE TABLE IF NOT EXISTS star_domain (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(100) UNIQUE,
  name VARCHAR(255),
  description TEXT,
  ai_description TEXT,
  coord_x DOUBLE,
  coord_y DOUBLE,
  color VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS location (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(100) UNIQUE,
  name VARCHAR(255),
  description TEXT,
  background_style VARCHAR(255),
  background_url VARCHAR(500),
  ai_description TEXT,
  coord_x DOUBLE,
  coord_y DOUBLE,
  unlocked TINYINT(1) DEFAULT 0,
  domain_id BIGINT,
  CONSTRAINT fk_location_domain FOREIGN KEY (domain_id) REFERENCES star_domain(id)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) UNIQUE NOT NULL,
  email VARCHAR(255),
  password VARCHAR(255),
  role VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS game_state (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  current_location_id BIGINT,
  location_dynamic_state TEXT,
  firefly_emotion VARCHAR(50),
  firefly_status VARCHAR(255),
  firefly_mood_details TEXT,
  game_time VARCHAR(20),
  CONSTRAINT fk_state_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_state_location FOREIGN KEY (current_location_id) REFERENCES location(id)
);

CREATE TABLE IF NOT EXISTS item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  description TEXT,
  icon VARCHAR(20),
  quantity INT,
  user_id BIGINT,
  CONSTRAINT fk_item_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  content TEXT,
  date DATE,
  tags VARCHAR(255),
  user_id BIGINT,
  CONSTRAINT fk_memory_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS npc_character (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255),
  prompt TEXT,
  role VARCHAR(100),
  description TEXT,
  avatar_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS firefly_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  emotion VARCHAR(50) UNIQUE,
  url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS llm_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  base_url VARCHAR(255),
  api_key VARCHAR(255),
  model_name VARCHAR(100),
  temperature DOUBLE
);

CREATE TABLE IF NOT EXISTS llm_api_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100),
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255),
  model_name VARCHAR(100) NOT NULL,
  temperature DOUBLE,
  role VARCHAR(20) DEFAULT 'PRIMARY',
  token_limit BIGINT,
  token_used BIGINT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  failure_count INT DEFAULT 0,
  last_failure_at TIMESTAMP NULL,
  last_success_at TIMESTAMP NULL,
  circuit_opened_at TIMESTAMP NULL,
  max_load INT DEFAULT 30,
  version BIGINT DEFAULT 0,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) UNIQUE NOT NULL,
  user_id BIGINT,
  active_api_id BIGINT,
  created_at TIMESTAMP,
  last_active_at TIMESTAMP,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_session_api FOREIGN KEY (active_api_id) REFERENCES llm_api_config(id)
);

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender VARCHAR(50),
  npc_name VARCHAR(100),
  content TEXT,
  narration TEXT,
  timestamp TIMESTAMP,
  user_id BIGINT,
  CONSTRAINT fk_msg_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_token_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNIQUE,
  input_tokens BIGINT DEFAULT 0,
  output_tokens BIGINT DEFAULT 0,
  updated_at TIMESTAMP,
  CONSTRAINT fk_token_usage_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_token_limit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNIQUE,
  custom_limit BIGINT,
  CONSTRAINT fk_token_limit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS system_setting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  setting_key VARCHAR(100) UNIQUE,
  setting_value VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_location_unlock (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  location_id BIGINT,
  unlocked_at TIMESTAMP,
  UNIQUE (user_id, location_id),
  CONSTRAINT fk_unlock_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_unlock_location FOREIGN KEY (location_id) REFERENCES location(id)
);
