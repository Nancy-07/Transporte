package com.example.transporte

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Usuario ya autenticado, redirigir a MainActivity
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("especificaciones").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userType = document.getString("type")
                        if (userType == "Usuario" || userType == "Transporte") {
                            // Redirigir a MainActivity con tipo de cuenta y ID de usuario
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                putExtra("accountType", userType)
                                putExtra("userId", userId)
                            }
                            startActivity(mainIntent)
                            finish()
                        } else {
                            // Tipo de cuenta desconocido
                            Log.e("LoginError", "Tipo de cuenta desconocido")
                            setContent { LoginScreen() }
                        }
                    } else {
                        // Documento no encontrado
                        Log.e("LoginError", "Error: Documento no encontrado")
                        setContent { LoginScreen() }
                    }
                }
                .addOnFailureListener { e ->
                    // Error al obtener tipo de cuenta
                    Log.e("LoginError", "Error al obtener tipo de cuenta: ${e.message}", e)
                    setContent { LoginScreen() }
                }
        } else {
            // Usuario no autenticado, mostrar pantalla de login
            setContent { LoginScreen() }
        }
    }
}


@Composable
@Preview
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(id = R.drawable.logot1), // Replace with your image ID
                contentDescription = "App Logo"
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    // Obtener el tipo de cuenta desde Firestore en la colección "especificaciones"
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("especificaciones").document(user.uid).get()
                                        .addOnSuccessListener { document ->
                                            if (document != null) {
                                                val userType = document.getString("type")
                                                if (userType == "Usuario") {
                                                    // Redirigir a MainActivity con tipo de cuenta "Usuario"
                                                    val mainIntent = Intent(context, MainActivity::class.java)
                                                    mainIntent.putExtra("accountType", "Usuario")
                                                    mainIntent.putExtra("userId", user.uid)
                                                    context.startActivity(mainIntent)
                                                    (context as ComponentActivity).finish()
                                                } else if (userType == "Transporte") {
                                                    // Redirigir a MainActivity con tipo de cuenta "Transporte"
                                                    val mainIntent = Intent(context, MainActivity::class.java)
                                                    mainIntent.putExtra("accountType", "Transporte")
                                                    mainIntent.putExtra("userId", user.uid)
                                                    context.startActivity(mainIntent)
                                                    (context as ComponentActivity).finish()
                                                } else {
                                                    // Tipo de cuenta desconocido, mostrar error o manejarlo según tu lógica
                                                    message = "Tipo de cuenta desconocido"
                                                }
                                            } else {
                                                message = "Error: Documento no encontrado"
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            message = "Error al obtener tipo de cuenta: ${e.message}"
                                        }
                                } else {
                                    message = "Usuario nulo"
                                }
                            } else {
                                message = task.exception?.message ?: "Login fallido"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Royal Blue
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Login", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(context, SignInActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Lighter blue for Sign Up button
            ) {
                Text("Sign Up", color = Color.White)
            }
        }
    }
}
