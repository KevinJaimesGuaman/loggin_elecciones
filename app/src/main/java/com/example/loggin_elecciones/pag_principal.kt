package com.example.loggin_elecciones

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
class pag_principal : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_pag_principal)
        auth = Firebase.auth
        // Configura GoogleSignInOptions
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

    }

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
}


