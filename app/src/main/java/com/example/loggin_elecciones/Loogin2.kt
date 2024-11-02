package com.example.loggin_elecciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
        db = FirebaseFirestore.getInstance()
        //inicio para el boton de google

        //enlaza con la vista principal usando view BIDING
        binding = ActivityLoogin2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        //iniciar firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //verificar si el usuario ya ha iniciado sesion
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            //si el usuario ya ha iniciado sesion, actualiza la UI
            updateUI(currentUser.email)
            val intent= Intent(this, home_elector::class.java)
            startActivity(intent)
            finish()
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
        }



    private fun signIn(){
        val signInIntent= googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                account?.let {
                    if (isInstitutionalEmail(it.email)) {
                        firebaseAuthWithGoogle(it)
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
    private fun isInstitutionalEmail(email: String?): Boolean {
        return email?.endsWith("@est.umss.edu") ?: false // Reemplaza con tu dominio
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        val credential= GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    updateUI(user?.email)

                    val intent = Intent(this, home_elector::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    Toast.makeText(this,"Error al autenticar con firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

    }
    private fun updateUI(email: String?) {
        Toast.makeText(this, "Bienvenido $email", Toast.LENGTH_SHORT).show()
    }
}




