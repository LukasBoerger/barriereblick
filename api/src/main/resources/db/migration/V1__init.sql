-- M0-01: Basisschema – GENAU zwei Tabellen (organization, app_user).
-- UUID-PKs: nicht enumerierbar (Multi-Tenant-SaaS), gen_random_uuid() ist seit PG 13 nativ.

CREATE TABLE organization (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    logo_url            VARCHAR(1024),
    brand_color         VARCHAR(16),
    plan                VARCHAR(32)  NOT NULL DEFAULT 'FREE',
    stripe_customer_id  VARCHAR(255),            -- bleibt leer (Nicht in Scope, M6)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id         UUID NOT NULL REFERENCES organization(id),
    email          VARCHAR(320) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));
CREATE INDEX ix_app_user_org_id ON app_user (org_id);
