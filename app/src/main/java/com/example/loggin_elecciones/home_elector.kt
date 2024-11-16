package com.example.loggin_elecciones

// Importaciones necesarias
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import java.util.Date

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

    data class Votacion(val nombre: String, val color: Int)
    // Método para mostrar el mensaje de no habilitado en forma de un dialogo
    private fun mostrarDialogoNoHabilitado() {
        val dialogView = layoutInflater.inflate(R.layout.no_habilitado, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Hacer que el diálogo no se cierre tocando fuera de él
            .create()

        val botonAceptar = dialogView.findViewById<Button>(R.id.btn_aceptar)
        botonAceptar.setOnClickListener {
            // Al hacer clic en "Aceptar", cerramos sesión y regresamos a la pantalla de login
            signOut()
            dialog.dismiss()  // Cerrar el diálogo
        }

        dialog.show()
    }

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
            drawable.setColor(votacion.color)
            holder.votacionItem.background = drawable

            // Establecer el color del botón según el estado de la votación
            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context

                // Verificar el estado de la votación (EMPEZO o YA PASO)
                val estadoVotacion = when (votacion.color) {
                    Color.GRAY -> "EMPEZO"
                    Color.RED -> "YA PASO"
                    else -> "ACTIVO"
                }

                when (estadoVotacion) {
                    "YA PASO" -> {
                        // Mostrar el diálogo de que la votación ya pasó
                        (context as home_elector).mostrarDialogoYaPaso()
                    }
                    "EMPEZO" -> {
                        // Mostrar el diálogo de que la votación aún no ha comenzado
                        (context as home_elector).mostrarDialogoAunNoEmpezo()
                    }
                    else -> {
                        // En el caso de que la votación esté activa, ir a la actividad de emitir voto
                        val intent = Intent(context, emitir_voto::class.java)
                        intent.putExtra("VOTACION_NOMBRE", votacion.nombre)
                        context.startActivity(intent)
                    }
                }
            }
        }



        override fun getItemCount() = votaciones.size
    }

    // Método para mostrar el mensaje cuando la votación YA PASO
    private fun mostrarDialogoYaPaso() {
        val dialogView = layoutInflater.inflate(R.layout.ya_paso, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Hacer que el diálogo no se cierre tocando fuera de él
            .create()

        val botonAceptar = dialogView.findViewById<Button>(R.id.btn_aceptar)
        botonAceptar.setOnClickListener {
            dialog.dismiss()  // Cerrar el diálogo
        }

        dialog.show()
    }

    // Método para mostrar el mensaje cuando la votación AUN NO EMPEZO
    private fun mostrarDialogoAunNoEmpezo() {
        val dialogView = layoutInflater.inflate(R.layout.aun_no_empezo, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Hacer que el diálogo no se cierre tocando fuera de él
            .create()

        val botonAceptar = dialogView.findViewById<Button>(R.id.btn_aceptar)
        botonAceptar.setOnClickListener {
            dialog.dismiss()  // Cerrar el diálogo
        }

        dialog.show()
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

                    // Obtener las fechas de inicio y fin como Timestamp
                    val fechaInicio = votacionDoc.getTimestamp("fechaIni")?.toDate() ?: Date()  // Convierte a Date
                    val fechaFin = votacionDoc.getTimestamp("fechaFin")?.toDate() ?: Date()        // Convierte a Date

                    // Obtener el estado de la votación (ACTIVO, AUN NO EMPEZO, YA PASO)
                    val estadoVotacion = obtenerEstadoVotacion(fechaInicio, fechaFin)

                    // Asignar el color según el estado
                    val color = when (estadoVotacion) {
                        "EMPEZO" -> Color.GRAY
                        "ACTIVO" -> Color.GREEN             // Votación activa: verde
                        // Votación aún no empieza: plomo (gris claro)
                        "YA PASO" -> Color.RED             // Votación ya pasó: rojo
                        else -> Color.BLACK                // Caso por defecto
                    }

                    // Verificar que el elector pertenece a esta votación (lo que ya estás haciendo)
                    val carrerasDestinadasId = votacionDoc.getString("carrerasDestinadas") ?: ""
                    db.collection("CarrerasDestinadas").document(carrerasDestinadasId)
                        .get()
                        .addOnSuccessListener { carrerasDestinadasDoc ->
                            if (carrerasDestinadasDoc.exists()) {
                                val carreras = carrerasDestinadasDoc.data?.filterKeys { it.startsWith("carrera") }
                                if (carreras != null) {
                                    for ((key, value) in carreras) {
                                        if (value == carreraElector) {
                                            // Si encontramos la carrera, la agregamos a la lista de votaciones
                                            votacionesOriginales.add(Votacion(tipoVotacion, color))
                                            votacionAdapter.notifyDataSetChanged()
                                            swipeRefreshLayout.isRefreshing = false
                                            break // Si ya encontramos la carrera, no necesitamos seguir buscando
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error al cargar carreras: ${exception.message}", Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }



    private fun obtenerEstadoVotacion(fechaIni: Date, fechaFin: Date): String {
        val currentDate = Date()

        return when {
            currentDate.before(fechaIni) -> "EMPEZO"
            currentDate.after(fechaFin) -> "YA PASO"
            else -> "ACTIVO"
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
                    estadoTextView.setTextColor(if (estado) Color.BLACK else Color.RED)

                    if (!estado) {
                        // Si el elector no está habilitado, mostramos el layout de no habilitado
                        mostrarDialogoNoHabilitado()
                    } else {
                        // Llama a cargarVotacionesFiltradasPorCarrera con la carrera obtenida
                        cargarVotacionesFiltradasPorCarrera(carrera)
                    }
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