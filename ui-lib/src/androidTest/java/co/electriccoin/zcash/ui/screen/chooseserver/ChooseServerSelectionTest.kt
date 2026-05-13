package co.electriccoin.zcash.ui.screen.chooseserver

import android.app.Application
import androidx.navigation.NavBackStackEntry
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PersistableWallet
import cash.z.ecc.android.sdk.model.SeedPhrase
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.ServerValidation
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.zcash.ui.BaseNavigationCommand
import co.electriccoin.zcash.ui.NavigationCommand
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.ConnectionMode
import co.electriccoin.zcash.ui.common.model.FastestServersState
import co.electriccoin.zcash.ui.common.model.ServerSelection
import co.electriccoin.zcash.ui.common.model.SynchronizerError
import co.electriccoin.zcash.ui.common.model.WalletRestoringState
import co.electriccoin.zcash.ui.common.provider.LightWalletEndpointProvider
import co.electriccoin.zcash.ui.common.provider.PersistableWalletProvider
import co.electriccoin.zcash.ui.common.provider.ServerSelectionProvider
import co.electriccoin.zcash.ui.common.provider.SynchronizerProvider
import co.electriccoin.zcash.ui.common.repository.WalletRepository
import co.electriccoin.zcash.ui.common.usecase.GetSelectedEndpointUseCase
import co.electriccoin.zcash.ui.common.usecase.GetServerSelectionUseCase
import co.electriccoin.zcash.ui.common.usecase.ObserveFastestServersUseCase
import co.electriccoin.zcash.ui.common.usecase.PersistServerSelectionUseCase
import co.electriccoin.zcash.ui.common.usecase.RefreshFastestServersUseCase
import co.electriccoin.zcash.ui.common.usecase.ValidateEndpointUseCase
import co.electriccoin.zcash.ui.common.viewmodel.SecretState
import co.electriccoin.zcash.ui.fixture.MockSynchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChooseServerSelectionTest {
    @Test
    @SmallTest
    fun automaticToManualSavePinsCurrentEndpointWithoutEndpointTap() =
        runTest {
            val application = ApplicationProvider.getApplicationContext<Application>()
            val lightWalletEndpointProvider = LightWalletEndpointProvider(application)
            val currentEndpoint = lightWalletEndpointProvider.getEndpoints().last()
            val persistableWalletProvider = FakePersistableWalletProvider(currentEndpoint)
            val serverSelectionProvider = FakeServerSelectionProvider(ServerSelection.automatic())
            val walletRepository = FakeWalletRepository(currentEndpoint)
            val getSelectedEndpoint = GetSelectedEndpointUseCase(persistableWalletProvider)
            val viewModel =
                ChooseServerVM(
                    application = application,
                    observeFastestServers = ObserveFastestServersUseCase(walletRepository),
                    getSelectedEndpoint = getSelectedEndpoint,
                    getServerSelection = GetServerSelectionUseCase(serverSelectionProvider),
                    lightWalletEndpointProvider = lightWalletEndpointProvider,
                    refreshFastestServersUseCase = RefreshFastestServersUseCase(walletRepository),
                    persistServerSelection =
                        PersistServerSelectionUseCase(
                            application = application,
                            walletRepository = walletRepository,
                            synchronizerProvider = FakeSynchronizerProvider(),
                            lightWalletEndpointProvider = lightWalletEndpointProvider,
                            serverSelectionProvider = serverSelectionProvider,
                            getSelectedEndpoint = getSelectedEndpoint
                        ),
                    validateEndpoint = ValidateEndpointUseCase(),
                    navigationRouter = FakeNavigationRouter
                )

            val initialState =
                withTimeout(STATE_TIMEOUT_MILLIS) {
                    viewModel.state.filterNotNull().first()
                }
            assertFalse(initialState.connectionMode.isManualSelected)

            initialState.connectionMode.manual.onClick()

            val manualState =
                withTimeout(STATE_TIMEOUT_MILLIS) {
                    viewModel.state
                        .filterNotNull()
                        .first { it.connectionMode.isManualSelected && it.saveButton.isEnabled }
                }
            assertTrue(
                manualState.fastest.servers.any {
                    it.radioButtonState.isChecked && it.badge != null
                }
            )

            manualState.saveButton.onClick()

            val persistedSelection =
                withTimeout(STATE_TIMEOUT_MILLIS) {
                    serverSelectionProvider.serverSelection
                        .filterNotNull()
                        .first { it.mode == ConnectionMode.MANUAL }
                }
            assertEquals(ConnectionMode.MANUAL, persistedSelection.mode)
            assertEquals(currentEndpoint, persistedSelection.endpoint)
            assertFalse(persistedSelection.isCustom)
            assertEquals(currentEndpoint, walletRepository.updatedEndpoint)
        }

    @Test
    @SmallTest
    fun currentKnownEndpointPinsAsManualWhenNoEndpointWasTapped() {
        val endpoint = knownEndpoints[1]

        val selection =
            endpoint.toCurrentManualServerSelection(
                persistedSelection = ServerSelection.automatic(),
                availableServers = knownEndpoints
            )

        assertEquals(ConnectionMode.MANUAL, selection.mode)
        assertEquals(endpoint, selection.endpoint)
        assertFalse(selection.isCustom)
    }

    @Test
    @SmallTest
    fun currentUnknownEndpointPinsAsCustomWhenNoEndpointWasTapped() {
        val endpoint = LightWalletEndpoint(host = "custom.example.com", port = 9067, isSecure = true)

        val selection =
            endpoint.toCurrentManualServerSelection(
                persistedSelection = ServerSelection.automatic(),
                availableServers = knownEndpoints
            )

        assertEquals(ConnectionMode.MANUAL, selection.mode)
        assertEquals(endpoint, selection.endpoint)
        assertTrue(selection.isCustom)
    }

    companion object {
        private const val STATE_TIMEOUT_MILLIS = 2_000L

        private val knownEndpoints =
            listOf(
                LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true),
                LightWalletEndpoint(host = "eu.zec.rocks", port = 443, isSecure = true)
            )
    }
}

