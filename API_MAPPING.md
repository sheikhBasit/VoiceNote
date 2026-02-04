# VoiceNote App: UI to API Mapping

This document details how the Android frontend interacts with the FastAPI backend endpoints.

## 1. Pulse (Dashboard) Screen
**Route:** `Screen.Dashboard` | **ViewModel:** `DashboardViewModel`

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Stats Cards** (Total, Completed, Pending) | `/api/v1/tasks/stats` | `GET` | Fetches counts for task completion and priority distribution. | **Response** |
| **Task Velocity & Heatmap** | `/api/v1/notes/dashboard` | `GET` | Shows productivity metrics and topic clusters. | **Response** |
| **AI Insights** | `/api/v1/ai/stats` | `GET` | Fetches high-priority pending counts and AI suggestions. | **Response** |

## 2. Notes (Home) Screen
**Route:** `Screen.Notes` | **ViewModel:** `HomeViewModel`

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **User Greeting** | `/api/v1/users/me` | `GET` | Displays the user's name and role in the top bar. | **Response** |
| **Recent Notes List** | `/api/v1/notes` | `GET` | Fetches the list of recent recordings and manual notes. | **Response** |
| **Record Button** (Mic) | `/api/v1/notes/process` | `POST` | Uploads audio file for transcription and analysis. | **Payload** (Multipart) |
| **Manual Note Dialog** | `/api/v1/notes/create` | `POST` | Creates a text-only note manually. | **Payload** |
| **Swipe/Toggle Actions** (Pin, Done) | `/api/v1/notes/{id}` | `PATCH` | Updates note status or priority. | **Payload** |
| **Wallet Balance** | `/api/v1/billing/wallet` | `GET` | Shows current credit balance. | **Response** |

## 3. Tasks Screen
**Route:** `Screen.Tasks` | **ViewModel:** `TasksViewModel`

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Tasks Due Today** | `/api/v1/tasks/due-today` | `GET` | Filters tasks with deadlines set for today. | **Response** |
| **Overdue Tasks** | `/api/v1/tasks/overdue` | `GET` | Shows missed deadlines. | **Response** |
| **All Tasks List** | `/api/v1/tasks` | `GET` | General task list with filtering support. | **Response** |
| **Task Completion** | `/api/v1/tasks/{id}` | `PATCH` | Marks a specific task as done/undone. | **Payload** |
| **Duplicate Task** | `/api/v1/tasks/{id}/duplicate` | `POST` | Clones an existing task. | **Payload** |

## 4. Note Detail Screen
**Route:** `detail/{noteId}` | **ViewModel:** `NoteDetailViewModel`

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Note Content** | `/api/v1/notes/{id}` | `GET` | Fetches full transcript, summary, and extracted tasks. | **Response** |
| **AI Chat** (Ask AI) | `/api/v1/notes/{id}/ask` | `POST` | Sends a question to the AI about the note context. | **Payload** |
| **WhatsApp Share** | `/api/v1/notes/{id}/whatsapp` | `GET` | Generates a formatted message draft for sharing. | **Response** |
| **Re-analyze Button** | `/api/v1/notes/{id}/semantic-analysis` | `POST` | Triggers a fresh AI analysis of the transcript. | **Payload** |

## 5. Deep AI (Settings) Screen
**Route:** `Screen.Settings` | **ViewModel:** `SettingsViewModel`

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Profile Settings** | `/api/v1/users/me` | `PATCH` | Updates system prompts, work hours, and jargon. | **Payload** |
| **Logout** | `/api/v1/users/logout` | `POST` | Invalidates the session token. | **Payload** |
| **Delete Account** | `/api/v1/users/me` | `DELETE` | Removes user data (Soft/Hard). | **Payload** |

## 6. Integration & Utilities

| Feature | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Join Meeting** | `/api/v1/meetings/join` | `POST` | Dispatches bot to a URL (Zoom/Meet/Teams). | **Payload** |
| **Global Search** | `/api/v1/notes/search` | `POST` | Semantic search across notes. | **Payload** |
| **Global Search** | `/api/v1/tasks/search` | `GET` | Keyword search across task descriptions. | **Response** |
| **Real-time Sync** | `/api/ws/sync` | `WS` | Receives live updates on note processing status. | **Stream** |
