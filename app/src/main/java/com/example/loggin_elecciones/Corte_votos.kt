package com.example.loggin_elecciones

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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


class corte_votos : AppCompatActivity() {
    private lateinit var tituloTextView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var listaCandidatosRecyclerView: RecyclerView
    private lateinit var chartView: AnyChartView // Inicializar AnyChartView
    private lateinit var candidatosAdapter: CandidatosAdapter
    private lateinit var pie: com.anychart.charts.Pie
    private var candidatosList = mutableListOf<Candidato>() // Lista global de candidatos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corte_votos)

        // Configuración inicial
        chartView = findViewById(R.id.chart)
        configurarGraficoDePastelInicial() // Inicializar el gráfico una vez

        // Inicializar FirebaseFirestore
        db = FirebaseFirestore.getInstance()

        // Configurar el botón de retroceso
        val atras = findViewById<ImageButton>(R.id.volver)
        atras.setOnClickListener {
            val intent = Intent(this, home_corte::class.java)
            startActivity(intent)
            finish()
        }

        // Recibir el nombre de la votación desde el Intent
        tituloTextView = findViewById(R.id.Nombre_Partido)
        val votacionNombre = intent.getStringExtra("VOTACION_NOMBRE") ?: "Votación no especificada"
        tituloTextView.text = votacionNombre

        // Obtener y mostrar los candidatos
        obtenerCandidatosYMostrar(votacionNombre)
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
                    .fontColor("BLACK") // Cambia el color de la fuente a negro
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

