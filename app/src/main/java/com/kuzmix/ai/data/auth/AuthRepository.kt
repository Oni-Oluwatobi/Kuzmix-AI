package com.kuzmix.ai.data.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.kuzmix.ai.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase email/password + Google authentication, plus the
 * `users/{uid}` Firestore profile (class, term, name, email).
 *
 * Design notes:
 * - `Task` results are awaited with a hand-rolled [await] extension so no
 *   extra coroutines-play-services dependency is needed.
 * - Profile writes are best-effort: a failed/offline Firestore write never
 *   breaks login or quiz flow (the local preference copy always lands).
 * - The user's class/term also live in [UserPreferences] so the quiz wizard
 *   and profile screen work fully offline; Firestore is the cross-device
 *   mirror of those values.
 */
class AuthRepository(
    private val context: Context,
    private val preferences: UserPreferences
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val users get() = FirebaseFirestore.getInstance().collection("users")

    /**
     * Current Firebase user (null = signed out). Seeded synchronously from
     * [FirebaseAuth.getCurrentUser] so the nav gate never flashes, then
     * kept in sync by an AuthStateListener (covers sign-in, sign-out and
     * restore-from-disk on process death).
     */
    private val _authState = MutableStateFlow(auth.currentUser)
    val authState: StateFlow<FirebaseUser?> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { fb -> _authState.value = fb.currentUser }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isSignedIn: Boolean get() = auth.currentUser != null

    /** True when the device has a usable internet connection. */
    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ------------------------------------------------------------ auth ops

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        require(isOnline()) { OFFLINE_MESSAGE }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        pullProfile()
    }

    suspend fun createAccount(
        email: String,
        password: String,
        displayName: String
    ): Result<Unit> = runCatching {
        require(isOnline()) { OFFLINE_MESSAGE }
        val cred = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val name = displayName.trim().ifEmpty { email.substringBefore('@') }
        cred.user?.updateProfile(UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build())?.await()
        preferences.setUserName(name)
        preferences.setUserEmail(email.trim())
        // seed the cloud profile with whatever class/term this device already knows
        pushProfile(name = name, email = email.trim(), forceCreate = true)
    }

    /**
     * Google Sign-In: launches the account chooser from the caller, then
     * exchanges the returned idToken for a Firebase credential.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        require(isOnline()) { OFFLINE_MESSAGE }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        val user = auth.currentUser
        user?.displayName?.takeIf { it.isNotBlank() }?.let { preferences.setUserName(it) }
        user?.email?.takeIf { it.isNotBlank() }?.let { preferences.setUserEmail(it) }
        pullProfile()
    }

    /** Google sign-in intent, or null when no Web client ID is configured. */
    fun googleSignInIntent(): android.content.Intent? {
        val webClientId = FirebaseConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) return null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    /** Parses the account chooser result into an idToken (null = cancelled). */
    suspend fun googleIdTokenFrom(data: android.content.Intent?): Result<String> =
        runCatching {
            require(isOnline()) { OFFLINE_MESSAGE }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java).idToken
                ?: error("Google sign-in returned no id token")
        }

    fun signOut() {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut()          // local Google session too — next login shows the chooser
        auth.signOut()
    }

    // -------------------------------------------------- profile (Firestore)

    /**
     * Mirrors class/term to `users/{uid}` after the quiz wizard pins them.
     * Fire-and-forget: login and quiz flow never wait on the network.
     */
    fun saveClassTerm(classKey: String?, className: String?, term: Int?) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf<String, Any?>(
            "classKey" to classKey,
            "className" to className,
            "term" to term,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        users.document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "class/term sync failed", it) }
    }

    /** Reads `users/{uid}` into local prefs after login (best effort). */
    private suspend fun pullProfile() {
        val uid = auth.currentUser?.uid ?: return
        if (!isOnline()) return
        try {
            val snap = users.document(uid).get().await()
            if (!snap.exists()) {
                pushProfile(
                    name = auth.currentUser?.displayName ?: "",
                    email = auth.currentUser?.email ?: "",
                    forceCreate = true
                )
                return
            }
            val classKey = snap.getString("classKey")
            val term = snap.getLong("term")?.toInt()
            val name = snap.getString("name")
            val email = snap.getString("email")
            if (!classKey.isNullOrBlank()) {
                preferences.setSavedClass(classKey, snap.getString("className"), term)
            }
            if (!name.isNullOrBlank()) preferences.setUserName(name)
            if (!email.isNullOrBlank()) preferences.setUserEmail(email)
        } catch (t: Throwable) {
            Log.w(TAG, "profile pull failed (offline?)", t)
        }
    }

    private suspend fun pushProfile(name: String, email: String, forceCreate: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf<String, Any?>(
            "name" to name,
            "email" to email,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        // class/term are optional — a brand-new account may not have taken a quiz yet
        preferences.peekSavedClass()?.let { saved ->
            data["classKey"] = saved.classKey
            data["className"] = saved.className
            data["term"] = saved.term
        }
        if (forceCreate) data["createdAt"] =
            com.google.firebase.firestore.FieldValue.serverTimestamp()
        users.document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "profile push failed", it) }
    }

    // -------------------------------------------------------------- errors

    /** Maps Firebase auth failures to student-friendly copy. */
    fun readableError(t: Throwable?): String = when (t) {
        null -> "Something went wrong. Please try again."
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        is FirebaseAuthInvalidUserException -> "No account found with that email address."
        is FirebaseAuthUserCollisionException -> "An account already exists with that email."
        is FirebaseFirestoreException ->
            "Couldn't reach the server. Check your connection and try again."
        else -> {
            val msg = t.message.orEmpty()
            when {
                msg.contains(OFFLINE_MESSAGE) -> OFFLINE_MESSAGE
                msg.contains("network", true) || msg.contains("unable to resolve", true) ->
                    "No internet connection. Connect to a network and try again."
                msg.contains("password", true) -> "Wrong email or password."
                else -> "Couldn't sign you in. Check your details and try again."
            }
        }
    }

    companion object {
        const val OFFLINE_MESSAGE =
            "You're offline. Connect to the internet to log in or create an account."
        private const val TAG = "AuthRepository"
    }
}

/**
 * Minimal Task awaiter — keeps us off kotlinx-coroutines-play-services.
 * Mirrors the well-known extension: resume with the result or the failure.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                cont.resume(task.result as T)
            } else {
                cont.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed")
                )
            }
        }
    }
