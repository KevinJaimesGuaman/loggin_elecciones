package com.example.loggin_elecciones

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.os.Build
import android.provider.Settings

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loggin_elecciones.R
import com.example.loggin_elecciones.home_corte
import com.google.firebase.firestore.FirebaseFirestore
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.graphics.Color
import android.net.Uri

class corte_votos : AppCompatActivity() {
    private lateinit var tituloTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var listaCandidatosRecyclerView: RecyclerView
    private lateinit var chartView: AnyChartView
    private lateinit var candidatosAdapter: CandidatosAdapter
    private lateinit var pie: com.anychart.charts.Pie
    private var candidatosList = mutableListOf<Candidato>()

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corte_votos)

        // Configuración inicial
        chartView = findViewById(R.id.chart)
        configurarGraficoDePastelInicial()

        // Inicializar FirebaseFirestore
        db = FirebaseFirestore.getInstance()

        // Configurar el botón de retroceso
        val atras = findViewById<ImageButton>(R.id.volver)
        atras.setOnClickListener {
            val intent = Intent(this, home_corte::class.java)
            startActivity(intent)
            finish()
        }

        val boton1 = findViewById<Button>(R.id.boton1)
        boton1.setOnClickListener {
            checkStoragePermission()
        }
        val boton2 = findViewById<Button>(R.id.boton2)
        boton2.setOnClickListener {
            val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE") ?: "Votación no especificada"
            generarPDFNoVotaron(votacionNombre)
        }
        val boton3 = findViewById<Button>(R.id.boton3)
        boton3.setOnClickListener {
            val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE") ?: "Votación no especificada"
            imprimirResultados(votacionNombre)
        }
        // Recibir el nombre de la votación desde el Intent
        tituloTextView = findViewById(R.id.Nombre_Partido)
        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE") ?: "Votación no especificada"
        tituloTextView.text = votacionNombre

        // Obtener y mostrar los candidatos
        obtenerCandidatosYMostrar(votacionNombre)
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para Android 11 (API nivel 30) y superior
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:${packageName}")
                    startActivityForResult(intent, STORAGE_PERMISSION_CODE)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, STORAGE_PERMISSION_CODE)
                }
            } else {
                generarPDF()
            }
        } else {
            // Para versiones anteriores a Android 11
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                generarPDF()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    generarPDF()
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generarPDF()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generarPDFEstudiantesNoVotaron(votacionNombre: String, estudiantesNoVotaron: List<String>) {
        // Crear un documento PDF
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Estudiantes que NO Votaron en: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Encabezado de tabla
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Total de Estudiantes que NO Votaron: ${estudiantesNoVotaron.size}", 50f, yPosition, paint)
        yPosition += 30f

        // Dibujar los nombres de los estudiantes que no votaron
        paint.textSize = 12f
        for (nombre in estudiantesNoVotaron) {
            canvas.drawText(nombre, 50f, yPosition, paint)
            yPosition += 20f

            // Agregar paginación si es necesario
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                val newPage = pdfDocument.startPage(pageInfo)
                val newCanvas = newPage.canvas
                yPosition = yStart
                paint.textSize = 12f
                newCanvas.drawText("(Continuación) Estudiantes que NO Votaron en: $votacionNombre", 50f, yPosition, paint)
                yPosition += 30f
            }
        }

        // Terminar el documento
        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = "no_votantes_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // No hay aplicación para abrir PDFs
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarPDF() {
        val votacionNombre = tituloTextView.text.toString()
        obtenerEstadoVotacionYGenerarPDF(votacionNombre)
    }

    private fun obtenerEstadoVotacionYGenerarPDF(votacionNombre: String) {
        val db = FirebaseFirestore.getInstance()

        // Referencia a la colección "Votaciones" -> "Nombre_Votacion" -> "EstadoVotacion"
        val estadoVotacionRef = db.collection("Votaciones")
            .document(votacionNombre)
            .collection("EstadoVotacion")

        estadoVotacionRef.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No se encontraron documentos en EstadoVotacion", Toast.LENGTH_SHORT).show()
                    Log.d("Firestore", "No se encontraron documentos en EstadoVotacion")
                } else {
                    val estudiantesPorCarrera = mutableMapOf<String, MutableList<String>>()
                    var documentosProcesados = 0
                    val totalDocumentos = documents.size()

                    for (document in documents) {
                        val documentId = document.id // El ID del documento en EstadoVotacion

                        // Buscar el documento en la colección Elector
                        db.collection("Elector").document(documentId).get()
                            .addOnSuccessListener { electorDoc ->
                                if (electorDoc.exists()) {
                                    val carrera = electorDoc.getString("carrera") ?: "Sin carrera"

                                    // Agregar el ID del documento a la lista de la carrera correspondiente
                                    estudiantesPorCarrera.getOrPut(carrera) { mutableListOf() }.add(documentId)
                                }
                            }
                            .addOnCompleteListener {
                                documentosProcesados++
                                if (documentosProcesados == totalDocumentos) {
                                    // Generar el PDF con los datos recopilados cuando todas las operaciones se completen
                                    generarPDFEstudiantesPorCarrera(votacionNombre, estudiantesPorCarrera)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener documentos", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error al obtener documentos: ${exception.message}", exception)
            }
    }

    private fun generarPDFEstudiantesPorCarrera(
        votacionNombre: String,
        estudiantesPorCarrera: Map<String, List<String>>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4
        var page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Estudiantes que Votaron en: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Encabezado por carrera
        val carreraHeaderPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLUE
        }

        // Lista de estudiantes
        val estudiantePaint = Paint().apply {
            textSize = 12f
        }

        var paginaActual = 1
        estudiantesPorCarrera.forEach { (carrera, listaEstudiantes) ->
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                yPosition = yStart
                paginaActual++

                paint.textSize = 14f
                canvas.drawText("Estudiantes que Votaron - Página $paginaActual", 50f, yPosition, paint)
                yPosition += 30f
            }

            // Encabezado de Carrera
            canvas.drawText(carrera, 50f, yPosition, carreraHeaderPaint)
            yPosition += 30f

            listaEstudiantes.forEach { estudiante ->
                if (yPosition > 750f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    yPosition = yStart
                    paginaActual++

                    paint.textSize = 14f
                    canvas.drawText("Estudiantes que Votaron - Página $paginaActual", 50f, yPosition, paint)
                    yPosition += 30f
                }

                canvas.drawText(estudiante, 70f, yPosition, estudiantePaint)
                yPosition += 20f
            }

            yPosition += 20f // Espacio entre carreras
        }

        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = "votantes_por_carrera_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }
    private fun imprimirResultados(votacionNombre: String) {
        val db = FirebaseFirestore.getInstance()

        // Referencia a la colección "TipoEleccion" -> "Nombre_Votacion" -> "Partido"
        val tipoEleccionRef = db.collection("TipoEleccion").document(votacionNombre)

        tipoEleccionRef.collection("Partido").get()
            .addOnSuccessListener { partidos ->
                if (partidos.isEmpty) {
                    Toast.makeText(this, "No se encontraron partidos para esta elección", Toast.LENGTH_SHORT).show()
                    Log.d("Firestore", "No se encontraron documentos en la colección Partido")
                } else {
                    // Crear un mapa para almacenar los nombres de partidos y sus votos
                    val partidosYVotos = mutableMapOf<String, Int>()

                    for (partido in partidos) {
                        val partidoNombre = partido.id
                        val votos = partido.getLong("votos")?.toInt() ?: 0

                        // Agregar al mapa
                        partidosYVotos[partidoNombre] = votos
                    }

                    // Llamar a la función para generar el PDF con los datos recopilados
                    generarPDFPartidosYVotos(votacionNombre, partidosYVotos)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener documentos de la colección Partido", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error al obtener documentos: ${exception.message}", exception)
            }
    }

    private fun generarPDFPartidosYVotos(
        votacionNombre: String,
        partidosYVotos: Map<String, Int>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4
        var page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Resultados de la Elección: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Lista de partidos y votos
        val partidoPaint = Paint().apply {
            textSize = 14f
        }

        var paginaActual = 1
        partidosYVotos.forEach { (partido, votos) ->
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                yPosition = yStart
                paginaActual++

                paint.textSize = 14f
                canvas.drawText("Resultados de la Elección - Página $paginaActual", 50f, yPosition, paint)
                yPosition += 30f
            }

            canvas.drawText("$partido: $votos votos", 50f, yPosition, partidoPaint)
            yPosition += 30f
        }

        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = "resultados_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarPDFArchivo(votacionNombre: String, nombresDocumentos: List<String>) {
        // Crear un documento PDF
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Personas que Votaron en: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Encabezado de tabla
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Lista de Votantes", 50f, yPosition, paint)
        yPosition += 30f

        // Dibujar los nombres de los documentos
        paint.textSize = 12f
        for (nombre in nombresDocumentos) {
            canvas.drawText(nombre, 50f, yPosition, paint)
            yPosition += 20f
        }

        // Terminar el documento
        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = "votantes_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: Exception) {
                // No hay aplicación para abrir PDFs
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarPDFEstudiantesPorCarrera(
        votacionNombre: String,
        estudiantesVotaron: Map<String, List<String>>,
        estudiantesNoVotaron: Map<String, List<String>>,
        isPDFVotaron: Boolean
    ) {
        // Crear un documento PDF
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        val tituloTexto = if (isPDFVotaron)
            "Estudiantes que Votaron en: $votacionNombre"
        else
            "Estudiantes que NO Votaron en: $votacionNombre"
        canvas.drawText(tituloTexto, 50f, yPosition, paint)
        yPosition += 50f

        // Estilo para encabezados de carrera
        val carreraHeaderPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLUE
        }

        // Estilo para lista de estudiantes
        val estudiantePaint = Paint().apply {
            textSize = 12f
        }

        // Dibujar estudiantes por carrera
        val estudiantes = if (isPDFVotaron) estudiantesVotaron else estudiantesNoVotaron

        var paginaActual = 1
        estudiantes.forEach { (carrera, listaEstudiantes) ->
            // Verificar espacio en página
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                yPosition = yStart
                paginaActual++

                // Título de continuación
                paint.textSize = 14f
                canvas.drawText("$tituloTexto - Página $paginaActual", 50f, yPosition, paint)
                yPosition += 30f
            }

            // Encabezado de Carrera
            canvas.drawText(carrera, 50f, yPosition, carreraHeaderPaint)
            yPosition += 30f

            // Total de estudiantes en la carrera
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Total: ${listaEstudiantes.size} estudiantes", 50f, yPosition, paint)
            yPosition += 20f

            // Listar estudiantes
            listaEstudiantes.forEach { estudiante ->
                if (yPosition > 750f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    yPosition = yStart
                    paginaActual++

                    // Título de continuación
                    paint.textSize = 14f
                    canvas.drawText("$tituloTexto - Página $paginaActual", 50f, yPosition, paint)
                    yPosition += 30f
                }

                canvas.drawText(estudiante, 70f, yPosition, estudiantePaint)
                yPosition += 20f
            }

            // Espacio entre carreras
            yPosition += 20f
        }

        // Terminar el último documento
        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = if (isPDFVotaron)
                "votantes_por_carrera_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            else
                "no_votantes_por_carrera_${votacionNombre}_${System.currentTimeMillis()}.pdf"

            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarPDFNoVotaron(votacionNombre: String) {
        val db = FirebaseFirestore.getInstance()

        // 1. Obtener las carreras destinadas para esta votación
        db.collection("TipoEleccion")
            .document(votacionNombre)
            .get()
            .addOnSuccessListener { tipoEleccionDoc ->
                val carrerasDestinadas = tipoEleccionDoc.get("carrerasDestinadas") as? List<String> ?: emptyList()

                // 2. Obtener los documentos de EstadoVotacion (estudiantes que votaron)
                db.collection("Votaciones")
                    .document(votacionNombre)
                    .collection("EstadoVotacion")
                    .get()
                    .addOnSuccessListener { estadoVotacionSnapshot ->
                        val estudiantesQueVotaron = estadoVotacionSnapshot.documents.map { it.id }

                        // 3. Buscar todos los electores de las carreras destinadas
                        db.collection("Elector")
                            .whereIn("carrera", carrerasDestinadas)
                            .get()
                            .addOnSuccessListener { electoresSnapshot ->
                                // Agrupar estudiantes que votaron y no votaron por carrera
                                val estudiantesPorCarreraVotaron = mutableMapOf<String, MutableList<String>>()
                                val estudiantesPorCarreraNoVotaron = mutableMapOf<String, MutableList<String>>()

                                electoresSnapshot.documents.forEach { elector ->
                                    val estudianteId = elector.id
                                    val carrera = elector.getString("carrera") ?: "Sin Carrera"

                                    if (estudianteId in estudiantesQueVotaron) {
                                        estudiantesPorCarreraVotaron.getOrPut(carrera) { mutableListOf() }.add(estudianteId)
                                    } else {
                                        estudiantesPorCarreraNoVotaron.getOrPut(carrera) { mutableListOf() }.add(estudianteId)
                                    }
                                }

                                // Generar PDFs para votantes y no votantes
                                generarPDFEstudiantesPorCarrera(votacionNombre,
                                    estudiantesPorCarreraVotaron,
                                    estudiantesPorCarreraNoVotaron,
                                    isPDFVotaron = true)

                                generarPDFEstudiantesPorCarrera(votacionNombre,
                                    estudiantesPorCarreraVotaron,
                                    estudiantesPorCarreraNoVotaron,
                                    isPDFVotaron = false)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error obteniendo electores: ${e.message}", e)
                                Toast.makeText(this, "Error al obtener electores", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error obteniendo estado de votación: ${e.message}", e)
                        Toast.makeText(this, "Error al obtener estado de votación", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error obteniendo tipo de elección: ${e.message}", e)
                Toast.makeText(this, "Error al obtener tipo de elección", Toast.LENGTH_SHORT).show()
            }
    }










    private fun configurarGraficoDePastelInicial() {
        pie = AnyChart.pie() // Crear una sola instancia del gráfico de pastel
        pie.labels().position("center")
            .format("{%percent}% ({%value})") // Muestra porcentaje y número de votos
        pie.labels().fontSize(14) // Ajusta el tamaño de fuente si es necesario
        chartView.setChart(pie) // Vincula el gráfico al AnyChartView
    }



    private fun generarPDF(votacionNombre: String, nombresDocumentos: List<String>) {
        // Crear un documento PDF
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val yStart = 50f
        var yPosition = yStart

        // Título del PDF
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Personas que Votaron en: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Encabezado de tabla
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Lista de Votantes", 50f, yPosition, paint)
        yPosition += 30f

        // Dibujar los nombres de los documentos
        paint.textSize = 12f
        for (nombre in nombresDocumentos) {
            canvas.drawText(nombre, 50f, yPosition, paint)
            yPosition += 20f
        }

        // Terminar el documento
        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF
        try {
            val fileName = "votantes_${votacionNombre}_${System.currentTimeMillis()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Compartir el archivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // No hay aplicación para abrir PDFs
                Toast.makeText(this, "No se encontró una aplicación para abrir PDFs", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            Log.e("PDF", "Error al guardar el archivo PDF", e)
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }



    private fun obtenerCandidatosYMostrar(votacionNombre: String) {
        getCandidatos2(votacionNombre) { candidatos ->
            if (candidatos.isNotEmpty()) {
                // Configurar el RecyclerView
                listaCandidatosRecyclerView = findViewById(R.id.lista_de_votos_partidos)
                listaCandidatosRecyclerView.layoutManager = LinearLayoutManager(this)
                candidatosAdapter = CandidatosAdapter(candidatos)
                listaCandidatosRecyclerView.adapter = candidatosAdapter

                // Configurar el gráfico de pastel con los datos obtenidos
                actualizarGraficoDePastel(candidatos)

                // Escuchar cambios en tiempo real en los votos
                escucharCambiosEnVotos(votacionNombre)
            } else {
                Log.d("Candidatos", "No se encontraron candidatos para la votación: $votacionNombre")
            }
        }
    }

    private fun escucharCambiosEnVotos(votacionNombre: String) {
        val partidosRef = db.collection("TipoEleccion")
            .document(votacionNombre)
            .collection("Partido")

        // Escuchar cambios en tiempo real en la subcolección Partido
        partidosRef.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e("Firestore", "Error al escuchar cambios: ${exception.message}", exception)
                return@addSnapshotListener
            }

            // Verificar si hay cambios en los datos
            Log.d("Firestore", "Cambios detectados en los partidos")

            val candidatosList = mutableListOf<Candidato>()
            snapshots?.documents?.forEach { partidoDoc ->
                val partidoId = partidoDoc.id
                val votosPartido = partidoDoc.getLong("votos")?.toInt() ?: 0
                val colorPartido = partidoDoc.getString("color") ?: "GRAY" // Valor predeterminado
                val candidato = Candidato(partidoId, votosPartido, colorPartido)
                candidatosList.add(candidato)
            }

            // Verificar los datos recibidos
            Log.d("Firestore", "Datos recibidos: $candidatosList")

            // Actualizar el RecyclerView y gráfico de pastel
            (listaCandidatosRecyclerView.adapter as CandidatosAdapter).actualizarCandidatos(candidatosList)
            actualizarGraficoDePastel(candidatosList) // Actualizar el gráfico de pastel
        }
    }
    private fun actualizarGraficoDePastel(candidatos: List<corte_votos.Candidato>) {
        if (candidatos.isEmpty()) {
            // Evitar configurar un gráfico vacío
            Log.d("Grafico", "No hay datos para el gráfico")
            return
        }

        val totalVotos = candidatos.sumBy { it.votos }
        if (totalVotos == 0) {
            // Evitar dividir por cero o mostrar un gráfico sin datos
            Log.d("Grafico", "Total de votos es 0")
            return
        }

        // Preparar los datos para el gráfico de pastel
        val data: MutableList<DataEntry> = mutableListOf()
        val colores: MutableList<String> = mutableListOf()

        candidatos.forEach { candidato ->
            val porcentaje = (candidato.votos.toFloat() / totalVotos.toFloat()) * 100
            data.add(ValueDataEntry(candidato.nombrePartido, porcentaje))

            // Verificar si el color es blanco
            val color = candidato.color.uppercase()
            colores.add(color)

            if (color == "#FFFFFF" || color == "WHITE") {
                // Configurar texto negro si el fondo es blanco
                pie.labels()
                    .fontColor("#000000") // Cambia el color de la fuente a negro
            }
        }

        // Establecer los datos en el gráfico
        pie.data(data)

        // Establecer los colores en la paleta
        pie.palette(colores.toTypedArray()) // Convertir a Array<String>

        Log.d("Grafico", "Datos actualizados en el gráfico: $data")
        Log.d("Grafico", "Colores aplicados: $colores")
    }



    private fun getCandidatos2(votacionNombre: String, callback: (List<Candidato>) -> Unit) {
        val candidatosList = mutableListOf<Candidato>()

        // Consultar Firestore
        val partidosRef = db.collection("TipoEleccion")
            .document(votacionNombre)
            .collection("Partido")

        partidosRef.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "No se encontraron documentos en la subcolección Partido")
                } else {
                    for (partidoDoc in documents) {
                        val partidoId = partidoDoc.id
                        val votosPartido = partidoDoc.getLong("votos")?.toInt() ?: 0
                        val colorPartido = partidoDoc.getString("color") ?: "GRAY" // Valor predeterminado
                        val candidato = Candidato(partidoId, votosPartido, colorPartido)
                        candidatosList.add(candidato)
                    }
                }
                callback(candidatosList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al obtener documentos: ${exception.message}", exception)
                callback(emptyList())
            }

    }



    // Data class para representar un candidato
    data class Candidato(val nombrePartido: String, val votos: Int, val color: String)

    // Adaptador para mostrar la lista de candidatos
    class CandidatosAdapter(private var candidatos: List<Candidato>) :
        RecyclerView.Adapter<CandidatosAdapter.CandidatoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_votos_conteo, parent, false)
            return CandidatoViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
            val candidato = candidatos[position]
            holder.nombreTextView.text = candidato.nombrePartido
            holder.votosTextView.text = "Votos: ${candidato.votos}"
        }

        override fun getItemCount(): Int = candidatos.size

        // Método para actualizar la lista de candidatos
        fun actualizarCandidatos(nuevosCandidatos: List<Candidato>) {
            candidatos = nuevosCandidatos
            notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado
        }

        inner class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombreTextView: TextView = itemView.findViewById(R.id.texto_partido_candidato)
            val votosTextView: TextView = itemView.findViewById(R.id.texto_numero_votos)
        }
    }

}

