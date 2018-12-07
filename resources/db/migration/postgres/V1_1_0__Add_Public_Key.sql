ALTER TABLE auth_token ADD CONSTRAINT fk_auth_token_auth_user FOREIGN KEY (user_id) REFERENCES auth_user(user_id);

CREATE TABLE crypt_pub_key (
    id UUID NOT NULL PRIMARY KEY,
    public_key TEXT NOT NULL,
    added_at TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL,

    CONSTRAINT fk_crypt_pub_key_auth_user FOREIGN KEY (user_id) REFERENCES auth_user(username)
);
