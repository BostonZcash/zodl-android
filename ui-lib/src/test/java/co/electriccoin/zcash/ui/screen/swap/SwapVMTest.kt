package co.electriccoin.zcash.ui.screen.swap

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.common.model.SwapAssetTestFixture
import co.electriccoin.zcash.ui.common.model.SwapMode
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.repository.EnhancedABContact
import co.electriccoin.zcash.ui.common.repository.SwapAssetsData
import co.electriccoin.zcash.ui.common.repository.SwapRepository
import co.electriccoin.zcash.ui.common.usecase.CancelSwapUseCase
import co.electriccoin.zcash.ui.common.usecase.GetSelectedWalletAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.GetSwapAssetsUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanGenericAddressUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSelectABSwapRecipientUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSlippageUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSwapAssetPickerUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSwapInfoUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSwapQuoteIfAvailableUseCase
import co.electriccoin.zcash.ui.common.usecase.PreselectSwapAssetUseCase
import co.electriccoin.zcash.ui.common.usecase.RequestSwapQuoteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [SwapVM] now owns the selected asset and slippage in its merged state: preselect applies only when
 * nothing is selected, the slippage/asset pickers round-trip their results back into the state, the
 * mode toggles, and the quote request is pinned to the asset + slippage selected at click time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwapVMTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun preselectAppliedWhenNothingSelected() =
        runTest {
            val harness = harness(preselect = SwapAssetTestFixture.asset(tokenTicker = "btc"))

            harness.collectState(this)

            assertEquals("btc", harness.capturedState.swapAsset?.tokenTicker)
        }

    @Test
    fun slippagePickerResultAppliedToState() =
        runTest {
            val harness = harness(slippageResult = BigDecimal("7"))
            harness.collectState(this)

            harness.onSlippageClick(null)

            assertEquals(BigDecimal("7"), harness.capturedState.slippage)
            coVerify { harness.navigateToSlippage(BigDecimal("2"), null, any()) }
        }

    @Test
    fun assetPickerResultAppliedToState() =
        runTest {
            val asset = SwapAssetTestFixture.asset(tokenTicker = "eth", chainTicker = "eth")
            val harness = harness(assetResult = asset)
            harness.collectState(this)

            harness.onSwapAssetPickerClick()

            assertEquals("eth", harness.capturedState.swapAsset?.tokenTicker)
        }

    @Test
    fun changeButtonTogglesMode() =
        runTest {
            val harness = harness()
            harness.collectState(this)

            assertEquals(Mode.SWAP_INTO_ZEC, harness.capturedState.mode)

            harness.onChangeButtonClick()

            assertEquals(Mode.SWAP_FROM_ZEC, harness.capturedState.mode)
        }

    @Test
    fun requestQuotePinsSelectedAssetAndSlippage() =
        runTest {
            val asset = SwapAssetTestFixture.asset(tokenTicker = "btc")
            val harness = harness(preselect = asset)
            harness.collectState(this)

            harness.onRequestSwapQuoteClick(BigDecimal("1"), "refund-address")

            // Default mode is SWAP_INTO_ZEC -> flex input, pinned to the preselected asset + default slippage.
            coVerify(exactly = 1) {
                harness.requestSwapQuote.requestFlexInputIntoZec(
                    amount = BigDecimal("1"),
                    refundAddress = "refund-address",
                    selectedAsset = asset,
                    slippage = BigDecimal("2"),
                    canNavigateToSwapQuote = any()
                )
            }
        }

    @Test
    fun swappingFromZecRequestsExactInputQuote() =
        runTest {
            val asset = SwapAssetTestFixture.asset(tokenTicker = "btc")
            val harness = harness(preselect = asset)
            harness.collectState(this)

            harness.onChangeButtonClick() // SWAP_INTO_ZEC -> SWAP_FROM_ZEC
            harness.onRequestSwapQuoteClick(BigDecimal("1"), "destination-address")

            coVerify(exactly = 1) {
                harness.requestSwapQuote.requestExactInput(
                    amount = BigDecimal("1"),
                    address = "destination-address",
                    selectedAsset = asset,
                    slippage = BigDecimal("2"),
                    canNavigateToSwapQuote = any()
                )
            }
        }

    @Test
    fun slippageModeFollowsSwapDirection() =
        runTest {
            val harness = harness()
            harness.collectState(this)

            // SWAP_INTO_ZEC opens the slippage screen in flex-input mode.
            harness.onSlippageClick(null)
            coVerify { harness.navigateToSlippage(any(), any(), SwapMode.FLEX_INPUT) }

            // After flipping to SWAP_FROM_ZEC it opens in exact-input mode.
            harness.onChangeButtonClick()
            harness.onSlippageClick(null)
            coVerify { harness.navigateToSlippage(any(), any(), SwapMode.EXACT_INPUT) }
        }

    @Test
    fun pickingAbContactOnNewChainWithSingleAssetSelectsThatAsset() =
        runTest {
            val eth = SwapAssetTestFixture.asset(tokenTicker = "eth", chainTicker = "eth")
            val harness =
                harness(
                    preselect = SwapAssetTestFixture.asset(tokenTicker = "btc", chainTicker = "btc"),
                    assets = SwapAssetTestFixture.assetsData(data = listOf(eth)),
                    recipient = contactOnChain("eth")
                )
            harness.collectState(this)

            harness.onAddressBookClick()

            assertEquals("eth", harness.capturedState.swapAsset?.tokenTicker)
        }

    @Test
    fun pickingAbContactOnNewChainWithMultipleAssetsClearsSelection() =
        runTest {
            val harness =
                harness(
                    preselect = SwapAssetTestFixture.asset(tokenTicker = "btc", chainTicker = "btc"),
                    assets =
                        SwapAssetTestFixture.assetsData(
                            data =
                                listOf(
                                    SwapAssetTestFixture.asset(tokenTicker = "eth", chainTicker = "eth"),
                                    SwapAssetTestFixture.asset(tokenTicker = "usdc", chainTicker = "eth"),
                                )
                        ),
                    recipient = contactOnChain("eth")
                )
            harness.collectState(this)

            harness.onAddressBookClick()

            // The chain has more than one asset, so the VM cannot disambiguate and clears the selection.
            assertNull(harness.capturedState.swapAsset)
        }

    private fun contactOnChain(chainTicker: String): EnhancedABContact =
        mockk(relaxed = true) { every { blockchain } returns SwapAssetTestFixture.blockchain(chainTicker) }

    @Suppress("LongMethod")
    private fun harness(
        preselect: SwapAsset = SwapAssetTestFixture.asset(),
        slippageResult: BigDecimal? = null,
        assetResult: SwapAsset? = null,
        recipient: EnhancedABContact? = null,
        assets: SwapAssetsData = SwapAssetTestFixture.assetsData()
    ): Harness {
        val navigateToSlippage =
            mockk<NavigateToSlippageUseCase> {
                coEvery { this@mockk.invoke(any(), any(), any()) } returns slippageResult
            }
        val navigateToSwapAssetPicker =
            mockk<NavigateToSwapAssetPickerUseCase> {
                coEvery { this@mockk.invoke(any()) } returns assetResult
            }
        val navigateToSelectSwapRecipient =
            mockk<NavigateToSelectABSwapRecipientUseCase> { coEvery { this@mockk.invoke() } returns recipient }
        val preselectSwapAsset =
            mockk<PreselectSwapAssetUseCase> { coEvery { this@mockk.invoke() } returns preselect }
        val requestSwapQuote = mockk<RequestSwapQuoteUseCase>(relaxed = true)
        val getSwapAssets =
            mockk<GetSwapAssetsUseCase> { every { observe() } returns MutableStateFlow(assets) }
        val getSelectedWalletAccount =
            mockk<GetSelectedWalletAccountUseCase> {
                every { observe() } returns MutableStateFlow<WalletAccount?>(null)
            }
        val mapper = mockk<SwapVMMapper>()
        val swapRepository =
            mockk<SwapRepository>(relaxed = true) { every { this@mockk.assets } returns MutableStateFlow(assets) }

        val harness =
            Harness(
                navigateToSlippage = navigateToSlippage,
                navigateToSwapAssetPicker = navigateToSwapAssetPicker,
                requestSwapQuote = requestSwapQuote
            )

        // The VM exposes its callbacks only through SwapVMMapper.createState; capture them by their
        // positional argument index (matching the createState parameter order) so the tests can drive
        // the VM the same way the View would.
        every {
            mapper.createState(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            harness.capturedState = firstArg()
            harness.onSwapAssetPickerClick = arg(3)
            harness.onSlippageClick = arg(5)
            harness.onRequestSwapQuoteClick = arg(6)
            harness.onAddressBookClick = arg(11)
            harness.onChangeButtonClick = arg(14)
            mockk()
        }

        harness.vm =
            SwapVM(
                getSwapAssetsUseCase = getSwapAssets,
                getSelectedWalletAccount = getSelectedWalletAccount,
                preselectSwapAsset = preselectSwapAsset,
                swapRepository = swapRepository,
                navigateToSwapInfo = mockk<NavigateToSwapInfoUseCase>(relaxed = true),
                cancelSwap = mockk<CancelSwapUseCase>(relaxed = true),
                navigationRouter = mockk<NavigationRouter>(relaxed = true),
                requestSwapQuote = requestSwapQuote,
                navigateToSwapQuoteIfAvailable = mockk<NavigateToSwapQuoteIfAvailableUseCase>(relaxed = true),
                swapVMMapper = mapper,
                navigateToScanAddress = mockk<NavigateToScanGenericAddressUseCase>(relaxed = true),
                navigateToSelectSwapRecipient = navigateToSelectSwapRecipient,
                navigateToSlippage = navigateToSlippage,
                navigateToSwapAssetPicker = navigateToSwapAssetPicker,
            )
        return harness
    }

    private class Harness(
        val navigateToSlippage: NavigateToSlippageUseCase,
        val navigateToSwapAssetPicker: NavigateToSwapAssetPickerUseCase,
        val requestSwapQuote: RequestSwapQuoteUseCase,
    ) {
        lateinit var vm: SwapVM
        lateinit var capturedState: InternalState
        var onSlippageClick: (BigDecimal?) -> Unit = {}
        var onSwapAssetPickerClick: () -> Unit = {}
        var onChangeButtonClick: () -> Unit = {}
        var onAddressBookClick: () -> Unit = {}
        var onRequestSwapQuoteClick: (BigDecimal, String) -> Unit = { _, _ -> }

        fun collectState(scope: kotlinx.coroutines.test.TestScope) {
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { vm.state.collect {} }
        }
    }
}
