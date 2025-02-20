package com.stripe.android.ui.core.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.StaticCardAccountRangeSource
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.events.CardNumberCompletedEventReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.payments.model.R as PaymentModelR

@RunWith(RobolectricTestRunner::class)
internal class CardNumberControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val cardNumberController = DefaultCardNumberController(
        CardNumberConfig(),
        FakeCardAccountRangeRepository(),
        testDispatcher,
        testDispatcher,
        initialValue = null
    )

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When invalid card number verify visible error`() = runTest {
        cardNumberController.error.distinctUntilChanged().test {
            assertThat(awaitItem()).isNull()

            cardNumberController.onValueChange("012")
            idleLooper()

            skipItems(2)

            assertThat(awaitItem()?.errorMessage)
                .isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `Verify get the form field value correctly`() = runTest {
        cardNumberController.formFieldValue.distinctUntilChanged().test {
            cardNumberController.onValueChange("4242")
            idleLooper()

            assertThat(awaitItem().isComplete)
                .isFalse()
            assertThat(awaitItem().value)
                .isEqualTo("4242")

            cardNumberController.onValueChange("4242424242424242")
            idleLooper()

            assertThat(awaitItem().isComplete)
                .isTrue()
            assertThat(awaitItem().value)
                .isEqualTo("4242424242424242")
        }
    }

    @Test
    fun `Verify error is visible based on the focus`() = runTest {
        // incomplete
        cardNumberController.visibleError.distinctUntilChanged().test {
            cardNumberController.onFocusChange(true)
            cardNumberController.onValueChange("4242")
            idleLooper()

            assertThat(awaitItem()).isFalse()

            cardNumberController.onFocusChange(false)
            idleLooper()

            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Entering VISA BIN does not call accountRangeRepository`() {
        var repositoryCalls = 0
        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            object : CardAccountRangeRepository {
                private val staticCardAccountRangeSource = StaticCardAccountRangeSource()
                override suspend fun getAccountRange(
                    cardNumber: CardNumber.Unvalidated
                ): AccountRange? {
                    repositoryCalls++
                    return cardNumber.bin?.let {
                        staticCardAccountRangeSource.getAccountRange(cardNumber)
                    }
                }

                override suspend fun getAccountRanges(
                    cardNumber: CardNumber.Unvalidated
                ): List<AccountRange>? {
                    repositoryCalls++
                    return cardNumber.bin?.let {
                        staticCardAccountRangeSource.getAccountRanges(cardNumber)
                    }
                }

                override val loading: Flow<Boolean> = flowOf(false)
            },
            testDispatcher,
            testDispatcher,
            initialValue = null
        )
        cardNumberController.onValueChange("42424242424242424242")
        idleLooper()
        assertThat(repositoryCalls).isEqualTo(0)
    }

    @Test
    fun `Entering valid 19 digit UnionPay BIN returns accountRange of 19`() {
        cardNumberController.onValueChange("6216828050000000000")
        idleLooper()
        assertThat(cardNumberController.accountRangeService.accountRange!!.panLength).isEqualTo(19)
    }

    @Test
    fun `Entering valid 16 digit UnionPay BIN returns accountRange of 16`() {
        cardNumberController.onValueChange("6282000000000000")
        idleLooper()
        assertThat(cardNumberController.accountRangeService.accountRange!!.panLength).isEqualTo(16)
    }

    @Test
    fun `trailingIcon should have multi trailing icons when field is empty`() = runTest {
        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("")
            idleLooper()
            assertThat(awaitItem() as TextFieldIcon.MultiTrailing)
                .isEqualTo(
                    TextFieldIcon.MultiTrailing(
                        staticIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.MasterCard.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.AmericanExpress.icon, isTintable = false)
                        ),
                        animatedIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.Discover.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.JCB.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.DinersClub.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.UnionPay.icon, isTintable = false)
                        )
                    )
                )
        }
    }

    @Test
    fun `trailingIcon should have trailing icon when field matches bin`() = runTest {
        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4")
            idleLooper()
            cardNumberController.onValueChange("")
            idleLooper()

            skipItems(1)
            assertThat(awaitItem()).isInstanceOf(TextFieldIcon.MultiTrailing::class.java)
            assertThat(awaitItem()).isEqualTo(TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false))
            assertThat(awaitItem()).isInstanceOf(TextFieldIcon.MultiTrailing::class.java)
        }
    }

    @Test
    fun `trailingIcon should be dropdown if card brand choice eligible`() = runTest {
        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = null,
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            idleLooper()
            skipItems(2)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = resolvableString(R.string.stripe_card_brand_choice_selection_header),
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.Unknown.code,
                            label = resolvableString(R.string.stripe_card_brand_choice_no_selection),
                            icon = CardBrand.Unknown.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = resolvableString("Cartes Bancaires"),
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = resolvableString("Visa"),
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on cbc eligible with preferred brands, should use the preferred brand if none are initially selected`() = runTest {
        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = null,
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            idleLooper()
            skipItems(3)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = resolvableString(R.string.stripe_card_brand_choice_selection_header),
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = resolvableString("Cartes Bancaires"),
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = resolvableString("Cartes Bancaires"),
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = resolvableString("Visa"),
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on dropdown item click, card brand should have been changed`() = runTest {
        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = null,
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            cardNumberController.onDropdownItemClicked(
                TextFieldIcon.Dropdown.Item(
                    id = CardBrand.CartesBancaires.code,
                    label = resolvableString("Cartes Bancaires"),
                    icon = PaymentModelR.drawable.stripe_ic_cartes_bancaires
                )
            )
            idleLooper()
            skipItems(3)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = resolvableString(R.string.stripe_card_brand_choice_selection_header),
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = resolvableString("Cartes Bancaires"),
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = resolvableString("Cartes Bancaires"),
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = resolvableString("Visa"),
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on number updated after update to number with no brands, user choice should be re-used if possible`() = runTest {
        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = null,
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            cardNumberController.onDropdownItemClicked(
                TextFieldIcon.Dropdown.Item(
                    id = CardBrand.CartesBancaires.code,
                    label = resolvableString("Cartes Bancaires"),
                    icon = PaymentModelR.drawable.stripe_ic_cartes_bancaires
                )
            )
            cardNumberController.onValueChange("400000250000100")
            skipItems(3)
            cardNumberController.onValueChange("4000002500001001")
            idleLooper()
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = resolvableString(R.string.stripe_card_brand_choice_selection_header),
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = resolvableString("Cartes Bancaires"),
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = resolvableString("Cartes Bancaires"),
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = resolvableString("Visa"),
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on number completed, should report event`() = runTest {
        val eventReporter: CardNumberCompletedEventReporter = mock()

        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = null,
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                cardNumberController.ComposeUI(
                    enabled = true,
                    field = SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig()
                        ),
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Next,
                    previousFocusDirection = FocusDirection.Next
                )
            }
        }

        cardNumberController.onValueChange("4242424242424242")

        verify(eventReporter, atMostOnce()).onCardNumberCompleted()
    }

    @Test
    fun `on initial number completed, should not report event`() = runTest {
        val eventReporter: CardNumberCompletedEventReporter = mock()

        val cardNumberController = DefaultCardNumberController(
            CardNumberConfig(),
            FakeCardAccountRangeRepository(),
            testDispatcher,
            testDispatcher,
            initialValue = "4242424242424242",
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                cardNumberController.ComposeUI(
                    enabled = true,
                    field = SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig()
                        ),
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Next,
                    previousFocusDirection = FocusDirection.Next
                )
            }
        }

        cardNumberController.onValueChange("4242424242424242")

        verifyNoInteractions(eventReporter)
    }

    private class FakeCardAccountRangeRepository : CardAccountRangeRepository {
        private val staticCardAccountRangeSource = StaticCardAccountRangeSource()
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? {
            return cardNumber.bin?.let {
                staticCardAccountRangeSource.getAccountRange(cardNumber)
            }
        }

        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): List<AccountRange>? {
            return cardNumber.bin?.let {
                staticCardAccountRangeSource.getAccountRanges(cardNumber)
            }
        }

        override val loading: Flow<Boolean> = flowOf(false)
    }

    private companion object {
        const val TEST_TAG = "CardNumberElement"
    }
}
