package com.example.llistadelacompraJH.ui.screens

import android.content.MutableContextWrapper
import android.util.Patterns
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.llistadelacompraJH.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(auth: FirebaseAuth) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val action = if (isRegistering) "Crear cuenta" else "Iniciar sesión"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Llista de la compra JH",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = action,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isSubmitting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("Contraseña") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isSubmitting,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && email.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    val address = email.trim()
                    when {
                        !Patterns.EMAIL_ADDRESS.matcher(address).matches() ->
                            error = "Introduce un correo electrónico válido."
                        isRegistering && password.length < 6 ->
                            error = "Usa una contraseña de al menos 6 caracteres."
                        else -> {
                            isSubmitting = true
                            error = null
                            val task = if (isRegistering) {
                                auth.createUserWithEmailAndPassword(address, password)
                            } else {
                                auth.signInWithEmailAndPassword(address, password)
                            }
                            task.addOnFailureListener {
                                error = authErrorMessage(it)
                                isSubmitting = false
                            }
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Un momento…" else action)
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && activity != null,
                onClick = {
                    val host = activity ?: return@OutlinedButton
                    isSubmitting = true
                    error = null
                    scope.launch {
                        val token = try {
                            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(option)
                                .build()
                            val credential = CredentialManager.create(host).getCredential(
                                context = MutableContextWrapper(host),
                                request = request
                            ).credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                            } else {
                                error = "Google no devolvió una credencial válida."
                                null
                            }
                        } catch (_: GetCredentialCancellationException) {
                            null
                        } catch (_: NoCredentialException) {
                            error = "No se encontró una cuenta de Google disponible."
                            null
                        } catch (_: GetCredentialException) {
                            error = "No se pudo abrir el acceso con Google. Revisa la conexión y la configuración de Firebase."
                            null
                        } catch (_: GoogleIdTokenParsingException) {
                            error = "Google no devolvió una credencial válida."
                            null
                        }
                        if (token == null) {
                            isSubmitting = false
                        } else {
                            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                                .addOnFailureListener {
                                    error = googleAuthErrorMessage(it)
                                    isSubmitting = false
                                }
                        }
                    }
                }
            ) {
                Text("Continuar con Google")
            }
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !isSubmitting,
                onClick = {
                    isRegistering = !isRegistering
                    password = ""
                    error = null
                }
            ) {
                Text(if (isRegistering) "Volver a iniciar sesión" else "Crear una cuenta")
            }
        }
    }
}

private fun googleAuthErrorMessage(error: Exception): String {
    return when {
        error is FirebaseNetworkException -> "Comprueba tu conexión a Internet y vuelve a intentarlo."
        (error as? FirebaseAuthException)?.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "Ese correo ya tiene una cuenta. Inicia sesión con el método que usaste al registrarte."
        else -> "Firebase no pudo iniciar sesión con Google. Inténtalo de nuevo."
    }
}

private fun authErrorMessage(error: Exception): String {
    if (error is FirebaseNetworkException) {
        return "Comprueba tu conexión a Internet y vuelve a intentarlo."
    }
    return when ((error as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> "Introduce un correo electrónico válido."
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
            "El correo electrónico o la contraseña son incorrectos."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo electrónico."
        "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" ->
            "La contraseña no cumple los requisitos de seguridad."
        "ERROR_OPERATION_NOT_ALLOWED" -> "El acceso con correo y contraseña está desactivado en Firebase."
        "ERROR_ADMIN_RESTRICTED_OPERATION" -> "Firebase no permite crear cuentas desde la app."
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento y vuelve a intentarlo."
        else -> "No se ha podido completar la solicitud. Inténtalo de nuevo."
    }
}
