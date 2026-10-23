package com.example.digitalstreetvendornetwork.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.digitalstreetvendornetwork.VendorMenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Firestore-sourced vendor marker for the Customer discovery map.
 *
 * Populated from the /vendors collection; only vendors with isStallOpen == true
 * and saved coordinates are included.
 *
 * Firestore fields read:
 *   stallName:              String
 *   isStallOpen:            Boolean
 *   isBroadcastingLocation: Boolean
 *   lat:                    Double
 *   lng:                    Double
 */
data class ActiveVendor(
    val vendorId: String = "",
    val stallName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Represents a customer order document written to the /orders Firestore collection.
 *
 * Firestore path: /orders/{orderId}
 * Fields written:
 *   orderId:     String  — auto-generated Firestore document ID (also stored in-doc for convenience)
 *   customerId:  String  — Firebase Auth UID of the placing customer
 *   vendorId:    String  — ID of the vendor whose stall is being ordered from
 *   items:       List<String> — item names with quantities (e.g. ["2x Vada Pav", "1x Chai"])
 *   totalAmount: String  — formatted total including delivery fee (e.g. "₹65")
 *   status:      String  — initial lifecycle state, always "PENDING" on creation
 *   timestamp:   Long    — epoch-millis at order creation (System.currentTimeMillis())
 */
data class Order(
    val orderId: String = "",
    val customerId: String = "",
    val vendorId: String = "",
    val items: List<String> = emptyList(),
    val totalAmount: String = "",
    val status: String = "PENDING",
    val timestamp: Long = 0L,
    val rating: Int? = null,
    val reviewText: String? = null
)

/** Result type for [CustomerViewModel.placeOrder]. */
sealed class OrderPlacementResult {
    object Idle : OrderPlacementResult()
    data class Success(val orderId: String) : OrderPlacementResult()
    data class Error(val message: String) : OrderPlacementResult()
}

/**
 * CustomerViewModel
 *
 * Attaches a real-time SnapshotListener to the /vendors Firestore collection.
 * Filters to vendors where isStallOpen == true AND isBroadcastingLocation == true
 * AND valid coordinates exist (lat != 0.0 || lng != 0.0).
 *
 * Exposes [activeVendors] as a StateFlow consumed by CustomerDiscoverScreen
 * to render live map markers.
 */
class CustomerViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _activeVendors = MutableStateFlow<List<ActiveVendor>>(emptyList())
    val activeVendors: StateFlow<List<ActiveVendor>> = _activeVendors.asStateFlow()

    private val _orderPlacementResult = MutableStateFlow<OrderPlacementResult>(OrderPlacementResult.Idle)
    /** Emits [OrderPlacementResult.Success] or [OrderPlacementResult.Error] after [placeOrder]. Reset to [OrderPlacementResult.Idle] via [clearOrderResult]. */
    val orderPlacementResult: StateFlow<OrderPlacementResult> = _orderPlacementResult.asStateFlow()

    init {
        listenToActiveVendors()
    }

    /**
     * Attaches a SnapshotListener to /vendors.
     * A vendor marker is shown only when:
     *   - isStallOpen == true
     *   - isBroadcastingLocation == true
     *   - The document has non-zero lat/lng coordinates saved by the vendor
     *
     * The listener is automatically removed when the ViewModel is cleared.
     */
    private fun listenToActiveVendors() {
        db.collection("vendors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CustomerViewModel", "Vendor listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val live = snapshot.documents.mapNotNull { doc ->
                    val isOpen = doc.getBoolean("isStallOpen") ?: false
                    val isBroadcasting = doc.getBoolean("isBroadcastingLocation") ?: false
                    val lat = doc.getDouble("lat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: 0.0

                    // Only show vendors who are open, broadcasting, and have saved coordinates
                    if (!isOpen || !isBroadcasting || (lat == 0.0 && lng == 0.0)) return@mapNotNull null

                    ActiveVendor(
                        vendorId = doc.id,
                        stallName = doc.getString("stallName") ?: "Unnamed Stall",
                        lat = lat,
                        lng = lng
                    )
                }
                _activeVendors.value = live
            }
    }

    // ── Order placement ───────────────────────────────────────────────────────

    /**
     * Writes a new order document to the /orders Firestore collection.
     *
     * The document is created at /orders/{auto-id}. The generated document ID is
     * also stored inside the document as [Order.orderId] for convenient queries.
     *
     * @param vendorId    Firestore vendor document ID (the vendor's Firebase UID).
     * @param items       Ordered [VendorMenuItem] entries from the cart.
     * @param quantities  Map of item name → quantity selected in the cart.
     * @param totalAmount Formatted payable string (e.g. "₹65").
     */
    fun placeOrder(
        vendorId: String,
        items: List<VendorMenuItem>,
        quantities: Map<String, Int>,
        totalAmount: String
    ) {
        val customerId = auth.currentUser?.uid
        if (customerId == null) {
            _orderPlacementResult.value = OrderPlacementResult.Error("You must be logged in to place an order.")
            return
        }

        // Build a human-readable item list: ["2x Vada Pav", "1x Chai"]
        val itemList = items
            .filter { (quantities[it.name] ?: 0) > 0 }
            .map { "${quantities[it.name]}x ${it.name}" }

        val orderData = hashMapOf(
            "customerId" to customerId,
            "vendorId" to vendorId,
            "items" to itemList,
            "totalAmount" to totalAmount,
            "status" to "PENDING",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener { docRef ->
                // Patch the generated orderId back into the document
                docRef.update("orderId", docRef.id)
                _orderPlacementResult.value = OrderPlacementResult.Success(docRef.id)
                Log.d("CustomerViewModel", "Order placed: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e("CustomerViewModel", "placeOrder failed", e)
                _orderPlacementResult.value = OrderPlacementResult.Error(
                    e.localizedMessage ?: "Failed to place order."
                )
            }
    }
    fun submitOrderReview(orderId: String, rating: Int, reviewText: String) {
        db.collection("orders").document(orderId)
            .update(
                mapOf(
                    "rating" to rating,
                    "reviewText" to reviewText
                )
            )
            .addOnFailureListener { Log.e("CustomerViewModel", "Failed to submit review", it) }
    }
    /** Resets [orderPlacementResult] to [OrderPlacementResult.Idle]. Call after consuming the result in the UI. */
    fun clearOrderResult() {
        _orderPlacementResult.value = OrderPlacementResult.Idle
    }
}
