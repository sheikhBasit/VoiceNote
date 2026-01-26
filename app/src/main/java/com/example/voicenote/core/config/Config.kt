package com.example.voicenote.core.config

object Config {
    private const val HOST = "oleta-lyolytic-overapprehensively.ngrok-free.dev"
    
    // HTTP Base URL
    const val BASE_URL = "https://$HOST/api/v1/"
    
    // WebSocket Base URL
    const val WS_URL = "wss://$HOST/api/ws/"
}
