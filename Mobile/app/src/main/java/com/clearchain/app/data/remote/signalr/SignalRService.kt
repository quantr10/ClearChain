package com.clearchain.app.data.remote.signalr

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.dto.InventoryItemData
import com.clearchain.app.data.remote.dto.ListingData
import com.clearchain.app.data.remote.dto.PickupRequestData
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalRService @Inject constructor(
    private val database: ClearChainDatabase
) {
    companion object {
        private const val TAG = "SignalRService"
        private const val PICKUP_HUB_URL = "http://10.0.2.2:5000/hubs/pickuprequests"
        private const val LISTING_HUB_URL = "http://10.0.2.2:5000/hubs/listings"
        private const val INVENTORY_HUB_URL = "http://10.0.2.2:5000/hubs/inventory"
        private const val ADMIN_HUB_URL = "http://10.0.2.2:5000/hubs/admin"  // ✅ ADD
    }

    private var pickupHubConnection: HubConnection? = null
    private var listingHubConnection: HubConnection? = null
    private var inventoryHubConnection: HubConnection? = null
    private var adminHubConnection: HubConnection? = null  // ✅ ADD
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Pickup Request Events
    private val _pickupRequestCreated = MutableSharedFlow<PickupRequestData>()
    val pickupRequestCreated: SharedFlow<PickupRequestData> = _pickupRequestCreated.asSharedFlow()

    private val _pickupRequestStatusChanged = MutableSharedFlow<StatusChangeNotification>()
    val pickupRequestStatusChanged: SharedFlow<StatusChangeNotification> = _pickupRequestStatusChanged.asSharedFlow()

    private val _pickupRequestCancelled = MutableSharedFlow<PickupRequestData>()
    val pickupRequestCancelled: SharedFlow<PickupRequestData> = _pickupRequestCancelled.asSharedFlow()

    // Listing Events
    private val _listingCreated = MutableSharedFlow<ListingData>()
    val listingCreated: SharedFlow<ListingData> = _listingCreated.asSharedFlow()

    private val _listingUpdated = MutableSharedFlow<ListingData>()
    val listingUpdated: SharedFlow<ListingData> = _listingUpdated.asSharedFlow()

    private val _listingDeleted = MutableSharedFlow<ListingDeletedNotification>()
    val listingDeleted: SharedFlow<ListingDeletedNotification> = _listingDeleted.asSharedFlow()

    private val _listingQuantityChanged = MutableSharedFlow<ListingQuantityNotification>()
    val listingQuantityChanged: SharedFlow<ListingQuantityNotification> = _listingQuantityChanged.asSharedFlow()

    // Inventory Events
    private val _inventoryItemAdded = MutableSharedFlow<InventoryItemData>()
    val inventoryItemAdded: SharedFlow<InventoryItemData> = _inventoryItemAdded.asSharedFlow()

    private val _inventoryItemDistributed = MutableSharedFlow<InventoryDistributedNotification>()
    val inventoryItemDistributed: SharedFlow<InventoryDistributedNotification> = _inventoryItemDistributed.asSharedFlow()

    private val _inventoryItemExpired = MutableSharedFlow<InventoryItemData>()
    val inventoryItemExpired: SharedFlow<InventoryItemData> = _inventoryItemExpired.asSharedFlow()

    private val _inventoryItemUpdated = MutableSharedFlow<InventoryItemData>()
    val inventoryItemUpdated: SharedFlow<InventoryItemData> = _inventoryItemUpdated.asSharedFlow()

    // ✅ NEW: Admin Events
    private val _newOrganizationRegistered = MutableSharedFlow<OrganizationRegisteredNotification>()
    val newOrganizationRegistered: SharedFlow<OrganizationRegisteredNotification> = _newOrganizationRegistered.asSharedFlow()

    private val _transactionCompleted = MutableSharedFlow<TransactionCompletedNotification>()
    val transactionCompleted: SharedFlow<TransactionCompletedNotification> = _transactionCompleted.asSharedFlow()

    private val _statsUpdated = MutableSharedFlow<PlatformStatsNotification>()
    val statsUpdated: SharedFlow<PlatformStatsNotification> = _statsUpdated.asSharedFlow()

    private val _systemAlert = MutableSharedFlow<SystemAlertNotification>()
    val systemAlert: SharedFlow<SystemAlertNotification> = _systemAlert.asSharedFlow()

    // Connection state
    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    suspend fun connect() {
        connectToPickupHub()
        connectToListingHub()
        connectToInventoryHub()
        connectToAdminHub()
    }

    private suspend fun connectToPickupHub() {
        try {
            if (pickupHubConnection?.connectionState == HubConnectionState.CONNECTED) {
                Log.d(TAG, "Already connected to Pickup Hub")
                return
            }

            val authToken = database.authTokenDao().getTokens()
            val token = authToken?.accessToken

            if (token == null) {
                Log.e(TAG, "No token available for SignalR connection")
                _connectionState.emit(ConnectionState.Error("Not authenticated"))
                return
            }

            Log.d(TAG, "Connecting to Pickup Hub: $PICKUP_HUB_URL")

            val urlWithToken = "$PICKUP_HUB_URL?access_token=$token"
            pickupHubConnection = HubConnectionBuilder.create(urlWithToken).build()

            setupPickupEventHandlers()

            pickupHubConnection?.start()?.blockingAwait()
            
            Log.d(TAG, "✅ Connected to Pickup Hub")
            _connectionState.emit(ConnectionState.Connected)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to connect to Pickup Hub: ${e.message}", e)
            _connectionState.emit(ConnectionState.Error(e.message ?: "Connection failed"))
        }
    }

    private suspend fun connectToListingHub() {
        try {
            if (listingHubConnection?.connectionState == HubConnectionState.CONNECTED) {
                Log.d(TAG, "Already connected to Listing Hub")
                return
            }

            val authToken = database.authTokenDao().getTokens()
            val token = authToken?.accessToken

            if (token == null) {
                Log.e(TAG, "No token available for Listing Hub")
                return
            }

            Log.d(TAG, "Connecting to Listing Hub: $LISTING_HUB_URL")

            val urlWithToken = "$LISTING_HUB_URL?access_token=$token"
            listingHubConnection = HubConnectionBuilder.create(urlWithToken).build()

            setupListingEventHandlers()

            listingHubConnection?.start()?.blockingAwait()
            
            Log.d(TAG, "✅ Connected to Listing Hub")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to connect to Listing Hub: ${e.message}", e)
        }
    }

    private suspend fun connectToInventoryHub() {
        try {
            if (inventoryHubConnection?.connectionState == HubConnectionState.CONNECTED) {
                Log.d(TAG, "Already connected to Inventory Hub")
                return
            }

            val authToken = database.authTokenDao().getTokens()
            val token = authToken?.accessToken

            if (token == null) {
                Log.e(TAG, "No token available for Inventory Hub")
                return
            }

            Log.d(TAG, "Connecting to Inventory Hub: $INVENTORY_HUB_URL")

            val urlWithToken = "$INVENTORY_HUB_URL?access_token=$token"
            inventoryHubConnection = HubConnectionBuilder.create(urlWithToken).build()

            setupInventoryEventHandlers()

            inventoryHubConnection?.start()?.blockingAwait()
            
            Log.d(TAG, "✅ Connected to Inventory Hub")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to connect to Inventory Hub: ${e.message}", e)
        }
    }

    // ✅ NEW: Connect to Admin Hub
    private suspend fun connectToAdminHub() {
        try {
            if (adminHubConnection?.connectionState == HubConnectionState.CONNECTED) {
                Log.d(TAG, "Already connected to Admin Hub")
                return
            }

            val authToken = database.authTokenDao().getTokens()
            val token = authToken?.accessToken

            if (token == null) {
                Log.d(TAG, "No token available for Admin Hub")
                return
            }

            Log.d(TAG, "Connecting to Admin Hub: $ADMIN_HUB_URL")

            val urlWithToken = "$ADMIN_HUB_URL?access_token=$token"
            adminHubConnection = HubConnectionBuilder.create(urlWithToken).build()

            setupAdminEventHandlers()

            adminHubConnection?.start()?.blockingAwait()
            
            Log.d(TAG, "✅ Connected to Admin Hub")

        } catch (e: Exception) {
            // ✅ Make 403 silent (not an error for non-admins)
            if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) {
                Log.d(TAG, "ℹ️ Admin Hub not accessible (user is not admin)")
            } else {
                Log.e(TAG, "❌ Failed to connect to Admin Hub: ${e.message}")
            }
            // Don't throw - let other connections succeed
        }
    }

    private fun setupPickupEventHandlers() {
        pickupHubConnection?.apply {
            on("PickupRequestCreated", { data: PickupRequestData ->
                Log.d(TAG, "📢 PickupRequestCreated: ${data.id}")
                serviceScope.launch {
                    _pickupRequestCreated.emit(data)
                }
            }, PickupRequestData::class.java)

            on("PickupRequestStatusChanged", { notification: StatusChangeNotification ->
                Log.d(TAG, "📢 PickupRequestStatusChanged: ${notification.request.id} → ${notification.newStatus}")
                serviceScope.launch {
                    _pickupRequestStatusChanged.emit(notification)
                }
            }, StatusChangeNotification::class.java)

            on("PickupRequestCancelled", { data: PickupRequestData ->
                Log.d(TAG, "📢 PickupRequestCancelled: ${data.id}")
                serviceScope.launch {
                    _pickupRequestCancelled.emit(data)
                }
            }, PickupRequestData::class.java)

            onClosed { error ->
                Log.w(TAG, "Pickup Hub connection closed", error)
                serviceScope.launch {
                    _connectionState.emit(ConnectionState.Disconnected)
                }
            }
        }
    }

    private fun setupListingEventHandlers() {
        listingHubConnection?.apply {
            on("ListingCreated", { data: ListingData ->
                Log.d(TAG, "📢 ListingCreated: ${data.id}")
                serviceScope.launch {
                    _listingCreated.emit(data)
                }
            }, ListingData::class.java)

            on("ListingUpdated", { data: ListingData ->
                Log.d(TAG, "📢 ListingUpdated: ${data.id}")
                serviceScope.launch {
                    _listingUpdated.emit(data)
                }
            }, ListingData::class.java)

            on("ListingDeleted", { notification: ListingDeletedNotification ->
                Log.d(TAG, "📢 ListingDeleted: ${notification.listingId}")
                serviceScope.launch {
                    _listingDeleted.emit(notification)
                }
            }, ListingDeletedNotification::class.java)

            on("ListingQuantityChanged", { notification: ListingQuantityNotification ->
                Log.d(TAG, "📢 ListingQuantityChanged: ${notification.listing.id} (${notification.oldQuantity} → ${notification.newQuantity})")
                serviceScope.launch {
                    _listingQuantityChanged.emit(notification)
                }
            }, ListingQuantityNotification::class.java)

            onClosed { error ->
                Log.w(TAG, "Listing Hub connection closed", error)
            }
        }
    }

    private fun setupInventoryEventHandlers() {
        inventoryHubConnection?.apply {
            on("InventoryItemAdded", { data: InventoryItemData ->
                Log.d(TAG, "📢 InventoryItemAdded: ${data.id}")
                serviceScope.launch {
                    _inventoryItemAdded.emit(data)
                }
            }, InventoryItemData::class.java)

            on("InventoryItemDistributed", { notification: InventoryDistributedNotification ->
                Log.d(TAG, "📢 InventoryItemDistributed: ${notification.itemId}")
                serviceScope.launch {
                    _inventoryItemDistributed.emit(notification)
                }
            }, InventoryDistributedNotification::class.java)

            on("InventoryItemExpired", { data: InventoryItemData ->
                Log.d(TAG, "📢 InventoryItemExpired: ${data.id}")
                serviceScope.launch {
                    _inventoryItemExpired.emit(data)
                }
            }, InventoryItemData::class.java)

            on("InventoryItemUpdated", { data: InventoryItemData ->
                Log.d(TAG, "📢 InventoryItemUpdated: ${data.id}")
                serviceScope.launch {
                    _inventoryItemUpdated.emit(data)
                }
            }, InventoryItemData::class.java)

            onClosed { error ->
                Log.w(TAG, "Inventory Hub connection closed", error)
            }
        }
    }

    // ✅ NEW: Setup Admin Event Handlers
    private fun setupAdminEventHandlers() {
        adminHubConnection?.apply {
            // Listen for new organization registrations
            on("NewOrganizationRegistered", { notification: OrganizationRegisteredNotification ->
                Log.d(TAG, "📢 NewOrganizationRegistered: ${notification.name} (${notification.type})")
                serviceScope.launch {
                    _newOrganizationRegistered.emit(notification)
                }
            }, OrganizationRegisteredNotification::class.java)

            // Listen for completed transactions
            on("TransactionCompleted", { notification: TransactionCompletedNotification ->
                Log.d(TAG, "📢 TransactionCompleted: ${notification.transactionId}")
                serviceScope.launch {
                    _transactionCompleted.emit(notification)
                }
            }, TransactionCompletedNotification::class.java)

            // Listen for stats updates
            on("StatsUpdated", { stats: PlatformStatsNotification ->
                Log.d(TAG, "📢 StatsUpdated: ${stats.totalDonations} total donations")
                serviceScope.launch {
                    _statsUpdated.emit(stats)
                }
            }, PlatformStatsNotification::class.java)

            // Listen for system alerts
            on("SystemAlert", { alert: SystemAlertNotification ->
                Log.d(TAG, "📢 SystemAlert [${alert.level}]: ${alert.message}")
                serviceScope.launch {
                    _systemAlert.emit(alert)
                }
            }, SystemAlertNotification::class.java)

            onClosed { error ->
                Log.w(TAG, "Admin Hub connection closed", error)
            }
        }
    }

    suspend fun disconnect() {
        try {
            pickupHubConnection?.stop()?.blockingAwait()
            listingHubConnection?.stop()?.blockingAwait()
            inventoryHubConnection?.stop()?.blockingAwait()
            adminHubConnection?.stop()?.blockingAwait()  // ✅ ADD
            Log.d(TAG, "Disconnected from all hubs")
            _connectionState.emit(ConnectionState.Disconnected)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from hubs", e)
        }
    }

    fun isConnected(): Boolean {
        return pickupHubConnection?.connectionState == HubConnectionState.CONNECTED ||
                listingHubConnection?.connectionState == HubConnectionState.CONNECTED ||
                inventoryHubConnection?.connectionState == HubConnectionState.CONNECTED ||
                adminHubConnection?.connectionState == HubConnectionState.CONNECTED  // ✅ ADD
    }

    // Pickup Request room management
    suspend fun joinPickupRequestRoom(requestId: String) {
        try {
            pickupHubConnection?.send("JoinPickupRequestRoom", requestId)
            Log.d(TAG, "Joined pickup request room: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Error joining pickup room: $requestId", e)
        }
    }

    suspend fun leavePickupRequestRoom(requestId: String) {
        try {
            pickupHubConnection?.send("LeavePickupRequestRoom", requestId)
            Log.d(TAG, "Left pickup request room: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving pickup room: $requestId", e)
        }
    }

    // Listing room management
    suspend fun joinListingRoom(listingId: String) {
        try {
            listingHubConnection?.send("JoinListingRoom", listingId)
            Log.d(TAG, "Joined listing room: $listingId")
        } catch (e: Exception) {
            Log.e(TAG, "Error joining listing room: $listingId", e)
        }
    }

    suspend fun leaveListingRoom(listingId: String) {
        try {
            listingHubConnection?.send("LeaveListingRoom", listingId)
            Log.d(TAG, "Left listing room: $listingId")
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving listing room: $listingId", e)
        }
    }

    // Inventory room management
    suspend fun joinInventoryItemRoom(itemId: String) {
        try {
            inventoryHubConnection?.send("JoinInventoryItemRoom", itemId)
            Log.d(TAG, "Joined inventory item room: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error joining inventory room: $itemId", e)
        }
    }

    suspend fun leaveInventoryItemRoom(itemId: String) {
        try {
            inventoryHubConnection?.send("LeaveInventoryItemRoom", itemId)
            Log.d(TAG, "Left inventory item room: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving inventory room: $itemId", e)
        }
    }
}

