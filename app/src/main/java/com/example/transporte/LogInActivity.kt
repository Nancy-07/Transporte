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
import androidx.compose.foundation.Image

import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
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
        //color = MaterialTheme.colorScheme.primary
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
                                    // Obtener el tipo de cuenta desde Firestore
                                    val db = FirebaseFirestore.getInstance()
                                    db.collection("users").document(user.uid).get()
                                        .addOnSuccessListener { document ->
                                            if (document != null) {
                                                val userType = document.getString("type")
                                                if (userType == "Usuario") {
                                                    // Redirigir a MainActivity con tipo de cuenta "Usuario"
                                                    val mainIntent = Intent(context, MainActivity::class.java)
                                                    mainIntent.putExtra("accountType", "Usuario")
                                                    context.startActivity(mainIntent)
                                                    (context as ComponentActivity).finish()
                                                } else if (userType == "Transporte") {
                                                    // Redirigir a MainActivity con tipo de cuenta "Transporte"
                                                    val mainIntent = Intent(context, MainActivity::class.java)
                                                    mainIntent.putExtra("accountType", "Transporte")
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
