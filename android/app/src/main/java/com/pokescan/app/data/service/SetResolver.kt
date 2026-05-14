package com.pokescan.app.data.service

import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.SetEntry
import javax.inject.Inject

class SetResolver @Inject constructor() {

    fun resolve(entries: List<SetEntry>, setNumber: String, language: CardLanguage): String {
        val parts = setNumber.split("/")
        if (parts.size != 2) return "unknown"
        val cardNum = parts[0].toIntOrNull() ?: return "unknown"
        val total = parts[1].toIntOrNull() ?: return "unknown"

        val candidates = entries.filter { it.total == total && it.language == language.raw }
        if (candidates.isEmpty()) return "unknown"
        if (candidates.size == 1) return if (cardNum in 1..(total * 2)) candidates[0].setCode else "unknown"

        // printedTotal disambiguation — base1 (printedTotal=102) vs ex5 (printedTotal=101)
        // Only applies when ALL candidates have printedTotal, to avoid partial data skewing result
        val withPrinted = candidates.filter { it.printedTotal != null }
        if (withPrinted.size == candidates.size) {
            val byPrinted = withPrinted.filter { it.printedTotal == total }
            if (byPrinted.size == 1) return byPrinted[0].setCode
        }

        // Newest-wins fallback: deterministic — releaseYear DESC, then setCode ASC
        return candidates
            .filter { cardNum <= it.total }
            .sortedWith(compareByDescending<SetEntry> { it.releaseYear }.thenBy { it.setCode })
            .firstOrNull()?.setCode ?: "unknown"
    }
}
