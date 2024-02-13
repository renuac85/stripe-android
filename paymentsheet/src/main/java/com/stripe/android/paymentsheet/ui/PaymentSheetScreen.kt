@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.topContentPadding
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.H4Text

@Composable
internal fun PaymentSheetScreen(
    viewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val contentVisible by viewModel.contentVisible.collectAsState()
    val processing by viewModel.processing.collectAsState()

    val topBarState by viewModel.topBarState.collectAsState()

    DismissKeyboardOnProcessing(processing)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                state = topBarState,
                handleBackPressed = viewModel::handleBackPressed,
                toggleEditing = viewModel::toggleEditing,
            )
        },
        content = {
            AnimatedVisibility(visible = contentVisible) {
                PaymentSheetScreenContent(viewModel)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DismissKeyboardOnProcessing(processing: Boolean) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (processing) {
        LaunchedEffect(Unit) {
            keyboardController?.hide()
        }
    }
}

@Composable
internal fun PaymentSheetScreenContent(
    viewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val headerText by viewModel.headerText.collectAsState(null)
    val walletsState by viewModel.walletsState.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val mandateText by viewModel.mandateText.collectAsState()

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier) {
        headerText?.let { text ->
            H4Text(
                text = stringResource(text),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        walletsState?.let { state ->
            val bottomSpacing = WalletDividerSpacing - currentScreen.topContentPadding
            Wallet(
                state = state,
                onGooglePayPressed = state.onGooglePayPressed,
                onLinkPressed = state.onLinkPressed,
                modifier = Modifier.padding(bottom = bottomSpacing),
            )
        }

        Box(modifier = Modifier.animateContentSize()) {
            currentScreen.Content(
                viewModel = viewModel,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (mandateText?.showAbovePrimaryButton == true) {
            Mandate(
                mandateText = mandateText?.text,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        error?.let { error ->
            ErrorMessage(
                error = error,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = horizontalPadding),
            )
        }

        PaymentSheetPrimaryButton(viewModel)

        if (mandateText?.showAbovePrimaryButton == false) {
            Mandate(
                mandateText = mandateText?.text,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        PaymentSheetContentPadding()
    }
}

@Composable
internal fun Wallet(
    state: WalletsState,
    onGooglePayPressed: () -> Unit,
    onLinkPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val padding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier = modifier.padding(horizontal = padding)) {
        state.googlePay?.let { googlePay ->
            GooglePayButton(
                state = when (val primaryButtonState = googlePay.buttonState) {
                    null,
                    is PaymentSheetViewState.Reset -> PrimaryButton.State.Ready
                    is PaymentSheetViewState.StartProcessing -> PrimaryButton.State.StartProcessing
                    is PaymentSheetViewState.FinishProcessing ->
                        PrimaryButton.State.FinishProcessing(primaryButtonState.onComplete)
                },
                allowCreditCards = googlePay.allowCreditCards,
                buttonType = googlePay.buttonType,
                billingAddressParameters = googlePay.billingAddressParameters,
                isEnabled = state.buttonsEnabled,
                onPressed = onGooglePayPressed,
            )
        }

        state.link?.let {
            if (state.googlePay != null) {
                Spacer(modifier = Modifier.requiredHeight(8.dp))
            }

            LinkButton(
                email = it.email,
                enabled = state.buttonsEnabled,
                onClick = onLinkPressed,
            )
        }

        state.googlePay?.buttonState?.errorMessage?.let { error ->
            ErrorMessage(
                error = error.message,
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
            )
        }

        Spacer(modifier = Modifier.requiredHeight(WalletDividerSpacing))

        val text = stringResource(state.dividerTextResource)
        WalletsDivider(text)
    }
}

@Composable
private fun PaymentSheetPrimaryButton(viewModel: BaseSheetViewModel) {
    val primaryButtonState by viewModel.primaryButtonState.collectAsState()
    val primaryButtonUiState by viewModel.primaryButtonUiState.collectAsState()

    primaryButtonUiState?.let { uiState ->
        val state = primaryButtonState

        PrimaryButton(
            label = uiState.label,
            locked = uiState.lockVisible,
            enabled = uiState.enabled,
            processingState = state.processingState,
            onClick = uiState.onClick,
            onProcessingCompleted = state::onProcessingComplete
        )
    }
}

private val PrimaryButton.State?.processingState: PrimaryButtonProcessingState
    get() = when (this) {
        null,
        is PrimaryButton.State.Ready -> PrimaryButtonProcessingState.Idle
        is PrimaryButton.State.StartProcessing -> PrimaryButtonProcessingState.Processing
        is PrimaryButton.State.FinishProcessing -> PrimaryButtonProcessingState.Completed
    }

private fun PrimaryButton.State?.onProcessingComplete() {
    when (this) {
        is PrimaryButton.State.FinishProcessing -> onComplete()
        else -> Unit
    }
}

const val PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON"