private class FakePersistableWalletProvider(
    endpoint: LightWalletEndpoint
) : PersistableWalletProvider {
    private val mutablePersistableWallet =
        MutableStateFlow(
            PersistableWallet(
                network = ZcashNetwork.Mainnet,
                endpoint = endpoint,
                birthday = null,
                seedPhrase = SeedPhrase(List(SeedPhrase.SEED_PHRASE_SIZE) { "abandon" }),
                walletInitMode = WalletInitMode.ExistingWallet
            )
        )

    override val persistableWallet: Flow<PersistableWallet?> = mutablePersistableWallet

    override suspend fun store(persistableWallet: PersistableWallet) {
        mutablePersistableWallet.value = persistableWallet
    }

    override suspend fun getPersistableWallet() = mutablePersistableWallet.value

    override suspend fun requirePersistableWallet() = checkNotNull(mutablePersistableWallet.value)
}

private class FakeServerSelectionProvider(
    initialSelection: ServerSelection?
) : ServerSelectionProvider {
    private val mutableServerSelection = MutableStateFlow(initialSelection)

    override val serverSelection: Flow<ServerSelection?> = mutableServerSelection

    override suspend fun store(serverSelection: ServerSelection) {
        mutableServerSelection.value = serverSelection
    }

    override suspend fun getServerSelection() = mutableServerSelection.value
}

private class FakeWalletRepository(
    fastestEndpoint: LightWalletEndpoint
) : WalletRepository {
    override val secretState = MutableStateFlow(SecretState.NONE)
    override val fastestEndpoints = MutableStateFlow(FastestServersState(listOf(fastestEndpoint), false))
    override val walletRestoringState = MutableStateFlow(WalletRestoringState.NONE)

    var updatedEndpoint: LightWalletEndpoint? = null
        private set

    override fun createNewWallet() = Unit

    override fun restoreWallet(
        network: ZcashNetwork,
        seedPhrase: SeedPhrase,
        birthday: BlockHeight
    ) = Unit

    override suspend fun updateWalletEndpoint(endpoint: LightWalletEndpoint) {
        updatedEndpoint = endpoint
    }

    override fun refreshFastestServers() = Unit
}

private class FakeSynchronizerProvider : SynchronizerProvider {
    override val error = MutableStateFlow<SynchronizerError?>(null)
    override val synchronizer = MutableStateFlow<Synchronizer?>(MockSynchronizer(ServerValidation.Valid))

    override suspend fun getSynchronizer(): Synchronizer = checkNotNull(synchronizer.value)

    override fun resetSynchronizer() = Unit
}

private object FakeNavigationRouter : NavigationRouter {
    override fun forward(vararg routes: Any) = Unit

    override fun replace(vararg routes: Any) = Unit

    override fun replaceAll(vararg routes: Any) = Unit

    override fun back() = Unit

    override fun backTo(route: KClass<*>) = Unit

    override fun custom(block: (NavBackStackEntry?) -> NavigationCommand?) = Unit

    override fun backToRoot() = Unit

    override fun observePipeline(): Flow<BaseNavigationCommand> = emptyFlow()
}
