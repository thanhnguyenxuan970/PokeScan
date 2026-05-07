package com.pokescan.app.data.remote.dto

import com.pokescan.app.domain.model.SetEntry
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PokemonTCGSetDto(
    val id: String,
    val name: String,
    val total: Int,
    val printedTotal: Int?,
    val releaseDate: String,
    val series: String,
)

@JsonClass(generateAdapter = true)
data class PokemonTCGSetsResponse(
    val data: List<PokemonTCGSetDto>,
)

@JsonClass(generateAdapter = true)
data class SetEntryDto(
    val setCode: String,
    val name: String,
    val total: Int,
    val printedTotal: Int?,
    val releaseYear: Int,
    val series: String,
    val language: String,
) {
    fun toDomain() = SetEntry(
        setCode = setCode,
        name = name,
        total = total,
        printedTotal = printedTotal,
        releaseYear = releaseYear,
        series = series,
        language = language,
    )
}
