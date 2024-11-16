package com.example.loggin_elecciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.loggin_elecciones.databinding.ActivityLoogin2Binding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore;


class Loogin2 : AppCompatActivity() {
    private lateinit var binding: ActivityLoogin2Binding
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tipoUsuario: String

    private lateinit var usuario_administrador: EditText
    private lateinit var contraseña_administrador: EditText
    private lateinit var button_iniciar_admin: Button

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "GoogleSignIn"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_loogin2)

        // Ajuste de los insets de las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //enlaza con la vista principal usando view BIDING
        binding = ActivityLoogin2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //boton docente
        val clickListener= View.OnClickListener { view ->
            binding.btEstudiante.setBackgroundColor(ContextCompat.getColor(this, R.color.default_gris))
            binding.btCorteElectoral.setBackgroundColor(ContextCompat.getColor(this, R.color.default_gris))
            binding.btAdministrador.setBackgroundColor(ContextCompat.getColor(this, R.color.default_gris))

            when (view.id) {
                R.id.bt_estudiante -> {
                    tipoUsuario = "estudiante"
                    binding.textViewLogin.text = "Por favor, ingresar con la cuenta Institucional"
                    binding.btEstudiante.setBackgroundColor(ContextCompat.getColor(this, R.color.seleccion_elector)) // Color seleccionado
                    if(binding.Administradorcontenedor.visibility == View.VISIBLE){
                        binding.Administradorcontenedor.visibility = View.GONE
                        binding.contenedorInicioSecion.visibility = View.VISIBLE
                    }
                    if(binding.contenedorInicioSecion.visibility != View.VISIBLE){
                        binding.contenedorInicioSecion.visibility = View.VISIBLE
                    }
                }
                R.id.bt_corte_electoral -> {
                    tipoUsuario = "corte electoral"
                    binding.textViewLogin.text = "Por favor, ingresar con la cuenta Corte Electoral"
                    binding.btCorteElectoral.setBackgroundColor(ContextCompat.getColor(this, R.color.seleccion_corte)) // Color seleccionado
                    if(binding.Administradorcontenedor.visibility == View.VISIBLE){
                        binding.Administradorcontenedor.visibility = View.GONE
                        binding.contenedorInicioSecion.visibility = View.VISIBLE
                    }
                    if(binding.contenedorInicioSecion.visibility != View.VISIBLE){
                        binding.contenedorInicioSecion.visibility = View.VISIBLE
                    }
                }

                R.id.bt_Administrador -> {
                    tipoUsuario = "administrador"
                    binding.AdministradorTextLogin.text = "Por favor, ingresar con la cuenta Administrador"
                    binding.btAdministrador.setBackgroundColor(ContextCompat.getColor(this, R.color.seleccion_admin)) // Color seleccionado
                    if(binding.contenedorInicioSecion.visibility == View.VISIBLE){
                        binding.contenedorInicioSecion.visibility = View.GONE
                        binding.Administradorcontenedor.visibility = View.VISIBLE
                    }
                    if(binding.Administradorcontenedor.visibility != View.VISIBLE){
                        binding.Administradorcontenedor.visibility = View.VISIBLE
                    }
                }
            }
        }
        binding.btEstudiante.setOnClickListener(clickListener)
        binding.btAdministrador.setOnClickListener(clickListener)
        binding.btCorteElectoral.setOnClickListener(clickListener)
        //boton docente fin
        db = FirebaseFirestore.getInstance()
        //inicio para el boton de google



        //iniciar firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Verificar si el usuario ha iniciado sesión
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            val email = currentUser.email // Obtener el email del usuario actual
            val usuarioElector = email?.substringBefore("@") // Extraer el nombre de usuario antes del @

            // Asegúrate de que usuarioElector no sea null
            if (usuarioElector != null) {
                val db = FirebaseFirestore.getInstance() // Inicializar Firestore
                val usuariosRef = db.collection("Elector") // Referencia a la colección "Elector"

                // Buscar el documento en Firestore
                usuariosRef.document(usuarioElector).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            /* Documento existe, puedes procesar los datos
                            val carrera = document.getString("carrera")
                            val carnetDeIdentidad = document.getString("carnetDeIdentidad")
                            val habilitado = document.getBoolean("habilitado")*/

                            // Aquí puedes manejar la redirección según tus necesidades
                            val intent = Intent(this, home_elector::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Documento no existe, redirigir a crear_cuenta
                            val intent = Intent(this, crear_cuenta::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Manejo de error al buscar el documento
                        Toast.makeText(this, "Error al verificar usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Manejar el caso donde usuarioElector es null
                Toast.makeText(this, "No se pudo obtener el nombre de usuario.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Manejar el caso donde no hay usuario autenticado
            Toast.makeText(this, "No hay usuario autenticado.", Toast.LENGTH_SHORT).show()
        }

        //sign in
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

           //crear cliente de google
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            //iniciar firebase auth
            firebaseAuth = FirebaseAuth.getInstance()

            // inicio de conf de google boton
            binding.signInButton.setOnClickListener {
                signIn()
            }
        //inicio sesion administrador
        usuario_administrador = findViewById(R.id.usuario_Administrador_loggin)
        contraseña_administrador = findViewById(R.id.contraseña_Administrador_loggin)
        button_iniciar_admin = findViewById(R.id.button_iniciar_admin)

        button_iniciar_admin.setOnClickListener {
            val usuario = usuario_administrador.text.toString().trim()
            val contraseña = contraseña_administrador.text.toString().trim()
            if (usuario.isNotEmpty() && contraseña.isNotEmpty()){
                login(usuario, contraseña)
            }else{
                Toast.makeText(this, "Por favor ingrese su usuario y contraseña", Toast.LENGTH_SHORT).show()

            }
        }
    }


    //iniciar sesion con google
    private fun signIn(){
        val signInIntent= googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

    }
    //fin de iniciar sesion con google

    //manejar el resultado de la actividad
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                account?.let {
                    if (isInstitutionalEmail(it.email,tipoUsuario)) {
                        if (tipoUsuario == "corte electoral") {
                            firebaseAuthWithGoogleCorteElectoral(it)
                        }else{
                            firebaseAuthWithGoogle(it)
                        }
                    }else{
                        googleSignInClient.signOut()
                        Toast.makeText(this,"Correo no institucional", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG,"Google sign in failed", e)
                Toast.makeText(this,"Error al iniciar sesion: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //fin de manejar el resultado de la actividad

    //verificar si el correo es institucional
    private fun isInstitutionalEmail(email: String?,tipoUsuario: String?): Boolean {
        return when (tipoUsuario) {
            "estudiante" -> email?.endsWith("@est.umss.edu") ?: false
            "docente" -> email?.endsWith("@fcyt.umss.edu.bo") ?: false
            "corte electoral" -> email?.endsWith("@est.umss.edu") ==true ||
                    email?.endsWith("@fcyt.umss.edu.bo") ==true
            else -> false

        }
    }
    //fin de verificar si el correo es institucional
    //autenticar con firebase corte Electoral
    private fun firebaseAuthWithGoogleCorteElectoral(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val email = user?.email

                    if (email != null) {
                        val usuarioElector = email.substringBefore("@") // Extraer parte antes del '@'
                        val db = FirebaseFirestore.getInstance()
                        val usuariosRef = db.collection("CorteElectoral")

                        usuariosRef.document(usuarioElector).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // Si el documento existe, redirigir a la pantalla principal
                                    val intent = Intent(this, home_corte::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Si no es parte de la Corte Electoral
                                    signOutAndRedirectToLogin("Usuario no habilitado.")
                                }
                            }
                            .addOnFailureListener { e ->
                                // Manejo de error en la consulta a Firestore
                                Toast.makeText(this, "Error al verificar el usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No se pudo obtener el email del usuario.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Manejar error de autenticación con Firebase
                    Toast.makeText(this, "Error al autenticar con Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun signOutAndRedirectToLogin(message: String) {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Loogin2::class.java)
            startActivity(intent)
            finish()
        }
    }

    //autenticar con firebase elector
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        val credential= GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    updateUI(user?.email)
                    //si el usuario ya ha iniciado sesion, actualiza la UI 21:16 ultima actualizacion
                    val email = user?.email // Obtener el email del usuario actual
                    val usuarioElector = email?.substringBefore("@") // Extraer el nombre de usuario antes del @
                    val db = FirebaseFirestore.getInstance() // Inicializar Firestore
                    val usuariosRef = db.collection("Elector") // Referencia a la colección "Elector"
                    usuariosRef.document(usuarioElector!!).get() // Buscar el documento en Firestore
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val intent = Intent(this, home_elector::class.java)
                                startActivity(intent)
                                finish()
                            }else{
                                val intent = Intent(this, crear_cuenta::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                }else{
                    Toast.makeText(this,"Error al autenticar con firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

    }
    //fin de autenticar con firebase

    //actualizar la UI
    private fun updateUI(email: String?) {
        Toast.makeText(this, "Bienvenido $email", Toast.LENGTH_SHORT).show()
    }
    //fin de actualizar la UI
    //iniciar sesion administrador
    private fun login(usuario: String, contraseña: String) {
        firebaseAuth.signInWithEmailAndPassword(usuario, contraseña)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    // Obtener el correo y extraer solo el nombre (parte antes del '@')
                    val email = user?.email ?: ""
                    val nombreUsuario = email.substringBefore("@") // Extrae la parte antes del '@'
                    // Pasa el nombre del usuario al siguiente activity
                    val intent = Intent(this, home_administrador::class.java)
                    intent.putExtra("USER_NAME", nombreUsuario) // Pasar el nombre del usuario
                    startActivity(intent)
                    finish()
                } else {
                    // Error en el inicio de sesión
                    Toast.makeText(this, "Error en el inicio de sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}




