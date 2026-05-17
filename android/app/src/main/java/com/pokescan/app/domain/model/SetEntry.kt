package com.snapdex.app.domain.model

data class SetEntry(
    val setCode: String,
    val name: String,
    val total: Int,
    val printedTotal: Int?,
    val releaseYear: Int,
    val series: String,
    val language: String,
)
