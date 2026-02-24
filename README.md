# 🔐 Secure AI-Based Voice Communication System  
**Android Frontend + Python Backend**

---

## 📌 Project Overview

This project implements a **Secure AI-Based Voice Communication System** that demonstrates how voice messages can be:

- securely transmitted  
- encrypted using modern cryptography  
- processed using AI models  
- analyzed for urgency and harmful intent  

The system is developed as an **academic prototype** and is **fully demo-ready**, emphasizing:

- correctness  
- secure design  
- AI integration  
- explainability  

rather than large-scale production deployment.

---

## 🚀 Current Version

**Version:** `v1.0-stable-demo`

### ✔ Stable Features

- End-to-end encrypted voice messaging (X25519 + AES-256-GCM)
- Encrypted storage (no plaintext audio at rest)
- Speech enhancement pipeline
- Speech-to-text using Whisper
- AI message classification (SAFE / URGENT / HARMFUL)
- Priority persistence in metadata
- Idempotent transcription (no recomputation)
- Unread/read state tracking
- Priority-aware chat sorting
- Urgent / Harmful visual indicators
- Secure backend processing

---

## 🧩 System Components

### 📱 Android Frontend

- WhatsApp-style chat interface
- Text and voice messaging
- Voice recording using microphone
- Secure upload of encrypted audio
- AI priority alerts (Urgent / Harmful)
- Unread message tracking
- Priority-based chat ordering
- No cryptographic secrets stored on device

---

### 🖥️ Python Backend

- X25519 secure key exchange
- AES-256-GCM voice encryption
- Encrypted audio storage
- Controlled decryption pipeline
- Neural-assisted speech enhancement
- Speech-to-Text (Whisper)
- AI classification engine
- Persistent message metadata
- FastAPI REST architecture

---

## 🏗️ System Architecture

```
Android App
│
├── Text Message
│
├── Voice Recording (.wav)
│       ↓
│   Secure Upload
│       ↓
│
Backend Server
│
├── X25519 Key Exchange
├── AES-256-GCM Encryption
├── Encrypted Storage
├── Controlled Decryption
├── Speech Enhancement
├── Speech-to-Text (Whisper)
├── AI Classification
│
└── SAFE / URGENT / HARMFUL
```

---

## 🛠️ Technology Stack

### Frontend

| Component   | Technology        |
|------------|------------------|
| Platform   | Android           |
| Language   | Kotlin            |
| UI         | XML Layouts       |
| Audio      | AudioRecord       |
| Networking | HttpURLConnection |
| Async      | Kotlin Coroutines |

---

### Backend

| Component          | Technology              |
|-------------------|--------------------------|
| Framework         | FastAPI                  |
| Cryptography      | X25519, AES-256-GCM      |
| Speech Recognition| Whisper (pretrained)     |
| NLP               | BERT (pretrained)        |
| Classification    | Rule + Lightweight AI    |
| Language          | Python                   |

---

## 📂 Repository Structure

```
Secure_AI_Based_Voice_Communication/
│
├── backend/
│   ├── app/
│   ├── storage/ (auto-generated)
│   ├── requirements.txt
│   └── README.md
│
├── frontend/ (Android)
│   ├── app/
│   ├── build.gradle
│   └── README.md
│
├── .gitignore
└── README.md
```

> Runtime data (`venv`, encrypted audio, keys) are excluded intentionally.

---

## ⚙️ Setup Instructions

### 🔹 Prerequisites

- Python 3.9+
- Android Studio (Giraffe / Hedgehog or newer)
- Android Emulator (recommended)
- Internet connection (first install only)

---

## ▶️ Clone Repository

```bash
git clone https://github.com/ShifanaKoormath/Secure_voice_communication.git
cd SecureVoiceChat
```

---

## ▶️ Backend Setup

```bash
cd backend
python -m venv venv
venv\Scripts\activate   # Windows
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Backend:

```
http://127.0.0.1:8000
http://127.0.0.1:8000/docs
```

---

## ▶️ Frontend Setup

1. Open Android Studio  
2. Open **frontend project folder**  
3. Wait for Gradle sync  
4. Run on emulator  

**Backend URL (Emulator):**

```
http://10.0.2.2:8000
```

---

## 🎤 Demo Flow

### Login

Use:

| User | Password |
|------|----------|
| userA | 1234 |
| userB | 1234 |
| userC | 1234 |
| userD | 1234 |

---

### Text Messaging

- Send message → appears instantly
- Receiver sees unread indicator
- Opening chat marks as read
- Urgent/Harmful messages trigger alerts

---

### Voice Messaging

1. Tap 🎤 → Record
2. Stop → Upload encrypted audio
3. Tap message → Enhanced playback
4. Automatic transcription + classification
5. Priority saved in metadata
6. Alert shown if URGENT / HARMFUL

---

## 🔐 Security Highlights

- No plaintext audio stored
- AES keys derived via X25519
- Controlled server-side decryption
- Android stores no secret keys
- Encrypted communication pipeline
- Secure metadata persistence

---

## 📊 AI Classification

| Priority | Description |
|----------|-------------|
| SAFE     | Normal message |
| URGENT   | Emergency / immediate attention |
| HARMFUL  | Threat / abusive content |

Classification uses:

- Rule-based detection
- Keyword logic
- Lightweight BERT signal

---

## 🚧 Limitations (Academic Scope)

- Simulated authentication
- No real-time streaming
- Prototype AI models
- Local storage (no database)
- Not production-scaled

Focus is on **secure system integration**.

---

## 🔮 Future Enhancements

- Real-time urgent alerts
- Admin monitoring dashboard
- Push notification simulation
- Improved harmful detection
- Secure key exchange UI
- Deployment containerization

---

## 📄 For Evaluators

This project demonstrates:

- Secure communication design  
- AI-assisted message intelligence  
- Encryption + AI integration  
- Persistent message state  
- Explainable architecture  

All runtime data and keys are excluded intentionally.

---

## 📜 License

For **academic and research demonstration only**.

---