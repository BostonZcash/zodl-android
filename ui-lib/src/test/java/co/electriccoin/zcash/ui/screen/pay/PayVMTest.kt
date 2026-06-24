package co.electriccoin.zcash.ui.screen.pay

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.common.model.SwapAssetTestFixture
import co.electriccoin.zcash.ui.common.model.SwapMode
import co.electriccoin.zcash.ui.common.model.WalletAccount
import co.electriccoin.zcash.ui.common.repository.SwapAssetsData
import co.electriccoin.zcash.ui.common.repository.SwapRepository
import co.electriccoin.zcash.ui.common.usecase.CancelSwapUseCase
import co.electriccoin.zcash.ui.common.usecase.GetSelectedWalletAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.GetSwapAssetsUseCase
import co.electriccoin.zcash.ui.common.usecase.IsABContactHintVisibleUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToScanGenericAddressUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSelectABSwapRecipientUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSlippageUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSwapAssetPickerUseCase
import co.electriccoin.zcash.ui.common.usecase.NavigateToSwapQuoteIfAvailableUseCase
import co.electriccoin.zcash.ui.common.usecase.PreselectSwapAssetUseCase
import co.electriccoin.zcash.ui.common.usecase.RequestSwapQuoteUseCase
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

/**
 * [PayVM] owns the selected asset + slippage. Selecting an asset atomically recomputes the fiat
 * amount in a single state update (no observer feedback loop), the slippage picker round-trips, and
 * the exact-output quote is pinned to the asset + slippage at click time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayVMTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun assetPickerResultRecomputesFiatExactlyOnce() =
        runTest {
            val asset = SwapAssetTestFixture.asset(tokenTicker = "eth", chainTicker = "eth")
            val sentinelFiat = NumberTextFieldInnerState.fromAmount(BigDecimal("123"))
            val harness = harness(assetResult = asset, fiatRecompute = sentinelFiat)
            harness.collectState(this)

            harness.onSwapAssetPickerClick()

            assertEquals("eth", harness.capturedState.asset?.tokenTicker)
            assertEquals(sentinelFiat, harness.capturedState.fiatAmount)
            // Recompute happens once for the single asset change — proves there is no feedback loop.
            verify(exactly = 1) { harness.mapper.createFiatAmountInnerState(any(), any(), asset) }
        }

    @Test
    fun slippagePickerResultAppliedToState() =
        runTest {
            val harness = harness(slippageResult = BigDecimal("7"))
            harness.collectState(this)

            harness.onSlippageClick(null)

            assertEquals(BigDecimal("7"), harness.capturedState.slippage)
            coVerify { harness.navigateToSlippage(BigDecimal("2"), null, SwapMode.EXACT_OUTPUT) }
        }

    @Test
    fun requestQuotePinsSelectedAssetAndSlippage() =
        runTest {
            val asset = SwapAssetTestFixture.asset(tokenTicker = "btc")
            val harness = harness(preselect = asset)
            harness.collectState(this)

            harness.onRequestSwapQuoteClick(BigDecimal("1"), "destination-address")

            coVerify(exactly = 1) {
                harness.requestSwapQuote.requestExactOutput(
                    amount = BigDecimal("1"),
                    address = "destination-address",
                    selectedAsset = asset,
                    slippage = BigDecimal("2"),
                    canNavigateToSwapQuote = any()
                )
            }
        }

    @Suppress("LongMethod")
    private fun harness(
        preselect: SwapAsset = SwapAssetTestFixture.asset(),
        slippageResult: BigDecimal? = null,
        assetResult: SwapAsset? = null,
        fiatRecompute: NumberTextFieldInnerState = NumberTextFieldInnerState(),
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
        val preselectSwapAsset =
            mockk<PreselectSwapAssetUseCase> { coEvery { this@mockk.invoke() } returns preselect }
        val requestSwapQuote = mockk<RequestSwapQuoteUseCase>(relaxed = true)
        val getSwapAssets =
            mockk<GetSwapAssetsUseCase> { every { observe() } returns MutableStateFlow(assets) }
        val getSelectedWalletAccount =
            mockk<GetSelectedWalletAccountUseCase> {
                every { observe() } returns MutableStateFlow<WalletAccount?>(null)
            }
        val isABContactHintVisible =
            mockk<IsABContactHintVisibleUseCase> { every { observe(any(), any()) } returns flowOf(false) }
        val mapper =
            mockk<ExactOutputVMMapper> {
                every { createFiatAmountInnerState(any(), any(), any()) } returns fiatRecompute
            }

        val harness = Harness(navigateToSlippage, requestSwapQuote, mapper)

        // The VM exposes its callbacks only through ExactOutputVMMapper.createState; capture them by
        // their positional argument index (matching the createState parameter order).
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
                any()
            )
        } answers {
            harness.capturedState = firstArg()
            harness.onSwapAssetPickerClick = arg(3)
            harness.onSlippageClick = arg(4)
            harness.onRequestSwapQuoteClick = arg(5)
            mockk()
        }

        harness.vm =
            PayVM(
                getSwapAssetsUseCase = getSwapAssets,
                getSelectedWalletAccount = getSelectedWalletAccount,
                swapRepository = mockk<SwapRepository>(relaxed = true),
                cancelSwap = mockk<CancelSwapUseCase>(relaxed = true),
                navigationRouter = mockk<NavigationRouter>(relaxed = true),
                requestSwapQuote = requestSwapQuote,
                navigateToSwapQuoteIfAvailable = mockk<NavigateToSwapQuoteIfAvailableUseCase>(relaxed = true),
                exactOutputVMMapper = mapper,
                navigateToScanAddress = mockk<NavigateToScanGenericAddressUseCase>(relaxed = true),
                navigateToSelectSwapRecipient = mockk<NavigateToSelectABSwapRecipientUseCase>(relaxed = true),
                isABContactHintVisible = isABContactHintVisible,
                preselectSwapAsset = preselectSwapAsset,
                navigateToSlippage = navigateToSlippage,
                navigateToSwapAssetPicker = navigateToSwapAssetPicker,
            )
        return harness
    }

    private class Harness(
        val navigateToSlippage: NavigateToSlippageUseCase,
        val requestSwapQuote: RequestSwapQuoteUseCase,
        val mapper: ExactOutputVMMapper,
    ) {
        lateinit var vm: PayVM
        lateinit var capturedState: InternalState
        var onSlippageClick: (BigDecimal?) -> Unit = {}
        var onSwapAssetPickerClick: () -> Unit = {}
        var onRequestSwapQuoteClick: (BigDecimal, String) -> Unit = { _, _ -> }

        fun collectState(scope: kotlinx.coroutines.test.TestScope) {
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { vm.state.collect {} }
        }
    }
}
