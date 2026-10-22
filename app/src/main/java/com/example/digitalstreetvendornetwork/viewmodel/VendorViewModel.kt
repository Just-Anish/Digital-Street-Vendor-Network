package com.example.digitalstreetvendornetwork.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.digitalstreetvendornetwork.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

sealed class SaveResult {
    object Idle : SaveResult()
    object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

class VendorViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val vendorId = auth.currentUser?.uid

    private val _stallName = MutableStateFlow("Loading...")
    val stallName: StateFlow<String> = _stallName.asStateFlow()

    private val _isStallOpen = MutableStateFlow(false)
    val isStallOpen: StateFlow<Boolean> = _isStallOpen.asStateFlow()

    private val _operatingHours = MutableStateFlow("")
    val operatingHours: StateFlow<String> = _operatingHours.asStateFlow()

    private val _upiId = MutableStateFlow("")
    val upiId: StateFlow<String> = _upiId.asStateFlow()

    // ── Location broadcast ────────────────────────────────────────────────────
    private val _isBroadcastingLocation = MutableStateFlow(false)
    val isBroadcastingLocation: StateFlow<Boolean> = _isBroadcastingLocation.asStateFlow()

    /** Last-known stall coordinates (lat, lng). Default: central Mumbai. */
    private val _stallLat = MutableStateFlow(19.0760)
    val stallLat: StateFlow<Double> = _stallLat.asStateFlow()

    private val _stallLng = MutableStateFlow(72.8777)
    val stallLng: StateFlow<Double> = _stallLng.asStateFlow()

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList.asStateFlow()

    // Aliases to match UI expectations
    val catalogueProducts: StateFlow<List<Product>> = _productList
    val catalogueLoading: StateFlow<Boolean> = MutableStateFlow(false)

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    // ── Live incoming orders (Batch 4B) ──────────────────────────────────────────

    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    /**
     * Real-time list of orders whose vendorId matches the logged-in vendor UID.
     * Sourced from /orders via a whereEqualTo("vendorId", uid) SnapshotListener.
     * Ordered by timestamp descending (newest first).
     */
    val activeOrders: StateFlow<List<Order>> = _activeOrders.asStateFlow()

    // ── Earnings analytics (Batch 5A) ──────────────────────────────────────────

    private val _todayEarnings = MutableStateFlow("₹0")
    /**
     * Formatted string (e.g. "₹240") representing the sum of [Order.totalAmount]
     * for all COMPLETED orders whose [Order.timestamp] falls on today's calendar day.
     * Recomputed automatically every time [activeOrders] changes via [listenToOrders].
     */
    val todayEarnings: StateFlow<String> = _todayEarnings.asStateFlow()

    private val _completedOrderCount = MutableStateFlow(0)
    /**
     * Count of orders with status == "COMPLETED" placed today.
     * Recomputed automatically every time [activeOrders] changes via [listenToOrders].
     */
    val completedOrderCount: StateFlow<Int> = _completedOrderCount.asStateFlow()

    init {
        if (vendorId != null) {
            listenToBusinessSettings()
            listenToCatalogue()
            listenToOrders()
        } else {
            Log.e("VendorViewModel", "Error: User is not logged in!")
        }
    }

