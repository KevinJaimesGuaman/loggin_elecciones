package com.example.loggin_elecciones

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DatosPartido : AppCompatActivity() {
    private val cargosList = mutableListOf<Cargo>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var nombrePartidoTextView: TextView
    private lateinit var volverButton: ImageButton
    private lateinit var adapter: CargosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_partido)

        // Inicializar elementos de la interfaz
        recyclerView = findViewById(R.id.cargos)
        nombrePartidoTextView = findViewById(R.id.Nombre_Partido)
        volverButton = findViewById(R.id.volver)

        // Recuperar el nombre del partido que se pasó desde la actividad anterior
        val nombrePartido = intent.getStringExtra("PARTIDO_NOMBRE")
        nombrePartidoTextView.text = nombrePartido ?: "Nombre del Partido no disponible"
        val tipoVotacionNombre = intent.getStringExtra("Tipo_De_Votacion")

        // Configurar el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar el adaptador antes de usarlo
        adapter = CargosAdapter(cargosList)
        recyclerView.adapter = adapter

        // Obtener los cargos del partido desde Firestore
        getCargos(nombrePartido, tipoVotacionNombre.toString())

        // Configurar el botón de regreso
        volverButton.setOnClickListener {
            onBackPressed()  // Regresar a la pantalla anterior
        }
    }

    // Función para obtener cargos desde Firestore
    private fun getCargos(nombrePartido: String?, tipoVotacionNombre: String) {
        val db = FirebaseFirestore.getInstance()
        val cargoRef = db.collection("TipoEleccion")
            .document(tipoVotacionNombre)
            .collection("Partido")
            .document(nombrePartido ?: "")
            .collection("Puestos")

        cargoRef.get()
            .addOnSuccessListener { documents ->
                // Vaciar la lista antes de agregar nuevos datos
                cargosList.clear()

                for (document in documents) {
                    val cargo = document.id
                    val nombre = document.getString("nombre") ?: "Nombre no disponible"
                    val datos = Cargo(cargo, nombre)
                    // Agregar el cargo a la lista original
                    cargosList.add(datos)
                }
                // Notificar al adaptador que los datos han cambiado
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Aquí puedes manejar el error, si ocurre
            }
    }

    // Clase para representar cada cargo
    data class Cargo(val cargo: String, val nombre: String)

    // Adaptador para el RecyclerView
    class CargosAdapter(private val cargos: MutableList<Cargo>) : RecyclerView.Adapter<CargosAdapter.CargoViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CargoViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_cargo, parent, false)
            return CargoViewHolder(view)
        }

        override fun onBindViewHolder(holder: CargoViewHolder, position: Int) {
            val cargo = cargos[position]
            holder.cargoTextView.text = "${cargo.cargo}:"  // Mostrar el cargo
            holder.nombreTextView.text = cargo.nombre      // Mostrar el nombre del miembro
        }

        override fun getItemCount(): Int = cargos.size

        // ViewHolder para los items de cargo
        class CargoViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val cargoTextView: TextView = view.findViewById(R.id.cargoTextView)
            val nombreTextView: TextView = view.findViewById(R.id.nombreTextView)
        }
    }
}