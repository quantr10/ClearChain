package com.clearchain.app.data.remote.signalr

import android.util.Log
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.dto.InventoryItemData
import com.clearchain.app.data.remote.dto.ListingData
import com.clearchain.app.data.remote.dto.NotificationData
import com.clearchain.app.data.remote.dto.PickupRequestData
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's single live connection to the server's SignalR hubs.
 *
 * Ownership matters here. This is a `@Singleton`, so its connection is shared by every screen —
 * which means no screen may tear it down. [connect] and [disconnect] belong to
 * [com.clearchain.app.data.remote.signalr.RealtimeLifecycleObserver] and the auth flow alone;
 * view models only collect the flows. (Every view model used to call `disconnect()` from
 * `onCleared()`, so navigating away from any one screen killed real-time updates for all the
 * others.)
 *
 * Connecting blocks the calling thread — the SignalR Java client is Rx-based and its
 * `Completable`s are awaited, not suspended on — so all of it runs on [Dispatchers.IO].
 */
@Singleton
class SignalRService @Inject constructor(
    private val database: ClearChainDatabase
) {
    companion object {
        private const val TAG = "SignalRService"
        private const val BASE_WS_URL = "http://10.0.2.2:5000"
        private const val PICKUP_HUB_URL = "$BASE_WS_URL/hubs/pickuprequests"
        private const val LISTING_HUB_URL = "$BASE_WS_URL/hubs/listings"
        private const val INVENTORY_HUB_URL = "$BASE_WS_URL/hubs/inventory"
        private const val ADMIN_HUB_URL = "$BASE_WS_URL/hubs/admin"
        private const val NOTIFICATION_HUB_URL = "$BASE_WS_URL/hubs/notifications"

        /** Events are dropped rather than blocking a hub callback when nothing is collecting. */
        private const val EVENT_BUFFER = 64

        /**
         * Reconnect backoff, holding at 30s. It never gives up because the usual cause is a
         * temporary loss of signal, and a client that stopped retrying would stay dark until
         * the user happened to background and foreground the app.
         */
        private val RECONNECT_DELAYS_MS = longArrayOf(2_000, 5_000, 10_000, 20_000, 30_000)
    }

    private var pickupHubConnection: HubConnection? = null
    private var listingHubConnection: HubConnection? = null
    private var inventoryHubConnection: HubConnection? = null
    private var adminHubConnection: HubConnection? = null
    private var notificationHubConnection: HubConnection? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Serializes connect/disconnect so overlapping calls can't build duplicate connections. */
    private val connectionMutex = Mutex()

    /**
     * Whether the app currently wants a connection. Distinguishes a drop worth retrying from a
     * deliberate close, so backgrounding and logout don't fight the reconnect loop.
     */
    @Volatile
    private var shouldStayConnected = false

    private var reconnectJob: Job? = null

    /**
     * Rooms joined per hub. A reconnect gives us a new connection id, and the server's groups
     * are keyed by connection id — so without replaying these, a dropped connection silently
     * stops receiving per-entity updates even though it looks healthy again.
     */
    private val joinedPickupRooms = Collections.synchronizedSet(mutableSetOf<String>())
    private val joinedListingRooms = Collections.synchronizedSet(mutableSetOf<String>())
    private val joinedInventoryRooms = Collections.synchronizedSet(mutableSetOf<String>())

    private fun <T> eventFlow() = MutableSharedFlow<T>(
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Pickup Request Events
    private val _pickupRequestCreated = eventFlow<PickupRequestData>()
    val pickupRequestCreated: SharedFlow<PickupRequestData> = _pickupRequestCreated.asSharedFlow()

    private val _pickupRequestStatusChanged = eventFlow<StatusChangeNotification>()
    val pickupRequestStatusChanged: SharedFlow<StatusChangeNotification> = _pickupRequestStatusChanged.asSharedFlow()

    private val _pickupRequestCancelled = eventFlow<PickupRequestData>()
    val pickupRequestCancelled: SharedFlow<PickupRequestData> = _pickupRequestCancelled.asSharedFlow()

    /** Fires only for clients that joined the request's room via [joinPickupRequestRoom]. */
    private val _pickupRequestUpdated = eventFlow<PickupRequestData>()
    val pickupRequestUpdated: SharedFlow<PickupRequestData> = _pickupRequestUpdated.asSharedFlow()

    // Listing Events
    private val _listingCreated = eventFlow<ListingData>()
    val listingCreated: SharedFlow<ListingData> = _listingCreated.asSharedFlow()

    private val _listingUpdated = eventFlow<ListingData>()
    val listingUpdated: SharedFlow<ListingData> = _listingUpdated.asSharedFlow()

    private val _listingDeleted = eventFlow<ListingDeletedNotification>()
    val listingDeleted: SharedFlow<ListingDeletedNotification> = _listingDeleted.asSharedFlow()

    private val _listingQuantityChanged = eventFlow<ListingQuantityNotification>()
    val listingQuantityChanged: SharedFlow<ListingQuantityNotification> = _listingQuantityChanged.asSharedFlow()

    // Inventory Events
    private val _inventoryItemAdded = eventFlow<InventoryItemData>()
    val inventoryItemAdded: SharedFlow<InventoryItemData> = _inventoryItemAdded.asSharedFlow()

    private val _inventoryItemDistributed = eventFlow<InventoryDistributedNotification>()
    val inventoryItemDistributed: SharedFlow<InventoryDistributedNotification> = _inventoryItemDistributed.asSharedFlow()

    private val _inventoryItemExpired = eventFlow<InventoryItemData>()
    val inventoryItemExpired: SharedFlow<InventoryItemData> = _inventoryItemExpired.asSharedFlow()

    private val _inventoryItemUpdated = eventFlow<InventoryItemData>()
    val inventoryItemUpdated: SharedFlow<InventoryItemData> = _inventoryItemUpdated.asSharedFlow()

    // Admin Events
    private val _newOrganizationRegistered = eventFlow<OrganizationRegisteredNotification>()
    val newOrganizationRegistered: SharedFlow<OrganizationRegisteredNotification> = _newOrganizationRegistered.asSharedFlow()

    private val _transactionCompleted = eventFlow<TransactionCompletedNotification>()
    val transactionCompleted: SharedFlow<TransactionCompletedNotification> = _transactionCompleted.asSharedFlow()

    private val _statsUpdated = eventFlow<PlatformStatsNotification>()
    val statsUpdated: SharedFlow<PlatformStatsNotification> = _statsUpdated.asSharedFlow()

    private val _systemAlert = eventFlow<SystemAlertNotification>()
    val systemAlert: SharedFlow<SystemAlertNotification> = _systemAlert.asSharedFlow()

    // Notification inbox
    private val _notificationReceived = eventFlow<NotificationData>()
    val notificationReceived: SharedFlow<NotificationData> = _notificationReceived.asSharedFlow()

    /**
     * Latest connection state. A [StateFlow] rather than an event stream so a screen that
     * subscribes late still learns the current state instead of waiting for the next change.
     */
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Brings every hub up. Safe to call repeatedly — hubs already connected are left alone, so
     * this doubles as the "make sure we're live" call after a login or a return to foreground.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        shouldStayConnected = true
        connectionMutex.withLock { openAll() }
    }

    /**
     * Tears every hub down. Called when the app goes to background and on logout — never from a
     * screen, which would cut real-time for whatever else is still open.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        // Cleared first so the onClosed callbacks fired by stop() below are recognised as
        // deliberate and don't kick off a reconnect against the connection we're closing.
        shouldStayConnected = false
        reconnectJob?.cancel()
        reconnectJob = null

        connectionMutex.withLock { closeAll() }
    }

    private fun openAll() {
        if (currentAccessTokenBlocking() == null) {
            Log.d(TAG, "No session — skipping SignalR connect")
            _connectionState.value = ConnectionState.Disconnected
            return
        }

        pickupHubConnection = ensureConnected(
            "Pickup", PICKUP_HUB_URL, pickupHubConnection,
            configure = ::setupPickupEventHandlers,
            onConnected = { replayRooms(it, joinedPickupRooms, "JoinPickupRequestRoom") }
        )
        listingHubConnection = ensureConnected(
            "Listing", LISTING_HUB_URL, listingHubConnection,
            configure = ::setupListingEventHandlers,
            onConnected = { replayRooms(it, joinedListingRooms, "JoinListingRoom") }
        )
        inventoryHubConnection = ensureConnected(
            "Inventory", INVENTORY_HUB_URL, inventoryHubConnection,
            configure = ::setupInventoryEventHandlers,
            onConnected = { replayRooms(it, joinedInventoryRooms, "JoinInventoryItemRoom") }
        )
        notificationHubConnection = ensureConnected(
            "Notification", NOTIFICATION_HUB_URL, notificationHubConnection,
            configure = ::setupNotificationEventHandlers
        )
        // Non-admins are refused here by design; ensureConnected logs it quietly.
        adminHubConnection = ensureConnected(
            "Admin", ADMIN_HUB_URL, adminHubConnection,
            configure = ::setupAdminEventHandlers
        )

        if (isConnected()) {
            _connectionState.value = ConnectionState.Connected
            return
        }

        // A failed *first* attempt needs the retry loop too. onClosed only fires for a
        // connection that was once open, so without this a server that was down at launch left
        // the app offline until the user happened to background and foreground it.
        _connectionState.value = ConnectionState.Reconnecting
        scheduleReconnect()
    }

    private fun closeAll() {
        listOf(
            "Pickup" to pickupHubConnection,
            "Listing" to listingHubConnection,
            "Inventory" to inventoryHubConnection,
            "Admin" to adminHubConnection,
            "Notification" to notificationHubConnection
        ).forEach { (name, connection) ->
            runCatching { connection?.stop()?.blockingAwait() }
                .onFailure { Log.w(TAG, "Error stopping $name hub", it) }
        }

        pickupHubConnection = null
        listingHubConnection = null
        inventoryHubConnection = null
        adminHubConnection = null
        notificationHubConnection = null

        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "Disconnected from all hubs")
    }

    /**
     * Reconnection is hand-rolled because the SignalR **Java** client has no equivalent of the
     * .NET and JavaScript clients' `withAutomaticReconnect` — `onClosed` is the only signal it
     * gives, and a closed connection otherwise stays closed for the rest of the session.
     *
     * Retries back off and then hold at a steady interval rather than giving up, because the
     * usual cause is a tunnel or a lost Wi-Fi signal that comes back on its own.
     */
    private fun scheduleReconnect() {
        if (!shouldStayConnected) return

        synchronized(this) {
            if (reconnectJob?.isActive == true) return
            reconnectJob = serviceScope.launch { reconnectLoop() }
        }
    }

    private suspend fun reconnectLoop() {
        var attempt = 0

        while (shouldStayConnected && !isConnected()) {
            val wait = RECONNECT_DELAYS_MS[min(attempt, RECONNECT_DELAYS_MS.lastIndex)]
            attempt++

            _connectionState.value = ConnectionState.Reconnecting
            Log.d(TAG, "Reconnecting in ${wait}ms (attempt $attempt)")
            delay(wait)

            if (!shouldStayConnected) return

            runCatching { connectionMutex.withLock { openAll() } }
                .onFailure { Log.w(TAG, "Reconnect attempt $attempt failed: ${it.message}") }
        }

        if (shouldStayConnected && isConnected()) {
            Log.d(TAG, "✅ Reconnected after $attempt attempt(s)")
        }
    }

    /**
     * Drops every connection and rebuilds it against whoever is signed in now. Used at login and
     * logout, where reusing a connection opened with the previous account's token would keep
     * delivering that account's events.
     */
    suspend fun reconnect() {
        disconnect()
        clearRooms()
        connect()
    }

    fun isConnected(): Boolean =
        listOf(
            pickupHubConnection,
            listingHubConnection,
            inventoryHubConnection,
            adminHubConnection,
            notificationHubConnection
        ).any { it?.connectionState == HubConnectionState.CONNECTED }

    // ── Connection plumbing ──────────────────────────────────────────────────

    /**
     * Returns a live connection for [url], building one if needed.
     *
     * The access token is supplied through a provider rather than baked into the query string,
     * so a reconnect after the token was refreshed presents the new one instead of retrying
     * forever with a token the server has stopped accepting.
     */
    private fun ensureConnected(
        name: String,
        url: String,
        existing: HubConnection?,
        configure: (HubConnection) -> Unit,
        onConnected: (HubConnection) -> Unit = {}
    ): HubConnection? {
        if (existing?.connectionState == HubConnectionState.CONNECTED) return existing

        return try {
            // Handlers are registered once, on the connection object — reusing it across a
            // restart keeps them, so `configure` must not run again or every event would be
            // delivered twice.
            val connection = existing ?: HubConnectionBuilder.create(url)
                .withAccessTokenProvider(
                    Single.fromCallable {
                        currentAccessTokenBlocking() ?: ""
                    }
                )
                .build()
                .also { built ->
                    configure(built)
                    built.onClosed { error ->
                        Log.w(TAG, "$name hub closed", error)
                        _connectionState.value = ConnectionState.Disconnected
                        scheduleReconnect()
                    }
                }

            connection.start().blockingAwait()
            onConnected(connection)
            Log.d(TAG, "✅ Connected to $name hub")
            connection
        } catch (e: Exception) {
            // A non-admin hitting the admin hub is expected, not a failure worth surfacing.
            val forbidden = e.message?.contains("403") == true ||
                e.message?.contains("Forbidden") == true
            if (forbidden) {
                Log.d(TAG, "ℹ️ $name hub not accessible for this account")
            } else {
                // State is not set here: openAll decides once, after every hub has been
                // tried, whether the app is connected at all. Setting it per-hub produced a
                // value that was always overwritten before anything could observe it.
                Log.e(TAG, "❌ Failed to connect to $name hub: ${e.message}", e)
            }
            existing
        }
    }

    private suspend fun currentAccessToken(): String? =
        database.authTokenDao().getTokens()?.accessToken

    /**
     * The SignalR client calls its token provider from its own background thread and wants a
     * value, not a coroutine — so the DAO read is bridged with [runBlocking]. Never called from
     * the main thread.
     */
    private fun currentAccessTokenBlocking(): String? =
        runCatching { runBlocking { currentAccessToken() } }.getOrNull()

    /**
     * Re-joins the rooms this hub had after a (re)connect. A new connection means a new
     * connection id, and the server's groups are keyed by connection id — so without this a
     * reconnected client looks healthy while silently receiving no per-entity updates.
     */
    private fun replayRooms(connection: HubConnection, rooms: Set<String>, method: String) {
        val pending = synchronized(rooms) { rooms.toList() }
        if (pending.isEmpty()) return

        pending.forEach { roomId ->
            runCatching { connection.send(method, roomId) }
                .onFailure { Log.w(TAG, "Could not rejoin $roomId via $method", it) }
        }
        Log.d(TAG, "Rejoined ${pending.size} room(s) via $method")
    }

    private fun clearRooms() {
        joinedPickupRooms.clear()
        joinedListingRooms.clear()
        joinedInventoryRooms.clear()
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    private fun setupPickupEventHandlers(connection: HubConnection) = with(connection) {
        on("PickupRequestCreated", { data: PickupRequestData ->
            Log.d(TAG, "📢 PickupRequestCreated: ${data.id}")
            _pickupRequestCreated.tryEmit(data)
        }, PickupRequestData::class.java)

        on("PickupRequestStatusChanged", { notification: StatusChangeNotification ->
            Log.d(TAG, "📢 PickupRequestStatusChanged: ${notification.request.id} → ${notification.newStatus}")
            _pickupRequestStatusChanged.tryEmit(notification)
        }, StatusChangeNotification::class.java)

        on("PickupRequestCancelled", { data: PickupRequestData ->
            Log.d(TAG, "📢 PickupRequestCancelled: ${data.id}")
            _pickupRequestCancelled.tryEmit(data)
        }, PickupRequestData::class.java)

        on("PickupRequestUpdated", { data: PickupRequestData ->
            Log.d(TAG, "📢 PickupRequestUpdated: ${data.id}")
            _pickupRequestUpdated.tryEmit(data)
        }, PickupRequestData::class.java)
    }

    private fun setupListingEventHandlers(connection: HubConnection) = with(connection) {
        on("ListingCreated", { data: ListingData ->
            Log.d(TAG, "📢 ListingCreated: ${data.id}")
            _listingCreated.tryEmit(data)
        }, ListingData::class.java)

        on("ListingUpdated", { data: ListingData ->
            Log.d(TAG, "📢 ListingUpdated: ${data.id}")
            _listingUpdated.tryEmit(data)
        }, ListingData::class.java)

        on("ListingDeleted", { notification: ListingDeletedNotification ->
            Log.d(TAG, "📢 ListingDeleted: ${notification.listingId}")
            _listingDeleted.tryEmit(notification)
        }, ListingDeletedNotification::class.java)

        on("ListingQuantityChanged", { notification: ListingQuantityNotification ->
            Log.d(TAG, "📢 ListingQuantityChanged: ${notification.listing.id} (${notification.oldQuantity} → ${notification.newQuantity})")
            _listingQuantityChanged.tryEmit(notification)
        }, ListingQuantityNotification::class.java)
    }

    private fun setupInventoryEventHandlers(connection: HubConnection) = with(connection) {
        on("InventoryItemAdded", { data: InventoryItemData ->
            Log.d(TAG, "📢 InventoryItemAdded: ${data.id}")
            _inventoryItemAdded.tryEmit(data)
        }, InventoryItemData::class.java)

        on("InventoryItemDistributed", { notification: InventoryDistributedNotification ->
            Log.d(TAG, "📢 InventoryItemDistributed: ${notification.itemId}")
            _inventoryItemDistributed.tryEmit(notification)
        }, InventoryDistributedNotification::class.java)

        on("InventoryItemExpired", { data: InventoryItemData ->
            Log.d(TAG, "📢 InventoryItemExpired: ${data.id}")
            _inventoryItemExpired.tryEmit(data)
        }, InventoryItemData::class.java)

        on("InventoryItemUpdated", { data: InventoryItemData ->
            Log.d(TAG, "📢 InventoryItemUpdated: ${data.id}")
            _inventoryItemUpdated.tryEmit(data)
        }, InventoryItemData::class.java)
    }

    private fun setupAdminEventHandlers(connection: HubConnection) = with(connection) {
        on("NewOrganizationRegistered", { notification: OrganizationRegisteredNotification ->
            Log.d(TAG, "📢 NewOrganizationRegistered: ${notification.name} (${notification.type})")
            _newOrganizationRegistered.tryEmit(notification)
        }, OrganizationRegisteredNotification::class.java)

        on("TransactionCompleted", { notification: TransactionCompletedNotification ->
            Log.d(TAG, "📢 TransactionCompleted: ${notification.transactionId}")
            _transactionCompleted.tryEmit(notification)
        }, TransactionCompletedNotification::class.java)

        on("StatsUpdated", { stats: PlatformStatsNotification ->
            Log.d(TAG, "📢 StatsUpdated: ${stats.totalDonations} total donations")
            _statsUpdated.tryEmit(stats)
        }, PlatformStatsNotification::class.java)

        on("SystemAlert", { alert: SystemAlertNotification ->
            Log.d(TAG, "📢 SystemAlert [${alert.level}]: ${alert.message}")
            _systemAlert.tryEmit(alert)
        }, SystemAlertNotification::class.java)
    }

    private fun setupNotificationEventHandlers(connection: HubConnection) = with(connection) {
        on("NotificationReceived", { notification: NotificationData ->
            Log.d(TAG, "📢 NotificationReceived: ${notification.type} — ${notification.title}")
            _notificationReceived.tryEmit(notification)
        }, NotificationData::class.java)
    }

    // Per-entity rooms
    //
    // Joining a room is what makes the server's `pickup_{id}` / `listing_{id}` / `item_{id}`
    // broadcasts reachable at all — those groups have no members until a client asks to join.

    suspend fun joinPickupRequestRoom(requestId: String) =
        joinRoom(pickupHubConnection, joinedPickupRooms, "JoinPickupRequestRoom", requestId)

    suspend fun leavePickupRequestRoom(requestId: String) =
        leaveRoom(pickupHubConnection, joinedPickupRooms, "LeavePickupRequestRoom", requestId)

    suspend fun joinListingRoom(listingId: String) =
        joinRoom(listingHubConnection, joinedListingRooms, "JoinListingRoom", listingId)

    suspend fun leaveListingRoom(listingId: String) =
        leaveRoom(listingHubConnection, joinedListingRooms, "LeaveListingRoom", listingId)

    suspend fun joinInventoryItemRoom(itemId: String) =
        joinRoom(inventoryHubConnection, joinedInventoryRooms, "JoinInventoryItemRoom", itemId)

    suspend fun leaveInventoryItemRoom(itemId: String) =
        leaveRoom(inventoryHubConnection, joinedInventoryRooms, "LeaveInventoryItemRoom", itemId)

    private suspend fun joinRoom(
        connection: HubConnection?,
        rooms: MutableSet<String>,
        method: String,
        roomId: String
    ) = withContext(Dispatchers.IO) {
        // Recorded even when the send fails: the room is replayed on the next reconnect, which
        // is usually how a send fails in the first place.
        rooms.add(roomId)
        runCatching { connection?.send(method, roomId) }
            .onSuccess { Log.d(TAG, "Joined room $roomId via $method") }
            .onFailure { Log.w(TAG, "Error joining room $roomId", it) }
        Unit
    }

    private suspend fun leaveRoom(
        connection: HubConnection?,
        rooms: MutableSet<String>,
        method: String,
        roomId: String
    ) = withContext(Dispatchers.IO) {
        rooms.remove(roomId)
        runCatching { connection?.send(method, roomId) }
            .onSuccess { Log.d(TAG, "Left room $roomId via $method") }
            .onFailure { Log.w(TAG, "Error leaving room $roomId", it) }
        Unit
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

// Admin notifications
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

/**
 * What the app can usefully say about real-time delivery.
 *
 * There is no error state: every failure path now schedules a retry, so "failed" and "trying
 * again" are the same situation from a caller's point of view, and a distinct error value was
 * only ever written and overwritten without being observed.
 */
sealed class ConnectionState {
    /** Live. Events are arriving. */
    object Connected : ConnectionState()

    /** Deliberately closed — backgrounded, or signed out. Not a problem; show nothing. */
    object Disconnected : ConnectionState()

    /** Dropped or unreachable, with the retry loop running. Data on screen may be stale. */
    object Reconnecting : ConnectionState()
}
