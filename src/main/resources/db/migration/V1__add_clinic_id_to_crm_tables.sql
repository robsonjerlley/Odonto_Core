ALTER TABLE crm_db.customers ADD COLUMN IF NOT EXISTS clinic_id UUID;

UPDATE crm_db.customers
SET clinic_id = '00000000-0000-0000-0000-000000000001'
WHERE clinic_id IS NULL;

ALTER TABLE crm_db.customers ALTER COLUMN clinic_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_clinic_id ON crm_db.customers (clinic_id);



ALTER TABLE crm_db.contact_logs ADD COLUMN IF NOT EXISTS clinic_id UUID;

UPDATE crm_db.contact_logs
SET clinic_id = '00000000-0000-0000-0000-000000000001'
WHERE clinic_id IS NULL;

ALTER TABLE crm_db.contact_logs ALTER COLUMN clinic_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_contact_logs_clinic_id ON crm_db.contact_logs (clinic_id);


ALTER TABLE crm_db.lead_tickets ADD COLUMN IF NOT EXISTS clinic_id UUID;

UPDATE crm_db.lead_tickets
SET clinic_id = '00000000-0000-0000-0000-000000000001'
WHERE clinic_id IS NULL;

ALTER TABLE crm_db.lead_tickets ALTER COLUMN clinic_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_lead_tickets_clinic_id ON crm_db.lead_tickets (clinic_id);