-- ===============================
-- CREATE AND USE MY_BANK_TEST SCHEMA
-- ===============================
DROP SCHEMA IF EXISTS MY_BANK_TEST;
CREATE SCHEMA MY_BANK_TEST;
USE MY_BANK_TEST;

-- ===============================
-- Create necessary tables first
-- ===============================

-- Create CUSTOMERS table
CREATE TABLE CUSTOMERS (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keycloak_id VARCHAR(40),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active TINYINT(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create ACCOUNT_TYPES table
CREATE TABLE ACCOUNT_TYPES (
    account_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    interest_rate DECIMAL(5,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create ACCOUNTS table
CREATE TABLE ACCOUNTS (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    balance DECIMAL(19,4) DEFAULT 0.0000,
    opening_date DATE NOT NULL,
    status VARCHAR(20),
    notes VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (account_type_id) REFERENCES ACCOUNT_TYPES(account_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create ACCOUNT_APPLICATIONS table
CREATE TABLE ACCOUNT_APPLICATIONS (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type_id BIGINT NOT NULL,
    application_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Pending',
    initial_deposit DECIMAL(19,4) DEFAULT 0.0000,
    purpose VARCHAR(255),
    source_of_funds VARCHAR(100),
    rejection_reason VARCHAR(255),
    approved_by VARCHAR(100),
    approval_date DATETIME,
    notes VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (account_type_id) REFERENCES ACCOUNT_TYPES(account_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create TRANSACTIONS table
CREATE TABLE TRANSACTIONS (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    reference_number VARCHAR(100),
    receiving_account_id INT,
    transaction_status VARCHAR(100) DEFAULT 'Completed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Create CARD_TYPES table
CREATE TABLE CARD_TYPES (
    card_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    default_limit DECIMAL(19,4),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create CARDS table
CREATE TABLE CARDS (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    card_type_id BIGINT NOT NULL,
    card_number VARCHAR(16) NOT NULL UNIQUE,
    cardholder_name VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv VARCHAR(3) NOT NULL,
    limit_amount DECIMAL(19,4) NOT NULL,
    status_name VARCHAR(10),
    billing_address VARCHAR(255),
    billing_city VARCHAR(50),
    billing_state VARCHAR(50),
    billing_zip VARCHAR(20),
    issue_date DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (account_id) REFERENCES ACCOUNTS(account_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPES(card_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create CARD_TRANSACTIONS table
CREATE TABLE CARD_TRANSACTIONS (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    merchant_category VARCHAR(50),
    transaction_location VARCHAR(255),
    transaction_date DATETIME NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'Completed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES CARDS(card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create CARD_APPLICATIONS table
CREATE TABLE CARD_APPLICATIONS (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    card_type_id BIGINT NOT NULL,
    application_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'Pending',
    credit_score INT,
    income DECIMAL(19,4),
    employment_status VARCHAR(50),
    years_at_current_job INT,
    rejection_reason VARCHAR(255),
    requested_credit_limit DECIMAL(19,4),
    approved_credit_limit DECIMAL(19,4),
    processing_officer_id INT,
    approval_date DATETIME,
    card_id BIGINT,
    notes VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPES(card_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create LOAN_TYPES table
CREATE TABLE LOAN_TYPES (
    loan_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    min_interest_rate DECIMAL(5,2) NOT NULL,
    max_interest_rate DECIMAL(5,2) NOT NULL,
    min_term INT NOT NULL COMMENT 'In months',
    max_term INT NOT NULL COMMENT 'In months',
    minimum_amount DECIMAL(19,4) NOT NULL,
    maximum_amount DECIMAL(19,4) NOT NULL,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create LOANS table
CREATE TABLE LOANS (
    loan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    loan_type_id BIGINT NOT NULL,
    principal_amount DECIMAL(19,4) NOT NULL,
    outstanding_amount DECIMAL(19,4) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    term_months INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    payment_frequency VARCHAR(50) DEFAULT 'Monthly',
    monthly_payment DECIMAL(19,4) NOT NULL,
    status VARCHAR(50) DEFAULT 'Active',
    loan_officer_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (loan_type_id) REFERENCES LOAN_TYPES(loan_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create LOAN_APPLICATIONS table
CREATE TABLE LOAN_APPLICATIONS (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    loan_type_id BIGINT NOT NULL,
    requested_amount DECIMAL(19,4) NOT NULL,
    requested_term INT NOT NULL COMMENT 'In months',
    purpose VARCHAR(50),
    application_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'Pending',
    rejection_reason VARCHAR(255),
    credit_score INT,
    monthly_income DECIMAL(19,4),
    monthly_expenses DECIMAL(19,4),
    loan_officer_id INT,
    loan_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMERS(customer_id),
    FOREIGN KEY (loan_type_id) REFERENCES LOAN_TYPES(loan_type_id),
    FOREIGN KEY (loan_id) REFERENCES LOANS(loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create LOAN_PAYMENTS table
CREATE TABLE LOAN_PAYMENTS (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    principal_component DECIMAL(19,4) NOT NULL,
    interest_component DECIMAL(19,4) NOT NULL,
    fees DECIMAL(19,4) DEFAULT 0.0000,
    payment_date DATETIME NOT NULL,
    due_date DATETIME NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'Completed',
    reference_number VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES LOANS(loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
