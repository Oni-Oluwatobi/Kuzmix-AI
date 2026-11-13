import { initializeApp, getApps, getApp } from "firebase/app";
import { getAnalytics, isSupported } from "firebase/analytics";
import {
  getAuth,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  type User
} from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBfR---jcYvNMQ18MvgvbRUdQtvZCLl4VA",
  authDomain: "kuzmix-ai.firebaseapp.com",
  projectId: "kuzmix-ai",
  storageBucket: "kuzmix-ai.firebasestorage.app",
  messagingSenderId: "225186319331",
  appId: "1:225186319331:web:fd6ab7d2ca2c0ff9892ed6",
  measurementId: "G-YH49W8MQ0H"
};

const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
export const auth = getAuth(app);

// Graceful analytics check for offline/dev environments
export const initAnalytics = async () => {
  if (typeof window !== "undefined" && (await isSupported())) {
    return getAnalytics(app);
  }
  return null;
};

export {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  type User
};
