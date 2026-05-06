CREATE SEQUENCE IF NOT EXISTS app_user_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE app_user
(
    id        BIGINT       NOT NULL,
    username  VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    firstName VARCHAR(255),
    lastName  VARCHAR(255),
    isAdmin   BOOLEAN      NOT NULL,
    status    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id)
);

ALTER TABLE app_user
    ADD CONSTRAINT uc_app_user_email UNIQUE (email);

ALTER TABLE app_user
    ADD CONSTRAINT uc_app_user_username UNIQUE (username);