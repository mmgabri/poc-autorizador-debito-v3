CREATE TABLE property_listings (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    city VARCHAR(100) NOT NULL,
    features TEXT,
    tags TEXT
);

CREATE TABLE inspections (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    inspector VARCHAR(100) NOT NULL,
    notes TEXT,
    CONSTRAINT fk_inspection_property FOREIGN KEY (property_id) REFERENCES property_listings (id)
);

CREATE TABLE images (
    id UUID PRIMARY KEY,
    data BYTEA NOT NULL,                     -- 👈 aqui muda!
    mime VARCHAR(100) NOT NULL,
    caption VARCHAR(255),
    property_id UUID,
    inspection_id UUID,
    CONSTRAINT fk_image_property FOREIGN KEY (property_id) REFERENCES property_listings (id),
    CONSTRAINT fk_image_inspection FOREIGN KEY (inspection_id) REFERENCES inspections (id)
);