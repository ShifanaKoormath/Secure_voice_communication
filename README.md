
README.md
```

---

# 🔐 Secure AI-Based Voice Communication System

**Android Frontend + Python Backend**

---

## 📌 Project Overview

This project implements a **Secure AI-Based Voice Communication System** that demonstrates how voice messages can be:

* securely transmitted
* encrypted using modern cryptography
* processed using AI models
* analyzed for harmful or urgent content

The system is built as an **academic prototype** and is **demo-ready**, focusing on correctness, security concepts, and explainability rather than production deployment.

---

## 🧩 System Components

### 1️⃣ Android Frontend

* WhatsApp-like chat interface
* Text and voice message support
* Voice recording using device microphone
* Secure transmission of audio to backend
* No encryption keys stored on device

### 2️⃣ Python Backend

* Secure key exchange using X25519
* AES-256-GCM encryption for voice data
* Encrypted storage (no plaintext audio at rest)
* Speech-to-text using pretrained ASR (Whisper)
* AI-based message classification
* REST API using FastAPI

---

## 🏗️ High-Level Architecture

```
Android App
│
├── Text Message
│
├── Voice Recording (.wav)
│       ↓
│   Secure Upload (HTTP)
│       ↓
│
Backend Server
│
├── X25519 Key Exchange
├── AES-256-GCM Encryption
├── Encrypted Storage
├── Decryption (controlled)
├── Speech-to-Text (ASR)
├── AI Classification
│
└── SAFE / HARMFUL / URGENT
```

---

## 🛠️ Technology Stack

### Frontend

| Component   | Technology        |
| ----------- | ----------------- |
| Platform    | Android           |
| Language    | Kotlin            |
| UI          | XML Layouts       |
| Audio       | AudioRecord       |
| Networking  | HttpURLConnection |
| Concurrency | Kotlin Coroutines |

### Backend

| Component          | Technology              |
| ------------------ | ----------------------- |
| Framework          | FastAPI                 |
| Cryptography       | X25519, AES-256-GCM     |
| Speech Recognition | Whisper (pretrained)    |
| NLP                | BERT (pretrained)       |
| Harm Detection     | CNN–LSTM inspired logic |
| Language           | Python                  |

---

## 📂 Repository Structure

```
Secure_AI_Based_Voice_Communication/
│
├── backend/
│   ├── app/
│   ├── requirements.txt
│   └── README.md
│
├── frontend/ (Android project root)
│   ├── app/
│   ├── build.gradle
│   └── README.md
│
├── .gitignore
└── README.md   ← (this file)
```

> Runtime data (`venv`, encrypted audio, keys) are intentionally excluded from version control.

---

## ⚙️ Setup Instructions (Complete System)

### 🔹 Prerequisites

* Python 3.9+
* Android Studio (Giraffe / Hedgehog or newer)
* Android Emulator (recommended) or physical device
* Internet connection (for first-time dependency installs)

---
✅Clone the Repository
git clone https://github.com/ShifanaKoormath/Secure_voice_communication.git


This will create a folder:

SECUREVOICECHAT


## ▶️ Backend Setup

### Step 1 — Navigate to backend

```bash
cd SecureVoiceChat\backend
```

### Step 2 — Create & activate virtual environment

```bash
python -m venv venv

# Windows
venv\Scripts\activate

# Linux / macOS
source venv/bin/activate
```

Make sure `(venv)` appears in the terminal.

---

### Step 3 — Install dependencies

```bash
pip install -r requirements.txt
```

Additional AI dependencies (if needed):

```bash
pip install torch torchaudio soundfile numpy scipy
```

---

### Step 4 — Run backend server

For Android Emulator:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Backend will be available at:

```
http://127.0.0.1:8000
http://127.0.0.1:8000/docs
```

---

## ▶️ Frontend Setup (Android)

### Step 1 — Open project

1. Open Android Studio
2. Select **Open**
3. Choose the Android frontend project folder
4. Wait for Gradle sync

---

### Step 2 — Backend connectivity (IMPORTANT)

For Android Emulator, the backend **must be accessed via**:

```
http://10.0.2.2:8000
```

This is already configured in the frontend code.

> ❌ Do NOT use `localhost` or `127.0.0.1` inside Android

---

### Step 3 — Run the app

1. Start Android Emulator
2. Click **Run ▶**
3. App launches

---

## 🎤 Voice Recording Permissions

* App requests **Microphone permission** at runtime
* Permission is required for voice messaging
* App safely handles permission denial (no crash)

---

## 🧪 Demo Flow (Recommended for Evaluation)

### 🔹 Text Messaging

1. Select **User A**
2. Open chat with **User B**
3. Send a text message
4. Switch to **User B**
5. Message appears

---

### 🔹 Voice Messaging

1. Open a chat
2. Tap 🎤 to start recording
3. Speak
4. Tap 🎤 again to stop
5. Voice is securely uploaded to backend
6. Backend encrypts and stores audio

This demonstrates **secure voice transmission**.

---

## 🔐 Security Highlights

* No plaintext audio stored on backend
* AES keys derived via X25519
* Encryption and decryption handled server-side
* Android client holds no cryptographic secrets
* Designed to demonstrate secure communication principles

---

## 🚧 Academic Scope & Limitations

* User authentication is simulated
* No real-time calling (asynchronous messaging only)
* AI models used in pretrained / prototype mode
* Focus is on **secure integration**, not large-scale training

---

## 🔮 Future Enhancements

* Enhanced voice playback in frontend
* Visual threat / urgency indicators
* On-device inference
* Real user authentication
* Push notifications
* Full model training

---

## 📄 Notes for Evaluators

* This project is an **academic prototype**
* Emphasis is on:

  * security
  * system design
  * integration
  * explainability
* Runtime data and keys are intentionally excluded from Git

---

## 📜 License

This project is intended **strictly for academic demonstration purposes**.

---
