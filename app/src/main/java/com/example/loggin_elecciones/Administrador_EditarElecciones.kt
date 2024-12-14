package com.example.loggin_elecciones


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Administrador_EditarElecciones : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val tiposEleccion= mutableListOf<PreEleccion>()
    private var tiempoSeleccionadoFin: Timestamp? = null
    private var tiempoSeleccionadoInicio: Timestamp? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PartidoAdapter
    private val partidoList = mutableListOf<PartidoItem>()
    val tipoVotacion = TipoVotacion(
        tipoDeVotacion = listOf("Rectorado", "OTRO"),
        estadoTipodeVotacion = listOf(false, false),
        tipoVotacionnombre = "",
        fechaIni = Timestamp.now()  ,  // Dejamos vacío por ahora
        fechayHoraFin =  Timestamp.now() ,  // Timestamp actual
        estado = "",    // Dejamos vacío por ahora
        otro="",
        carrerasDestinadas = emptyList(),  // Lista vacía
        partidos = mutableListOf()  // Lista vacía
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_administrador_editar_elecciones)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val nombreEleccion= intent.getStringExtra("votacionId")
        // Configurar el RecyclerView
        recyclerView = findViewById(R.id.lista_de_partidos_añadidos)
        adapter = PartidoAdapter(partidoList,nombreEleccion.toString(),context = this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Cargar los datos desde Firestore
        cargarPartidosDesdeFirestore(nombreEleccion.toString())
        // Configurar el botón de volver
        val buttonVolver = findViewById<ImageButton>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }

        // Configurar el Spinner para Ver en  que votacion estas
        val botonEleccion: Button = findViewById(R.id.button_eleccion_actual)

        botonEleccion.text= nombreEleccion

        // para seleccionar carreras
        val botonCarrerasDestinadas: Button = findViewById(R.id.carrerasDestinadas_crear)
        val listaCarrerasSeleccionadas = mutableListOf<String>() // Lista de carreras seleccionadas
        val listaCarrerasFirebase = mutableListOf<String>() // Lista de carreras obtenidas de Firebase
        val estadoSeleccionCarreras = mutableMapOf<String, Boolean>() // Estado de selección de carreras

        // Obtener la lista de carreras destinadas del documento "rectorado" en la colección "TipoEleccion"
        db.collection("TipoEleccion").document(nombreEleccion.toString())
            .get()
            .addOnSuccessListener { tipoEleccionDoc ->
                if (tipoEleccionDoc != null && tipoEleccionDoc.exists()) {
                    val carrerasDestinadas = tipoEleccionDoc.get("carrerasDestinadas") as? List<String> ?: emptyList()

                    // Obtener las carreras desde la colección "carreras"
                    db.collection("carreras").document("fcyt")
                        .get()
                        .addOnSuccessListener { carrerasDoc ->
                            if (carrerasDoc != null && carrerasDoc.exists()) {
                                for ((key, value) in carrerasDoc.data ?: emptyMap<String, Any>()) {
                                    if (value is String) {
                                        listaCarrerasFirebase.add(value)
                                        // Verificar si la carrera está en "carrerasDestinadas" para asignar true
                                        estadoSeleccionCarreras[value] = carrerasDestinadas.contains(value)
                                    }
                                }
                            }

                            // Configurar el botón para mostrar el diálogo cuando se presione
                            botonCarrerasDestinadas.setOnClickListener {
                                if (listaCarrerasFirebase.isEmpty()) {
                                    Toast.makeText(this, "No hay carreras disponibles para seleccionar.", Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }

                                val opciones = listaCarrerasFirebase.toTypedArray()
                                val seleccionados = BooleanArray(opciones.size) { index ->
                                    estadoSeleccionCarreras[opciones[index]] ?: false
                                }

                                val dialog = AlertDialog.Builder(this)
                                    .setTitle("Seleccionar Carreras")
                                    .setMultiChoiceItems(opciones, seleccionados) { _, which, isChecked ->
                                        seleccionados[which] = isChecked
                                        estadoSeleccionCarreras[opciones[which]] = isChecked
                                    }
                                    .setPositiveButton("Aceptar") { _, _ ->
                                        listaCarrerasSeleccionadas.clear()

                                        for (i in opciones.indices) {
                                            if (seleccionados[i]) {
                                                listaCarrerasSeleccionadas.add(opciones[i])
                                            }
                                        }

                                        Log.d("CarrerasSeleccionadas", "Seleccionadas: $listaCarrerasSeleccionadas")
                                        tipoVotacion.carrerasDestinadas = listaCarrerasSeleccionadas
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .create()

                                dialog.show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firebase", "Error al obtener las carreras", exception)
                        }
                } else {
                    Log.e("Firebase", "No se encontró el documento rectorado en TipoEleccion.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener el documento rectorado", exception)
            }
// Inicializar botones
        val buttonFechaHoraInicio: Button = findViewById(R.id.button_fecha_inicio_crear)
        val buttonFechaHoraFin: Button = findViewById(R.id.button_fecha_fin_crear)

// Verificar y cargar datos de Firebase
        db.collection("TipoEleccion").document(nombreEleccion.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fechaHoraInicio = document.getTimestamp("fechaInicio")
                    val fechaHoraFin = document.getTimestamp("fechaFin")

                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    // Mostrar fecha de inicio si existe
                    if (fechaHoraInicio != null) {
                        val formattedDate = format.format(fechaHoraInicio.toDate())
                        buttonFechaHoraInicio.text = formattedDate
                        tiempoSeleccionadoInicio = fechaHoraInicio
                    }

                    // Mostrar fecha de fin si existe
                    if (fechaHoraFin != null) {
                        val formattedDate = format.format(fechaHoraFin.toDate())
                        buttonFechaHoraFin.text = formattedDate
                        tiempoSeleccionadoFin = fechaHoraFin
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener fechas de Firebase", exception)
            }
     // Configurar el botón de fecha y hora de inicio
    buttonFechaHoraInicio.setOnClickListener {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Mínimo un día después de hoy
        val minDate = calendar.timeInMillis // Timestamp para la fecha mínima
        val datePicker = DatePickerDialog(
                this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                val dayOfWeek = selectedCalendar.get(Calendar.DAY_OF_WEEK)

                // Verificar si el día seleccionado no es sábado ni domingo
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    Toast.makeText(
                        this,
                        "No puedes seleccionar fines de semana. Por favor, elige otro día.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@DatePickerDialog
                }

                val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear 08:00" // Fecha con hora fija 08:00

                buttonFechaHoraInicio.text = formattedDate

                // Parsear la fecha y asignarla como Timestamp
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                try {
                    val dateIni: Date = format.parse(formattedDate) ?: throw IllegalArgumentException("Fecha inválida: $formattedDate")
                        tiempoSeleccionadoInicio = Timestamp(dateIni)
                        Toast.makeText(this, "Fecha y hora de inicio seleccionadas correctamente.", Toast.LENGTH_SHORT).show()
                        // Actualizar automáticamente la fecha de fin con la misma fecha y hora fija
                        val formattedEndDate = "$selectedDay/${selectedMonth + 1}/$selectedYear 16:00"
                        buttonFechaHoraFin.text = formattedEndDate
                        val dateFin: Date = format.parse(formattedEndDate) ?: throw IllegalArgumentException("Fecha inválida: $formattedEndDate")
                        tiempoSeleccionadoFin = Timestamp(dateFin)
                    } catch (e: Exception) {
                        Log.e(
                            "ErrorFechaHora",
                            "Error al procesar la fecha y hora: $formattedDate",
                            e
                        )
                        Toast.makeText(
                            this,
                            "Error al seleccionar fecha y hora. Revisa el formato.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Configurar la fecha mínima como un día después de la actual
            datePicker.datePicker.minDate = minDate

            datePicker.show()
        }

        // Deshabilitar el botón de fin
        buttonFechaHoraFin.isEnabled = false


//boton añadir------------------------------------------------------------------------------------------------
    // Configurar el botón para guardar en Firebase
        val buttonGuardar: Button = findViewById(R.id.añadir_Eleccion)
        buttonGuardar.setOnClickListener {
            showWarningDialog(nombreEleccion.toString())
        }

    //boton añadir Partido
        val imageViewAñadirPartido= findViewById<ImageView>(R.id.imageView_añadir_editar)
        imageViewAñadirPartido.setOnClickListener {
                val intent = Intent(this, add_partido_rector::class.java)
                intent.putExtra("nombreEleccion", nombreEleccion.toString())
                startActivity(intent)
        }
    }
    private fun cargarPartidosDesdeFirestore(nombreEleccion: String) {
        partidoList.clear()  // Limpiar la lista antes de cargar los nuevos partidos
        val coleccionPartidos =
            db.collection("TipoEleccion")
                .document(nombreEleccion)
                .collection("Partido")

        // Usar addSnapshotListener para escuchar cambios en tiempo real
        coleccionPartidos.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                // Manejar el error
                Log.e("FirestoreError", "Error al cargar partidos en tiempo real", exception)
                Toast.makeText(this, "No se pudieron cargar los partidos", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            // Iterar sobre los documentos y agregar solo los partidos no duplicados
            querySnapshot?.forEach { document ->
                val nombre = document.id

                // Verificar si el partido ya está en la lista antes de agregarlo
                if (partidoList.none { it.nombre == nombre }) {
                    partidoList.add(PartidoItem(nombre))  // Agregar el partido solo si no existe
                }
            }

            // Notificar al adaptador sobre los cambios
            adapter.notifyDataSetChanged()
        }
    }

    private fun showWarningDialog(nombreEleccion: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Advertencia")
        builder.setMessage("¿Estás seguro de que deseas continuar?")
        builder.setPositiveButton("Sí") { _, _ ->

            if (tiempoSeleccionadoFin != null) {
                // Crear el mapa de datos a guardar
                val data = hashMapOf(
                    "fechaFin" to tiempoSeleccionadoFin!!,
                    "fechaInicio" to tiempoSeleccionadoInicio!!,
                    "carrerasDestinadas" to tipoVotacion.carrerasDestinadas
                )


                // Guardar el nuevo documento en "TipoEleccion"
                db.collection("TipoEleccion")
                    .document(nombreEleccion)
                    .set(data)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Dato guardado con éxito")
                        Toast.makeText(this, "Dato guardado con éxito", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, home_administrador::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error al guardar el dato", e)
                        Toast.makeText(this, "Error al guardar el dato", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Selecciona una fecha y hora antes de guardar", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    data class PartidoItem(val nombre: String)

    class PartidoAdapter(
        private val items: MutableList<PartidoItem>,
        private val nombreEleccion: String,
        private val context: Context
    ) : RecyclerView.Adapter<PartidoAdapter.PartidoViewHolder>() {

        private val db = FirebaseFirestore.getInstance()

        class PartidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombrePartido: TextView = itemView.findViewById(R.id.nombrePartido)
            val imageView: ImageView = itemView.findViewById(R.id.imageView_borrarPartido)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.partidos_item, parent, false)
            return PartidoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PartidoViewHolder, position: Int) {
            val partido = items[position]
            holder.nombrePartido.text = partido.nombre

            holder.imageView.setOnClickListener {
                // Verificar si el partido es "Blanco"
                if (partido.nombre == "Blanco") {
                    Toast.makeText(context, "El partido 'Blanco' no se puede eliminar.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Mostrar el diálogo de confirmación si no es "Blanco"
                mostrarDialogoConfirmacion(partido.nombre, position)
            }
        }

        private fun mostrarDialogoConfirmacion(nombre: String, position: Int) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirmar eliminación")
            builder.setMessage("¿Estás seguro de que deseas eliminar el partido '$nombre'?")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                // Confirmar eliminación
                borrarPartido(nombre, position)
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                // Cancelar la eliminación
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        private fun borrarPartido(nombre: String, position: Int) {
            items.removeAt(position)
            val partidoRef = db.collection("TipoEleccion").document(nombreEleccion)
                .collection("Partido").document(nombre)

            // Primero eliminamos los documentos dentro de la subcolección "Puestos"
            val puestosRef = partidoRef.collection("Puestos")

            puestosRef.get().addOnSuccessListener { querySnapshot ->
                // Usamos un batch para eliminar todos los documentos de la subcolección
                val batch = db.batch()

                for (document in querySnapshot) {
                    batch.delete(document.reference)  // Eliminar cada documento de "Puestos"
                }

                // Ejecutamos el batch para eliminar todos los documentos de "Puestos"
                batch.commit().addOnSuccessListener {
                    // Después de eliminar los documentos de "Puestos", eliminamos el documento del partido
                    partidoRef.delete().addOnSuccessListener {
                        Log.d("Firestore", "Documento con ID $nombre eliminado exitosamente.")

                        // Limpiar la lista de partidos antes de agregar los nuevos datos

                        // Mostrar el Toast
                        Toast.makeText(context, "Documento eliminado.", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        Log.e("FirestoreError", "Error al eliminar el documento", exception)
                        Toast.makeText(context, "Error al eliminar el documento.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error al eliminar los documentos de Puestos", exception)
                    Toast.makeText(context, "Error al eliminar los candidatos.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error al obtener documentos de Puestos", exception)
                Toast.makeText(context, "Error al obtener los candidatos.", Toast.LENGTH_SHORT).show()
            }
        }


        override fun getItemCount(): Int = items.size
    }

}