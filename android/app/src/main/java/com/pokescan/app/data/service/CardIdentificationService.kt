package com.snapdex.app.data.service

import android.graphics.Bitmap
import com.snapdex.app.domain.model.CardLanguage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardIdentificationService @Inject constructor(
    private val setDatabaseService: SetDatabaseService,
    private val setResolver: SetResolver,
    private val pHashService: PHashService,
) {
    // OCR sometimes reads '/' as 'l' or '|' — accept all three
    private val setNumberRegex = Regex("""(?<![.])\b\d{1,3}[/|l]\d{1,3}\b""")
    private val noiseLineRegex = Regex(
        """HP\s*\d+|©|Nintendo|Creatures|GAME\s*FREAK|\bTrainer\b|\bItem\b|\bSupporter\b|\bStadium\b""",
        RegexOption.IGNORE_CASE,
    )

    data class IdentifiedCard(
        val cardName: String,
        val setNumber: String,
        val setCode: String,
        val language: CardLanguage,
        val setName: String? = null,
        val setYear: Int? = null,
    )

    fun identify(lines: List<String>, frame: Bitmap? = null): IdentifiedCard? {
        if (lines.isEmpty()) return null
        val rawSetNumber = lines.firstNotNullOfOrNull { setNumberRegex.find(it)?.value } ?: return null
        // Normalize OCR misreads before passing to SetResolver
        val setNumber = rawSetNumber.replace('l', '/').replace('|', '/')
        val cardName = lines.firstOrNull { line ->
            line.isNotBlank()
                && line != rawSetNumber
                && !line.matches(Regex("""^\d+([/|l]\d+)?$"""))
                && line.length >= 3
                && !noiseLineRegex.containsMatchIn(line)
        } ?: "Unknown Card"
        val language = if (lines.any { line -> line.any { ch -> ch.code in 0x3000..0x9FFF } })
            CardLanguage.JAPANESE else CardLanguage.ENGLISH
        val entries = setDatabaseService.sets.value
        val resolved = setResolver.resolve(entries, setNumber, language)

        // pHash disambiguation — only runs on newest-wins collision path
        val finalSetCode = if (resolved.candidates != null && frame != null) {
            pHashService.findBestMatch(frame, resolved.candidates) ?: resolved.setCode
        } else {
            resolved.setCode
        }
        val finalEntry = if (finalSetCode != resolved.setCode) {
            resolved.candidates?.find { it.setCode == finalSetCode }
        } else null

        return IdentifiedCard(
            cardName,
            setNumber,
            finalSetCode,
            language,
            finalEntry?.name ?: resolved.setName,
            finalEntry?.releaseYear ?: resolved.releaseYear,
        )
    }
}
