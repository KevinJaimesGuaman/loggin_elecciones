package com.example.loggin_elecciones

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import yuku.ambilwarna.AmbilWarnaDialog
import java.util.UUID

class add_partido_rector : AppCompatActivity() {

    private lateinit var etNombrePartido: EditText
    private lateinit var etAcronimo: EditText
    private lateinit var btnSeleccionarColor: Button
    private lateinit var btnAddCandidato: Button
    private lateinit var btnRemoveCandidato: Button
    private lateinit var rvCandidatos: RecyclerView

    private var selectedColor: Int = 0xFFFFFF
    private var colorPartido: String = ""
    private val listaCandidatos = mutableListOf<CandidatoEdit>() // Cambiado a CandidatoEdit
    private lateinit var candidatoAdapter2: CandidatoEditAdapter // Cambiado a CandidatoEditAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_partido_rector)
        val nombreEleccion = intent.getStringExtra("nombreEleccion")

        // Inicializar vistas
        etNombrePartido = findViewById(R.id.et_nombre_partido)
        etAcronimo = findViewById(R.id.et_acronimo)
        btnSeleccionarColor = findViewById(R.id.btn_seleccionar_color)
        btnAddCandidato = findViewById(R.id.btn_add_candidato)
        btnRemoveCandidato = findViewById(R.id.btn_remove_candidato)
        rvCandidatos = findViewById(R.id.rv_candidatos)

        // Configurar RecyclerView
        candidatoAdapter2 = CandidatoEditAdapter(listaCandidatos) // Asegúrate de usar el adaptador correcto
        rvCandidatos.layoutManager = LinearLayoutManager(this)
        rvCandidatos.adapter = candidatoAdapter2

        // Botón para seleccionar color
        btnSeleccionarColor.setOnClickListener {
            openColorPicker()
        }

        // Botón para agregar candidato
        btnAddCandidato.setOnClickListener {
            listaCandidatos.add(CandidatoEdit("", "")) // Añadido CandidatoEdit
            candidatoAdapter2.notifyItemInserted(listaCandidatos.size - 1)
        }

        // Botón para remover candidato
        btnRemoveCandidato.setOnClickListener {
            if (listaCandidatos.isNotEmpty()) {
                val index = listaCandidatos.size - 1
                listaCandidatos.removeAt(index)
                candidatoAdapter2.notifyItemRemoved(index)
            } else {
                Toast.makeText(this, "No hay candidatos para eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para guardar partido
        findViewById<Button>(R.id.boton_add).setOnClickListener {
            guardarPartido(nombreEleccion.toString())
        }

        // Botón para retroceder
        val botonVolver = findViewById<ImageButton>(R.id.volver)
        botonVolver.setOnClickListener {
            finish()
        }
    }

    private fun openColorPicker() {
        val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                Toast.makeText(this@add_partido_rector, "Selección de color cancelada", Toast.LENGTH_SHORT).show()
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                // Asignar el color seleccionado
                selectedColor = color

                // Convertir el color a formato hexadecimal
                val hexColor = String.format("#%06X", (0xFFFFFF and selectedColor))
                colorPartido = hexColor
                // Mostrar el color seleccionado en el botón
                btnSeleccionarColor.setBackgroundColor(selectedColor)

                // Puedes usar el color en formato hexadecimal para lo que necesites
                Toast.makeText(this@add_partido_rector, "Color seleccionado: $hexColor", Toast.LENGTH_SHORT).show()
            }
        })
        colorPicker.show()
    }

    private fun guardarPartido(nombreEleccion: String) {
        val nombrePartido = etNombrePartido.text.toString().trim().uppercase()
        val acronimo = etAcronimo.text.toString().trim().uppercase()

        // Validar que los campos no estén vacíos
        if (nombrePartido.isEmpty() ) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val partido = hashMapOf(
            "votos" to 0,
            "acronimo" to acronimo,
            "color" to colorPartido
        )

        val db = FirebaseFirestore.getInstance()

        // Verificar si el partido ya existe
        val eleccionesRef = db.collection("TipoEleccion").document(nombreEleccion)
        val partidosRef = eleccionesRef.collection("Partido").document(nombrePartido)

        partidosRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Si ya existe el partido, mostrar un mensaje
                Toast.makeText(this, "El partido ya existe", Toast.LENGTH_SHORT).show()
            } else {
                // Guardar partido si no existe
                partidosRef.set(partido).addOnSuccessListener {
                    Toast.makeText(this, "Partido"+nombrePartido+ "guardado correctamente en la eleccion"+nombreEleccion, Toast.LENGTH_SHORT).show()
                    guardarCandidatos(partidosRef)  // Guardar candidatos asociados
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Error al guardar el partido: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error al verificar la existencia del partido: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarCandidatos(partidoRef: DocumentReference) {
        listaCandidatos.forEach { candidato ->
            // Validar que los campos de candidato no estén vacíos
            if (candidato.nombre.isNotEmpty() && candidato.cargo.isNotEmpty()) {
                // Convertir el nombre y el cargo a mayúsculas
                val nombreEnMayusculas = candidato.nombre.uppercase()
                val cargoEnMayusculas = candidato.cargo.uppercase()

                val candidatoData = hashMapOf("nombre" to nombreEnMayusculas)
                val candidatoRef = partidoRef.collection("Puestos").document(cargoEnMayusculas)

                // Verificar si ya existe un documento con el mismo cargo antes de guardar
                candidatoRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Toast.makeText(
                                this,
                                "El cargo $cargoEnMayusculas ya está registrado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Guardar el candidato si no existe
                            candidatoRef.set(candidatoData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Candidato guardado: $nombreEnMayusculas",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        this,
                                        "Error al guardar el candidato: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Error al verificar duplicados: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(this, "Nombre y cargo del candidato son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class CandidatoEdit(
    var nombre: String = "",
    var cargo: String = ""
)
// Clase CandidatoEditAdapter
class CandidatoEditAdapter(
    private val listaCandidatos: MutableList<CandidatoEdit> // Cambié Candidato a CandidatoEdit
) : RecyclerView.Adapter<CandidatoEditAdapter.CandidatoViewHolder>() { // Asegúrate de usar CandidatoViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nuevo_candidato, parent, false)
        return CandidatoViewHolder(view) // Asegúrate de devolver el tipo correcto
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

    // Definición del ViewHolder
    inner class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etNombreCandidato: EditText = itemView.findViewById(R.id.et_nombre_candidato)
        val etCargo: EditText = itemView.findViewById(R.id.et_cargo)
    }
}
