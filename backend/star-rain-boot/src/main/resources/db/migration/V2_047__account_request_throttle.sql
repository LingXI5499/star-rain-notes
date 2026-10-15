-- Short lived, pseudonymous request reservations. No account or credential data is stored.
CREATE TABLE sr_login_throttle (
    throttle_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    window_started_at DATETIME(3) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (throttle_key),
    KEY idx_login_throttle_window (window_started_at)
) ENGINE=InnoDB;
