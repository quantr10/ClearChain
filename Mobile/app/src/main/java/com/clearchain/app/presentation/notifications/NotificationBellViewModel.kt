package com.clearchain.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.remote.dto.toEntity
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the header bell's unread badge.
 *
 * Deliberately smaller than [NotificationInboxViewModel]: the dashboard only needs a number, so
 * this holds no list state. It still syncs and listens on SignalR, because the badge is the
 * first thing a user sees after opening the app — a count that only refreshed once the inbox
 * was opened would be stale exactly when it matters.
 */
@HiltViewModel
class NotificationBellViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val signalRService: SignalRService
) : ViewModel() {

    val unreadCount: StateFlow<Int> = notificationRepository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch { notificationRepository.sync() }

        viewModelScope.launch {
            signalRService.notificationReceived.collect { notification ->
                notificationRepository.insert(notification.toEntity().toDomain())
            }
        }
    }
}
