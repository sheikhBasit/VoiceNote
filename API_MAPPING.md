# VoiceNote App: UI to API Mapping

This document details how the Android frontend interacts with the FastAPI backend endpoints, along with a preview of the UI/UX for each screen.

## 0. Onboarding & Security
**ViewModel:** `MainActivity` / `SecurityManager`

### UI Preview & Interaction
- **Visuals:** Shifting mesh gradients (Primary Cyan & Violet) with a pulsing "AutoAwesome" AI branding icon.
- **Login:** A minimalist `GlassyTextField` for email with real-time validation (Green Tick appears upon success).
- **Security:** Professional Biometric prompt triggers automatically on entry. Includes a "Trust Footer" explicitly stating "End-to-End Encryption."

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Login Screen** | `/api/v1/users/sync` | `POST` | Primary entry point. Auth by email and registers/authorizes the device. | **Payload** |
| **Magic Link Flow** | `/api/v1/users/verify-device` | `GET` | Verifies device using token from magic link. | **Response** |

## 1. Pulse (Dashboard) Screen
**Route:** `Screen.Dashboard` | **ViewModel:** `DashboardViewModel`

### UI Preview & Interaction
- **Greeting:** Personalized "Welcome back, [Name]" extracted from the user's email.
- **Metrics:** High-level stats cards showing Total, Completed, and Pending tasks with trending percentages.
- **Pulse:** A dynamic "Topic Heatmap" showing analyzed discussion themes and a "Productivity Velocity" headline.
- **Context:** An AI-generated greeting at the top providing a smart summary of the user's current workload.

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Stats Cards** | `/api/v1/tasks/stats` | `GET` | Fetches task completion rates, counts, and priority distribution. | **Response** |
| **Productivity Flow** | `/api/v1/notes/dashboard` | `GET` | Fetches task velocity and heatmap data for topic trends. | **Response** |
| **AI Suggestions** | `/api/v1/ai/stats` | `GET` | Fetches high-priority task alerts and AI-generated work suggestions. | **Response** |

## 2. Notes (Home) Screen
**Route:** `Screen.Notes` | **ViewModel:** `HomeViewModel`

### UI Preview & Interaction
- **Activity:** A "Recent Intel" scrollable list showing notes with AI-summarized snippets and processing status.
- **Capture:** A large, central pulsing Microphone button that triggers the high-fidelity recording service.
- **Manual:** A "Glass Card" floating dialog for creating quick text-only notes without voice input.
- **Wallet:** A quick-access credit balance shown in the header to track AI analysis remaining.

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Profile Section** | `/api/v1/users/me` | `GET` | Displays user name and role. | **Response** |
| **Recent Notes List** | `/api/v1/notes` | `GET` | Lists recent notes with transcription summaries. | **Response** |
| **Mic Button** | `/api/v1/notes/voice` | `POST` | Uploads audio for AI transcription and analysis. | **Payload** (Multipart) |
| **Add Note Dialog** | `/api/v1/notes/create` | `POST` | Manually creates a text-only note. | **Payload** |
| **Note State** (Pin/Done) | `/api/v1/notes/{id}` | `PATCH` | Updates metadata (is_pinned, status, is_deleted). | **Payload** |
| **Wallet Quick-Check**| `/api/v1/billing/wallet` | `GET` | Shows remaining credits in the Home header. | **Response** |

## 3. Tasks Screen
**Route:** `Screen.Tasks` | **ViewModel:** `TasksViewModel`

### UI Preview & Interaction
- **Organization:** Filter tabs for "All", "Due Today", "Overdue", and "Assigned to Me."
- **List:** Task cards featuring color-coded priority badges (Red/Yellow/Green) and countdown deadline timers.
- **Actions:** Long-press for multi-selection mode (Bulk Delete/Complete). Swipe to quickly archive tasks.
- **Search:** A "V-RAG" powered search bar at the top for querying specific action items across the entire brain.

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Today's Priorities** | `/api/v1/tasks/due-today` | `GET` | Filters action items due in the next 24 hours. | **Response** |
| **Overdue Alerts** | `/api/v1/tasks/overdue` | `GET` | Highlights missed deadlines. | **Response** |
| **Task Board List** | `/api/v1/tasks` | `GET` | General listing with filtering (note_id, priority, me). | **Response** |
| **Task Toggle** | `/api/v1/tasks/{id}` | `PATCH` | Marks tasks as complete or updates description. | **Payload** |
| **Duplicate Button** | `/api/v1/tasks/{id}/duplicate` | `POST` | Clones a task for repeated action items. | **Payload** |

