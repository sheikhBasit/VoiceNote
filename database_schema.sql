-- VoiceNote Application Database Schema

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    verification_token VARCHAR(255),
    verified_at TIMESTAMP WITH TIME ZONE,
    device_tokens TEXT[] -- Array of device tokens for push notifications
);

-- Indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Voice Notes table
CREATE TABLE IF NOT EXISTS voice_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    filename VARCHAR(500) NOT NULL, -- Path to stored audio file
    file_size BIGINT, -- Size in bytes
    duration INTEGER, -- Duration in seconds
    transcript TEXT, -- Full transcript of the audio
    summary TEXT, -- AI-generated summary
    status VARCHAR(50) DEFAULT 'processing', -- pending, processing, completed, failed
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    meeting_title VARCHAR(255), -- Extracted from calendar event if available
    meeting_start_time TIMESTAMP WITH TIME ZONE,
    meeting_end_time TIMESTAMP WITH TIME ZONE,
    sentiment_score DECIMAL(3,2), -- Sentiment score between -1 and 1
    language_code VARCHAR(10) DEFAULT 'en',
    processing_metadata JSONB -- Additional processing info
);

-- Indexes for voice_notes table
CREATE INDEX idx_voice_notes_user_id ON voice_notes(user_id);
CREATE INDEX idx_voice_notes_created_at ON voice_notes(created_at);
CREATE INDEX idx_voice_notes_status ON voice_notes(status);
CREATE INDEX idx_voice_notes_meeting_time ON voice_notes(meeting_start_time);
CREATE INDEX idx_voice_notes_title_gin ON voice_notes USING gin(to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(summary, '')));

-- Action Items extracted from voice notes
CREATE TABLE IF NOT EXISTS action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voice_note_id UUID NOT NULL REFERENCES voice_notes(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    assigned_to VARCHAR(255), -- Could be email or name
    due_date DATE,
    priority VARCHAR(20) DEFAULT 'medium', -- low, medium, high, urgent
    completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for action_items table
CREATE INDEX idx_action_items_voice_note_id ON action_items(voice_note_id);
CREATE INDEX idx_action_items_assigned_to ON action_items(assigned_to);
CREATE INDEX idx_action_items_completed ON action_items(completed);
CREATE INDEX idx_action_items_due_date ON action_items(due_date);

-- AI Analysis Results table
CREATE TABLE IF NOT EXISTS ai_analysis_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voice_note_id UUID NOT NULL REFERENCES voice_notes(id) ON DELETE CASCADE,
    analysis_type VARCHAR(50) NOT NULL, -- sentiment, topic, keyword, summary, etc.
    result_data JSONB NOT NULL, -- The actual analysis result
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for ai_analysis_results table
CREATE INDEX idx_ai_analysis_voice_note_id ON ai_analysis_results(voice_note_id);
CREATE INDEX idx_ai_analysis_type ON ai_analysis_results(analysis_type);

-- User Sessions table for authentication
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    refresh_token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    device_info JSONB -- Information about the device used
);

-- Indexes for user_sessions table
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

-- Device Registration table
CREATE TABLE IF NOT EXISTS user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL, -- Android ID or similar
    device_name VARCHAR(255),
    device_model VARCHAR(255),
    device_os VARCHAR(50),
    fcm_token VARCHAR(500), -- Firebase Cloud Messaging token
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Indexes for user_devices table
CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_device_id ON user_devices(device_id);

-- Billing/Usage tracking table
CREATE TABLE IF NOT EXISTS usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature VARCHAR(100) NOT NULL, -- 'voice_note_upload', 'ai_analysis', etc.
    quantity INTEGER DEFAULT 1,
    unit VARCHAR(50) DEFAULT 'count', -- count, minutes, bytes, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for usage_records table
CREATE INDEX idx_usage_records_user_id ON usage_records(user_id);
CREATE INDEX idx_usage_records_feature ON usage_records(feature);
CREATE INDEX idx_usage_records_created_at ON usage_records(created_at);

-- Reminders table
CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voice_note_id UUID REFERENCES voice_notes(id) ON DELETE SET NULL, -- Optional reference to a voice note
    title VARCHAR(255) NOT NULL,
    description TEXT,
    reminder_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(50), -- daily, weekly, monthly
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    triggered_at TIMESTAMP WITH TIME ZONE, -- When was it last triggered
    completed BOOLEAN DEFAULT FALSE
);

-- Indexes for reminders table
CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_voice_note_id ON reminders(voice_note_id);
CREATE INDEX idx_reminders_reminder_time ON reminders(reminder_time);
CREATE INDEX idx_reminders_completed ON reminders(completed);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- May be null for system events
    action VARCHAR(100) NOT NULL, -- login, logout, upload, delete, etc.
    resource_type VARCHAR(50), -- voice_note, user, session, etc.
    resource_id UUID,
    details JSONB, -- Additional details about the action
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for audit_log table
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_resource_type_id ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- Triggers to update the 'updated_at' timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_voice_notes_updated_at 
    BEFORE UPDATE ON voice_notes 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();