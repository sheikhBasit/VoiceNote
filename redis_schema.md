# VoiceNote Redis Schema Design

## Purpose
Redis is used in the VoiceNote application for:
- Session management and authentication tokens
- Caching frequently accessed data
- Rate limiting
- Temporary storage for processing queues
- Real-time communication data

## Redis Keys Schema

### 1. User Sessions
```
Key: `session:{session_token}`
Value: Hash
  - user_id: UUID
  - created_at: Unix timestamp
  - expires_at: Unix timestamp
  - last_accessed: Unix timestamp
  - device_info: JSON string
  - ip_address: String
  - user_agent: String
TTL: Set to match session expiration time (typically 24-48 hours)

Example:
session:a1b2c3d4-e5f6-7890-1234-567890abcdef
{
  "user_id": "12345678-1234-5678-9012-123456789012",
  "created_at": 1678886400,
  "expires_at": 1678972800,
  "last_accessed": 1678886400,
  "device_info": "{\"os\":\"Android\",\"version\":\"12\"}",
  "ip_address": "192.168.1.100",
  "user_agent": "VoiceNoteApp/1.0"
}
```

### 2. User Authentication Cache
```
Key: `user:{user_id}`
Value: Hash
  - email: String
  - name: String
  - created_at: Unix timestamp
  - last_login: Unix timestamp
  - is_verified: Boolean
TTL: 1 hour (short-lived cache for frequently accessed user data)

Example:
user:12345678-1234-5678-9012-123456789012
{
  "email": "user@example.com",
  "name": "John Doe",
  "created_at": 1678886400,
  "last_login": 1678886400,
  "is_verified": true
}
```

### 3. Active Recordings
```
Key: `recording:{user_id}:{session_id}`
Value: Hash
  - status: String (recording, paused, stopped)
  - started_at: Unix timestamp
  - duration: Integer (seconds)
  - file_path: String
  - amplitude_history: JSON array of recent amplitude values
  - vad_threshold: Integer
TTL: Duration of recording + 1 hour grace period

Example:
recording:12345678-1234-5678-9012-123456789012:abc123
{
  "status": "recording",
  "started_at": 1678886400,
  "duration": 120,
  "file_path": "/storage/rec/abc123.mp4",
  "amplitude_history": [150, 200, 180, 220],
  "vad_threshold": 800
}
```

### 4. Rate Limiting
```
Key: `rate_limit:{user_id}:{endpoint}`
Value: Hash
  - count: Integer
  - reset_time: Unix timestamp
TTL: Until reset_time

Key: `rate_limit:{ip_address}:{endpoint}`
Value: Hash
  - count: Integer
  - reset_time: Unix timestamp
TTL: Until reset_time

Example:
rate_limit:12345678-1234-5678-9012-123456789012:/api/upload
{
  "count": 5,
  "reset_time": 1678887000
}
```

### 5. Processing Queue Status
```
Key: `processing_queue:{job_id}`
Value: Hash
  - status: String (pending, processing, completed, failed)
  - progress: Float (0.0 to 1.0)
  - started_at: Unix timestamp
  - completed_at: Unix timestamp (when applicable)
  - error_message: String (when status is failed)
  - input_file: String
  - output_result: String
TTL: 24 hours after completion

Example:
processing_queue:job_12345
{
  "status": "processing",
  "progress": 0.65,
  "started_at": 1678886400,
  "completed_at": null,
  "error_message": null,
  "input_file": "/storage/temp/audio_123.mp4",
  "output_result": null
}
```

### 6. Meeting Detection Cache
```
Key: `meeting_cache:{user_id}:{date}`
Value: Hash
  - current_meeting: String (title of current meeting)
  - next_meeting: String (title of next meeting)
  - next_meeting_time: Unix timestamp
  - calendar_sync_time: Unix timestamp
TTL: 1 hour (since calendar events can change frequently)

Example:
meeting_cache:12345678-1234-5678-9012-123456789012:2023-03-15
{
  "current_meeting": "Team Standup",
  "next_meeting": "Project Review",
  "next_meeting_time": 1678920000,
  "calendar_sync_time": 1678886400
}
```

### 7. Voice Activity Detection Buffer
```
Key: `vad_buffer:{user_id}:{session_id}`
Value: List of amplitude values (recent 100 values)
TTL: Duration of recording + 10 minutes

Example:
vad_buffer:12345678-1234-5678-9012-123456789012:abc123
[150, 200, 180, 220, 100, 90, 80, 150, 180, 200, ...]
```

### 8. WebSocket Connection Mapping
```
Key: `websocket_connections:{user_id}`
Value: Set of connection IDs
TTL: When user disconnects

Example:
websocket_connections:12345678-1234-5678-9012-123456789012
{"conn_123", "conn_456"}
```

### 9. User Preferences Cache
```
Key: `user_preferences:{user_id}`
Value: Hash
  - theme: String (light, dark, auto)
  - notification_enabled: Boolean
  - auto_save_enabled: Boolean
  - default_language: String
  - haptic_feedback_enabled: Boolean
  - last_sync_time: Unix timestamp
TTL: 24 hours

Example:
user_preferences:12345678-1234-5678-9012-123456789012
{
  "theme": "dark",
  "notification_enabled": true,
  "auto_save_enabled": true,
  "default_language": "en",
  "haptic_feedback_enabled": true,
  "last_sync_time": 1678886400
}
```

### 10. Feature Flags
```
Key: `feature_flags:{feature_name}`
Value: String (enabled/disabled or JSON configuration)
TTL: No TTL (persistent flags)

Example:
feature_flags:realtime_transcription
"enabled"

feature_flags:premium_features
{
  "realtime_transcription": true,
  "advanced_analytics": true,
  "export_options": ["pdf", "docx"]
}
```

## Redis Commands for Common Operations

### Setting a Session
```bash
HSET session:{token} user_id {user_id} created_at {timestamp} expires_at {timestamp}
EXPIRE session:{token} {ttl_seconds}
```

### Getting User Data
```bash
HGETALL user:{user_id}
```

### Updating Recording Status
```bash
HSET recording:{user_id}:{session_id} status {status} duration {duration}
EXPIRE recording:{user_id}:{session_id} {ttl_seconds}
```

### Rate Limiting Check
```bash
# Increment counter
INCR rate_limit:{user_id}:{endpoint}
# Set expiration if it's the first increment
EXPIRE rate_limit:{user_id}:{endpoint} {time_window_seconds}
# Get current count
GET rate_limit:{user_id}:{endpoint}
```

### Adding to Processing Queue
```bash
LPUSH processing_queue_list {job_id}
```

## Performance Considerations

1. **TTL Management**: Set appropriate TTL values for different types of data
2. **Memory Optimization**: Use efficient data structures and compress large JSON values
3. **Connection Pooling**: Use connection pooling for Redis connections
4. **Monitoring**: Monitor Redis memory usage and performance metrics
5. **Persistence**: Configure appropriate persistence settings (RDB+AOF) for critical data
6. **Clustering**: Consider Redis Cluster for high availability and scalability