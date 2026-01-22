CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- USERS
CREATE TABLE IF NOT EXISTS users (
                                     id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar(120) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    password_hash text NOT NULL,
    is_email_verified boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
    );

-- CATEGORIES
CREATE TABLE IF NOT EXISTS categories (
                                          id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name varchar(80) NOT NULL,
    type varchar(10) NOT NULL CHECK (type IN ('INCOME','EXPENSE')),
    color_hex varchar(7) DEFAULT '#C8C8C8',
    icon varchar(40),
    is_archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
    );

-- Evitar categorias duplicadas por usuário (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS uq_category_user_lower_name_type
    ON categories (user_id, lower(name), type);

CREATE INDEX IF NOT EXISTS idx_categories_user_active
    ON categories (user_id, is_archived, type);

-- TRANSACTIONS
CREATE TABLE IF NOT EXISTS transactions (
                                            id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id uuid REFERENCES categories(id) ON DELETE SET NULL,

    type varchar(10) NOT NULL CHECK (type IN ('INCOME','EXPENSE')),
    amount numeric(12,2) NOT NULL CHECK (amount >= 0),
    date date NOT NULL,
    description varchar(140) NOT NULL,
    notes text,

    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_tx_user_date
    ON transactions (user_id, date);

CREATE INDEX IF NOT EXISTS idx_tx_user_category_date
    ON transactions (user_id, category_id, date);

CREATE INDEX IF NOT EXISTS idx_tx_user_type_date
    ON transactions (user_id, type, date);
