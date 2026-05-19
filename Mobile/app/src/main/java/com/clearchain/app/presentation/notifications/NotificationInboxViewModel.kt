package com.clearchain.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationInboxState())
    val state: StateFlow<NotificationInboxState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                notificationRepository.observeAll(),
                notificationRepository.observeUnreadCount()
            ) { notifications, unreadCount ->
                NotificationInboxState(notifications = notifications, unreadCount = unreadCount)
            }.collect { _state.value = it }
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
