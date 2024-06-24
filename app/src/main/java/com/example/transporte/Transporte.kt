package com.example.transporte

import androidx.compose.material3.Text
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

class TransporteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransporteScreen()
        }
    }
}

@Composable
fun TransporteScreen() {
    var saldoTransporte by remember { mutableStateOf(0.0) }
    var nombreTransporte by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Cargar saldo y nombre del transporte desde Firestore
    LaunchedEffect(Unit) {
        firestore.collection("transporte").document("Transporte_1") // Reemplaza "Transporte_1" con el ID del transporte real
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    saldoTransporte = document.getDouble("saldo") ?: 0.0
                    nombreTransporte = document.getString("nombre") ?: ""
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al cargar datos del transporte: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = nombreTransporte, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Saldo del Transporte", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "$saldoTransporte", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}