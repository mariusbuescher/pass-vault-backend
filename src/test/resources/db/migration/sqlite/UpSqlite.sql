CREATE TABLE auth_user (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password BYTEA NOT NULL
);

CREATE TABLE auth_token (
    token TEXT NOT NULL PRIMARY KEY,
    issue_date TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_auth_token_auth_user FOREIGN KEY (user_id) REFERENCES auth_user(username)
);

CREATE TABLE crypt_pub_key (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    public_key TEXT NOT NULL,
    added_at TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_crypt_pub_key_pub_key_auth_user FOREIGN KEY (user_id) REFERENCES auth_user(username)
);
