package com.pokescan.app.data.service

import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.SetEntry
import javax.inject.Inject

data class ResolvedSet(val setCode: String, val setName: String?, val releaseYear: Int?)

class SetResolver @Inject constructor() {

    fun resolve(entries: List<SetEntry>, setNumber: String, language: CardLanguage): ResolvedSet {
        val parts = setNumber.split("/")
        if (parts.size != 2) return ResolvedSet("unknown", null, null)
        val cardNum = parts[0].toIntOrNull() ?: return ResolvedSet("unknown", null, null)
        val total = parts[1].toIntOrNull() ?: return ResolvedSet("unknown", null, null)

        val candidates = entries.filter { it.total == total && it.language == language.raw }
        if (candidates.isEmpty()) return ResolvedSet("unknown", null, null)
        if (candidates.size == 1) {
            val e = candidates[0]
            return if (cardNum in 1..(total * 2)) ResolvedSet(e.setCode, e.name, e.releaseYear) else ResolvedSet("unknown", null, null)
        }

        // printedTotal disambiguation — base1 (printedTotal=102) vs ex5 (printedTotal=101)
        // Only applies when ALL candidates have printedTotal, to avoid partial data skewing result
        val withPrinted = candidates.filter { it.printedTotal != null }
        if (withPrinted.size == candidates.size) {
            val byPrinted = withPrinted.filter { it.printedTotal == total }
            if (byPrinted.size == 1) {
                val e = byPrinted[0]
                return ResolvedSet(e.setCode, e.name, e.releaseYear)
            }
        }

        // Newest-wins fallback: deterministic — releaseYear DESC, then setCode ASC
        val winner = candidates
            .filter { cardNum <= it.total }
            .sortedWith(compareByDescending<SetEntry> { it.releaseYear }.thenBy { it.setCode })
            .firstOrNull()
        return if (winner != null) ResolvedSet(winner.setCode, winner.name, winner.releaseYear) else ResolvedSet("unknown", null, null)
    }
}
