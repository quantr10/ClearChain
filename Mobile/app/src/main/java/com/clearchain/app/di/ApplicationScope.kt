package com.clearchain.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A process-lifetime [CoroutineScope], for fire-and-forget work that must outlive whatever
 * (typically a ViewModel's `onCleared()`) started it — e.g. leaving a SignalR room. A ViewModel
 * building its own ad hoc `CoroutineScope(Dispatchers.IO)` for this is never cancelled, since
 * `viewModelScope` is already cancelled by the time `onCleared()` runs; this shared scope is
 * cancelled only if the process dies, which is the correct lifetime for that kind of cleanup.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object ApplicationScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
