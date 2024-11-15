package com.example.loggin_elecciones

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loggin_elecciones.databinding.ActivityCrearCuentaBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class home_administrador : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var votacionAdapter: VotacionAdapter
    private val votacionesOriginales = mutableListOf<Votacion>()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityCrearCuentaBinding? = null
    private val db = FirebaseFirestore.getInstance() // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_home_administrador)
        auth = Firebase.auth

        // GoogleSignIn setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val buttonCerrarSecion = findViewById<Button>(R.id.boton_cerrar_secion)
        buttonCerrarSecion.setOnClickListener {
            signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Obtener el nombre de usuario pasado desde la actividad de login
        val nombreUsuario = intent.getStringExtra("USER_NAME") ?: "Invitado"  // "Invitado" por defecto si no hay usuario

// Asignar el nombre del usuario al TextView
        val nombreTextView: TextView = findViewById(R.id.nombre)
        nombreTextView.text = "Nombre: $nombreUsuario"  // Mostrar el nombre del usuario



        // Para el EditText y botón de búsqueda
        val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
        val buscadorButton = findViewById<ImageButton>(R.id.buscador)

        // Configuración del RecyclerView
        recyclerView = findViewById(R.id.lista_votaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializa el adaptador
        votacionAdapter = VotacionAdapter(votacionesOriginales)
        recyclerView.adapter = votacionAdapter

        // Cargar votaciones desde Firestore
        cargarVotaciones()

        // Configura el botón de búsqueda
        buscadorButton.setOnClickListener {
            val textoBuscado = buscarVotaciones.text.toString().trim()
            filtrarVotaciones(textoBuscado)
        }

        // Muestra todas las votaciones al cargar
        filtrarVotaciones("") // Mostrar todas las votaciones
    }

    data class Votacion(val nombre: String, val estado: String, val color: Int)

    // Función para cerrar sesión
    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Loogin2::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    class VotacionAdapter(private val votaciones: List<Votacion>) : RecyclerView.Adapter<VotacionAdapter.VotacionViewHolder>() {

        class VotacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nombreButton: Button = view.findViewById(R.id.nombreVotacion)
            val estadoTextView: TextView = view.findViewById(R.id.estadoVotacion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotacionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.votacion_item, parent, false)
            return VotacionViewHolder(view)
        }

        override fun onBindViewHolder(holder: VotacionViewHolder, position: Int) {
            val votacion = votaciones[position]
            holder.nombreButton.text = votacion.nombre
            holder.estadoTextView.text = votacion.estado
            holder.estadoTextView.setTextColor(votacion.color)

            // Manejar el clic del botón para abrir la actividad emitir_voto
            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, emitir_voto::class.java)
                intent.putExtra("VOTACION_NOMBRE", votacion.nombre)
                context.startActivity(intent)
            }
        }

        override fun getItemCount() = votaciones.size
    }

    // Función para cargar votaciones desde Firestore
    private fun cargarVotaciones() {
        db.collection("Votacion")
            .get()
            .addOnSuccessListener { result ->
                votacionesOriginales.clear() // Limpiar la lista antes de agregar nuevos datos

                for (document in result) {
                    val tipoVotacion = document.getString("tipoVotacion") ?: "Desconocido"
                    val estado = document.getLong("estado")?.toInt() ?: 0

                    // Determina el texto y el color basado en el estado
                    val estadoTexto = when (estado) {
                        0 -> "ACTIVO"
                        1 -> "AUN NO EMPEZO"
                        2 -> "YA PASO"
                        else -> "DESCONOCIDO"
                    }
                    val color = when (estado) {
                        0 -> Color.GREEN
                        1 -> Color.GRAY
                        2 -> Color.RED
                        else -> Color.BLACK
                    }

                    // Agrega la votación a la lista
                    votacionesOriginales.add(Votacion(tipoVotacion, estadoTexto, color))
                }

                // Notificar al adaptador que los datos han cambiado
                votacionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para filtrar las votaciones
    private fun filtrarVotaciones(textoBuscado: String) {
        val listaFiltrada = if (textoBuscado.isEmpty()) {
            votacionesOriginales
        } else {
            votacionesOriginales.filter { it.nombre.contains(textoBuscado, ignoreCase = true) }
        }
        votacionAdapter = VotacionAdapter(listaFiltrada)
        recyclerView.adapter = votacionAdapter
        votacionAdapter.notifyDataSetChanged() // Notificar que los datos han cambiado
    }
}
