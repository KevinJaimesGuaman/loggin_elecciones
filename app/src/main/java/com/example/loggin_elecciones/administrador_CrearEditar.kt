package com.example.loggin_elecciones

//importar datosAdministrador
import actividades_de_la_App.Partido
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class administrador_CrearEditar : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val tiposEleccion= mutableListOf<PreEleccion>()
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PartidoAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val partidoList = mutableListOf<PartidoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_administrador_crear_editar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Configurar el RecyclerView
        recyclerView = findViewById(R.id.lista_de_partidos_añadidos)
        adapter = PartidoAdapter(partidoList,tipoVotacion,context = this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Cargar los datos desde Firestore
        cargarPartidosDesdeFirestore()

        // Configurar el botón de volver................................................................
        val buttonVolver = findViewById<ImageButton>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }
        //..............................................................................................

        //spinner para el tipo de votacion--------------------------------------------------------------

        // Configurar el Spinner para el tipo de votación
        val spinnerTipoVotacion: Spinner = findViewById(R.id.spinnerTipoEleccion_crear)
        val editTextOtro: EditText = findViewById(R.id.editTextText_eleccionOtro)

        // Llamar a la función para cargar los tipos de elección
        cargarTiposEleccion()

        // Crear un ArrayAdapter para el Spinner con los nombres de los tipos de elección
        val tipoEleccionNombres = tiposEleccion.map { it.nombre }  // Obtener los nombres de los tipos de elección
        val adapterSpinner = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            tipoEleccionNombres
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoVotacion.adapter = adapterSpinner

        // Establecer el listener para cuando se selecciona un tipo de votación
        spinnerTipoVotacion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Actualizar el tipo de votación seleccionado
                tipoVotacion.tipoVotacionnombre = tiposEleccion[position].nombre
                if (tipoVotacion.tipoVotacionnombre == "Otro") {
                    editTextOtro.visibility = View.VISIBLE
                } else {
                    editTextOtro.visibility = View.GONE
                }
                // Llamar a la función para cargar los partidos basados en el tipo de votación seleccionado
                cargarPartidosDesdeFirestore()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Si no se selecciona nada, no hacer nada
            }
        }
        //..................................................................................................
        // Configuración del EditText cuando se escoge "otro" en el spinner
// Añadir el TextWatcher para capturar los cambios en el texto
        editTextOtro.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // No necesitamos hacer nada antes de que el texto cambie
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Guardar el texto tal como lo escribe el usuario
                tipoVotacion.otro = charSequence.toString()
            }

            override fun afterTextChanged(editable: Editable?) {
                // Este método se ejecuta después de que el texto ha cambiado
            }
        })

