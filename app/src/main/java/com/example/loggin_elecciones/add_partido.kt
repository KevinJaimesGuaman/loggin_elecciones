package com.example.loggin_elecciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import yuku.ambilwarna.AmbilWarnaDialog
import java.util.Date

class add_partido : AppCompatActivity() {

    private lateinit var etNombrePartido: EditText
    private lateinit var etAcronimo: EditText
    private lateinit var btnSeleccionarColor: Button
    private lateinit var btnAddCandidato: Button
    private lateinit var btnRemoveCandidato: Button
    private lateinit var rvCandidatos: RecyclerView

    private var selectedColor: Int = 0xFFFFFF
    private val listaCandidatos = mutableListOf<Candidato>()
    private lateinit var candidatoAdapter: CandidatoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_partido)

        // Inicializar vistas
        etNombrePartido = findViewById(R.id.et_nombre_partido)
        etAcronimo = findViewById(R.id.et_acronimo)
        btnSeleccionarColor = findViewById(R.id.btn_seleccionar_color)
        btnAddCandidato = findViewById(R.id.btn_add_candidato)
        btnRemoveCandidato = findViewById(R.id.btn_remove_candidato)
        rvCandidatos = findViewById(R.id.rv_candidatos)

        // Configurar RecyclerView
        candidatoAdapter = CandidatoAdapter(listaCandidatos)
        rvCandidatos.layoutManager = LinearLayoutManager(this)
        rvCandidatos.adapter = candidatoAdapter

        // Botón para seleccionar color
        btnSeleccionarColor.setOnClickListener {
            openColorPicker()
        }

        // Botón para agregar candidato
        btnAddCandidato.setOnClickListener {
            listaCandidatos.add(Candidato("", ""))
            candidatoAdapter.notifyItemInserted(listaCandidatos.size - 1)
        }

        // Botón para remover candidato
        btnRemoveCandidato.setOnClickListener {
            if (listaCandidatos.isNotEmpty()) {
                val index = listaCandidatos.size - 1
                listaCandidatos.removeAt(index)
                candidatoAdapter.notifyItemRemoved(index)
            } else {
                Toast.makeText(this, "No hay candidatos para eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para guardar partido
        findViewById<Button>(R.id.boton_add).setOnClickListener {
            guardarPartido()
        }
        //boton para retroceder
        val botonVolver = findViewById<ImageButton>(R.id.volver)
        botonVolver.setOnClickListener {
            finish()
        }
    }

    private fun openColorPicker() {
        val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                Toast.makeText(this@add_partido, "Selección de color cancelada", Toast.LENGTH_SHORT).show()
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                selectedColor = color
                btnSeleccionarColor.setBackgroundColor(selectedColor)
            }
        })
        colorPicker.show()
    }

    private fun guardarPartido() {
        val nombrePartido = etNombrePartido.text.toString().trim()
        val acronimo = etAcronimo.text.toString().trim()

        if (nombrePartido.isEmpty() || acronimo.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val partido = hashMapOf(
            "nombre" to nombrePartido,
            "acronimo" to acronimo,
            "color" to selectedColor
        )

        val db = FirebaseFirestore.getInstance()
        val eleccionesRef = db.collection("PreColeccion").document("EleccionesDefault")

        // Comprobar si el documento EleccionesDefault ya existe
        eleccionesRef.get().addOnSuccessListener { documento ->
            if (!documento.exists()) {
                // Si no existe, lo creamos con los campos predeterminados
                val fechaInicio = Timestamp(Date()) // Fecha actual
                val fechaFin = Timestamp(Date()) // Se puede ajustar a una fecha específica
                val estado = true // Estado activado por defecto
                val carrerasDestinadas = "Todas las carreras" // Valor predeterminado de ejemplo

                val eleccionesData = hashMapOf(
                    "carrerasDestinadas" to carrerasDestinadas,
                    "fechaInicio" to fechaInicio,
                    "fechaFin" to fechaFin,
                    "estado" to estado
                )

                // Crear el documento EleccionesDefault
                eleccionesRef.set(eleccionesData).addOnSuccessListener {
                    Toast.makeText(this, "EleccionesDefault creada con éxito", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al crear EleccionesDefault", Toast.LENGTH_SHORT).show()
                }
            }

            // Guardamos el partido
            val partidosRef = eleccionesRef.collection("Partidos").document(nombrePartido)
            partidosRef.set(partido).addOnSuccessListener {
                guardarCandidatos(partidosRef)
            }.addOnFailureListener {
                Toast.makeText(this, "Error al guardar el partido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarCandidatos(partidoRef: DocumentReference) {
        listaCandidatos.forEach { candidato ->
            if (candidato.nombre.isNotEmpty() && candidato.cargo.isNotEmpty()) {
                val candidatoData = hashMapOf("nombre" to candidato.nombre)
                partidoRef.collection("Puestos").document(candidato.cargo).set(candidatoData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Candidato guardado: ${candidato.nombre}", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al guardar el candidato: ${candidato.nombre}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}

data class Candidato(
    var nombre: String = "",
    var cargo: String = ""
)

class CandidatoAdapter(
    private val listaCandidatos: MutableList<Candidato>
) : RecyclerView.Adapter<CandidatoAdapter.CandidatoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nuevo_candidato, parent, false)
        return CandidatoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
        val candidato = listaCandidatos[position]

        holder.etNombreCandidato.setText(candidato.nombre)
        holder.etCargo.setText(candidato.cargo)

        // Actualizar los valores en la lista al modificar los campos
        holder.etNombreCandidato.addTextChangedListener { text ->
            listaCandidatos[position].nombre = text.toString()
        }
        holder.etCargo.addTextChangedListener { text ->
            listaCandidatos[position].cargo = text.toString()
        }
    }

    override fun getItemCount(): Int = listaCandidatos.size

    inner class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etNombreCandidato: EditText = itemView.findViewById(R.id.et_nombre_candidato)
        val etCargo: EditText = itemView.findViewById(R.id.et_cargo)
    }
}
