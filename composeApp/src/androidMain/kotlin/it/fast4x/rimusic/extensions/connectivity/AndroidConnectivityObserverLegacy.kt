package it.fast4x.rimusic.extensions.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

class AndroidConnectivityObserverLegacy(context: Context) {
    // Fixed: Added null safety check for ConnectivityManager
    private val connectivityManager: ConnectivityManager? = context.getSystemService<ConnectivityManager>()
    
    private val _networkStatus = Channel<Boolean>(Channel.CONFLATED)
    val networkStatus = _networkStatus.receiveAsFlow()

    private val internetNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _networkStatus.trySend(true)
            Timber.d("Network available: $network")
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _networkStatus.trySend(false)
            Timber.d("Network lost: $network")
        }

        override fun onUnavailable() {
            super.onUnavailable()
            _networkStatus.trySend(false)
            Timber.d("Network unavailable")
        }
        
        // Fixed: Added error handling for network callback failures
        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            Timber.w("Network losing connection: $network, maxMsToLive: $maxMsToLive")
        }
    }

    init {
        try {
            if (connectivityManager == null) {
                Timber.e("ConnectivityManager is null, network detection may not work properly")
                // Send default value to indicate no network
                _networkStatus.trySend(false)
                return
            }
            
            val request = NetworkRequest.Builder()
                // add Internet capability to request
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            // Fixed: Added try-catch for registration
            try {
                connectivityManager.registerNetworkCallback(request, internetNetworkCallback)
                Timber.d("Network callback registered successfully")
                
                // Fixed: Check initial network state to avoid race condition
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                val isInternetAvailable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                _networkStatus.trySend(isInternetAvailable)
                Timber.d("Initial network state: $isInternetAvailable")
                
            } catch (e: Exception) {
                Timber.e("Failed to register network callback: ${e.stackTraceToString()}")
            }
        } catch (e: Exception) {
            Timber.e("Error initializing AndroidConnectivityObserverLegacy: ${e.stackTraceToString()}")
        }
    }

    fun unregister() {
        try {
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(internetNetworkCallback)
                Timber.d("Network callback unregistered successfully")
            }
            // Fixed: Close the channel to prevent memory leaks
            _networkStatus.close()
        } catch (e: Exception) {
            Timber.e("Error unregistering network callback: ${e.stackTraceToString()}")
        }
    }
}
