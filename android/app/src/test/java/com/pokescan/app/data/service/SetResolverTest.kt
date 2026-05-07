package com.pokescan.app.data.service

import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SetResolverTest {

    private lateinit var resolver: SetResolver

    private val base1 = SetEntry("base1", "Base Set", total = 102, printedTotal = 102, releaseYear = 1999, series = "Base", language = "english")
    private val ex5 = SetEntry("ex5", "Hidden Legends", total = 102, printedTotal = 101, releaseYear = 2004, series = "EX", language = "english")
    private val sv1 = SetEntry("sv1", "Scarlet & Violet", total = 198, printedTotal = null, releaseYear = 2023, series = "SV", language = "english")
    private val neo2 = SetEntry("neo2", "Neo Discovery", total = 75, printedTotal = null, releaseYear = 2001, series = "Neo", language = "english")
    private val jpBase = SetEntry("base1-jp", "Expansion Pack", total = 102, printedTotal = null, releaseYear = 1996, series = "Base", language = "japanese")

    @Before
    fun setUp() {
        resolver = SetResolver()
    }

    @Test
    fun `single candidate resolves directly`() {
        assertEquals("neo2", resolver.resolve(listOf(neo2), "025/075", CardLanguage.ENGLISH))
    }

    @Test
    fun `sv1 198-card set resolves correctly`() {
        assertEquals("sv1", resolver.resolve(listOf(sv1), "025/198", CardLanguage.ENGLISH))
    }

    @Test
    fun `base1 vs ex5 — printedTotal disambiguates to base1`() {
        assertEquals("base1", resolver.resolve(listOf(base1, ex5), "025/102", CardLanguage.ENGLISH))
    }

    @Test
    fun `base1 vs ex5 — card 101 still resolves to base1`() {
        assertEquals("base1", resolver.resolve(listOf(base1, ex5), "101/102", CardLanguage.ENGLISH))
    }

    @Test
    fun `collision without printedTotal — newest wins`() {
        val b = base1.copy(printedTotal = null)
        val e = ex5.copy(printedTotal = null)
        assertEquals("ex5", resolver.resolve(listOf(b, e), "025/102", CardLanguage.ENGLISH))
    }

    @Test
    fun `collision partial printedTotal — newest wins`() {
        val b = base1.copy(printedTotal = null)
        assertEquals("ex5", resolver.resolve(listOf(b, ex5), "025/102", CardLanguage.ENGLISH))
    }

    @Test
    fun `english and japanese entries with same total are isolated by language`() {
        assertEquals("base1", resolver.resolve(listOf(base1, jpBase), "025/102", CardLanguage.ENGLISH))
    }

    @Test
    fun `japanese query resolves jp set`() {
        assertEquals("base1-jp", resolver.resolve(listOf(base1, jpBase), "025/102", CardLanguage.JAPANESE))
    }

    @Test
    fun `unknown returned for bad format`() {
        assertEquals("unknown", resolver.resolve(listOf(base1), "025", CardLanguage.ENGLISH))
        assertEquals("unknown", resolver.resolve(listOf(base1), "abc/102", CardLanguage.ENGLISH))
        assertEquals("unknown", resolver.resolve(listOf(base1), "025/xyz", CardLanguage.ENGLISH))
    }

    @Test
    fun `unknown returned for no matching total`() {
        assertEquals("unknown", resolver.resolve(listOf(base1), "025/999", CardLanguage.ENGLISH))
    }

    @Test
    fun `card number exceeding all candidate totals returns unknown`() {
        val b = base1.copy(printedTotal = null)
        val e = ex5.copy(printedTotal = null)
        assertEquals("unknown", resolver.resolve(listOf(b, e), "103/102", CardLanguage.ENGLISH))
    }
}
