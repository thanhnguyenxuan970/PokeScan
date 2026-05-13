package com.pokescan.app.agents

import com.pokescan.app.data.service.CardIdentificationService
import com.pokescan.app.data.service.SetDatabaseService
import com.pokescan.app.data.service.SetResolver
import com.pokescan.app.domain.model.SetEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Non-Card Agent — scans random non-Pokémon objects to verify the app
 * returns null (no result) rather than a random/wrong card match.
 *
 * Objects tested: coins, hands, MTG cards, Yu-Gi-Oh cards, banknotes,
 * books, business cards, foreign-language card games.
 *
 * Feedback collected:
 * - Does scanning a coin produce a false Pokémon result?
 * - Does an MTG power/toughness (e.g. "3/3") false-match a Pokémon set?
 * - Does a book paragraph silently produce garbage output?
 * - Are any non-card OCR patterns slipping through to the result sheet?
 *
 * Note on MTG test: "3/3" matches the set number regex but no Pokémon set
 * has exactly 3 cards total — SetResolver returns "unknown". The test
 * documents this known limitation and asserts setCode == "unknown".
 */
class NonCardAgentTest {

    private val mockSetDb = mockk<SetDatabaseService>()
    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = 198, releaseYear = 2023, series = "SV", language = "english")
    private val base1 = SetEntry("base1", "Base Set", total = 102, printedTotal = 102, releaseYear = 1999, series = "Base", language = "english")
    private lateinit var service: CardIdentificationService

    @Before
    fun setUp() {
        every { mockSetDb.sets } returns MutableStateFlow(listOf(sv1, base1))
        service = CardIdentificationService(mockSetDb, SetResolver())
    }

    // ---- Physical objects ----

    @Test
    fun `coin — year plus denomination text, no set number format`() {
        assertNull(service.identify(listOf("LIBERTY", "2024", "IN GOD WE TRUST", "E PLURIBUS UNUM")))
    }

    @Test
    fun `hand — sparse or empty OCR output`() {
        assertNull(service.identify(listOf("", "")))
    }

    @Test
    fun `blank surface — completely empty OCR`() {
        assertNull(service.identify(emptyList()))
    }

    // ---- Other card games ----

    @Test
    fun `Yu-Gi-Oh card — ATK and DEF stats, no XX slash YYY format`() {
        assertNull(service.identify(listOf(
            "Dark Magician",
            "DARK",
            "Spellcaster / Normal",
            "ATK / 2500  DEF / 2100",
            "LDK2-ENY26",
        )))
    }

    @Test
    fun `MTG card — power slash toughness looks like set number but resolves to unknown`() {
        // "3/3" matches the regex \b\d{1,3}/\d{1,3}\b but no Pokémon set has total=3
        val result = service.identify(listOf("Lightning Bolt", "3/3", "Instant"))
        // Either null (no match at all) or setCode="unknown" — never a real Pokémon set
        if (result != null) {
            assert(result.setCode == "unknown") {
                "MTG power/toughness '3/3' must not resolve to a real Pokémon set, got: ${result.setCode}"
            }
        }
        // null is also acceptable and preferred
    }

    @Test
    fun `MTG card large set number — resolves to unknown not a real Pokémon set`() {
        val result = service.identify(listOf("Counterspell", "29/350", "Instant", "UMA"))
        // 350 does not match any set total in our DB
        if (result != null) {
            assert(result.setCode == "unknown") {
                "MTG card resolved to real Pokémon set: ${result.setCode}"
            }
        }
    }

    @Test
    fun `Digimon card — no standard XX slash YYY numbering`() {
        assertNull(service.identify(listOf("Agumon", "BT1-010", "R", "Rookie")))
    }

    // ---- Documents and text ----

    @Test
    fun `banknote — serial number format differs from set number`() {
        // Serial like "MB12345678A" does not match \d{1,3}/\d{1,3}
        assertNull(service.identify(listOf("FEDERAL RESERVE NOTE", "MB12345678A", "ONE DOLLAR", "THE UNITED STATES OF AMERICA")))
    }

    @Test
    fun `book page — paragraphs of text, no set number`() {
        assertNull(service.identify(listOf(
            "Chapter 1: The Beginning",
            "Once upon a time in a land far away,",
            "there lived a young trainer who dreamed",
            "of becoming the Pokémon Champion.",
        )))
    }

    @Test
    fun `business card — email and phone, no set number`() {
        assertNull(service.identify(listOf(
            "John Smith",
            "john.smith@example.com",
            "+1 555 123 4567",
            "Software Engineer",
        )))
    }

    @Test
    fun `receipt — prices and item codes, no set number`() {
        // Prices like "1.99" don't match \d{1,3}/\d{1,3}
        assertNull(service.identify(listOf("GROCERY STORE", "Milk 1.99", "Eggs 3.49", "TOTAL 5.48")))
    }

    // ---- Foreign text ----

    @Test
    fun `Chinese card game text without set number — returns null`() {
        assertNull(service.identify(listOf("神奇宝贝", "皮卡丘", "雷电属性", "基础精灵")))
    }
}
