package com.stripe.android.paymentsheet.state

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState

internal enum class WalletsProcessingState {
    Active,
    Finished;

    companion object {

        fun create(
            processing: Boolean,
            buyButtonState: PaymentSheetViewState?,
            selection: PaymentSelection?,
        ): WalletsProcessingState? {
            val isWallet = when (selection) {
                is PaymentSelection.GooglePay,
                is PaymentSelection.Link -> {
                    true
                }
                is PaymentSelection.Saved -> {
                    selection.walletType != null
                }
                else -> {
                    false
                }
            }

            val isWalletProcessing = processing && isWallet

            return if (isWalletProcessing && buyButtonState is PaymentSheetViewState.FinishProcessing) {
                Finished
            } else if (isWalletProcessing) {
                Active
            } else {
                null
            }
        }
    }
}
