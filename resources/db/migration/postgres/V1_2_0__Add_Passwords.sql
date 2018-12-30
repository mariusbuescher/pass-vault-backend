CREATE TABLE password (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL DEFAULT '',
    domain VARCHAR(1023) NOT NULL DEFAULT '',
    account VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,

    CONSTRAINT fk_password_auth_user FOREIGN KEY (user_id) REFERENCES auth_user(username)
);
