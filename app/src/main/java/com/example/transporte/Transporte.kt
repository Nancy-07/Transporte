package com.example.transporte

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.Text
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

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
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    if (user != null) {
        val userId = user.uid

        LaunchedEffect(userId) {
            // Obtener el transporteId directamente del documento del usuario
            firestore.collection("transporte").document(userId) // Usar userId como documento de transporteId
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val transporteId = document.id // El ID del documento del transporte

                        // Acceder al documento del transporte usando transporteId
                        firestore.collection("transporte").document(transporteId)
                            .get()
                            .addOnSuccessListener { transporteDocument ->
                                if (transporteDocument != null && transporteDocument.exists()) {
                                    saldoTransporte = transporteDocument.getDouble("saldo") ?: 0.0
                                    nombreTransporte = transporteDocument.getString("nombre") ?: ""
                                } else {
                                    Toast.makeText(context, "No se encontró el documento del transporte para este usuario", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error al cargar datos del transporte: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "No se encontró el documento del transporte para este usuario", Toast.LENGTH_LONG).show()
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
            val transportId =  user.uid // Retrieve transport ID from retrieved data (replace with your logic)
            val qrCodeBitmap = generateQRCode(transportId)
            if (qrCodeBitmap != null) {
                Image(
                    bitmap = qrCodeBitmap,
                    contentDescription = "QR Code for Transport ID",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(top = 16.dp)
                )
            } else {
                Text(
                    text = "Error generating QR Code",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

        }

    } else {
        Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_LONG).show()
    }

}
fun generateQRCode(transportId: String): ImageBitmap {
    val qrCodeWriter = QRCodeWriter()
    val dataMatrix = qrCodeWriter.encode(transportId, BarcodeFormat.QR_CODE, 500, 500)
    val bos = ByteArrayOutputStream()
    val bmp = Bitmap.createBitmap(dataMatrix.width, dataMatrix.height, Bitmap.Config.ARGB_8888)
    for (y in 0 until dataMatrix.height) {
        for (x in 0 until dataMatrix.width) {
            val color = if (dataMatrix[y, x]) Color.Black else Color.White
            bmp.setPixel(x, y, color.toArgb())
        }
    }
    bmp.compress(Bitmap.CompressFormat.PNG, 0, bos)
    return bmp.asImageBitmap()
}
