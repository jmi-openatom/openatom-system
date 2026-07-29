CREATE TABLE mailbox_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    oauth_sub VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(190),
    primary_address VARCHAR(254),
    local_part VARCHAR(64),
    mail_domain VARCHAR(190) NOT NULL,
    stalwart_account_id VARCHAR(128),
    quota_bytes BIGINT NOT NULL DEFAULT 2147483648,
    status VARCHAR(24) NOT NULL,
    provision_status VARCHAR(24) NOT NULL,
    last_event_id VARCHAR(64),
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mailbox_oauth_sub (oauth_sub),
    UNIQUE KEY uk_mailbox_user_id (user_id),
    UNIQUE KEY uk_mailbox_address (primary_address),
    KEY idx_mailbox_status (status, provision_status)
);

CREATE TABLE mailbox_alias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mailbox_id BIGINT NOT NULL,
    alias_address VARCHAR(254) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_mailbox_alias_account FOREIGN KEY (mailbox_id) REFERENCES mailbox_account(id),
    UNIQUE KEY uk_mailbox_alias (alias_address),
    KEY idx_mailbox_alias_mailbox (mailbox_id)
);

CREATE TABLE mailbox_processed_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    mailbox_id BIGINT,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mailbox_processed_event (event_id),
    KEY idx_mailbox_processed_mailbox (mailbox_id)
);
