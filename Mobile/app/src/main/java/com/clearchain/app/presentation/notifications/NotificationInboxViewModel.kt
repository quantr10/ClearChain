package com.clearchain.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.remote.dto.toEntity
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationInboxState())
    val state: StateFlow<NotificationInboxState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                notificationRepository.observeAll(),
                notificationRepository.observeUnreadCount()
            ) { notifications, unreadCount -> notifications to unreadCount }
                .collect { (notifications, unreadCount) ->
                    // Copied onto current state rather than replacing it: the retention window
                    // comes from sync, and rebuilding the state here would drop it on every
                    // Room emission.
                    _state.update {
                        it.copy(notifications = notifications, unreadCount = unreadCount)
                    }
                }
        }

        // Catches up on anything that arrived while the app was closed, or whose push was
        // dropped — neither reaches FCMService, so Room alone would be missing it.
        refresh()

        // While the inbox is open, a notification should appear as it is sent rather than on the
        // next manual refresh.
        viewModelScope.launch {
            signalRService.notificationReceived.collect { notification ->
                notificationRepository.insert(notification.toEntity().toDomain())
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            notificationRepository.sync().onSuccess { retention ->
                _state.update { it.copy(retention = retention) }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch { notificationRepository.markAsRead(id) }
    }

    fun markAllAsRead() {
        viewModelScope.launch { notificationRepository.markAllAsRead() }
    }

    fun clearAll() {
        viewModelScope.launch { notificationRepository.clearAll() }
    }
}
