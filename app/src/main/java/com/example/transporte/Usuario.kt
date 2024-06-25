package com.example.transporte

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Pago(val transporte: String, val transporteId: String, val monto: Double, val fecha: String)

class UsuarioActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransporteTheme {
                UsuarioScreen()
            }
        }
    }
}

@Composable
fun UsuarioScreen() {
    var nombre by remember { mutableStateOf("") }
    var saldo by remember { mutableStateOf(0.0) }
    var showDialog by remember { mutableStateOf(false) }
    var historialPagos by remember { mutableStateOf(listOf<Pago>()) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    if (user != null) {
        val userId = user.uid

        // Cargar datos del usuario desde Firestore
        LaunchedEffect(Unit) {
            try {
                val userData = firestore.collection("usuarios").document(userId).get().await()
                nombre = userData.getString("nombre") ?: ""
                saldo = userData.getDouble("saldo") ?: 0.0
            } catch (e: Exception) {
                // showSnackbar(context, "Error al cargar datos del usuario: ${e.message}")
            }

            // Cargar historial de pagos desde Firestore
            try {
                val pagosSnapshot = firestore.collection("historialPagos")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                historialPagos = pagosSnapshot.map { document ->
                    Pago(
                        transporte = document.getString("transporte") ?: "",
                        transporteId = document.getString("transporteId") ?: "",
                        monto = document.getDouble("monto") ?: 0.0,
                        fecha = document.getString("fecha") ?: ""
                    )
                }
            } catch (e: Exception) {
                //showSnackbar(context, "Error al cargar historial de pagos: ${e.message}")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Nombre: $nombre", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF0A74DA))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Saldo: $saldo", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF0A74DA))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A74DA))) {
                Text(text = "Pagar")
            }
            if (showDialog) {
                PagarDialog(onDismiss = { showDialog = false }, onPagar = { nombreTransporte, monto ->
                    // Buscar ID del transporte por nombre
                    firestore.collection("transporte")
                        .whereEqualTo("nombre", nombreTransporte)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                //showSnackbar(context, "Error: El transporte no existe")
                            } else {
                                val transporteId = documents.first().id

                                // Realizar el pago
                                val nuevoSaldo = saldo - monto
                                firestore.collection("usuarios").document(userId)
                                    .update("saldo", nuevoSaldo)
                                    .addOnSuccessListener {
                                        // showSnackbar(context, "Pago realizado correctamente")
                                    }
                                    .addOnFailureListener { e ->
                                        // showSnackbar(context, "Error al realizar el pago: ${e.message}")
                                    }

                                // Actualizar saldo del transporte
                                firestore.collection("transporte").document(transporteId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val saldoActual = document.getDouble("saldo") ?: 0.0
                                            val nuevoSaldoTransporte = saldoActual + monto
                                            firestore.collection("transporte").document(transporteId)
                                                .update("saldo", nuevoSaldoTransporte)
                                                .addOnSuccessListener {

                                                    Toast.makeText(context,   "Saldo del transporte actualizado correctamente", Toast.LENGTH_LONG).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(context,   "Error al actualizar saldo del transporte: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            Toast.makeText(context,  "Error: El transporte no existe", Toast.LENGTH_LONG).show()

                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context,   "Error al obtener el saldo del transporte: ${e.message}", Toast.LENGTH_LONG).show()

                                    }

                                // Guardar el pago en el historial
                                val nuevoPago = hashMapOf(
                                    "userId" to userId,
                                    "transporte" to nombreTransporte,
                                    "transporteId" to transporteId,
                                    "monto" to monto,
                                    "fecha" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                )
                                firestore.collection("historialPagos")
                                    .add(nuevoPago)
                                    .addOnSuccessListener {

                                        Toast.makeText(context,  "Pago registrado en el historial", Toast.LENGTH_LONG).show()
                                        historialPagos = historialPagos + Pago(nombreTransporte, transporteId, monto, nuevoPago["fecha"] as String)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context,  "Error al registrar el pago en el historial: ${e.message}", Toast.LENGTH_LONG).show()

                                    }

                                showDialog = false
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context,  "Error al buscar el ID del transporte: ${e.message}", Toast.LENGTH_LONG).show()

                        }
                })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Historial de Pagos", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF0A74DA))
            LazyColumn {
                items(historialPagos) { pago ->
                    PagoItem(pago)
                }
            }
        }
    } else {
        Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PagarDialog(onDismiss: () -> Unit, onPagar: (String, Double) -> Unit) {
    var nombreTransporte by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("") }
    val opciones = listOf("Opción 1: $10", "Opción 2: $20", "Opción 3: $30")

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Pagar Transporte", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Blue) },
        text = {
            Column {
                TextField(
                    value = nombreTransporte,
                    onValueChange = { nombreTransporte = it },
                    label = { Text("Nombre del Transporte") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                opciones.forEach { opcion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = opcion == selectedOption,
                            onClick = { selectedOption = opcion },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.Blue)
                        )
                        Text(
                            text = opcion,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = when (selectedOption) {
                        "Opción 1: $10" -> 10.0
                        "Opción 2: $20" -> 20.0
                        "Opción 3: $30" -> 30.0
                        else -> 0.0
                    }
                    onPagar(nombreTransporte, monto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A74DA))
            ) {
                Text("Pagar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun PagoItem(pago: Pago) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFFEDEDED))
            .padding(16.dp)
    ) {

        Text(text = "Transporte: ${pago.transporte}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0A74DA))
        Text(text = "Monto: ${pago.monto}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0A74DA))
        Text(text = "Fecha: ${pago.fecha}", style = MaterialTheme.typography.bodyLarge)
    }
}



private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A74DA), // Royal Blue for dark theme primary
    secondary = Color(0xFF1565C0), // Slightly darker blue for secondary
    tertiary = Color(0xFF0D47A1) // Dark blue for tertiary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0A74DA), // Royal Blue for light theme primary
    secondary = Color(0xFF1565C0), // Slightly darker blue for secondary
    tertiary = Color(0xFF0D47A1) // Dark blue for tertiary
)

@Composable
fun TransporteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}


