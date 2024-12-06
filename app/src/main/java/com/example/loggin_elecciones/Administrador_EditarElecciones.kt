package com.example.loggin_elecciones

import actividades_de_la_App.Partido
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loggin_elecciones.administrador_CrearEditar.PartidoAdapter
import com.example.loggin_elecciones.administrador_CrearEditar.PartidoItem
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
    private val firestore = FirebaseFirestore.getInstance()
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
        // Configurar el botón de volver................................................................
        val buttonVolver = findViewById<ImageButton>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }
        //..............................................................................................
        // Configurar el Spinner para Ver en  que votacion estas
        val botonEleccion: Button = findViewById(R.id.button_eleccion_actual)

        botonEleccion.text= nombreEleccion

        //------------------------------------------------------------------------------------------------------

        // para seleccionar carreras:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
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
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//fecha y hora inicio
        val buttonFechaHoraInicio: Button = findViewById(R.id.button_fecha_inicio_crear)
// Verificar si ya existe una fecha y hora de inicio en Firestore
        db.collection("TipoEleccion").document(nombreEleccion.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fechaHoraInicio = document.getTimestamp("fechaInicio") // Obtener Timestamp
                    if (fechaHoraInicio != null) {
                        val dateIni = fechaHoraInicio.toDate() // Convertir Timestamp a Date
                        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val formattedDate = format.format(dateIni) // Formatear la fecha

                        buttonFechaHoraInicio.text = formattedDate // Mostrar la fecha y hora almacenada
                        tiempoSeleccionadoInicio = fechaHoraInicio // Asignar el Timestamp a la variable global
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener fecha y hora de inicio", exception)
            }
// Configurar el botón de fecha y hora de inicio
        buttonFechaHoraInicio.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Mostrar DatePickerDialog
            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate =
                        "$selectedDay/${selectedMonth + 1}/$selectedYear" // Formato dd/MM/yyyy

                    // Mostrar TimePickerDialog para formato de 24 horas
                    val timePicker = TimePickerDialog(
                        this,
                        { _, selectedHour, selectedMinute ->

                            // No se necesita AM/PM porque usaremos el formato de 24 horas
                            val formattedTime = String.format(
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            ) // Formato HH:mm

                            // Combinamos la fecha y la hora
                            val dateString = "$formattedDate $formattedTime" // Cadena combinada

                            // Mostramos el valor de la fecha y hora seleccionada en el botón
                            buttonFechaHoraInicio.text = dateString

                            // Define el formato adecuado para 24 horas
                            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                            try {
                                // Intentamos parsear la fecha y hora a un objeto Date
                                val dateIni: Date = format.parse(dateString)
                                    ?: throw IllegalArgumentException("Fecha inválida: $dateString")

                                // Convertimos Date a Timestamp y asignamos a la variable global
                                tiempoSeleccionadoInicio = Timestamp(dateIni)
                                Toast.makeText(
                                    this,
                                    "Fecha y hora seleccionadas correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                // Log y mensaje de error si hay un problema al procesar la fecha y hora
                                Log.e(
                                    "ErrorFechaHora",
                                    "Error al procesar la fecha y hora: $dateString",
                                    e
                                )
                                Toast.makeText(
                                    this,
                                    "Error al seleccionar fecha y hora. Revisa el formato.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ) // true para 24 horas

                    timePicker.show()

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }
//.............................................................................................................
// Configurar el botón de fecha y hora de fin
        val buttonFechaHoraFin: Button = findViewById(R.id.button_fecha_fin_crear)
        db.collection("TipoEleccion").document(nombreEleccion.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fechaHoraFin = document.getTimestamp("fechaFin") // Obtener Timestamp
                    if (fechaHoraFin != null) {
                        val dateFin = fechaHoraFin.toDate() // Convertir Timestamp a Date
                        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val formattedDate = format.format(dateFin) // Formatear la fecha

                        buttonFechaHoraFin.text = formattedDate // Mostrar la fecha y hora almacenada
                        tiempoSeleccionadoFin = fechaHoraFin // Asignar el Timestamp a la variable global
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener fecha y hora de fin", exception)
            }
        buttonFechaHoraFin.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Mostrar DatePickerDialog
            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate =
                        "$selectedDay/${selectedMonth + 1}/$selectedYear" // Formato dd/MM/yyyy

                    // Mostrar TimePickerDialog con formato de 24 horas
                    val timePicker = TimePickerDialog(
                        this,
                        { _, selectedHour, selectedMinute ->

                            // Formato de hora en 24 horas (sin AM/PM)
                            val formattedTime = String.format(
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            ) // Formato HH:mm

                            // Combinamos la fecha y la hora
                            val dateString = "$formattedDate $formattedTime" // Cadena combinada

                            // Mostramos el valor de la fecha y hora seleccionada en el botón
                            buttonFechaHoraFin.text = dateString

                            // Define el formato adecuado para 24 horas
                            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                            try {
                                // Intentamos parsear la fecha y hora a un objeto Date
                                val date: Date = format.parse(dateString)
                                    ?: throw IllegalArgumentException("Fecha inválida: $dateString")

                                // Convertimos Date a Timestamp y asignamos a la variable global
                                tiempoSeleccionadoFin = Timestamp(date)
                                Toast.makeText(
                                    this,
                                    "Fecha y hora seleccionadas correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                // Log y mensaje de error si hay un problema al procesar la fecha y hora
                                Log.e(
                                    "ErrorFechaHora",
                                    "Error al procesar la fecha y hora: $dateString",
                                    e
                                )
                                Toast.makeText(
                                    this,
                                    "Error al seleccionar fecha y hora. Revisa el formato.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ) // true para formato 24 horas

                    timePicker.show()

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }
//-------------------------------------------------------------------------------------------------------------
        //boton añadir------------------------------------------------------------------------------------------------
        // Configurar el botón para guardar en Firebase
        val buttonGuardar: Button = findViewById(R.id.añadir_Eleccion)
        buttonGuardar.setOnClickListener {
            showWarningDialog(nombreEleccion.toString())
        }
        //  ..............................................................................................................
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