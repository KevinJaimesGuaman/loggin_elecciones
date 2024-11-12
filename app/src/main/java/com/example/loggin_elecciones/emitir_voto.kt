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
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class emitir_voto : AppCompatActivity() {

    private lateinit var tituloTextView: TextView
    private lateinit var listaCandidatosRecyclerView: RecyclerView
    private lateinit var emitirVotoButton: Button
    private lateinit var iconoInfoButton: ImageButton
    private lateinit var db: FirebaseFirestore
    private lateinit var usuarioId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emitir_voto)

        // Inicializar la base de datos
        db = FirebaseFirestore.getInstance()

        // Obtener el correo institucional del usuario (suponiendo que el usuario ya está logueado)
        val usuarioCorreo = "201904903@est.umss.edu"  // Este es un valor de ejemplo, usa el correo del usuario logueado
        usuarioId = usuarioCorreo.substringBefore("@")  // Esto obtiene el número antes de "@"

        // Recibir el nombre de la votación desde el Intent
        tituloTextView = findViewById(R.id.Tipo_Votacion)
        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE")
        tituloTextView.text = votacionNombre

        // Inicializar los elementos de la interfaz
        listaCandidatosRecyclerView = findViewById(R.id.lista_candidatos)
        emitirVotoButton = findViewById(R.id.emitir_voto)

        // Configurar el RecyclerView con la lista de candidatos
        listaCandidatosRecyclerView.layoutManager = LinearLayoutManager(this)

        // Verificar si ya votó en esta votación
        if (votacionNombre != null) {
            verificarVotacion(votacionNombre)
        } else {
            Log.e("emitir_voto", "Error: El nombre de la votación es nulo")
        }

        // Configurar el botón de emitir voto
        emitirVotoButton.setOnClickListener {
            val candidato = (listaCandidatosRecyclerView.adapter as CandidatosAdapter).candidatoSeleccionado
            if (candidato != null) {
                // Hacer algo con el candidato seleccionado
                mostrarDialogoConfirmacion(candidato)
            } else {
                Log.d("CandidatoVoto", "Ningún candidato seleccionado.")
            }
        }

        // Configurar el botón de regreso
        iconoInfoButton = findViewById(R.id.volver)
        iconoInfoButton.setOnClickListener {
            onBackPressed()  // Esto hace que regrese a la pantalla anterior
        }
    }

    // Función para verificar si el usuario ya ha votado en esta votación
    private fun verificarVotacion(votacionNombre: String) {
        val estadoVotacionRef = db.collection("Votaciones")
            .document(votacionNombre)  // Aquí se accede al documento de la votación
            .collection("EstadoVotacion")
            .document(usuarioId)  // Aquí se accede al estado de votación del usuario

        estadoVotacionRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val yaVoto = document.getBoolean("votacion1") ?: false  // Comprobar si el campo 'votacion1' existe y es verdadero
                if (yaVoto) {
                    // Ya ha votado, mostrar diálogo de "ya votó"
                    mostrarDialogoYaVoto()
                } else {
                    // No ha votado, continuar normalmente con el proceso de votación
                    obtenerCandidatosYMostrar(votacionNombre)
                }
            } else {
                // Si el documento no existe, significa que el usuario no ha votado aún, mostrar normalmente
                obtenerCandidatosYMostrar(votacionNombre)
            }
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Error al verificar estado de votación", e)
        }
    }

    // Función para obtener los candidatos y configurar la interfaz
    private fun obtenerCandidatosYMostrar(votacionNombre: String) {
        getCandidatos2(votacionNombre) { candidatos ->
            if (candidatos.isNotEmpty()) {
                listaCandidatosRecyclerView.adapter = CandidatosAdapter(candidatos)
            } else {
                Log.d("Candidatos", "No se encontraron candidatos.")
            }
        }
    }

    // Función para mostrar el diálogo de "Ya votó"
    private fun mostrarDialogoYaVoto() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.ya_voto)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val btRegresar = dialog.findViewById<Button>(R.id.btn_regresar_a_home)
        btRegresar.setOnClickListener {
            val intent = Intent(this, home_elector::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    // Función para mostrar el diálogo de confirmación
    private fun mostrarDialogoConfirmacion(candidato: Candidato) {
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
            val votacionNombre = tituloTextView.text.toString()
            votar(candidato, votacionNombre)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Función para votar
    private fun votar(candidato: Candidato, votacionNombre: String) {
        val partidoref = db.collection("TipoEleccion")
            .document(votacionNombre)
            .collection("Partido")
            .document(candidato.nombrePartido)

        partidoref.update("votos", FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d("Firestore", "Voto registrado correctamente")
                mostrarDialogoExito()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al registrar el voto", e)
            }

        // Marcar que el usuario ya votó en la votación
        val estadoVotacionRef = db.collection("Votaciones")
            .document(votacionNombre)
            .collection("EstadoVotacion")
            .document(usuarioId)

        estadoVotacionRef.set(mapOf("votacion1" to true)) // Marcar como votado
    }

    private fun mostrarDialogoExito() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.exito_votacion_ventana_emergente)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val btRegresar = dialog.findViewById<Button>(R.id.btn_regresar_a_home)

        btRegresar.setOnClickListener {
            val intent = Intent(this, home_elector::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    // Data class de Candidato
    data class Candidato(val nombrePartido: String, val info: String)

    // Adaptador para la lista de Candidatos
    class CandidatosAdapter(private val candidatos: List<Candidato>) :
        RecyclerView.Adapter<CandidatosAdapter.CandidatoViewHolder>() {

        var candidatoSeleccionado: Candidato? = null  // Variable para almacenar el CheckBox seleccionado

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_candidato, parent, false)
            return CandidatoViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
            val candidato = candidatos[position]
            holder.nombreTextView.text = candidato.nombrePartido
            holder.partidoTextView.text = candidato.info
            holder.checkBox.isChecked = false // Establecer el CheckBox como desmarcado

            // Establecer un listener para el clic en el CheckBox
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    candidatoSeleccionado = candidato  // Almacenar el candidato seleccionado
                } else {
                    if (candidatoSeleccionado == candidato) {
                        candidatoSeleccionado = null
                    }
                }
            }
        }

        override fun getItemCount(): Int = candidatos.size

        inner class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombreTextView: TextView = itemView.findViewById(R.id.texto_partido_candidato)
            val partidoTextView: TextView = itemView.findViewById(R.id.texto_nombre_candidato)
            val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_candidato)
        }
    }
    // Función para obtener la lista de candidatos desde la base de datos de Firestore
    private fun getCandidatos2(votacionNombre: String, callback: (List<Candidato>) -> Unit) {
        val candidatosList = mutableListOf<Candidato>()

        // Acceder a la colección TipoEleccion y buscar el documento con el nombre de la votación
        val partidosRef = db.collection("TipoEleccion")
            .document(votacionNombre)  // Usamos votacionNombre para acceder al documento correcto
            .collection("Partido")  // Accedemos a la subcolección 'Partido'

        // Obtener todos los documentos de la subcolección Partido
        partidosRef.get()
            .addOnSuccessListener { documents ->
                for (partidoDoc in documents) {
                    // Obtiene el ID del documento (nombre del partido)
                    val partidoId = partidoDoc.id
                    // Crea un objeto Candidato y lo añade a la lista
                    val candidato = Candidato(partidoId, "más info")  // Puedes agregar más campos aquí si es necesario
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

}

