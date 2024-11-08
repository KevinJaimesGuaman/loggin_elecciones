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

    class home_elector : AppCompatActivity() {


        private lateinit var auth: FirebaseAuth
        private lateinit var googleSignInClient: GoogleSignInClient
        private val currentUser = FirebaseAuth.getInstance().currentUser
        private var binding: ActivityCrearCuentaBinding? = null
        // Variables para almacenar las votaciones y adaptador
        private lateinit var votacionesOriginales: MutableList<Votacion>
        private lateinit var votacionAdapter: VotacionAdapter
        private lateinit var recyclerView: RecyclerView
        private lateinit var db: FirebaseFirestore

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            this.enableEdgeToEdge()
            setContentView(R.layout.activity_home_elector)
            auth = Firebase.auth
            val authget= FirebaseAuth.getInstance()
            // Configura GoogleSignInOptions
            // Infla el binding y establece el contenido de la actividad
            binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Asegúrate de tener el ID correcto en tu archivo strings.xml
                .requestEmail()
                .build()
            // Crea el GoogleSignInClient
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val buttonCerrarSecion = findViewById<Button>(R.id.boton_cerrar_secion)
            buttonCerrarSecion.setOnClickListener {
                // Cerrar sesión de Firebase
                signOut()
                // Mostrar un mensaje de cierre de sesión
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            // Para nombre automatica desel e firebase que lo sacaa
            val nombreTextView: TextView = findViewById(R.id.nombre)
            val userName = intent.getStringExtra("USER_NAME")

            if (currentUser != null) {
                // Obtiene el nombre del usuario
                val displayName = currentUser.displayName
                // Asigna el nombre del usuario al TextView
                if (displayName != null) {
                    nombreTextView.text = "Nombre: $displayName"
                }
            }
            //Para la carrera ejemplo SE DEBE MODIFICAR
            // Inicializa Firebase Auth y Firestore
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            // Obtén el correo electrónico del usuario autenticado
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Obtén el correo y elimina la parte después de '@'
                val email = currentUser.email
                val userId = email?.substringBefore('@') // Esto te da el ID que corresponde al documento de Firestore

                // Recupera el documento de Firestore con ese ID
                if (userId != null) {
                    obtenerCarrera(userId)
                } else {
                    Toast.makeText(this, "Error al obtener el ID del usuario.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }
            //para saber si esta habilitado o no
            // para saber si esta habilitado o no
            val estadoTextView: TextView = findViewById(R.id.estado)  // El TextView donde mostrarás el estado

            // Verificar si el usuario está autenticado
            if (currentUser != null) {
                // Obtener el correo del usuario y extraer el ID antes del '@'
                val email = currentUser.email
                val userId = email?.substringBefore('@')  // Esto te da el ID del usuario

                if (userId != null) {
                    // Accede a Firestore para verificar el estado de habilitación
                    val db = FirebaseFirestore.getInstance()
                    db.collection("Elector")
                        .document(userId)  // Usa el ID como nombre del documento
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // Obtener el campo "habilitado" de Firestore
                                val habilitado = document.getBoolean("habilitado") ?: false
                                // Mostrar el estado basado en el valor "habilitado"
                                if (habilitado) {
                                    estadoTextView.text = "Estado: HABILITADO"
                                    estadoTextView.setTextColor(Color.GREEN)
                                } else {
                                    estadoTextView.text = "Estado: NO HABILITADO"
                                    estadoTextView.setTextColor(Color.RED)
                                }
                            } else {
                                // Si el documento no existe
                                estadoTextView.text = "Usuario no encontrado"
                                estadoTextView.setTextColor(Color.GRAY)
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Si ocurre un error al obtener el documento
                            Toast.makeText(this, "Error al verificar el estado: ${exception.message}", Toast.LENGTH_SHORT).show()
                            estadoTextView.text = "Error al verificar estado"
                            estadoTextView.setTextColor(Color.GRAY)
                        }
                } else {
                    Toast.makeText(this, "No se pudo obtener el ID del usuario.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }

            // para el edit text de buscar votaciones
            val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
            //para el boton de buscar
            val buscadorButton = findViewById<ImageButton>(R.id.buscador)
            // Inicializa el RecyclerView
            recyclerView = findViewById(R.id.lista_votaciones)
            recyclerView.layoutManager = LinearLayoutManager(this)
            // Inicializa la lista de votaciones
            votacionesOriginales = mutableListOf()
            // Carga las votaciones desde Firestore
            cargarVotaciones()
            // Inicializa el adaptador con la lista vacía de votaciones
            votacionAdapter = VotacionAdapter(votacionesOriginales)
            recyclerView.adapter = votacionAdapter


            buscadorButton.setOnClickListener {
                val textoBuscado = buscarVotaciones.text.toString().trim()
                filtrarVotaciones(textoBuscado)
            }
            // Inicializa el adaptador con todas las votaciones
            votacionAdapter = VotacionAdapter(votacionesOriginales)
            recyclerView.adapter = votacionAdapter

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
            // Cerrar sesión de Firebase
            auth.signOut()


            // Cerrar sesión de Google
            googleSignInClient.signOut().addOnCompleteListener {
                // También puedes revocar el acceso para que no recuerde el correo
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    // Mostrar un mensaje de cierre de sesión
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

                    // Redirige al layout de inicio de sesión (Loggin2Activity)
                    val intent = Intent(this, Loogin2::class.java)
                    startActivity(intent)
                    finish() // Cierra la actividad actual para que no pueda volver con el botón "atrás"
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

                // Manejar el clic del botón
                holder.nombreButton.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(context, DetalleVotacionActivity::class.java)
                    intent.putExtra("VOTACION_NOMBRE", votacion.nombre) // Pasar el nombre o cualquier otra información necesaria
                    context.startActivity(intent)
                }
            }

            override fun getItemCount() = votaciones.size
        }
        private fun filtrarVotaciones(textoBuscado: String) {
            val listaFiltrada = if (textoBuscado.isEmpty()) {
                votacionesOriginales // Mostrar todas si no hay texto
            } else {
                votacionesOriginales.filter { it.nombre.contains(textoBuscado, ignoreCase = true) }
            }
            votacionAdapter = VotacionAdapter(listaFiltrada)
            recyclerView.adapter = votacionAdapter
        }
        private fun obtenerCarrera(userId: String) {
            // Accede al documento del usuario en la colección "Elector"
            db.collection("Elector")
                .document(userId)  // Usa el ID de usuario como ID del documento
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Obtén el campo "carrera" del documento
                        val carrera = document.getString("carrera")
                        // Muestra la carrera en el TextView
                        if (carrera != null) {
                            val carreraTextView: TextView = findViewById(R.id.carrera)
                            carreraTextView.text = "Carrera: $carrera"
                        } else {
                            Toast.makeText(this, "No se encontró el campo 'carrera'.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No se encontró el documento del usuario.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error al obtener la carrera: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
        // Función para cargar las votaciones desde Firebase
        private fun cargarVotaciones() {
            // Accede a la colección "Votacion" de Firestore
            db.collection("Votacion")
                .get()  // Obtiene todos los documentos de la colección
                .addOnSuccessListener { result ->
                    // Limpia la lista actual antes de agregar los nuevos datos
                    votacionesOriginales.clear()

                    // Itera sobre los documentos obtenidos
                    for (document in result) {
                        val tipoVotacion = document.getString("tipoVotacion") ?: "Desconocido" // Obtiene el tipoVotacion (si no existe, usa "Desconocido")
                        val estado = document.getBoolean("estado") ?: false // Obtiene el estado (si no existe, usa 'false')

                        // Agrega la votación a la lista
                        votacionesOriginales.add(Votacion(tipoVotacion, if (estado) "ACTIVO" else "VENCIDO", if (estado) Color.GREEN else Color.RED))
                    }

                    // Notifica al adaptador que los datos han cambiado
                    votacionAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    // Si ocurre un error al obtener los documentos
                    Toast.makeText(this, "Error al cargar votaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }