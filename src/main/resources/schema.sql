CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(800),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0),
    active BOOLEAN NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- Restrictive FK proves that BJORM deletes nested children before the parent.
CREATE TABLE IF NOT EXISTS demo_orders (
    id UUID PRIMARY KEY,
    customer VARCHAR(120) NOT NULL
);
CREATE TABLE IF NOT EXISTS demo_order_lines (
    id UUID PRIMARY KEY,
    orderId UUID NOT NULL REFERENCES demo_orders(id),
    sku VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_demo_order_lines_order ON demo_order_lines(orderId);
