-- One database per bounded context, even on a single Postgres instance.
--
-- Principle P2: one writer per aggregate, no shared databases. Co-locating them on one server is
-- an on-premises deployment convenience; it must never become a shared schema, because that is
-- how independently deployable services quietly turn back into a monolith.

-- catalog-service's database is created by POSTGRES_DB; the rest are added here.

CREATE DATABASE mpria_config;
GRANT ALL PRIVILEGES ON DATABASE mpria_config TO mpria;

-- Reserved for contexts that arrive in later phases (ARCHITECTURE.md §15).
CREATE DATABASE mpria_access;
GRANT ALL PRIVILEGES ON DATABASE mpria_access TO mpria;
