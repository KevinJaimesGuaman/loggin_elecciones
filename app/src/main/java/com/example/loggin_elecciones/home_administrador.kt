package com.example.loggin_elecciones

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.loggin_elecciones.databinding.ActivityCrearCuentaBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class home_administrador : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var votacionAdapter: VotacionAdapter
    private val votacionesOriginales = mutableListOf<Votacion>()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityCrearCuentaBinding? = null
    private val db = FirebaseFirestore.getInstance() // Firestore instance
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_home_administrador)
        auth = Firebase.auth
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout) // Asignación de SwipeRefreshLayout
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
        swipeRefreshLayout.setOnRefreshListener {
            refrescarVotaciones()
        }
    }


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
    data class Votacion(val nombre: String, val color: Int)

    class VotacionAdapter(private val votaciones: List<Votacion>) : RecyclerView.Adapter<VotacionAdapter.VotacionViewHolder>() {

        class VotacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nombreButton: Button = view.findViewById(R.id.nombreVotacion)
            val votacionItem: LinearLayout = view.findViewById(R.id.votacionItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotacionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.votacion_item, parent, false)
            return VotacionViewHolder(view)
        }

        override fun onBindViewHolder(holder: VotacionViewHolder, position: Int) {
            val votacion = votaciones[position]
            holder.nombreButton.text = votacion.nombre
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = 40f
            // Asignar el color según el estado de la votación
            drawable.setColor(votacion.color)
            holder.votacionItem.background = drawable

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
                    val fechaIni = document.getTimestamp("fechaIni")?.toDate()
                    val fechaFin = document.getTimestamp("fechaFin")?.toDate()

                    if (fechaIni != null && fechaFin != null) {
                        val estado = obtenerEstadoVotacion(fechaIni, fechaFin)
                        val color = when (estado) {
                            "ACTIVO" -> Color.GREEN
                            "YA PASO" -> Color.RED
                            "AUN NO EMPEZO" -> Color.GRAY
                            else -> Color.BLACK // Para estados desconocidos
                        }

                        // Agregar la votación a la lista
                        votacionesOriginales.add(Votacion(tipoVotacion, color))
                    }
                }

                // Notificar al adaptador que los datos han cambiado
                votacionAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun obtenerEstadoVotacion(fechaIni: Date, fechaFin: Date): String {
        val currentDate = Date()

        return when {
            currentDate.before(fechaIni) -> "AUN NO EMPEZO"
            currentDate.after(fechaFin) -> "YA PASO"
            else -> "ACTIVO"
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
    private fun refrescarVotaciones() {
        val currentUser = auth.currentUser
        currentUser?.email?.let { email ->
            val userId = email.substringBefore("@")
            cargarVotaciones() // Llama de nuevo para refrescar las votaciones
        }
    }
}
