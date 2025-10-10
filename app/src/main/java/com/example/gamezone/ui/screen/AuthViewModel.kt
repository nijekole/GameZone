package com.example.gamezone.ui.screen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.gamezone.ui.helpers.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.app.Application
import androidx.lifecycle.AndroidViewModel

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState = _registrationState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    private val _fullName = MutableStateFlow("")
    val fullName = _fullName.asStateFlow()
    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    fun onUsernameChange(username: String) { _username.value = username }
    fun onEmailChange(email: String) { _email.value = email }
    fun onPasswordChange(password: String) { _password.value = password }
    fun onFullNameChange(name: String) { _fullName.value = name }
    fun onPhoneChange(phone: String) { _phone.value = phone }
    fun onImageUriChange(uri: Uri?) {
        _imageUri.value = uri
        Log.d("AuthViewModel", "Slika odabrana: $uri")
    }

    fun registerUser() {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            Log.d("AuthViewModel", "Početak registracije...")

            try {

                if (_username.value.isBlank() || _email.value.isBlank() || _password.value.isBlank() ||
                    _fullName.value.isBlank() || _phone.value.isBlank()) {
                    _registrationState.value = RegistrationState.Error("Molimo popunite sva polja")
                    return@launch
                }

                val usernameExists = db.collection("users").whereEqualTo("username", _username.value).get().await().isEmpty.not()
                if (usernameExists) {
                    _registrationState.value = RegistrationState.Error("Korisničko ime je već zauzeto.")
                    return@launch
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()) {
                    _registrationState.value = RegistrationState.Error("Unesite validan email")
                    return@launch
                }

                if (_password.value.length < 6) {
                    _registrationState.value = RegistrationState.Error("Šifra mora imati najmanje 6 karaktera")
                    return@launch
                }

                Log.d("AuthViewModel", "Kreiranje korisnika u Firebase Auth...")
                val authResult = auth.createUserWithEmailAndPassword(_email.value, _password.value).await()
                val user = authResult.user

                if (user == null) {
                    _registrationState.value = RegistrationState.Error("Greška pri kreiranju korisnika")
                    return@launch
                }

                Log.d("AuthViewModel", "Korisnik kreiran: ${user.uid}")

                var profileImageUrl: String? = null
                if (_imageUri.value != null) {
                    try {
                        Log.d("AuthViewModel", "Upload slike na Cloudinary... URI: ${_imageUri.value}")

                        profileImageUrl = CloudinaryUploader.uploadImage(
                            imageUri = _imageUri.value!!,
                            publicId = "profile_${user.uid}"
                        )

                        Log.d("AuthViewModel", "Slika uploadovana: $profileImageUrl")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Greška pri uploadu slike: ${e.message}", e)
                        profileImageUrl = null
                    }
                } else {
                    Log.d("AuthViewModel", "Nema slike za upload")
                }

                Log.d("AuthViewModel", "Čuvanje podataka u Firestore...")
                val userData = hashMapOf(
                    "username" to _username.value,
                    "fullName" to _fullName.value,
                    "phone" to _phone.value,
                    "email" to _email.value,
                    "points" to 0,
                    "profileImageUrl" to profileImageUrl,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(user.uid).set(userData).await()
                Log.d("AuthViewModel", "Podaci sačuvani u Firestore")

                _registrationState.value = RegistrationState.Success
                Log.d("AuthViewModel", "Registracija uspešna! State postavljen na Success")

                clearForm()

            } catch (e: FirebaseAuthException) {
                Log.e("AuthViewModel", "Firebase Auth greška: ${e.errorCode}", e)
                val errorMessage = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email adresa je već u upotrebi"
                    "ERROR_WEAK_PASSWORD" -> "Šifra je previše slaba"
                    "ERROR_INVALID_EMAIL" -> "Nevažeća email adresa"
                    else -> "Greška pri registraciji: ${e.message}"
                }
                _registrationState.value = RegistrationState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Opšta greška: ${e.message}", e)
                _registrationState.value = RegistrationState.Error(
                    e.message ?: "Došlo je do nepoznate greške."
                )
            }
        }
    }

    private fun clearForm() {
        _username.value = ""
        _email.value = ""
        _password.value = ""
        _fullName.value = ""
        _phone.value = ""
        _imageUri.value = null
    }

    fun resetRegistrationStateToIdle() {
        _registrationState.value = RegistrationState.Idle
    }

    fun loginUser() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (_username.value.isBlank() || _password.value.isBlank()) {
                    _loginState.value = LoginState.Error("Unesite korisničko ime i šifru")
                    return@launch
                }

                val querySnapshot = db.collection("users")
                    .whereEqualTo("username", _username.value)
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    _loginState.value = LoginState.Error("Korisnik sa tim korisničkim imenom ne postoji.")
                    return@launch
                }

                val userDoc = querySnapshot.documents.first()
                val email = userDoc.getString("email")

                if (email == null) {
                    _loginState.value = LoginState.Error("Greška u podacima, email nije pronađen.")
                    return@launch
                }

                auth.signInWithEmailAndPassword(email, _password.value).await()

                _loginState.value = LoginState.Success
                clearForm()

            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> "Pogrešna šifra"
                    "ERROR_USER_NOT_FOUND" -> "Došlo je do greške, pokušajte ponovo"
                    else -> "Greška pri prijavljivanju: ${e.message}"
                }
                _loginState.value = LoginState.Error(errorMessage)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Došlo je do nepoznate greške.")
            }
        }
    }

    fun resetLoginStateToIdle() {
        _loginState.value = LoginState.Idle
    }
}