## 4. Note Detail Screen
**Route:** `detail/{noteId}` | **ViewModel:** `NoteDetailViewModel`

### UI Preview & Interaction
- **Content:** The screen is split into a "Read Mode" (AI Summary) and "Analyze Mode" (Full Transcript).
- **Intelligence:** A floating chat interface ("Ask V-RAG") at the bottom for contextual questions about the note.
- **Extraction:** A dedicated section listing all AI-detected tasks specific to this recording.
- **Integration:** One-tap sharing to WhatsApp with a pre-formatted message draft.

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Full Content** | `/api/v1/notes/{id}` | `GET` | Retrieves full transcript, summary, and extracted tasks. | **Response** |
| **V-RAG Chat** | `/api/v1/notes/{id}/ask` | `POST` | Asks the AI a question restricted to the note's context. | **Payload** |
| **WhatsApp Action** | `/api/v1/notes/{id}/whatsapp` | `GET` | Fetches a pre-formatted draft for external sharing. | **Response** |
| **Re-Analyze UI** | `/api/v1/notes/{id}/semantic-analysis` | `POST` | Force-triggers a new AI pass over the transcript. | **Payload** |

## 5. Billing & Wallet Screen
**Route:** `billing` | **ViewModel:** `BillingViewModel`

### UI Preview & Interaction
- **Balance:** A glowing Glass Card displaying the current "AI Credits" remaining.
- **Refill:** Clean pricing cards for purchasing credit bundles (e.g., 500 Credits / $5.00).
- **Ledger:** A "Ledger History" list showing every credit deduction (Analysis) and addition (Top-up).

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Balance Card** | `/api/v1/billing/wallet` | `GET` | Displays current credit balance. | **Response** |
| **Ledger History** | `/api/v1/billing/wallet` | `GET` | Lists recent transactions (usage/refills). | **Response** |
| **Top-up Actions** | `/api/v1/billing/checkout` | `POST` | Generates a Stripe session URL for buying credits. | **Payload** |
| **Credit funding** | `/api/v1/billing/webhook` | `POST` | (Backend Internal) Processes successful payments. | **Payload** |

## 6. Deep AI (Settings) Screen
**Route:** `Screen.Settings` | **ViewModel:** `SettingsViewModel`

### UI Preview & Interaction
- **Identity:** Displays user profile details and role management.
- **Configuration:** Segmented controls for choosing between AI providers (Whisper, Google, Deepgram).
- **Privacy:** Toggles for "FaceID Protection" and "On-Device Mode" (Offline processing).
- **Work Context:** Inputs for customizing "Work Hours" and "Industry Jargon" to improve AI analysis.

| UI Component | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **Work Context** | `/api/v1/users/me` | `PATCH` | Updates role, work hours, and domain-specific jargon. | **Payload** |
| **Account Destruction**| `/api/v1/users/me` | `DELETE` | Removes user profile (Supports hard/soft delete). | **Payload** |

## 7. Real-time & Global Services

### UI Preview & Interaction
- **Diagnostics:** The **STT Logs** page provides a raw developer-style view of the local transcription pipeline and status.
- **Search:** The **Global Search** screen uses a full-screen layout with instant keyboard popup for lightning-fast queries.

| Feature | Endpoint | Method | Purpose | Data Type |
| :--- | :--- | :--- | :--- | :--- |
| **STT Logs** | `LOCAL ONLY` | `N/A` | Displays local transcription cache and service status. | **Internal State** |
| **Join Meeting** | `/api/v1/meetings/join` | `POST` | Dispatches bot to external meeting (Zoom/Meet). | **Payload** |
| **Bot Status** | `/api/v1/meetings/{id}/status` | `GET` | Checks if bot is recording or idle. | **Response** |
| **OmniSearch (Notes)**| `/api/v1/notes/search` | `POST` | Semantic search across the user's brain. | **Payload** |
| **OmniSearch (Tasks)**| `/api/v1/tasks/search` | `GET` | Keyword search across all tasks. | **Response** |
| **Live Updates** | `/api/ws/sync` | `WS` | Receives live analysis progress and task notifications. | **Stream** |
