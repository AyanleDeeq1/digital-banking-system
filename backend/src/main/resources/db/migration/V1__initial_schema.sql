-- Initial schema for MySQL 8.4, matching the current JPA entities and
-- Spring Boot's default snake_case naming strategy.
-- Hibernate maps STRING enums to native MySQL ENUM and Instant to DATETIME(6).
-- ORM cascade/orphan removal does not imply database ON DELETE CASCADE.
-- Constraint names match Hibernate's generated names so the existing test
-- profile's create-drop mode can remove the Flyway-created constraints.

CREATE TABLE password_credential (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hashed_password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE customer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_credential_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKdwk6cx0afu8bs9o4t536v1j5v UNIQUE (email),
    CONSTRAINT UKawh00r6hfr4wi4eoaqrgt4qen UNIQUE (password_credential_id),
    CONSTRAINT FK8cykrd4ida4e9v2anqbsamig1
        FOREIGN KEY (password_credential_id) REFERENCES password_credential (id)
) ENGINE=InnoDB;

CREATE TABLE account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    account_type ENUM('CHECKING', 'SAVINGS') NOT NULL,
    status ENUM('ACTIVE', 'CLOSED', 'FROZEN') NOT NULL,
    customer_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK66gkcp94endmotfwb8r4ocxm9 UNIQUE (account_number),
    CONSTRAINT FKnnwpo0lfq4xai1rs6887sx02k
        FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE=InnoDB;

CREATE TABLE card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    card_number VARCHAR(16) NOT NULL,
    cvc2 VARCHAR(3) NOT NULL,
    expiry_date DATE NOT NULL,
    pin VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UKby1nk98m2hq5onhl68bo09sc1 UNIQUE (card_number),
    CONSTRAINT UK6tyglx8w4ntwa1ipqwrplpljj UNIQUE (account_id),
    CONSTRAINT FK8v67eys6tqflsm6hrdgru2phu
        FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE=InnoDB;

CREATE TABLE bank_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type ENUM('DEPOSIT', 'TRANSFER', 'WITHDRAWAL') NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    status ENUM('COMPLETED', 'FAILED', 'PENDING') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE ledger_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    amount DECIMAL(19,2) NOT NULL,
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FKn763t766d31cgf7m4w4kj3hs0
        FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT FKmvmmlsvs82bfmjxm6o61nggyj
        FOREIGN KEY (transaction_id) REFERENCES bank_transaction (id)
) ENGINE=InnoDB;

-- InnoDB supplies the required indexes for non-unique foreign keys.
-- No entity declares additional indexes, join tables, or stored balances.
