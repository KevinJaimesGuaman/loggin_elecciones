package com.example.loggin_elecciones

// Importaciones necesarias
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class home_elector : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var votacionAdapter: VotacionAdapter
    private val votacionesOriginales = mutableListOf<Votacion>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_elector)
        auth = Firebase.auth

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // BOTON CERRAR SESION
        val buttonCerrarSecion = findViewById<Button>(R.id.boton_cerrar_secion)
        buttonCerrarSecion.setOnClickListener {
            signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        }
        // Configuración de SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout) // Asignación de SwipeRefreshLayout

        // Configurar el RecyclerView
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.lista_votaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)
        votacionAdapter = VotacionAdapter(votacionesOriginales)
        recyclerView.adapter = votacionAdapter

        val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
        val buscadorButton = findViewById<ImageButton>(R.id.buscador)
        buscadorButton.setOnClickListener {
            val textoBuscado = buscarVotaciones.text.toString().trim()
            filtrarVotaciones(textoBuscado)
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val email = currentUser.email ?: ""
            val userId = email.substringBefore("@")
            obtenerDatosElector(userId)
        }
        // Configuración de la acción de refresco en SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refrescarVotaciones() // Método añadido para refrescar la lista
        }
    }

    data class Votacion(val nombre: String, val estado: String, val color: Int)

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
            val votacionItem: LinearLayout = view.findViewById(R.id.votacionItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotacionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.votacion_item, parent, false)
            return VotacionViewHolder(view)
        }

        override fun onBindViewHolder(holder: VotacionViewHolder, position: Int) {
            val votacion = votaciones[position]
            holder.nombreButton.text = votacion.nombre
            holder.estadoTextView.text = votacion.estado

            // Verifica si el estado es reconocido y asigna el color al LinearLayout
            when (votacion.estado) {
                "ACTIVO" -> {
                    holder.votacionItem.setBackgroundColor(Color.GREEN)
                }
                "AUN NO EMPEZO" -> {
                    holder.votacionItem.setBackgroundColor(Color.GRAY)
                }
                "YA PASO" -> {
                    holder.votacionItem.setBackgroundColor(Color.RED)
                }
                else -> {
                    holder.votacionItem.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            // El TextView también puede tener un color, como lo estás haciendo
            holder.estadoTextView.setTextColor(votacion.color)

            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, emitir_voto::class.java)
                intent.putExtra("VOTACION_NOMBRE", votacion.nombre) // Pasa el nombre de la votación
                context.startActivity(intent)
            }
        }

        override fun getItemCount() = votaciones.size
    }

    private fun cargarVotacionesFiltradasPorCarrera(carreraElector: String) {
        db.collection("Votacion")
            .get()
            .addOnSuccessListener { votaciones ->
                votacionesOriginales.clear() // Limpiamos la lista original

                // Recorremos todas las votaciones
                for (votacionDoc in votaciones) {

                    val tipoVotacion = votacionDoc.getString("tipoVotacion") ?: "Desconocido"
                    val estado = votacionDoc.getLong("estado")?.toInt() ?: 0
                    val carrerasDestinadasId = votacionDoc.getString("carrerasDestinadas") ?: ""

                    // Buscamos el documento correspondiente en "CarrerasDestinadas" usando el ID
                    db.collection("CarrerasDestinadas").document(carrerasDestinadasId)
                        .get()
                        .addOnSuccessListener { carrerasDestinadasDoc ->
                            if (carrerasDestinadasDoc.exists()) {
                                // Verificar si la carrera del elector está en las carreras del documento
                                val carreras = carrerasDestinadasDoc.data?.filterKeys { it.startsWith("carrera") }
                                if (carreras != null) {
                                    for ((key, value) in carreras) {
                                        if (value == carreraElector) {
                                            // Si encontramos la carrera, la agregamos a la lista de votaciones
                                            val estadoTexto = when (estado) {
                                                0 -> "ACTIVO"
                                                1 -> "AUN NO EMPEZO"
                                                2 -> "YA PASO"
                                                else -> "DESCONOCIDO"
                                            }

                                            val color = when (estado) {
                                                0 -> Color.BLACK
                                                1 -> Color.BLACK
                                                2 -> Color.BLACK
                                                else -> Color.BLACK
                                            }

                                            votacionesOriginales.add(Votacion(tipoVotacion, estadoTexto, color))
                                            votacionAdapter.notifyDataSetChanged()
                                            break // Si ya encontramos la carrera, no necesitamos seguir buscando
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error al cargar carreras: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                swipeRefreshLayout.isRefreshing = false // Finaliza la animación de refresco
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Finaliza la animación de refresco en caso de error
            }
    }

    private fun refrescarVotaciones() {
        val currentUser = auth.currentUser
        currentUser?.email?.let { email ->
            val userId = email.substringBefore("@")
            obtenerDatosElector(userId) // Llama de nuevo para refrescar las votaciones
        }
    }
    private fun filtrarVotaciones(textoBuscado: String) {
        val listaFiltrada = if (textoBuscado.isEmpty()) {
            votacionesOriginales
        } else {
            votacionesOriginales.filter { it.nombre.contains(textoBuscado, ignoreCase = true) }
        }
        votacionAdapter = VotacionAdapter(listaFiltrada)
        recyclerView.adapter = votacionAdapter
        votacionAdapter.notifyDataSetChanged()
    }

    private fun obtenerDatosElector(userId: String) {
        val estadoTextView: TextView = findViewById(R.id.textView_estado)
        val carreraTextView: TextView = findViewById(R.id.carrera)
        val nombreTextView: TextView = findViewById(R.id.nombre)

        db.collection("Elector").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("Nombre") ?: "Nombre no encontrado"
                    val carrera = document.getString("carrera") ?: "Carrera no encontrada"
                    val estado = document.getBoolean("habilitado") ?: false

                    nombreTextView.text = "Nombre: $nombre"
                    carreraTextView.text = "Carrera: $carrera"
                    estadoTextView.text = "Estado: ${if (estado) "HABILITADO" else "NO HABILITADO"}"
                    estadoTextView.setTextColor(if (estado) Color.GREEN else Color.RED)

                    // Llama a cargarVotacionesFiltradasPorCarrera con la carrera obtenida
                    cargarVotacionesFiltradasPorCarrera(carrera)
                } else {
                    carreraTextView.text = "Carrera: No disponible"
                    estadoTextView.text = "Estado: No disponible"
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener datos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}