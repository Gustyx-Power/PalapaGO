package id.gustyxganteng.agent.core.network

sealed interface NetworkStatus {
    data object Available : NetworkStatus
    data object Lost : NetworkStatus
    data object Connecting : NetworkStatus
}
