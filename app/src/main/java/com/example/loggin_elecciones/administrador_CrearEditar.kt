package com.example.loggin_elecciones

import actividades_de_la_App.Candidato
import actividades_de_la_App.Partido
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import java.util.Calendar
//importar datosAdministrador
import actividades_de_la_App.TipoVotacion
import android.app.AlertDialog
import android.util.Log
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger


class administrador_CrearEditar : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    // Variable global para almacenar el tiempo seleccionado
    private var tiempoSeleccionadoFin: Timestamp? = null
    private var tiempoSeleccionadoInicio: Timestamp? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_administrador_crear_editar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Configurar el botón de volver................................................................
        val buttonVolver = findViewById<ImageButton>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }
        //..............................................................................................

        //spinner para el tipo de votacion--------------------------------------------------------------
        val spinnerTipoVotacion: Spinner = findViewById(R.id.spinnerTipoEleccion_crear)
        val editTextOtro: TextView = findViewById(R.id.editTextText_eleccionOtro)
        // Crear un ArrayAdapter para el Spinner
        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            tipoVotacion.tipoDeVotacion
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoVotacion.adapter = adapter

        // Agregar un listener para detectar cambios en la selección del Spinner
        spinnerTipoVotacion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                //mientras tenga la primera opcion
                if (position == 0) {
                    tipoVotacion.tipoVotacionnombre = "Rectorado"
                }
                // Mostrar el TextView si se selecciona "OTRO"
                if (parent.getItemAtPosition(position) == "OTRO") {
                    tipoVotacion.tipoVotacionnombre = "OTRO"
                    editTextOtro.visibility = View.VISIBLE

                } else {
                    editTextOtro.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Aquí puedes manejar qué hacer si no hay ninguna selección, aunque no es común
                editTextOtro.visibility = View.GONE
            }
        }
        //------------------------------------------------------------------------------------------------------

        // para seleccionar carreras:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
        val botonCarrerasDestinadas: Button = findViewById(R.id.carrerasDestinadas_crear)
        val listaCarrerasSeleccionadas =
            mutableListOf<String>() // Lista para almacenar las carreras seleccionadas
        val listaCarrerasFirebase =
            mutableListOf<String>() // Lista para almacenar las carreras obtenidas de Firebase
        val estadoSeleccionCarreras =
            mutableMapOf<String, Boolean>() // Mapa para guardar el estado de selección

        // Obtener la lista de carreras desde Firebase
        val db = FirebaseFirestore.getInstance() // Inicializar Firebase Firestore
        db.collection("carreras").document("fcyt")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Extraer las carreras desde el documento (ejemplo con claves dinámicas c1, c2...)
                    for ((key, value) in document.data ?: emptyMap<String, Any>()) {
                        if (value is String) {
                            listaCarrerasFirebase.add(value)
                            estadoSeleccionCarreras[value] =
                                false // Estado inicial de cada carrera (no seleccionada)
                        }
                    }
                }

                // Configurar el botón para mostrar el diálogo cuando se presione
                botonCarrerasDestinadas.setOnClickListener {
                    // Convertir la lista de Firebase a un array para el diálogo
                    val opciones = listaCarrerasFirebase.toTypedArray()
                    val seleccionados = BooleanArray(opciones.size) { index ->
                        estadoSeleccionCarreras[opciones[index]]
                            ?: false // Estado inicial basado en el mapa
                    }

                    // Crear un AlertDialog con opciones de selección múltiple
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("Seleccionar Carreras")
                        .setMultiChoiceItems(opciones, seleccionados) { _, which, isChecked ->
                            // Actualizar el estado de las opciones seleccionadas
                            seleccionados[which] = isChecked // Actualizar el estado
                            estadoSeleccionCarreras[opciones[which]] =
                                isChecked // Actualizar el mapa
                        }
                        .setPositiveButton("Aceptar") { _, _ ->
                            // Limpiar la lista antes de llenarla nuevamente
                            listaCarrerasSeleccionadas.clear()

                            // Agregar las carreras seleccionadas a la lista
                            for (i in opciones.indices) {
                                if (seleccionados[i]) {
                                    listaCarrerasSeleccionadas.add(opciones[i]) // Añadir las seleccionadas
                                }
                            }

                            // Mostrar las seleccionadas en el Log o usarlas según lo necesites
                            Log.d(
                                "CarrerasSeleccionadas",
                                "Seleccionadas: $listaCarrerasSeleccionadas"
                            )
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
        //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
        //fecha y hora inicio
        val buttonFechaHoraInicio: Button = findViewById(R.id.button_fecha_inicio_crear)

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
            if (tiempoSeleccionadoFin != null) {
                // Crear el mapa de datos a guardar
                val data = hashMapOf(
                    "fechaHoraFin" to tiempoSeleccionadoFin!!, // Usar el valor global
                    "fechaHoraInicio" to tiempoSeleccionadoInicio!!, // Usar el valor global"
                    "carrerasDestinadas" to tipoVotacion.carrerasDestinadas,
                    "estado" to true
                )

                // Guardar en Firestore
                db.collection("PreColeccion")
                    .document(tipoVotacion.tipoVotacionnombre)  // Usamos el nombre de la votación como el ID del documento
                    .set(data) // Usa `set` para guardar el mapa
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
                Toast.makeText(
                    this,
                    "Selecciona una fecha y hora antes de guardar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //  ..............................................................................................................

    }
    // Adapter para el RecyclerView
    class PartidosAdapter(private val partidos: List<Partido>) : RecyclerView.Adapter<PartidosAdapter.PartidoViewHolder>() {

        // ViewHolder para cada item de la lista
        class PartidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val partidoNombre: TextView = itemView.findViewById(R.id.textView_nombrePartido)
            val editarPartido: ImageView = itemView.findViewById(R.id.imageView_editar_partido)
            val borrarPartido: ImageView = itemView.findViewById(R.id.imageView_borrar_partido)
        }

        // Inflar el layout para cada item
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidoViewHolder {
            // Asegúrate de que el layout sea el correcto
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_crear_editar_administrador, parent, false)
            return PartidoViewHolder(itemView)
        }

        // Enlazar los datos con las vistas
        override fun onBindViewHolder(holder: PartidoViewHolder, position: Int) {
            val partido = partidos[position]

            // Asigna el nombre del partido
            holder.partidoNombre.text = partido.nombre

            // Puedes agregar listeners o lógica adicional para los iconos de editar y borrar si lo necesitas
            holder.editarPartido.setOnClickListener {
                // Acción para editar el partido
                Log.d("Partido", "Editar partido: ${partido.nombre}")
            }

            holder.borrarPartido.setOnClickListener {
                // Acción para borrar el partido
                Log.d("Partido", "Borrar partido: ${partido.nombre}")
            }
        }

        // Número total de items en la lista
        override fun getItemCount(): Int = partidos.size
    }

}

    class CalendarDialogFragment(private val onDateSelected: (String) -> Unit) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = android.app.AlertDialog.Builder(requireActivity())

            // Inflar el layout para el calendario
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_calendario, null)

            val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
            val textViewFechaSeleccionada = view.findViewById<TextView>(R.id.textViewFechaSeleccionada)

            // Configurar el Listener para la selección de fecha
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                textViewFechaSeleccionada.text = "Fecha seleccionada: $selectedDate"
                onDateSelected(selectedDate)  // Retornar la fecha seleccionada
                dismiss()  // Cerrar el calendario después de seleccionar una fecha
            }

            builder.setView(view)
            return builder.create()
        }
    }
