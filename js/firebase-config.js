// firebase-config.js — Firebase Web SDK initialization
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyBwaLoDfISxU3JQvXSsako4F2fyVaGwCrY",
  authDomain: "preboardexam-checker.firebaseapp.com",
  projectId: "preboardexam-checker",
  storageBucket: "preboardexam-checker.firebasestorage.app",
  messagingSenderId: "239567967479",
  appId: "1:239567967479:android:2abbc4c3c6f141ebe5aff2"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
