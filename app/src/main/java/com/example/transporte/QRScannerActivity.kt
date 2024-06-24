package com.example.transporte

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class QRScannerActivity : ComponentActivity() {
    companion object {
        const val QR_REQUEST_CODE = 101
        const val QR_RESULT = "QR_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initiateScan()
    }

    private fun initiateScan() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Escanea el código QR")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val resultIntent = Intent().apply {
                    putExtra(QR_RESULT, result.contents)
                }
                setResult(Activity.RESULT_OK, resultIntent)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }
    }
}