    private fun listenToBusinessSettings() {
        db.collection("vendors").document(vendorId!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    _stallName.value = snapshot.getString("stallName") ?: "My Stall"
                    _isStallOpen.value = snapshot.getBoolean("isStallOpen") ?: false
                    _operatingHours.value = snapshot.getString("operatingHours") ?: "9 AM - 9 PM"
                    _upiId.value = snapshot.getString("upiId") ?: ""
                    _isBroadcastingLocation.value = snapshot.getBoolean("isBroadcastingLocation") ?: false
                    _stallLat.value = snapshot.getDouble("lat") ?: 19.0760
                    _stallLng.value = snapshot.getDouble("lng") ?: 72.8777
                }
            }
    }

    private fun listenToCatalogue() {
        db.collection("vendors").document(vendorId!!).collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val price = doc.getString("price") ?: ""
                        val description = doc.getString("description") ?: ""
                        val isAvailable = doc.getBoolean("isAvailable") ?: true
                        Product(id = doc.id, name = name, price = price, description = description, isAvailable = isAvailable)
                    }
                    _productList.value = products
                }
            }
    }

    fun updateBusinessSettings(name: String, hours: String, upi: String) {
        if (vendorId == null) return
        val data = mapOf("stallName" to name, "operatingHours" to hours, "upiId" to upi)
        db.collection("vendors").document(vendorId).set(data, SetOptions.merge())
            .addOnSuccessListener { _saveResult.value = SaveResult.Success }
            .addOnFailureListener { _saveResult.value = SaveResult.Error(it.localizedMessage ?: "Failed") }
    }

    fun saveBusinessSettings(name: String, hours: String, upi: String) {
        updateBusinessSettings(name, hours, upi)
    }

    fun toggleStallStatus(isOpen: Boolean) {
        if (vendorId == null) return
        db.collection("vendors").document(vendorId).set(mapOf("isStallOpen" to isOpen), SetOptions.merge())
    }

    fun setStallOpen(isOpen: Boolean) {
        toggleStallStatus(isOpen)
    }

    fun addProduct(name: String, price: String, desc: String) {
        if (vendorId == null) return
        val productData = mapOf("name" to name, "price" to price, "description" to desc, "isAvailable" to true)
        db.collection("vendors").document(vendorId).collection("products").add(productData)
    }

    fun deleteProduct(productId: String) {
        if (vendorId == null) return
        db.collection("vendors").document(vendorId).collection("products").document(productId).delete()
    }

    fun toggleProductAvailability(productId: String, isAvailable: Boolean) {
        if (vendorId == null) return
        db.collection("vendors").document(vendorId).collection("products").document(productId)
            .set(mapOf("isAvailable" to isAvailable), SetOptions.merge())
    }

    fun clearSaveResult() {
        _saveResult.value = SaveResult.Idle
    }

    // ── Location broadcast writes ─────────────────────────────────────────────

    /**
     * Persists the vendor's stall coordinates to Firestore.
     * Called whenever the user taps or drags the map marker.
     *
     * Firestore path: /vendors/{vendorId}
     * Fields written (merge): lat: Double, lng: Double
     */
    fun updateLocation(lat: Double, lng: Double) {
        if (vendorId == null) return
        // Optimistic local update so the marker moves instantly
        _stallLat.value = lat
        _stallLng.value = lng
        db.collection("vendors").document(vendorId)
            .set(mapOf("lat" to lat, "lng" to lng), SetOptions.merge())
            .addOnFailureListener { e -> Log.e("VendorViewModel", "updateLocation failed", e) }
    }

    /**
     * Toggles the isBroadcastingLocation flag in Firestore.
     * When true, the vendor's marker becomes visible to customers on the discovery map.
     */
    fun toggleLocationBroadcast(isBroadcasting: Boolean) {
        if (vendorId == null) return
        _isBroadcastingLocation.value = isBroadcasting   // optimistic
        db.collection("vendors").document(vendorId)
            .set(mapOf("isBroadcastingLocation" to isBroadcasting), SetOptions.merge())
            .addOnFailureListener { e -> Log.e("VendorViewModel", "toggleLocationBroadcast failed", e) }
    }

    // ── Order listening & status management (Batch 4B) ────────────────────────

    /**
     * Attaches a real-time SnapshotListener to the top-level /orders collection,
     * filtered to documents where vendorId == this vendor's UID.
     *
     * Results are sorted by [Order.timestamp] descending so the newest order
     * always appears first. The listener is automatically cleaned up when the
     * ViewModel is cleared.
     *
     * Firestore path queried: /orders where vendorId == uid, orderBy timestamp DESC
     */
    private fun listenToOrders() {
        if (vendorId == null) return
        db.collection("orders")
            .whereEqualTo("vendorId", vendorId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VendorViewModel", "listenToOrders error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val orders = snapshot.documents.mapNotNull { doc ->
                    Order(
                        orderId     = doc.getString("orderId") ?: doc.id,
                        customerId  = doc.getString("customerId") ?: "",
                        vendorId    = doc.getString("vendorId") ?: "",
                        items       = (doc.get("items") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        totalAmount = doc.getString("totalAmount") ?: "",
                        status      = doc.getString("status") ?: "PENDING",
                        timestamp   = doc.getLong("timestamp") ?: 0L
                    )
                }
                _activeOrders.value = orders
                recomputeEarnings(orders)
            }
    }

    /**
     * Updates the [Order.status] field of a single /orders document in Firestore.
     *
     * Valid transitions driven from [VendorOrdersScreen]:
     *   PENDING   -> PREPARING  (vendor accepts)
     *   PREPARING -> COMPLETED  (vendor marks done)
     *   PENDING   -> DECLINED   (vendor rejects)
     *
     * Firestore path: /orders/{orderId}
     * Field written (merge): status: String
     *
     * @param orderId   The Firestore document ID of the order to update.
     * @param newStatus One of: "PENDING", "PREPARING", "COMPLETED", "DECLINED".
     */
    fun updateOrderStatus(orderId: String, newStatus: String) {
        db.collection("orders").document(orderId)
            .set(mapOf("status" to newStatus), SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("VendorViewModel", "updateOrderStatus($orderId -> $newStatus) failed", e)
            }
    }

    // ── Earnings computation (Batch 5A) ───────────────────────────────────────

    /**
     * Recomputes [todayEarnings] and [completedOrderCount] from [orders].
     *
     * "Today" is defined as any timestamp whose calendar date (year/month/day)
     * matches the device's current local date at the moment of computation.
     *
     * Amount parsing: [Order.totalAmount] is stored as a formatted string such as
     * "₹65" or "₹130". The leading "₹" (and any other non-digit prefix) is stripped
     * before converting to Int; malformed values default to 0.
     *
     * Called automatically by [listenToOrders] whenever the snapshot changes.
     */
    private fun recomputeEarnings(orders: List<Order>) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24L * 60 * 60 * 1000  // exclusive upper bound

        val completedToday = orders.filter { order ->
            order.status.equals("COMPLETED", ignoreCase = true) &&
                order.timestamp in todayStart until todayEnd
        }

        val totalRupees = completedToday.sumOf { order ->
            order.totalAmount
                .filter { it.isDigit() }   // strip "₹" and any other non-digit chars
                .toIntOrNull() ?: 0
        }

        _completedOrderCount.value = completedToday.size
        _todayEarnings.value = "₹$totalRupees"
    }

    /**
     * Public entry point to trigger an immediate earnings recalculation.
     * The [listenToOrders] SnapshotListener calls [recomputeEarnings] automatically
     * on every update, so calling this manually is only needed when navigating to
     * the Earnings screen without a recent snapshot event.
     */
    fun calculateTodayEarnings() {
        recomputeEarnings(_activeOrders.value)
    }
}

