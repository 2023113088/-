USE userdb;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    email VARCHAR(120),
    role VARCHAR(32),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE bookdb;

CREATE TABLE IF NOT EXISTS books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(32),
    publisher VARCHAR(120),
    category VARCHAR(80),
    stock INT NOT NULL,
    price DOUBLE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_books_isbn (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE borrowdb;

CREATE TABLE IF NOT EXISTS borrow_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_time DATETIME(6) NOT NULL,
    due_time DATETIME(6) NOT NULL,
    return_time DATETIME(6),
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_borrow_records_user_id (user_id),
    KEY idx_borrow_records_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
