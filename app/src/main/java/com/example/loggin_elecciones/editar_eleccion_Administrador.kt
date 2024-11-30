package com.example.loggin_elecciones

import actividades_de_la_App.TipoVotacion
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loggin_elecciones.emitir_voto.Candidato
import com.example.loggin_elecciones.emitir_voto.CandidatosAdapter
import com.google.firebase.firestore.FirebaseFirestore

class editar_eleccion_Administrador : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    private lateinit var listaPartidosRecyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editar_eleccion_administrador)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //boton volver
        val buttonVolver = findViewById<ImageView>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }
        val votacionId = intent.getStringExtra("votacionId")
        val botonTipoVotacion = findViewById<Button>(R.id.tipoEleccion_editar)
        botonTipoVotacion.text = votacionId ?: "No disponible"
        //Escoger carreras
        val botonCarrerasDestinadas=findViewById<Button>(R.id.BotonCarrerasDestinadas_editar)
        val listaCarrerasSeleccionadas= mutableListOf<String>()
        // Lista de opciones con CheckBoxes
        val opciones = arrayOf("Opción 1", "Opción 2", "Opción 3", "Opción 4")
        val seleccionados = booleanArrayOf(false, false, false, false)  // Estado inicial de los CheckBox
        val listaCarreras = mutableListOf<CarreraItem>()
        db.collection("carreras").document("fcyt")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Itera sobre los campos del documento
                    for ((_, value) in document.data ?: emptyMap<String, Any>()) {
                        listaCarreras.add(CarreraItem(nombre = value.toString()))
                    }
                    // Llamar a la función para mostrar los CheckBox

                } else {
                    Log.e("Firebase", "El documento no existe")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener los datos", exception)
            }
        // Configurar el comportamiento del botón
        botonCarrerasDestinadas.setOnClickListener {
            // Crear un AlertDialog con opciones de selección múltiple
            val dialog = AlertDialog.Builder(this)
                .setTitle("Seleccionar Opciones")
                .setMultiChoiceItems(opciones, seleccionados) { _, which, isChecked ->
                    // Actualizar el estado de las opciones seleccionadas
                    seleccionados[which] = isChecked
                }
                .setPositiveButton("Aceptar") { _, _ ->
                    // Limpiar la lista de opciones seleccionadas antes de llenarla nuevamente
                    listaCarrerasSeleccionadas.clear()

                    // Guardar las opciones seleccionadas en la lista
                    for (i in opciones.indices) {
                        if (seleccionados[i]) {
                            listaCarrerasSeleccionadas.add(opciones[i])  // Añadir a la lista las opciones seleccionadas
                        }
                    }
                    // Aquí puedes hacer algo con las opciones guardadas, como guardarlas en la base de datos
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
        }




    }
    data class CarreraItem(
        val nombre: String,
        var seleccionado: Boolean = false // Agrega el booleano con un valor por defecto
    )

}