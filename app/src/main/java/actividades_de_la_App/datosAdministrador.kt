// VotacionModel.kt
package actividades_de_la_App

import com.google.firebase.Timestamp

data class Candidato(
    val nombre: String,
    val cargo: String
)

data class Partido(
    val nombre: String,
    var votos: Int, // Los votos pueden variar
)

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
    var partidos: MutableList<Partido> // Lista mutable de partidos asociados
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

