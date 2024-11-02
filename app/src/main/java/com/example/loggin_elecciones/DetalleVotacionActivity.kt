package com.example.loggin_elecciones

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalleVotacionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_votacion)

        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE")
        val textView = findViewById<TextView>(R.id.textViewDetalle) // Asegúrate de tener un TextView para mostrar detalles
        textView.text = votacionNombre ?: "Sin detalles"
    }
}
