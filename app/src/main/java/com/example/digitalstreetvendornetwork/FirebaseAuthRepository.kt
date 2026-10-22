package com.example.digitalstreetvendornetwork

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuthRepository
 *
 * Single source of truth for all Firebase Auth and Firestore user operations.
 * All public functions are suspend functions — call from a coroutine scope
 * (e.g. LaunchedEffect + rememberCoroutineScope in the calling composable).
 *
 * Firestore schema:
 *   /users/{uid}
 *     name:  String
 *     phone: String
 *     role:  "VENDOR" | "CUSTOMER"
 */
class FirebaseAuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /** The currently signed-in Firebase user, or null. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** True when there is an active Firebase session. */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // ── Sign-up ───────────────────────────────────────────────────────────────

    /**
     * Creates a new Firebase Auth user with email (derived from phone) + password,
     * then writes the user profile document to /users/{uid}.
     *
     * @param name      Display name collected from the signup form.
     * @param phone     10-digit phone number; used as email prefix → "<phone>@localvends.app".
     * @param password  Plain-text password entered by the user.
     * @param role      Either "VENDOR" or "CUSTOMER".
     * @return          The role string on success.
     * @throws          Exception (FirebaseAuthException / FirebaseFirestoreException) on failure.
     */
    suspend fun signUp(name: String, phone: String, password: String, role: String): String {
        val email = phoneToEmail(phone)

        // 1. Create the Auth user
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("Firebase Auth returned a null UID after signup")

        // 2. Persist the profile document
        val profile = mapOf(
            "name" to name,
            "phone" to phone,
            "role" to role
        )
        db.collection("users").document(uid).set(profile).await()

        return role
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Signs in with email+password and reads the stored role from Firestore.
     *
     * @param phone    10-digit phone number used at registration.
     * @param password Plain-text password.
     * @return         The stored role string ("VENDOR" or "CUSTOMER").
     * @throws         Exception on authentication failure or missing Firestore document.
     */
    suspend fun login(phone: String, password: String): String {
        val email = phoneToEmail(phone)

        // 1. Sign in
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("Firebase Auth returned a null UID after login")

        // 2. Fetch the role from Firestore
        return fetchRole(uid)
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /** Signs the current user out of Firebase Auth. */
    fun logout() {
        auth.signOut()
    }

    // ── Role fetch ────────────────────────────────────────────────────────────

    /**
     * Reads the role field for the given UID from /users/{uid}.
     * Useful on app cold-start to restore session without re-authenticating.
     *
     * @return The stored role string, or "CUSTOMER" as a safe default.
     */
    suspend fun fetchRole(uid: String): String {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.getString("role") ?: "CUSTOMER"
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Firebase Auth requires an email address.
     * We synthesize one from the phone number so the UX stays phone-centric.
     */
    private fun phoneToEmail(phone: String): String = "$phone@localvends.app"
}
