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
import com.google.firebase.firestore.CollectionReference
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class emitir_voto : AppCompatActivity() {

    private lateinit var tituloTextView: TextView
    private lateinit var tipoVotacionTextView: TextView
    private lateinit var listaCandidatosRecyclerView: RecyclerView
    private lateinit var emitirVotoButton: Button
    private lateinit var iconoInfoButton: ImageButton
    private lateinit var textoNombreCandidato: TextView
    private lateinit var checkboxCandidato: CheckBox
    private lateinit var db: FirebaseFirestore
    private lateinit var TipoEleccionRef: CollectionReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emitir_voto)

        // Inicializar la base de datos
        db = FirebaseFirestore.getInstance()

        // Recibir el nombre de la votación desde el Intent
        tituloTextView = findViewById(R.id.Tipo_Votacion)
        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE")
        tituloTextView.text = votacionNombre

        // Inicializar los elementos de la interfaz
        listaCandidatosRecyclerView = findViewById(R.id.lista_candidatos)
        emitirVotoButton = findViewById(R.id.emitir_voto)

        // Configurar el RecyclerView con la lista de candidatos
        listaCandidatosRecyclerView.layoutManager = LinearLayoutManager(this)
        // Llamar a getCandidatos2 para obtener candidatos desde Firestore
        getCandidatos2 { candidatos ->
            // Configurar el adaptador cuando la lista de candidatos esté lista
            listaCandidatosRecyclerView.adapter = CandidatosAdapter(candidatos)
        }
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
    // Función para obtener la lista de candidatos con un callback
    private fun getCandidatos2(callback: (List<Candidato>) -> Unit) {
        val candidatosList = mutableListOf<Candidato>()
        val partidosRef = db.collection("TipoEleccion")
            .document("Rectorado")
            .collection("Partido")

        partidosRef.get()
            .addOnSuccessListener { documents ->
                for (partidoDoc in documents) {
                    // Obtiene el ID del documento (nombre del partido)
                    val partidoId = partidoDoc.id

                    // Crea un objeto Candidato y lo añade a la lista
                    val candidato = Candidato(partidoId, "más info")
                    candidatosList.add(candidato)
                }

                // Llama al callback con la lista de candidatos
                callback(candidatosList)
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error al obtener partidos", exception)
                callback(emptyList()) // En caso de error, devuelve una lista vacía
            }
    }

// Función para mostrar el diálogo de confirmación
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
    data class Candidato(val nombrePartido: String, val info: String)

    // Adaptador para la lista de Candidatos
    class CandidatosAdapter(private val candidatos: List<Candidato>) :
        RecyclerView.Adapter<CandidatosAdapter.CandidatoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_candidato, parent, false)
            return CandidatoViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
            val candidato = candidatos[position]
            holder.nombreTextView.text = candidato.nombrePartido
            holder.partidoTextView.text = candidato.info
            holder.checkBox.isChecked = false

            // Establecer un listener para el clic en el nombre del partido
            holder.partidoTextView.setOnClickListener {
                // Crear el Intent para abrir la actividad DatosPartido
                val intent = Intent(holder.itemView.context, DatosPartido::class.java)
                intent.putExtra("PARTIDO_NOMBRE", candidato.nombrePartido)  // Pasar el nombre del candidato

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