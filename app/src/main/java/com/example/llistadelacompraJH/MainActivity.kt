package com.example.llistadelacompraJH

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.example.llistadelacompraJH.ui.screens.HomeScreen
import com.example.llistadelacompraJH.ui.screens.LoginScreen
import com.example.llistadelacompraJH.ui.theme.LlistaDeLaCompraJHTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LlistaDeLaCompraJHTheme {
                LlistaDeLaCompraJHApp()
            }
        }
    }
}

@Composable
fun LlistaDeLaCompraJHApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var user by remember(auth) { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val signedInUser = user
    if (signedInUser == null) {
        LoginScreen(auth)
    } else {
        key(signedInUser.uid) {
            ShoppingListsApp(signedInUser.uid, signedInUser.email) {
                auth.signOut()
                scope.launch {
                    try {
                        CredentialManager.create(context)
                            .clearCredentialState(ClearCredentialStateRequest())
                    } catch (error: ClearCredentialException) {
                        Log.w("ShoppingLists", "Could not clear the credential provider state", error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListsApp(userId: String, email: String?, onSignOut: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(userId, modifier = Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> Column(
                    modifier = Modifier.padding(innerPadding).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    email?.let { Text(it) }
                    OutlinedButton(onClick = onSignOut) { Text("Cerrar sesión") }
                }
                AppDestinations.FAVORITES -> Greeting(
                    name = currentDestination.label,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Pantalla $name",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LlistaDeLaCompraJHTheme {
        Greeting("Android")
    }
}