// Detectar cuando el EditText se vuelve GONE
        editTextOtro.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (editTextOtro.visibility == View.GONE) {
                // Si el EditText se vuelve GONE, borrar el texto de la variable global
                tipoVotacion.otro = ""
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
        val buttonFechaHoraInicio: Button = findViewById(R.id.button_fecha_inicio_crear)
        val buttonFechaHoraFin: Button = findViewById(R.id.button_fecha_fin_crear)
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
                        val dateIni: Date = format.parse(formattedDate)
                            ?: throw IllegalArgumentException("Fecha inválida: $formattedDate")
                        tiempoSeleccionadoInicio = Timestamp(dateIni)
                        Toast.makeText(
                            this,
                            "Fecha y hora de inicio seleccionadas correctamente.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Actualizar automáticamente la fecha de fin con la misma fecha y hora fija
                        val formattedEndDate = "$selectedDay/${selectedMonth + 1}/$selectedYear 16:00"
                        buttonFechaHoraFin.text = formattedEndDate

                        val dateFin: Date = format.parse(formattedEndDate)
                            ?: throw IllegalArgumentException("Fecha inválida: $formattedEndDate")
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

// Deshabilitar la interacción en el botón de fecha de fin
        buttonFechaHoraFin.isEnabled = false

//-------------------------------------------------------------------------------------------------------------
        //boton añadir------------------------------------------------------------------------------------------------
        // Configurar el botón para guardar en Firebase
        val buttonGuardar: Button = findViewById(R.id.añadir_Eleccion)
        buttonGuardar.setOnClickListener {
            showWarningDialog()
        }
        //  ..............................................................................................................
        //boton añadir Partido
        val imageViewAñadirPartido= findViewById<ImageView>(R.id.imageView_añadir_editar)
        imageViewAñadirPartido.setOnClickListener {
            //Precrear eleccion
            if(tipoVotacion.tipoVotacionnombre == "Otro"){
                val intent = Intent(this, add_partido::class.java)
                intent.putExtra("tipoEleccion", "Otro")
                startActivity(intent)
            }else{
                precrear(tipoVotacion.tipoVotacionnombre)
                val intent = Intent(this, add_partido::class.java)
                intent.putExtra("tipoEleccion", tipoVotacion.tipoVotacionnombre)
                startActivity(intent)
            }


        }
    }
    private fun cargarTiposEleccion() {


        // Construir la referencia a la colección de partidos
        val coleccionPartidos = firestore.collection("PreColeccion")

        // Usar addSnapshotListener para escuchar cambios en tiempo real
        coleccionPartidos.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                // Manejar el error
                Log.e("FirestoreError", "Error al cargar las elecciones", exception)
                Toast.makeText(this, "No se pudieron cargar las elecciones", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            // Limpiar la lista de partidos
            tiposEleccion.clear()

            // Iterar sobre los documentos
            for (document in querySnapshot!!) {
                val nombre = document.id
                val estado = false
                val paraMandar = PreEleccion(nombre, estado)
                tiposEleccion.add(paraMandar)
            }

            // Verificar si la lista de tipos de elección tiene elementos
            if (tiposEleccion.isNotEmpty()) {
                // Crear un ArrayAdapter para el Spinner con los nombres de los tipos de elección
                val tipoEleccionNombres = tiposEleccion.map { it.nombre }
                val adapterSpinner = ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    tipoEleccionNombres
                )
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                val spinnerTipoVotacion: Spinner = findViewById(R.id.spinnerTipoEleccion_crear)
                spinnerTipoVotacion.adapter = adapterSpinner
            }

            // Notificar al adaptador sobre los cambios
            adapter.notifyDataSetChanged()
        }
    }

    private fun cargarPartidosDesdeFirestore() {
        // Limpiar la lista existente antes de cargar nuevos partidos
        partidoList.clear()

        // Validar que tipoEleccionNombre no esté vacío o nulo
        val tipoEleccionNombre = tipoVotacion.tipoVotacionnombre
        if (tipoEleccionNombre.isNullOrEmpty()) {
            // Notificar al adaptador para mostrar una lista vacía
            adapter.notifyDataSetChanged()
            return
        }

        val coleccionPartidos = if (tipoEleccionNombre == "Otro") {
            // Si es "Otro", buscar en PreColeccion
            firestore.collection("PreColeccion")
                .document("Otro")
                .collection("Partido")
        } else {
            // Si no, buscar en TipoEleccion
            firestore.collection("TipoEleccion")
                .document(tipoEleccionNombre)
                .collection("Partido")
        }

        // Usar addSnapshotListener para escuchar cambios en tiempo real
        coleccionPartidos.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                // Manejar el error
                Log.e("FirestoreError", "Error al cargar partidos en tiempo real", exception)
                Toast.makeText(this, "No se pudieron cargar los partidos", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            // Limpiar la lista de partidos
            partidoList.clear()

            // Iterar sobre los documentos
            for (document in querySnapshot!!) {
                val nombre = document.id
                partidoList.add(PartidoItem(nombre))
            }

            // Notificar al adaptador sobre los cambios
            adapter.notifyDataSetChanged()
        }
    }

    private fun precrear(nombreEleccion: String) {
        if (nombreEleccion.isNullOrEmpty()) {
            Log.e("Firestore", "El nombre de la elección no es válido.")
            return
        }

        val coleccionTipoEleccion = db.collection("TipoEleccion")
        val documentoNombreEleccion = coleccionTipoEleccion.document(nombreEleccion)
        val documentoBlanco= mapOf(
            "color" to "GRAY",
            "votos" to 0,
            "acronimo" to "BLANCO"
        )
        // Crear el documento dinámico vacío
        documentoNombreEleccion.set(mapOf<String, Any>()) // Guardamos un documento vacío
            .addOnSuccessListener {
                Log.d("Firestore", "Documento '$nombreEleccion' creado exitosamente.")
                // Crear la subcolección "Partido" (sin documentos iniciales)
                val partidosRef = documentoNombreEleccion.collection("Partido")
                partidosRef.document("Blanco").set(documentoBlanco)
                Log.d("Firestore", "Subcolección 'Partido' creada en '$nombreEleccion'.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al crear documento '$nombreEleccion': $e")
            }
    }

    private fun showWarningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Advertencia")
        builder.setMessage("¿Estás seguro de que deseas continuar?")
        builder.setPositiveButton("Sí") { _, _ ->
            // Verificar si el tipo de votación es "Otro" y si el campo "Otro" está vacío
            if (tipoVotacion.tipoVotacionnombre == "Otro" && tipoVotacion.otro.isNullOrEmpty()) {
                Toast.makeText(this, "Por favor ingresa un nombre en el campo 'otro'", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (tiempoSeleccionadoFin != null) {
                // Crear el mapa de datos a guardar
                val data = hashMapOf(
                    "fechaFin" to tiempoSeleccionadoFin!!,
                    "fechaInicio" to tiempoSeleccionadoInicio!!,
                    "carrerasDestinadas" to tipoVotacion.carrerasDestinadas
                )

                // Determinar el nombre del documento
                val documentName = if (tipoVotacion.tipoVotacionnombre == "Otro" && tipoVotacion.otro.isNotEmpty()) {
                    tipoVotacion.otro
                } else {
                    tipoVotacion.tipoVotacionnombre
                }

                val db = FirebaseFirestore.getInstance()

                if (tipoVotacion.tipoVotacionnombre == "Otro") {
                    val preColeccionRef = db.collection("PreColeccion").document("Otro").collection("Partido")
                    val tipoEleccionRef = db.collection("TipoEleccion").document(documentName).collection("Partido")

                    preColeccionRef.get().addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (document in querySnapshot.documents) {
                                val partidoData = document.data
                                partidoData?.let {
                                    val partidoDocRef = tipoEleccionRef.document(document.id)

                                    // Copiar el documento actual en la nueva ubicación
                                    partidoDocRef.set(it).addOnSuccessListener {
                                        // Transferir la subcolección "Puestos"
                                        val puestosRef = preColeccionRef.document(document.id).collection("Puestos")
                                        val puestosDestinoRef = partidoDocRef.collection("Puestos")

                                        puestosRef.get().addOnSuccessListener { puestosSnapshot ->
                                            for (puesto in puestosSnapshot.documents) {
                                                val puestoData = puesto.data
                                                puestoData?.let {
                                                    puestosDestinoRef.document(puesto.id).set(it).addOnFailureListener { e ->
                                                        Log.e("Firestore", "Error al copiar el documento de la subcolección Puestos: ${puesto.id}", e)
                                                    }
                                                }
                                            }

                                            // Después de transferir los puestos, eliminar el documento original si no es "Blanco"
                                            if (document.id != "Blanco") {
                                                puestosRef.get().addOnSuccessListener { puestosSnapshot ->
                                                    val batch = db.batch()
                                                    for (puesto in puestosSnapshot.documents) {
                                                        batch.delete(puesto.reference)
                                                    }
                                                    batch.commit().addOnSuccessListener {
                                                        preColeccionRef.document(document.id).delete()
                                                            .addOnSuccessListener {
                                                                Log.d("Firestore", "Documento ${document.id} y su subcolección 'Puestos' eliminados con éxito")
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("Firestore", "Error al eliminar el documento ${document.id}", e)
                                                            }
                                                    }
                                                }
                                            }
                                        }.addOnFailureListener { e ->
                                            Log.e("Firestore", "Error al transferir la subcolección 'Puestos'", e)
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("Firestore", "Error al copiar el documento ${document.id}", e)
                                    }
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Error al obtener los documentos", e)
                        Toast.makeText(this, "Error al acceder a los partidos en 'PreColeccion'", Toast.LENGTH_SHORT).show()
                    }
                }

                // Guardar el nuevo documento en "TipoEleccion"
                db.collection("TipoEleccion")
                    .document(documentName)
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
        private val tipoEleccion: TipoVotacion,
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
            val tipoVotacionNombre = tipoEleccion.tipoVotacionnombre
            if (tipoVotacionNombre.isNullOrEmpty()) {
                Log.e("FirestoreError", "El tipo de votación no está definido.")
                return
            }

            db.collection("TipoEleccion").document(tipoVotacionNombre).collection("Partidos")
                .document(nombre).delete()
                .addOnSuccessListener {
                    Log.d("Firestore", "Documento con ID $nombre eliminado exitosamente.")

                    // Eliminar el elemento de la lista local y notificar al adaptador
                    items.removeAt(position)
                    notifyItemRemoved(position)

                    // Mostrar el Toast
                    Toast.makeText(context, "Documento eliminado.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error al eliminar el documento", exception)
                    Toast.makeText(context, "Error al eliminar el documento.", Toast.LENGTH_SHORT).show()
                }
        }

        override fun getItemCount(): Int = items.size
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

data class TipoVotacion(
    var tipoDeVotacion: List<String> = listOf("Rectorado","OTRO"),
    var estadoTipodeVotacion: List<Boolean> = listOf(false,false),
    //esta parte se envia a firebase
    var tipoVotacionnombre: String,
    var fechaIni: Timestamp,  // Permite que sea null
    var fechayHoraFin: Timestamp,  // Permite que sea null
    var estado: String,
    var otro: String,
    var carrerasDestinadas: List<String>, // Lista de carreras a las que está destinada la votación
    var partidos: MutableList<Partido> // Lista mutable de partidos asociados
)
data class Partido(
    val nombre: String,
    var votos: Int, // Los votos pueden variar
)
data class PreEleccion (
        val nombre: String,
        val estado:Boolean
        )

data class paraEneviar(
    val tipoDeVotacion: String,
    val fechaIni: String,
    val fechaFin: String,
    val estado: String,
    var otro: String,
    val carrerasDestinadas: List<String>,
    val partidos: MutableList<Partido>
)

