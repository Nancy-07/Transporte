package com.example.transporte

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el usuario ha iniciado sesión
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Si el usuario no ha iniciado sesión, redirigir a LoginActivity
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish() // Cerrar MainActivity para que no pueda volver atrás sin iniciar sesión
        } else {
            // Obtener el tipo de cuenta de los extras
            val accountType = intent.getStringExtra("accountType")

            // Redirigir a la actividad correspondiente
            when (accountType) {
                "Usuario" -> {
                    val usuarioIntent = Intent(this, UsuarioActivity::class.java)
                    startActivity(usuarioIntent)
                }
                "Transporte" -> {
                    val transporteIntent = Intent(this, TransporteActivity::class.java)
                    startActivity(transporteIntent)
                }
                else -> {
                    // En caso de que el tipo de cuenta no esté especificado o sea desconocido, mostrar MainScreen
                    setContent {
                        MainScreen()
                    }
                }
            }
            finish() // Cerrar MainActivity para que no se pueda volver atrás sin cerrar sesión
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            context.startActivity(Intent(context, UsuarioActivity::class.java))
        }) {
            Text("Ir a Usuario")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            context.startActivity(Intent(context, TransporteActivity::class.java))
        }) {
            Text("Ir a Transporte")
        }
    }
}
