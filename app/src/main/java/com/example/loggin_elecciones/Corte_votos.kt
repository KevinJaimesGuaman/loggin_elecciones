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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
                    // Crear mapas para estudiantes que votaron y no votaron
                    val estudiantesVotaron = mutableMapOf<String, MutableList<String>>()
                    val estudiantesNoVotaron = mutableMapOf<String, MutableList<String>>()
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
                                    estudiantesVotaron.getOrPut(carrera) { mutableListOf() }.add(documentId)
                                }
                            }
                            .addOnCompleteListener {
                                documentosProcesados++
                                if (documentosProcesados == totalDocumentos) {
                                    // Obtener todos los electores para comparar
                                    db.collection("Elector").get()
                                        .addOnSuccessListener { electorDocuments ->
                                            // Llenar estudiantes no votaron
                                            electorDocuments.forEach { electorDoc ->
                                                val carrera = electorDoc.getString("carrera") ?: "Sin carrera"
                                                val documentId = electorDoc.id

                                                // Si no está en votaron, agregarlo a no votaron
                                                if (!estudiantesVotaron.any { it.value.contains(documentId) }) {
                                                    estudiantesNoVotaron.getOrPut(carrera) { mutableListOf() }.add(documentId)
                                                }
                                            }

                                            // Generar PDF de votantes
                                            generarPDFEstudiantesPorCarrera(
                                                votacionNombre,
                                                estudiantesVotaron,
                                                estudiantesNoVotaron,
                                                isPDFVotaron = true
                                            )
                                        }
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

        // Estilo para total de votos por carrera
        val totalVotosPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.BLACK
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

            // Mostrar total de votos por carrera
            canvas.drawText(
                "TOTAL VOTOS POR CARRERA: ${listaEstudiantes.size}",
                50f,
                yPosition,
                totalVotosPaint
            )
            yPosition += 40f // Espacio adicional entre carreras
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

    private fun imprimirResultados(votacionNombre: String) {
        val db = FirebaseFirestore.getInstance()
        val tipoEleccionRef = db.collection("TipoEleccion").document(votacionNombre)

        tipoEleccionRef.collection("Partido").get()
            .addOnSuccessListener { partidos ->
                if (partidos.isEmpty) {
                    Toast.makeText(this, "No se encontraron partidos para esta elección", Toast.LENGTH_SHORT).show()
                    Log.d("Firestore", "No se encontraron documentos en la colección Partido")
                    return@addOnSuccessListener
                }

                // Mapas para agrupar resultados
                val partidosYVotos = mutableMapOf<String, Int>()
                val resultadosPorCarrera = mutableMapOf<String, MutableMap<String, Int>>()
                val carrerasVotos = mutableMapOf<String, Int>()

                // Iterar sobre todos los documentos de Partido
                for (partido in partidos) {
                    val partidoNombre = partido.id
                    val votos = partido.getLong("votos")?.toInt() ?: 0

                    // Acumular votos por partido
                    partidosYVotos[partidoNombre] = (partidosYVotos[partidoNombre] ?: 0) + votos

                    // Obtener carreras para este partido
                    val carrerasArray = partido.get("carrerasVotos") as? List<String> ?: emptyList()

                    // Procesar cada carrera en este documento
                    carrerasArray.forEach { carrera ->
                        // Normalizar el nombre de la carrera
                        val carreraNormalizada = carrera.trim().uppercase()

                        // Inicializar mapas si no existen
                        if (!resultadosPorCarrera.containsKey(carreraNormalizada)) {
                            resultadosPorCarrera[carreraNormalizada] = mutableMapOf()
                        }

                        // Sumar votos para este partido en esta carrera
                        val votosCarreraPartido = resultadosPorCarrera[carreraNormalizada]!!
                        votosCarreraPartido[partidoNombre] = (votosCarreraPartido[partidoNombre] ?: 0) + 1

                        // Sumar votos totales por carrera
                        carrerasVotos[carreraNormalizada] = (carrerasVotos[carreraNormalizada] ?: 0) + 1
                    }
                }

                // Generar PDF con resultados completos
                generarPDFResultadosCompletos(
                    votacionNombre,
                    partidosYVotos,
                    carrerasVotos,
                    resultadosPorCarrera
                )
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener documentos de la colección Partido", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error al obtener documentos: ${exception.message}", exception)
            }
    }

    private fun generarPDFResultadosCompletos(
        votacionNombre: String,
        partidosYVotos: Map<String, Int>,
        carrerasVotos: Map<String, Int>,
        resultadosPorCarrera: Map<String, Map<String, Int>>
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size

        // Variables para manejar páginas
        var paginaActual = 1
        var yPosition: Float
        var page: PdfDocument.Page
        var canvas: Canvas

        // Función para agregar nueva página
        fun agregarNuevaPagina(): Pair<PdfDocument.Page, Canvas> {
            val nuevaPagina = pdfDocument.startPage(pageInfo)
            val nuevaCanvas = nuevaPagina.canvas

            // Título de página adicional
            val tituloPaint = Paint().apply {
                textSize = 14f
                isFakeBoldText = true
            }
            nuevaCanvas.drawText("Resultados de la Elección - Página $paginaActual", 50f, 50f, tituloPaint)

            return Pair(nuevaPagina, nuevaCanvas)
        }

        // Iniciar primera página
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        yPosition = 50f

        // Título del PDF
        val tituloPaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText("Resultados de la Elección: $votacionNombre", 50f, yPosition, tituloPaint)
        yPosition += 50f

        // SECCIÓN 1: Resultados por Partidos
        // Calcular total de votos para porcentajes de partidos
        val totalVotosPartidos = partidosYVotos.values.sum()

        // Crear gráfico de pastel de partidos
        val partidosPieChartBitmap = createPieChartBitmap(partidosYVotos, totalVotosPartidos)

        // Dibujar gráfico de pastel de partidos
        if (partidosPieChartBitmap != null) {
            val scaleWidth = 400f
            val scaleHeight = 300f
            val left = (pageInfo.pageWidth - scaleWidth) / 2

            // Verificar si hay espacio suficiente
            if (yPosition + scaleHeight > 750f) {
                pdfDocument.finishPage(page)
                paginaActual++
                val (nuevaPagina, nuevaCanvas) = agregarNuevaPagina()
                page = nuevaPagina
                canvas = nuevaCanvas
                yPosition = 100f
            }

            canvas.drawBitmap(
                Bitmap.createScaledBitmap(partidosPieChartBitmap, scaleWidth.toInt(), scaleHeight.toInt(), true),
                left, yPosition, Paint()
            )
            yPosition += scaleHeight + 30f
        }

        // Resultados de partidos
        val seccionPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("Resultados por Partidos", 50f, yPosition, seccionPaint)
        yPosition += 30f

        // Dibujar resultados de partidos
        val partidoPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = false
        }

        partidosYVotos.forEach { (partido, votos) ->
            // Verificar espacio en página
            if (yPosition > 750f) {
                pdfDocument.finishPage(page)
                paginaActual++
                val (nuevaPagina, nuevaCanvas) = agregarNuevaPagina()
                page = nuevaPagina
                canvas = nuevaCanvas
                yPosition = 100f
            }

            val porcentaje = (votos.toFloat() / totalVotosPartidos * 100).roundToTwoDecimalPlaces()
            canvas.drawText("$partido: $votos votos ($porcentaje%)", 50f, yPosition, partidoPaint)
            yPosition += 30f
        }

        // SECCIÓN 2: Resultados por Carrera
        // Verificar si necesitamos una nueva página
        if (yPosition > 650f) {
            pdfDocument.finishPage(page)
            paginaActual++
            val (nuevaPagina, nuevaCanvas) = agregarNuevaPagina()
            page = nuevaPagina
            canvas = nuevaCanvas
            yPosition = 100f
        }

        // Título de sección de carreras
        val tituloCarrerasPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText("Resultados por Carreras", 50f, yPosition, tituloCarrerasPaint)
        yPosition += 50f

        // Iterar sobre cada carrera
        resultadosPorCarrera.forEach { (carrera, resultadosPartidos) ->
            // Verificar espacio en página
            if (yPosition > 700f) {
                pdfDocument.finishPage(page)
                paginaActual++
                val (nuevaPagina, nuevaCanvas) = agregarNuevaPagina()
                page = nuevaPagina
                canvas = nuevaCanvas
                yPosition = 100f
            }

            // Título de carrera
            val carreraPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }
            canvas.drawText("Carrera: $carrera", 50f, yPosition, carreraPaint)
            yPosition += 40f

            // Crear gráfico de pastel para esta carrera
            val totalVotosCarrera = resultadosPartidos.values.sum()
            val carrerasPieChartBitmap = createPieChartBitmap(resultadosPartidos, totalVotosCarrera)

            // Detalles de votos por partido en la carrera
            val detalleCarreraPaint = Paint().apply {
                textSize = 12f
                isFakeBoldText = false
            }

            resultadosPartidos.forEach { (partido, votos) ->
                val porcentajeCarrera = (votos.toFloat() / totalVotosCarrera * 100).roundToTwoDecimalPlaces()
                canvas.drawText("$partido: $votos votos ($porcentajeCarrera%)", 50f, yPosition, detalleCarreraPaint)
                yPosition += 25f
            }

            // Dibujar gráfico de pastel de la carrera
            if (carrerasPieChartBitmap != null) {
                val scaleWidth = 400f
                val scaleHeight = 300f
                val left = (pageInfo.pageWidth - scaleWidth) / 2

                // Verificar si hay espacio suficiente
                if (yPosition + scaleHeight > 750f) {
                    pdfDocument.finishPage(page)
                    paginaActual++
                    val (nuevaPagina, nuevaCanvas) = agregarNuevaPagina()
                    page = nuevaPagina
                    canvas = nuevaCanvas
                    yPosition = 100f
                }

                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(carrerasPieChartBitmap, scaleWidth.toInt(), scaleHeight.toInt(), true),
                    left, yPosition, Paint()
                )
                yPosition += scaleHeight + 30f
            }

            // Espacio entre secciones de carreras
            yPosition += 50f
        }

        // Finalizar la última página
        pdfDocument.finishPage(page)
        fun obtenerFechaActual(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        // Código para guardar y compartir PDF
        try {
            val fileName = "Reporte_Electoral_${votacionNombre}_${obtenerFechaActual()}.pdf"
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

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






    private fun generarPDFPartidosYVotos(
        votacionNombre: String,
        partidosYVotos: Map<String, Int>
    ) {
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
        canvas.drawText("Resultados de la Elección: $votacionNombre", 50f, yPosition, paint)
        yPosition += 50f

        // Calcular total de votos para porcentajes
        val totalVotos = partidosYVotos.values.sum()

        // Crear un gráfico de pastel como bitmap
        val pieChartBitmap = createPieChartBitmap(partidosYVotos, totalVotos)

        // Dibujar el gráfico de pastel
        if (pieChartBitmap != null) {
            val scaleWidth = 400f
            val scaleHeight = 300f
            val left = (pageInfo.pageWidth - scaleWidth) / 2
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(pieChartBitmap, scaleWidth.toInt(), scaleHeight.toInt(), true),
                left, yPosition, paint
            )
            yPosition += scaleHeight + 30f
        }

        // Encabezado de resultados detallados
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Detalle de Resultados", 50f, yPosition, paint)
        yPosition += 30f

        // Lista de partidos y votos con porcentajes
        val partidoPaint = Paint().apply {
            textSize = 12f
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

            val porcentaje = (votos.toFloat() / totalVotos * 100).roundToTwoDecimalPlaces()
            canvas.drawText("$partido: $votos votos ($porcentaje%)", 50f, yPosition, partidoPaint)
            yPosition += 30f
        }

        // Total de votos
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Total de Votos: $totalVotos", 50f, yPosition, paint)

        pdfDocument.finishPage(page)

        // Guardar y compartir el PDF (código existente de generarPDFPartidosYVotos)
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

    // Función auxiliar para crear el gráfico de pastel como bitmap
    private fun createPieChartBitmap(partidosYVotos: Map<String, Int>, totalVotos: Int): Bitmap? {
        // Configurar un bitmap para dibujar el gráfico
        val width = 800
        val height = 700 // Increased height to accommodate labels
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Dibujar fondo blanco
        canvas.drawColor(Color.WHITE)

        // Preparar para dibujar el pastel
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 3f
        val rectF = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Colores predeterminados
        val colors = arrayOf(
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.GRAY
        )

        var startAngle = 0f
        var colorIndex = 0

        // Preparar lista para almacenar información de cada sector
        val sectors = mutableListOf<Pair<String, Float>>()

        // Calcular ángulos de los sectores
        partidosYVotos.forEach { (partido, votos) ->
            val sweepAngle = (votos.toFloat() / totalVotos) * 360f
            sectors.add(Pair(partido, sweepAngle))
        }

        // Dibujar cada sector del pastel
        for ((index, sector) in sectors.withIndex()) {
            val (partido, sweepAngle) = sector

            // Seleccionar color
            paint.color = colors[index % colors.size]

            // Dibujar sector
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            // Calcular punto medio del sector para la etiqueta
            val midAngle = startAngle + sweepAngle / 2
            val midRadians = Math.toRadians(midAngle.toDouble())

            // Calcular porcentaje
            val porcentaje = (sweepAngle / 360f) * 100f

            // Configurar pintura para texto
            paint.color = Color.BLACK
            paint.textSize = 25f
            paint.textAlign = Paint.Align.LEFT

            // Calcular posición de la etiqueta
            val labelRadius = radius + 50 // Distancia desde el centro
            val labelX = centerX + (labelRadius * cos(midRadians)).toFloat()
            val labelY = centerY + (labelRadius * sin(midRadians)).toFloat()

            // Dibujar etiqueta con nombre del partido y porcentaje
            canvas.drawText(
                "$partido (${String.format("%.1f", porcentaje)}%)",
                labelX,
                labelY,
                paint
            )

            // Preparar para el siguiente sector
            startAngle += sweepAngle
        }

        // Agregar título
        paint.color = Color.BLACK
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Resultados", centerX, 50f, paint)

        return bitmap
    }
    // Extensión para redondear a dos decimales
    private fun Float.roundToTwoDecimalPlaces(): String {
        return "%.2f".format(this)
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

