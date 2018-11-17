CREATE TABLE auth_user (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password BYTEA NOT NULL
);

CREATE TABLE auth_token (
    token TEXT NOT NULL PRIMARY KEY,
    issue_date TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL REFERENCES auth_user(username)
);