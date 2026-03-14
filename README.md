# PBEC — PreBoard Exam Checker Portal

A powerful, web-based management system for the PreBoard Exam Checker (PBEC) platform. This portal allows administrators and teachers to manage student records, maintain a comprehensive question bank, generate exams, and analyze results.

## 🚀 Getting Started

Since this project uses **ES Modules** for its logic, it must be served via a local web server to function correctly in modern browsers (due to security restrictions on the `file://` protocol).

### 1. Using VS Code (Highly Recommended)
1. Install the **Live Server** extension.
2. Open this folder in VS Code.
3. Click **"Go Live"** in the bottom-right corner of the status bar.

### 2. Using Python
If you have Python installed, run this command in your terminal:
```bash
# Python 3.x
python3 -m http.server 8000
```
Then visit `http://localhost:8000` in your browser.

### 3. Using Node.js (npm)
If you have Node.js installed, you can use the built-in scripts:
```bash
npm install
npm run dev
```
This will start a local server at `http://localhost:3000`.

---

## 🛠 Tech Stack

- **Frontend**: Vanilla HTML5, CSS3 (Custom Design System), and JavaScript (ES6+).
- **Backend**: [Firebase](https://firebase.google.com/) (Authentication & Firestore).
- **Icons**: Emoji-based iconography for a lightweight, modern feel.
- **Components**: Custom-built shell, sidebar, and data tables.

---

## 📂 Project Structure

```text
web/
├── admin/            # Admin-only pages (Students, Question Bank, etc.)
├── teacher/          # Teacher-specific pages and dashboards
├── css/              # Global styles and design system tokens
├── js/               # Core logic (Auth, Firebase config, UI utilities)
├── index.html        # Main login portal
└── README.md         # You are here!
```

## 🗄️ Database Initialization

If your Firestore database is empty, you can automatically create the necessary collections and add sample data:
1. Start your local server (`npm run dev`).
2. Visit `http://localhost:3000/setup.html` in your browser.
3. Click **"Initialize Collections"**.

---

## 🔑 Authentication

Access is restricted based on user roles defined in Firestore:
- **Admin**: Full access to all management tools.
- **Teacher**: Access to exam tools and student summaries.

Authentication is handled via **Firebase Auth**. Configuration is set to the `preboardexam-checker` project and can be found in `js/firebase-config.js`.

---

## 🌟 Key Features

- **Dashboard**: High-level overview of system stats.
- **Student Management**: Full CRUD operations for student records.
- **Question Bank**: Centralized repository for exam questions.
- **Exam Generator**: Automated tool to create pre-board exams.
- **Analytics**: Performance tracking and summary reports.

---

© 2026 PBEC — CATCI (Legazpi • Ligao • Polangui)
