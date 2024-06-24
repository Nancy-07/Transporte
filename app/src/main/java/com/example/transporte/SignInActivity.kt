@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignInScreen()
        }
    }
}@Preview(showBackground = true)
@Composable
fun SignInScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Usuario") }
    val context = LocalContext.current

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Surface(
        modifier = Modifier.fillMaxSize()
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
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Selección del tipo de cuenta
            Text(text = "Tipo de cuenta:")
            Row {
                RadioButton(
                    selected = accountType == "Usuario",
                    onClick = { accountType = "Usuario" }
                )
                Text(text = "Usuario")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = accountType == "Transporte",
                    onClick = { accountType = "Transporte" }
                )
                Text(text = "Transporte")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (password == confirmPassword) {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Save user info to Firestore
                                    val user = auth.currentUser
                                    val userInfo = hashMapOf(
                                        "nombre" to userName,
                                        "email" to email,
                                        "saldo" to 0,
                                        "type" to accountType // Agregar el campo type aquí
                                    )
                                    val collection = if (accountType == "Usuario") "usuarios" else "transporte"
                                    db.collection(collection).document(user!!.uid)
                                        .set(userInfo)
                                        .addOnCompleteListener { userInfoTask ->
                                            isLoading = false
                                            if (userInfoTask.isSuccessful) {
                                                message = "¡Cuenta creada exitosamente!"
                                                // Redirigir a MainActivity después de un registro exitoso y pasar el tipo de cuenta
                                                val mainIntent = Intent(context, MainActivity::class.java).apply {
                                                    putExtra("accountType", accountType)
                                                }
                                                context.startActivity(mainIntent)
                                                (context as ComponentActivity).finish() // Cerrar SignInActivity para que no se pueda volver atrás sin cerrar sesión
                                            } else {
                                                message = userInfoTask.exception?.message ?: "¡Fallo en el registro!"
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    message = task.exception?.message ?: "¡Fallo en el registro!"
                                }
                            }

                    } else {
                        message = "Passwords do not match!"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Royal Blue
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Sign Up", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
