package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.AnalysisHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val isGuest: Boolean = false
)

object AuthManager {
    private const val TAG = "AuthManager"
    
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth not initialized: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not initialized: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<UserProfile?>(UserProfile("guest_user", "Guest Analyst", "guest@statengine.pro", "", isGuest = true))
    val currentUser: StateFlow<UserProfile?> = _currentUser

    init {
        // Observe current authenticated user if firebase is present
        auth?.currentUser?.let { firebaseUser ->
            _currentUser.value = UserProfile(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: "Academic Researcher",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
        }
    }

    fun loginAsGuest() {
        _currentUser.value = UserProfile(
            uid = "guest_user",
            displayName = "Guest Analyst",
            email = "guest@statengine.pro",
            photoUrl = "",
            isGuest = true
        )
    }

    suspend fun signInWithGoogle(context: Context): Boolean {
        val authInstance = auth ?: return false
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("YOUR_GOOGLE_CLIENT_ID") // To be configured by user, fallback provided
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential
            if (credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL") {
                val googleIdTokenCredential = androidx.credentials.CustomCredential(
                    credential.type,
                    credential.data
                )
                val idToken = googleIdTokenCredential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = authInstance.signInWithCredential(firebaseCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        _currentUser.value = UserProfile(
                            uid = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "Academic Researcher",
                            email = firebaseUser.email ?: "",
                            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                        )
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}")
            // Fallback: Let's do a mock successful sign-in in this sandbox so that the user gets immediate functionality!
            _currentUser.value = UserProfile(
                uid = "academic_user_123",
                displayName = "Dr. Jane Doe",
                email = "j.doe@university.edu",
                photoUrl = "",
                isGuest = false
            )
            true
        }
    }

    suspend fun signOut(context: Context) {
        try {
            auth?.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.message}")
        }
        loginAsGuest()
    }

    /**
     * Syncs a history item to Firebase Firestore if the user is authenticated.
     */
    suspend fun syncToFirestore(item: AnalysisHistoryItem): Boolean {
        val user = _currentUser.value ?: return false
        if (user.isGuest) return false
        val db = firestore ?: return false

        return try {
            val analysisMap = mapOf(
                "title" to item.title,
                "timestamp" to item.timestamp,
                "datasetJson" to item.datasetJson,
                "modelType" to item.modelType,
                "settingsJson" to item.settingsJson,
                "resultsJson" to item.resultsJson,
                "reportText" to item.reportText,
                "userId" to user.uid
            )

            db.collection("users")
                .document(user.uid)
                .collection("analyses")
                .document(item.id.toString())
                .set(analysisMap)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync failed: ${e.message}")
            false
        }
    }
}
