package com.sametozdemir.tini.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Olay tabanlı bağlantı takibi; yoklama (polling) yapılmaz. */
class ConnectivityObserver(context: Context) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(currentlyOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        override fun onAvailable(network: Network) {
            _online.value = true
        }

        override fun onLost(network: Network) {
            _online.value = false
        }
    }

    fun start() {
        val m = manager ?: return
        if (registered) return
        _online.value = currentlyOnline()
        runCatching { m.registerDefaultNetworkCallback(callback) }
            .onSuccess { registered = true }
    }

    fun stop() {
        val m = manager ?: return
        if (!registered) return
        runCatching { m.unregisterNetworkCallback(callback) }
        registered = false
    }

    private fun currentlyOnline(): Boolean {
        val m = manager ?: return true
        val caps = m.getNetworkCapabilities(m.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
