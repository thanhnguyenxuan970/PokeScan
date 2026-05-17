package com.snapdex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snapdex.app.domain.model.SetEntry

@Entity(tableName = "set_entries")
data class SetEntryEntity(
    @PrimaryKey val setCode: String,
    val name: String,
    val total: Int,
    val printedTotal: Int?,
    val releaseYear: Int,
    val series: String,
    val language: String,
)

fun SetEntryEntity.toDomain(): SetEntry = SetEntry(
    setCode = setCode,
    name = name,
    total = total,
    printedTotal = printedTotal,
    releaseYear = releaseYear,
    series = series,
    language = language,
)

fun SetEntry.toEntity(): SetEntryEntity = SetEntryEntity(
    setCode = setCode,
    name = name,
    total = total,
    printedTotal = printedTotal,
    releaseYear = releaseYear,
    series = series,
    language = language,
)
