package com.example.loggin_elecciones

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import yuku.ambilwarna.AmbilWarnaDialog

class add_partido_rector : AppCompatActivity() {

    private var selectedColor: Int = Color.GRAY
    private lateinit var btnSeleccionarColor: Button
    private lateinit var rvCandidatos: RecyclerView
    private lateinit var candidatoAdapter: CandidatoRectorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_partido_rector)

        // Referencias de botones
        btnSeleccionarColor = findViewById(R.id.btn_seleccionar_color)
        rvCandidatos = findViewById(R.id.rv_candidatos)

        // Configurar lista fija de candidatos
        val candidatosList = mutableListOf(
            Candidato("", "Rector"),
            Candidato("", "Vice Rector")
        )

        // Configurar RecyclerView
        candidatoAdapter = CandidatoRectorAdapter(candidatosList)
        rvCandidatos.layoutManager = LinearLayoutManager(this)
        rvCandidatos.adapter = candidatoAdapter

        // Configuración inicial del botón de color
        btnSeleccionarColor.setBackgroundColor(selectedColor)
        btnSeleccionarColor.setOnClickListener {
            openColorPicker()
        }

        // Configuración de la acción de cerrar
        val buttonCerrar = findViewById<ImageButton>(R.id.volver)
        buttonCerrar.setOnClickListener {
            finish()
        }
    }

    // Abre el selector de colores
    private fun openColorPicker() {
        val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                Toast.makeText(this@add_partido_rector, "Selección de color cancelada", Toast.LENGTH_SHORT).show()
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                selectedColor = color
                btnSeleccionarColor.setBackgroundColor(selectedColor)
            }
        })
        colorPicker.show()
    }
}


class CandidatoRectorAdapter(private val candidatos: MutableList<Candidato>) :
    RecyclerView.Adapter<CandidatoRectorAdapter.CandidatoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nuevo_rector, parent, false)
        return CandidatoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
        val candidato = candidatos[position]
        holder.bind(candidato)
    }

    override fun getItemCount(): Int = candidatos.size

    class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val etNombreCandidato: EditText = itemView.findViewById(R.id.et_nombre_rector)
        private val etCargo: EditText = itemView.findViewById(R.id.et_cargo_rector)

        fun bind(candidato: Candidato) {
            etNombreCandidato.setText(candidato.nombre)
            etCargo.setText(candidato.cargo)

            // Bloquear edición del campo cargo
            etCargo.isEnabled = false
        }
    }
}
