package id.gustyxganteng.agent.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _networkStatus = MutableStateFlow<NetworkStatus>(
        if (checkInitialConnectivity()) NetworkStatus.Available else NetworkStatus.Lost
    )
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _networkStatus.value = NetworkStatus.Available
        }

        override fun onLost(network: Network) {
            val hasOtherActiveNetwork = checkInitialConnectivity()
            _isOnline.value = hasOtherActiveNetwork
            _networkStatus.value = if (hasOtherActiveNetwork) NetworkStatus.Available else NetworkStatus.Lost
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            _networkStatus.value = if (hasInternet) NetworkStatus.Available else NetworkStatus.Lost
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Fallback for restricted environments
            _isOnline.value = true
            _networkStatus.value = NetworkStatus.Available
        }
    }

    fun checkConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isOnline.value = online
        _networkStatus.value = if (online) NetworkStatus.Available else NetworkStatus.Lost
        return online
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
