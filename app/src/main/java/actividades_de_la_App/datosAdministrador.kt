// VotacionModel.kt
package actividades_de_la_App

data class Candidato(
    val nombre: String,
    val cargo: String
)

data class Partido(
    val nombre: List<String> = listOf("Rectorado","PRUEBA1","PRUEBA2","OTRO"),
    var votos: Int, // Los votos pueden variar
    val candidatos: MutableList<Candidato> // Lista mutable de candidatos
)

data class Votacion(
    val tipoDeVotacion: String,
    val fechaIni: String, // Podrías usar un tipo `Date` si deseas más control sobre las fechas
    val fechaFin: String,
    val estado: String,
    val carrerasDestinadas: List<String>, // Lista de carreras a las que está destinada la votación
    val partidos: MutableList<Partido> // Lista mutable de partidos asociados
)
