package com.example.loggin_elecciones

// Importaciones necesarias
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
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
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // El usuario está autenticado, continuamos con la carga de los datos
            val email = currentUser.email ?: ""
            val userId = email.substringBefore("@")
            obtenerDatosElector(userId)  // Obtén los datos del elector
            votacionAdapter = VotacionAdapter(votacionesOriginales, userId)  // Pasa el userId al adaptador
            recyclerView.adapter = votacionAdapter
        } else {
            // Si el usuario no está autenticado, redirige al login
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Loogin2::class.java)
            startActivity(intent)
            finish()
        }


        val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
        val buscadorButton = findViewById<ImageButton>(R.id.buscador)
        buscadorButton.setOnClickListener {
            val textoBuscado = buscarVotaciones.text.toString().trim()
            filtrarVotaciones(textoBuscado)
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

    class VotacionAdapter(private var votaciones: List<Votacion>, private val userId: String) : RecyclerView.Adapter<VotacionAdapter.VotacionViewHolder>() {

        class VotacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nombreButton: Button = view.findViewById(R.id.nombreVotacion)
            val votacionItem: LinearLayout = view.findViewById(R.id.votacionItem)
            val checkBox: CheckBox = view.findViewById(R.id.votoCheckBox)  // El CheckBox
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotacionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.votacion_item_elector, parent, false)
            return VotacionViewHolder(view)
        }

        override fun onBindViewHolder(holder: VotacionViewHolder, position: Int) {
            val votacion = votaciones[position]
            holder.nombreButton.text = votacion.nombre

            // Asignar el color del fondo del item
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = 40f
            drawable.setColor(votacion.color)
            holder.votacionItem.background = drawable

            // Comprobar si el usuario ya ha votado
            verificarVotacion(votacion.nombre, holder.checkBox)

            // Deshabilitar el checkbox para que no pueda ser modificado
            holder.checkBox.isEnabled = false

            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context

                // Lógica para manejar la votación, dependiendo del estado
                val estadoVotacion = when (votacion.color) {
                    Color.GRAY -> "EMPEZO"
                    Color.RED -> "YA PASO"
                    else -> "ACTIVO"
                }

                when (estadoVotacion) {
                    "YA PASO" -> {
                        (context as home_elector).mostrarDialogoYaPaso()
                    }
                    "EMPEZO" -> {
                        (context as home_elector).mostrarDialogoAunNoEmpezo()
                    }
                    else -> {
                        val intent = Intent(context, emitir_voto::class.java)
                        intent.putExtra("VOTACION_NOMBRE", votacion.nombre)
                        context.startActivity(intent)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return votaciones.size
        }

        // Método para actualizar la lista de votaciones en el adaptador
        fun actualizarLista(nuevaLista: List<Votacion>) {
            votaciones = nuevaLista
            notifyDataSetChanged()  // Notificar al RecyclerView que los datos han cambiado
        }

        // Método para verificar si el usuario ya votó
        private fun verificarVotacion(votacionNombre: String, checkBox: CheckBox) {
            // Verifica el estado de votación del usuario en Firestore
            val estadoVotacionRef = FirebaseFirestore.getInstance().collection("Votaciones")
                .document(votacionNombre)  // Aquí se accede al documento de la votación
                .collection("EstadoVotacion")
                .document(userId)  // Aquí se accede al estado de votación del usuario

            estadoVotacionRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val yaVoto = document.getBoolean("votacion1") ?: false  // Comprobar si el campo 'votacion1' existe y es verdadero
                    checkBox.isChecked = yaVoto  // Marcar el checkbox si el usuario ya votó
                } else {
                    checkBox.isChecked = false  // Si no existe el documento, se desmarca
                }
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error al verificar estado de votación", e)
            }
        }
    }



    private fun verificarVotacion(votacionNombre: String, usuarioId: String): Boolean {
        var yaVoto = false
        val estadoVotacionRef = db.collection("Votaciones")
            .document(votacionNombre)  // Aquí se accede al documento de la votación
            .collection("EstadoVotacion")
            .document(usuarioId)  // Aquí se accede al estado de votación del usuario

        estadoVotacionRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                yaVoto = document.getBoolean("votacion1") ?: false  // Comprobar si el campo 'votacion1' existe y es verdadero
            }
        }
        return yaVoto
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
    // Método para filtrar las votaciones por el texto de búsqueda
    private fun filtrarVotaciones(textoBuscado: String) {
        val listaFiltrada = if (textoBuscado.isEmpty()) {
            // Si el texto de búsqueda está vacío, mostrar todas las votaciones
            votacionesOriginales
        } else {
            // Filtrar las votaciones que contienen el texto buscado en su nombre (sin distinguir entre mayúsculas y minúsculas)
            votacionesOriginales.filter { it.nombre.contains(textoBuscado, ignoreCase = true) }
        }

        // Actualizar el adaptador con la lista filtrada
        votacionAdapter.actualizarLista(listaFiltrada)

        // Notificar al adaptador que los datos han cambiado para que la vista se actualice
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