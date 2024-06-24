package com.example.transporte

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Pago(val transporteId: String, val monto: Double, val fecha: String)

class UsuarioActivity : ComponentActivity() {
    private val qrScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrResult = result.data?.getStringExtra(QRScannerActivity.QR_RESULT)
            qrResult?.let {
                UsuarioScreen.pagarDialogTransporteId = it
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransporteTheme {
                UsuarioScreen(qrScannerLauncher)
            }
        }
    }

    companion object {
        var pagarDialogTransporteId: String? = null
    }
}
@Composable
fun UsuarioScreen(qrScannerLauncher: ActivityResultLauncher<Intent>) {
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
            firestore.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nombre = document.getString("nombre") ?: ""
                        saldo = document.getDouble("saldo") ?: 0.0
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al cargar datos del usuario: ${e.message}", Toast.LENGTH_LONG).show()
                }

            // Cargar historial de pagos desde Firestore
            firestore.collection("historialPagos")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val pagos = documents.map { document ->
                        Pago(
                            transporteId = document.getString("transporteId") ?: "",
                            monto = document.getDouble("monto") ?: 0.0,
                            fecha = document.getString("fecha") ?: ""
                        )
                    }
                    historialPagos = pagos
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al cargar historial de pagos: ${e.message}", Toast.LENGTH_LONG).show()
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
                PagarDialog(onDismiss = { showDialog = false }, onPagar = { transporteId, monto ->
                    saldo -= monto
                    firestore.collection("usuarios").document(userId)
                        .update("saldo", saldo)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pago realizado", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al realizar pago: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    // Actualizar saldo del transporte
                    firestore.collection("transporte").document(transporteId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Extraer el valor actual del saldo
                                val saldoActual = document.getDouble("saldo") ?: 0.0

                                // Calcular el nuevo saldo
                                val nuevoSaldo = saldoActual + monto

                                // Actualizar el saldo en Firestore
                                firestore.collection("transporte").document(transporteId)
                                    .update("saldo", nuevoSaldo)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Saldo del transporte actualizado", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error al actualizar saldo del transporte: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(context, "Error: El transporte no existe", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al obtener el saldo del transporte: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    // Guardar el pago en el historial
                    val nuevoPago = hashMapOf(
                        "userId" to userId,
                        "transporteId" to transporteId,
                        "monto" to monto,
                        "fecha" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    firestore.collection("historialPagos")
                        .add(nuevoPago)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pago registrado en el historial", Toast.LENGTH_LONG).show()
                            // Actualizar historial en la interfaz
                            historialPagos = historialPagos + Pago(transporteId, monto, nuevoPago["fecha"] as String)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error al registrar el pago en el historial: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                    showDialog = false
                }, qrScannerLauncher)
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
fun PagarDialog(onDismiss: () -> Unit, onPagar: (String, Double) -> Unit, qrScannerLauncher: ActivityResultLauncher<Intent>) {
    var transporteId by remember { mutableStateOf(UsuarioActivity.pagarDialogTransporteId ?: "") }
    var selectedOption by remember { mutableStateOf("") }
    val opciones = listOf("Opción 1: $10", "Opción 2: $20", "Opción 3: $30")
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Pagar Transporte", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0A74DA)) },
        text = {
            Column {
                TextField(
                    value = transporteId,
                    onValueChange = { transporteId = it },
                    label = { Text("Número de Transporte (Manual o Escaneado)") },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    qrScannerLauncher.launch(Intent(context, QRScannerActivity::class.java))
                }) {
                    Text("Escanear QR")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Seleccione una opción:", fontWeight = FontWeight.SemiBold, color = Color(0xFF0A74DA))
                opciones.forEach { opcion ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = opcion == selectedOption,
                            onClick = { selectedOption = opcion },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0A74DA))
                        )
                        Text(text = opcion)
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
                    onPagar(transporteId, monto)
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

    LaunchedEffect(transporteId) {
        UsuarioActivity.pagarDialogTransporteId = null
    }
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
        Text(text = "Transporte: ${pago.transporteId}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0A74DA))
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
