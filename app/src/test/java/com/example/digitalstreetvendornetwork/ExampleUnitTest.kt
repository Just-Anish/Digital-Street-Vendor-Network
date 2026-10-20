
package com.example.digitalstreetvendornetwork

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    private data class TestVendor(
        val name: String,
        val category: String,
        val products: List<String>
    )

    private val vendors = listOf(
        TestVendor(
            "Raju Vegetables",
            "Vegetables",
            listOf("Tomatoes", "Potatoes", "Onions")
        ),
        TestVendor(
            "Mumbai Fresh Fruits",
            "Fruits",
            listOf("Apples", "Bananas", "Mangoes")
        ),
        TestVendor(
            "Mumbai Chaat Corner",
            "Street Food",
            listOf("Pani Puri", "Bhel Puri")
        )
    )

    private fun searchTestVendors(query: String): List<TestVendor> {
        val text = query.trim()

        if (text.isEmpty()) return vendors

        return vendors.filter { vendor ->
            vendor.name.contains(text, ignoreCase = true) ||
            vendor.category.contains(text, ignoreCase = true) ||
            vendor.products.any {
                it.contains(text, ignoreCase = true)
            }
        }
    }

    @Test
    fun emptySearchReturnsAllVendors() {
        assertEquals(3, searchTestVendors("").size)
    }

    @Test
    fun searchIgnoresCapitalization() {
        val results = searchTestVendors("RAJU")
        assertEquals(1, results.size)
        assertEquals("Raju Vegetables", results[0].name)
    }

    @Test
    fun searchFindsProductNames() {
        val results = searchTestVendors("mangoes")
        assertEquals(1, results.size)
        assertEquals("Mumbai Fresh Fruits", results[0].name)
    }

    @Test
    fun searchFindsPartialVendorNames() {
        val results = searchTestVendors("chaat")
        assertEquals(1, results.size)
        assertEquals("Mumbai Chaat Corner", results[0].name)
    }

    @Test
    fun searchReturnsEmptyForUnknownVendor() {
        val results = searchTestVendors("unknownvendor")
        assertTrue(results.isEmpty())
    }

    @Test
    fun searchIgnoresLeadingAndTrailingSpaces() {
        val results = searchTestVendors("  raju  ")
        assertEquals(1, results.size)
    }

    @Test
    fun categoryFilteringWorks() {
        val fruits = vendors.filter {
            it.category == "Fruits"
        }

        assertEquals(1, fruits.size)
        assertEquals("Mumbai Fresh Fruits", fruits[0].name)
    }

    @Test
    fun validPriceIsAccepted() {
        val price = "50".toDoubleOrNull()
        assertTrue(price != null && price > 0)
    }

    @Test
    fun zeroPriceIsRejected() {
        val price = "0".toDoubleOrNull()
        assertFalse(price != null && price > 0)
    }

    @Test
    fun negativePriceIsRejected() {
        val price = "-25".toDoubleOrNull()
        assertFalse(price != null && price > 0)
    }

    @Test
    fun invalidPriceTextIsRejected() {
        val price = "abc".toDoubleOrNull()
        assertNull(price)
    }

    @Test
    fun duplicateProductNameIsDetected() {
        val existingProducts = listOf(
            "Vada Pav",
            "Misal Pav",
            "Cutting Chai"
        )

        val newProduct = "vada pav"

        val duplicate = existingProducts.any {
            it.equals(newProduct, ignoreCase = true)
        }

        assertTrue(duplicate)
    }
}
