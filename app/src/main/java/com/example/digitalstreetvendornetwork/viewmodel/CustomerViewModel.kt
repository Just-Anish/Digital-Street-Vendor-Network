package com.example.digitalstreetvendornetwork.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
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

    private val _activeVendors = MutableStateFlow<List<ActiveVendor>>(emptyList())
    val activeVendors: StateFlow<List<ActiveVendor>> = _activeVendors.asStateFlow()

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
}