// Pickup Request notifications
data class StatusChangeNotification(
    val request: PickupRequestData,
    val oldStatus: String,
    val newStatus: String,
    val timestamp: String
)

// Listing notifications
data class ListingDeletedNotification(
    val listingId: String,
    val timestamp: String
)

data class ListingQuantityNotification(
    val listing: ListingData,
    val oldQuantity: Int,
    val newQuantity: Int,
    val timestamp: String
)

// Inventory notifications
data class InventoryDistributedNotification(
    val itemId: String,
    val timestamp: String
)

// ✅ NEW: Admin notifications
data class OrganizationRegisteredNotification(
    val organizationId: String,
    val name: String,
    val type: String,
    val email: String,
    val location: String,
    val registeredAt: String
)

data class TransactionCompletedNotification(
    val transactionId: String,
    val ngoId: String,
    val ngoName: String,
    val groceryId: String,
    val groceryName: String,
    val productName: String,
    val quantity: Int,
    val unit: String,
    val completedAt: String
)

data class PlatformStatsNotification(
    val totalNGOs: Int,
    val totalGroceries: Int,
    val totalDonations: Int,
    val activeListings: Int,
    val pendingRequests: Int,
    val completedToday: Int,
    val updatedAt: String
)

data class SystemAlertNotification(
    val level: String, // "info", "warning", "error"
    val message: String,
    val details: String?,
    val timestamp: String
)

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}