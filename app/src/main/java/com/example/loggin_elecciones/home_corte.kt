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

class home_corte : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityCrearCuentaBinding? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var votacionesOriginales: MutableList<Votacion>
    private lateinit var votacionAdapter: VotacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_home_corte)

        // Inicialización de Firebase
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Configura GoogleSignInOptions
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configura el botón de cerrar sesión
        val buttonCerrarSecion = findViewById<Button>(R.id.boton_cerrar_secion)
        buttonCerrarSecion.setOnClickListener {
            signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }

        // Configura la interfaz de usuario
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa las vistas
        val nombreTextView: TextView = findViewById(R.id.nombre)
        val estadoTextView= findViewById<TextView>(R.id.textView_estado)

        if (currentUser != null) {
            val displayName = currentUser.displayName
            if (displayName != null) {
                nombreTextView.text = "Nombre: $displayName"
            }

            // Verificar el estado del usuario
            val email = currentUser.email
            val userId = email?.substringBefore('@')
            if (userId != null) {
                db.collection("CorteElectoral")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val habilitado = document.getBoolean("habilitado") ?: false
                            estadoTextView.text = if (habilitado) "Estado: HABILITADO" else "Estado: NO HABILITADO"
                            estadoTextView.setTextColor(if (habilitado) Color.GREEN else Color.RED)
                        } else {
                            estadoTextView.text = "Usuario no encontrado"
                            estadoTextView.setTextColor(Color.GRAY)
                        }
                    }

                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error al verificar el estado: ${exception.message}", Toast.LENGTH_SHORT).show()
                        estadoTextView.text = "Error al verificar estado"
                        estadoTextView.setTextColor(Color.GRAY)
                    }
            }
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }

        // Inicialización del RecyclerView
        recyclerView = findViewById(R.id.lista_votaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista vacía para las votaciones
        votacionesOriginales = mutableListOf()
        votacionAdapter = VotacionAdapter(votacionesOriginales)
        recyclerView.adapter = votacionAdapter

        // Cargar votaciones desde Firestore
        cargarVotaciones()

        // Configurar el buscador
        val buscadorButton = findViewById<ImageButton>(R.id.buscador)
        val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
        buscadorButton.setOnClickListener {
            val textoBuscado = buscarVotaciones.text.toString().trim()
            filtrarVotaciones(textoBuscado)
        }
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

            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, DetalleVotacionActivity::class.java)
                intent.putExtra("VOTACION_NOMBRE", votacion.nombre)
                context.startActivity(intent)
            }
        }

        override fun getItemCount() = votaciones.size
    }

    // Función para cargar las votaciones desde Firestore
    private fun cargarVotaciones() {
        db.collection("Votacion")
            .get() // Obtiene todos los documentos de la colección "Votacion"
            .addOnSuccessListener { result ->
                votacionesOriginales.clear() // Limpia la lista antes de agregar los nuevos datos

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

                // Notifica al adaptador que los datos han cambiado
                votacionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para filtrar las votaciones
    private fun filtrarVotaciones(textoBuscado: String) {
        val listaFiltrada = if (textoBuscado.isEmpty()) {
            votacionesOriginales // Mostrar todas si no hay texto
        } else {
            votacionesOriginales.filter { it.nombre.contains(textoBuscado, ignoreCase = true) }
        }
        votacionAdapter = VotacionAdapter(listaFiltrada)
        recyclerView.adapter = votacionAdapter
    }
}
