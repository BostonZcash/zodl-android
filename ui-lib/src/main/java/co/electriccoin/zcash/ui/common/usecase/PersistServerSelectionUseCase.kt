package co.electriccoin.zcash.ui.common.usecase

import android.app.Application
import cash.z.ecc.android.sdk.type.ServerValidation
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.zcash.ui.common.model.ConnectionMode
import co.electriccoin.zcash.ui.common.model.ServerSelection
import co.electriccoin.zcash.ui.common.provider.LightWalletEndpointProvider
import co.electriccoin.zcash.ui.common.provider.ServerSelectionProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.WalletRepository
import kotlinx.coroutines.CancellationException

class PersistServerSelectionUseCase(
    private val application: Application,
    private val walletRepository: WalletRepository,
    private val synchronizerProvider: SynchronizerProvider,
    private val lightWalletEndpointProvider: LightWalletEndpointProvider,
    private val serverSelectionProvider: ServerSelectionProvider,
    private val getSelectedEndpoint: GetSelectedEndpointUseCase,
) {
    @Throws(PersistEndpointException::class)
    suspend operator fun invoke(selection: ServerSelection) {
        when (selection.mode) {
            ConnectionMode.AUTOMATIC -> persistAutomatic()
            ConnectionMode.MANUAL -> persistManual(checkNotNull(selection.endpoint))
        }
    }

    private suspend fun persistAutomatic() {
        val endpoint = getAutomaticEndpoint()
        persistSelectionAndEndpoint(ServerSelection.automatic(), endpoint)
    }

    @Throws(PersistEndpointException::class)
    private suspend fun persistManual(endpoint: LightWalletEndpoint) {
        when (val result = validateServerEndpoint(endpoint)) {
            ServerValidation.Valid -> {
                persistSelectionAndEndpoint(ServerSelection.manual(endpoint), endpoint)
            }

            is ServerValidation.InValid -> {
                throw PersistEndpointException(result.reason.message)
            }

            ServerValidation.Running -> {
                throw PersistEndpointException(null)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persistSelectionAndEndpoint(
        selection: ServerSelection,
        endpoint: LightWalletEndpoint
    ) {
        val previousSelection = serverSelectionProvider.getServerSelection()
        try {
            serverSelectionProvider.store(selection)
            walletRepository.updateWalletEndpoint(endpoint)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            previousSelection?.let {
                runCatching { serverSelectionProvider.store(it) }
            }
            throw PersistEndpointException(e.message)
        }
    }

    private suspend fun getAutomaticEndpoint(): LightWalletEndpoint {
        val fastestEndpoint =
            walletRepository.fastestEndpoints.value.let { fastestServers ->
                if (fastestServers.isLoading) {
                    null
                } else {
                    fastestServers.servers?.firstOrNull()
                }
            }

        return fastestEndpoint
            ?: getSelectedEndpoint()?.takeIf { lightWalletEndpointProvider.getEndpoints().contains(it) }
            ?: lightWalletEndpointProvider.getDefaultEndpoint()
    }

    private suspend fun validateServerEndpoint(endpoint: LightWalletEndpoint) =
        synchronizerProvider
            .getSynchronizer()
            .validateServerEndpoint(application, endpoint)
}
