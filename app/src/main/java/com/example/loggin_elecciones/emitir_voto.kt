package com.example.loggin_elecciones

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent

class emitir_voto : AppCompatActivity() {

    private lateinit var tituloTextView: TextView
    private lateinit var tipoVotacionTextView: TextView
    private lateinit var listaCandidatosRecyclerView: RecyclerView
    private lateinit var emitirVotoButton: Button
    private lateinit var iconoInfoButton: ImageButton
    private lateinit var textoNombreCandidato: TextView
    private lateinit var checkboxCandidato: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emitir_voto)

        // Recibir el nombre de la votación desde el Intent
        tituloTextView = findViewById(R.id.Tipo_Votacion)
        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE")
        tituloTextView.text = votacionNombre

        // Inicializar los elementos de la interfaz
        listaCandidatosRecyclerView = findViewById(R.id.lista_candidatos)
        emitirVotoButton = findViewById(R.id.emitir_voto)

        // Configurar el RecyclerView con la lista de candidatos
        listaCandidatosRecyclerView.layoutManager = LinearLayoutManager(this)
        listaCandidatosRecyclerView.adapter =
            CandidatosAdapter(getCandidatos()) // Configura con una lista de candidatos

        // Configurar el botón de emitir voto
        emitirVotoButton.setOnClickListener {
            mostrarDialogoConfirmacion()
        }

        // Configurar el botón de regreso
        iconoInfoButton = findViewById(R.id.volver)
        iconoInfoButton.setOnClickListener {
            onBackPressed()  // Esto hace que regrese a la pantalla anterior
        }
    }

    private fun getCandidatos(): List<Candidato> {
        // Lista de candidatos
        return listOf(
            Candidato("Nombre del Partido 1", "más info"),
            Candidato("Nombre del Partido 2", "más info"),
            Candidato("Nombre del Partido 3", "más info"),
            Candidato("Nombre del Partido 4", "más info"),
            Candidato("Nombre del Partido 5", "más info"),
            Candidato("Nombre del Partido 6", "más info"),
            Candidato("Nombre del Partido 7", "más info"),
            Candidato("Nombre del Partido 8", "más info"),
            Candidato("Nombre del Partido 9", "más info"),
            Candidato("Nombre del Partido 10", "más info")
        )
    }

    private fun mostrarDialogoConfirmacion() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.ventana_emergente)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val btnCancelar = dialog.findViewById<Button>(R.id.btn_cancelar)
        val btnConfirmar = dialog.findViewById<Button>(R.id.btn_confirmar)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Data class de Candidato
    data class Candidato(val nombre: String, val partido: String)

    // Adaptador para la lista de Candidatos
    class CandidatosAdapter(private val candidatos: List<Candidato>) :
        RecyclerView.Adapter<CandidatosAdapter.CandidatoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_candidato, parent, false)
            return CandidatoViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
            val candidato = candidatos[position]
            holder.nombreTextView.text = candidato.nombre
            holder.partidoTextView.text = candidato.partido
            holder.checkBox.isChecked = false

            // Establecer un listener para el clic en el nombre del partido
            holder.partidoTextView.setOnClickListener {
                // Crear el Intent para abrir la actividad DatosPartido
                val intent = Intent(holder.itemView.context, DatosPartido::class.java)
                intent.putExtra("PARTIDO_NOMBRE", candidato.nombre)  // Pasar el nombre del candidato

                // Iniciar la actividad
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = candidatos.size

        inner class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombreTextView: TextView = itemView.findViewById(R.id.texto_partido_candidato)
            val partidoTextView: TextView = itemView.findViewById(R.id.texto_nombre_candidato)
            val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_candidato)
        }
    }
}
