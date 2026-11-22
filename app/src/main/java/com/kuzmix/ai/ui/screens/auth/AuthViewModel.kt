package com.kuzmix.ai.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, CREATE_ACCOUNT }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val online: Boolean = true,
    /** False until the first connectivity check lands. */
    val checkedOnline: Boolean = false,
    /** Google button only shows when a Web client ID is configured. */
    val googleAvailable: Boolean = false,
    /** True once Firebase confirms a user — the nav gate flips to the app. */
    val signedIn: Boolean = false
)

/**
 * Login / create-account form state. All Firebase calls are suspend and
 * awaited here, so the screen only renders [AuthUiState].
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                googleAvailable = authRepository.googleSignInIntent() != null,
                signedIn = authRepository.isSignedIn
            )
        }
        refreshConnectivity()
        // mirrors sign-out from the profile screen back into the gate
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                _state.update { s -> s.copy(signedIn = user != null) }
            }
        }
    }

    fun refreshConnectivity() {
        _state.update {
            it.copy(online = authRepository.isOnline(), checkedOnline = true)
        }
    }

    fun setMode(mode: AuthMode) =
        _state.update { it.copy(mode = mode, error = null) }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val s = _state.value
        val email = s.email.trim()
        val password = s.password
        val error = when {
            !s.online -> AuthRepository.OFFLINE_MESSAGE
            !EMAIL_REGEX.matches(email) -> "Enter a valid email address."
            password.length < MIN_PASSWORD -> "Password must be at least $MIN_PASSWORD characters."
            s.mode == AuthMode.CREATE_ACCOUNT && s.name.isBlank() -> "Enter your name."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = if (s.mode == AuthMode.CREATE_ACCOUNT) {
                authRepository.createAccount(email, password, s.name)
            } else {
                authRepository.signIn(email, password)
            }
            _state.update { st ->
                st.copy(
                    loading = false,
                    signedIn = result.isSuccess,
                    error = result.exceptionOrNull()?.let { t -> authRepository.readableError(t) }
                )
            }
        }
    }

    /** Continues Google Sign-In from the account-chooser result. */
    fun signInWithGoogle(data: android.content.Intent?) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val token = authRepository.googleIdTokenFrom(data)
            val result = token.fold(
                onSuccess = { id -> authRepository.signInWithGoogle(id) },
                onFailure = { t ->
                    // a cancelled chooser is not an error worth showing
                    if (t is com.google.android.gms.common.api.ApiException &&
                        t.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                    ) {
                        Result.success(Unit)
                    } else {
                        Result.failure(t)
                    }
                }
            )
            _state.update { st ->
                st.copy(
                    loading = false,
                    signedIn = result.isSuccess,
                    error = if (result.isSuccess) null
                    else authRepository.readableError(result.exceptionOrNull())
                )
            }
        }
    }

    companion object {
        private const val MIN_PASSWORD = 6
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
