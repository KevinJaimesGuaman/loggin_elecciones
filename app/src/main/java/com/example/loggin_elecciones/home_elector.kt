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
import com.google.firebase.ktx.Firebase

class home_elector : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var votacionAdapter: VotacionAdapter
    private val votacionesOriginales = mutableListOf<Votacion>()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityCrearCuentaBinding? = null
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
        val currentUser = auth.currentUser


        if (currentUser != null) {
            // Obtiene el nombre del usuario
            val displayName = currentUser.displayName
            // Asigna el nombre del usuario al TextView
            if (displayName != null) {
                nombreTextView.text = "Nombre: $displayName"
            }
        }
        //Para la carrera ejemplo SE DEBE MODIFICAR
        val carreraTextView: TextView = findViewById(R.id.carrera)
        carreraTextView.text = "Carrera: ING ELECTROMECANICA DE FLUIDOS"
        //Para habilitado IGUAL DE EJEMPLO
        val estadoTextView: TextView = findViewById(R.id.estado)
        estadoTextView.text = "Estado: HABILITADO"
        // para el edit text de buscar votaciones
        val buscarVotaciones = findViewById<EditText>(R.id.buscar_votaciones)
        //para el boton de buscar
        val buscadorButton = findViewById<ImageButton>(R.id.buscador)
        // Inicializa el RecyclerView
        recyclerView = findViewById(R.id.lista_votaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Datos de votación y estados
        votacionesOriginales.addAll(listOf(
            Votacion("VOTACION 1", "ACTIVO", Color.GREEN),
            Votacion("VOTACION 2", "VENCIDO", Color.RED),
            Votacion("VOTACION 3", "AUN NO EMPEZO", Color.GRAY),
            Votacion("VOTACION 4", "ACTIVO", Color.GREEN),
            Votacion("VOTACION 5", "ACTIVO", Color.GREEN),
            Votacion("VOTACION 6", "ACTIVO", Color.GREEN),
            Votacion("Votacion de  Rector", "ACTIVO", Color.GREEN),
            Votacion("Votacion de  facultativo", "vencido", Color.GREEN)
        ))
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

            // Manejar el clic del botón para abrir la actividad emitir_voto
            holder.nombreButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, emitir_voto::class.java)
                // Pasar el nombre de la votación o cualquier otra información que necesites
                intent.putExtra("VOTACION_NOMBRE", votacion.nombre)
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

}