package com.example.digitalstreetvendornetwork.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.digitalstreetvendornetwork.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList: StateFlow<List<Product>> = _productList.asStateFlow()

    // Aliases to match UI expectations
    val catalogueProducts: StateFlow<List<Product>> = _productList
    val catalogueLoading: StateFlow<Boolean> = MutableStateFlow(false)

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    init {
        if (vendorId != null) {
            listenToBusinessSettings()
            listenToCatalogue()
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
}
