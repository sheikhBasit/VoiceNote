# VoiceNote AI - Personal Productivity Assistant

A robust, AI-powered meeting assistant that records, transcribes, and organizes your professional life using state-of-the-art LLMs.

## 🚀 Key Features

### 1. Smart Recording & Transcription
- **Silent Background Listening**: Captures peer voices during calls/meetings without interrupting audio.
- **Dual-STT Pipeline**: Uses **Groq Whisper-large-v3-turbo** for high-accuracy final notes and Android Native STT for real-time feedback.
- **VAD (Voice Activity Detection)**: Automatically strips silence and noise from recordings to save battery and data.
- **20MB Audio Chunking**: Handles long-duration meetings by splitting and processing context in chunks.

### 2. Expert AI Brain (Llama 3.1)
- **Automatic Organization**: Generates descriptive titles, 2-sentence summaries, and actionable tasks.
- **Multilingual Support**: Processes multiple languages and outputs everything in English.
- **Speaker Diarization**: Identifies different speakers (Speaker A, B, etc.) in the transcript.
- **Actionable Insights**: Generates specific Google Search queries and AI prompts for every task.

### 3. Deep System Integration
- **Calendar & Alarms**: Automatically schedules extracted tasks to your Google/Native Calendar and sets system alarms.
- **Contact Assignment**: Search and assign tasks to peers directly from your phone's contact list.
- **3rd-Party Launchers**: Direct buttons to open **WhatsApp**, **Google Meet**, **Slack**, **ChatGPT**, and **Gemini**.
- **Visual Memory**: Attach photos or screenshots to specific tasks for better remembrance.

### 4. Enterprise-Grade Security
- **Biometric Gate**: Hardware-backed fingerprint/face unlock.
- **One-Time Bypass**: Unlock once and stay authorized for faster access.
- **Encrypted Storage**: Uses `EncryptedSharedPreferences` to store session tokens and API keys.
- **Failover Key Rotation**: Rotates through multiple Groq API keys automatically if rate limits are hit.

### 5. Modern UX
- **Task-First Home Page**: Focuses on your immediate to-do list organized by priority tabs.
- **Floating Shortcut Hub**: Draggable menu overlay accessible from any app and the lock screen.
- **Dark Mode Only**: Sleek, high-contrast Material 3 interface.
- **Real-time Sync**: Multi-device support via Firestore listeners.

## 🛠 Tech Stack
- **Jetpack Compose**: Modern UI components.
- **Groq API**: Whisper (STT) and Llama 3.1 (LLM).
- **Firebase**: Firestore Database.
- **WorkManager**: Background task reminders and auto-escalation.
- **OkHttp/Retrofit**: Network communication.
- **Coil**: Dynamic image loading.

## 📝 Setup
1. Add your `google-services.json` to the `app/` directory.
2. In the app's **Settings** tab, add your **Groq API Key**.
3. Grant permissions for Microphone, Contacts, and Calendar on first launch.
4. (Optional) Define your **Office Hours** in Settings to optimize battery life.
