package com.pokescan.app.data.service

import com.pokescan.app.domain.model.CardLanguage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardIdentificationService @Inject constructor(
    private val setDatabaseService: SetDatabaseService,
    private val setResolver: SetResolver,
) {
    private val setNumberRegex = Regex("""\b\d{1,3}/\d{1,3}\b""")

    data class IdentifiedCard(
        val cardName: String,
        val setNumber: String,
        val setCode: String,
        val language: CardLanguage,
    )

    fun identify(lines: List<String>): IdentifiedCard? {
        if (lines.isEmpty()) return null
        val setNumber = lines.firstNotNullOfOrNull { setNumberRegex.find(it)?.value } ?: return null
        val cardName = lines.firstOrNull { line ->
            line.isNotBlank()
                && line != setNumber
                && !line.matches(Regex("""^\d+(/\d+)?$"""))
                && line.length >= 3
        } ?: "Unknown Card"
        val language = if (lines.any { line -> line.any { ch -> ch.code in 0x3000..0x9FFF } })
            CardLanguage.JAPANESE else CardLanguage.ENGLISH
        val entries = setDatabaseService.sets.value
        val setCode = setResolver.resolve(entries, setNumber, language)
        return IdentifiedCard(cardName, setNumber, setCode, language)
    }
}
