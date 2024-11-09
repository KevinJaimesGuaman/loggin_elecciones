package com.example.loggin_elecciones

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DatosPartido : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nombrePartidoTextView: TextView
    private lateinit var volverButton: ImageButton

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

        // Datos de ejemplo para el RecyclerView
        val cargos = listOf(
            Cargo("Secretaria", "Jose Luis Torres"),
            Cargo("Tesorero", "Ana Pérez"),
            Cargo("Vocal", "Carlos Ruiz")
        )

        // Configurar el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = CargosAdapter(cargos)
        recyclerView.adapter = adapter

        // Configurar el botón de regreso
        volverButton.setOnClickListener {
            onBackPressed()  // Regresar a la pantalla anterior
        }
    }
}

// Clase para representar cada cargo
data class Cargo(val cargo: String, val nombre: String)

// Adaptador para el RecyclerView
class CargosAdapter(private val cargos: List<Cargo>) : RecyclerView.Adapter<CargosAdapter.CargoViewHolder>() {

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

