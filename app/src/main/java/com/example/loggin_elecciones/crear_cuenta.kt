package com.example.loggin_elecciones

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.loggin_elecciones.databinding.ActivityCrearCuentaBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class crear_cuenta : AppCompatActivity() {
    private var db: FirebaseFirestore? = null
    private lateinit var auth: FirebaseAuth
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var binding: ActivityCrearCuentaBinding? = null
    private var spinnerCarrera: Spinner? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private var carrera_usuario: String? = null
    private var nombreCompleto: String? = null
    private var codigoSiss: String? = null
    private var habilitado: Boolean = false

    private lateinit var editTextCarnet: EditText
    private lateinit var editTextComplementoCI: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth
        // Infla el binding y establece el contenido de la actividad
        binding = ActivityCrearCuentaBinding.inflate(layoutInflater)
        // Configura el contenido de la actividad
        setContentView(binding!!.root)
        if (currentUser != null) {
            val displayName = currentUser.displayName
            binding!!.textViewNombreFirebase.text = displayName
            val email = currentUser.email
            binding!!.textViewInstitucionalFirebase.text = email
        }
        //spiner de carreras
        spinnerCarrera = findViewById(R.id.spinner_carreras)
        loadCarrerasFromFirestore()
        //fin de spiner de carreras
        val btCancelCreate = findViewById<Button>(R.id.bt_cancel_create)
        // Crea el GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Asegúrate de tener el ID correcto en tu archivo strings.xml
            .requestEmail()
            .build()
        // Crea el GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        val buttonCerrarSecion = findViewById<Button>(R.id.bt_cancel_create)
        // Configura el botón de cierre de sesión
        buttonCerrarSecion.setOnClickListener {
            // Cerrar sesión de Firebase
            signOut()

        }
        //configurar el boton de crear cuenta
        val buttonCrearCuenta = findViewById<Button>(R.id.boton_crear_cuenta)
        editTextCarnet = findViewById<EditText>(R.id.editTextText_carnet)
        editTextComplementoCI = findViewById<EditText>(R.id.editTextText_complemento_ci)
        // Configura el botón de crear cuenta
        buttonCrearCuenta.setOnClickListener {

            validarEntradas()
        }



    }

    // Validar entradas
    private fun validarEntradas() {
        val datosF=FirebaseFirestore.getInstance()
        val Carnet = editTextCarnet.text.toString().trim()
        val complementoCI = editTextComplementoCI.text.toString().trim()
        val email = currentUser?.email // Obtener el email del usuario actual
        nombreCompleto = currentUser?.displayName.toString()
        codigoSiss = email?.substringBefore("@") // Extraer el nombre de usuario antes del @
        carrera_usuario = spinnerCarrera!!.selectedItem.toString() // Obtener la carrera seleccionada
        habilitado= true;
        val datos = hashMapOf(
            "carnet" to Carnet,
            "complementoCI" to complementoCI,
            "carrera" to carrera_usuario,
            "habilitado" to habilitado,
            "Nombre" to nombreCompleto
        )
        if(Carnet.isEmpty()){
            Toast.makeText(this, "Por favor ingrese su carnet", Toast.LENGTH_SHORT).show()
        }
        //registrar usuario en firestore
        datosF.collection("Elector").document(codigoSiss.toString()).set(datos).addOnSuccessListener {
            Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, home_elector::class.java)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
        }
    }
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
    // Carga las carreras desde Firestore
    private fun loadCarrerasFromFirestore() {
        db!!.collection("carreras").document("fcyt").get() // Obtén el documento "fcyt"
            .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                if (task.isSuccessful && task.result != null) {
                    val carreras: MutableList<String> = ArrayList()
                    for (key in task.result!!.data!!.keys) {
                        val carrera = task.result!!.getString(key!!)
                        if (carrera != null) {
                            carreras.add(carrera)
                        }
                    }

                    setupSpinner(carreras)
                } else {
                    Toast.makeText(
                        this@crear_cuenta,
                        "Error al cargar las carreras",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
    // Configura el Spinner con las carreras
    private fun setupSpinner(carreras: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, carreras) // Utiliza el adaptador
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)          // Establecer el diseño desplegable
        spinnerCarrera!!.adapter = adapter                                                      // Establecer el adaptador en el Spinner
        // Manejar la selección de carreras
        spinnerCarrera!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedCarrera = parent.getItemAtPosition(position).toString() // Obtener la carrera seleccionada
                Toast.makeText(
                    this@crear_cuenta,
                    "Seleccionaste: $selectedCarrera",
                    Toast.LENGTH_SHORT
                ).show()// Muestra un mensaje con la carrera seleccionada
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No se seleccionó nada
            }
        }
    }
}