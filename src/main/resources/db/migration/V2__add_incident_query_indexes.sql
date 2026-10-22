CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_priority ON incidents (priority);
CREATE INDEX idx_incidents_category ON incidents (category);
CREATE INDEX idx_incidents_created_at ON incidents (created_at);
CREATE INDEX idx_incidents_updated_at ON incidents (updated_at);
