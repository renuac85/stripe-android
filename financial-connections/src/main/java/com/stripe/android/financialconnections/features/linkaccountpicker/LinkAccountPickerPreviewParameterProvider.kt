@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.ReturningNetworkingUserAccountPicker

internal class LinkAccountPickerPreviewParameterProvider :
    PreviewParameterProvider<LinkAccountPickerState> {
    override val values = sequenceOf(
        canonical(),
        accountSelected()
    )

    override val count: Int
        get() = super.count

    private val returningNetworkingUserAccountPicker = ReturningNetworkingUserAccountPicker(
        title = "title",
        defaultCta = "cta",
        accounts = emptyList(),
        addNewAccount = AddNewAccount(
            body = "New bank account",
            icon = null,
            nextPane = Pane.SUCCESS
        )
    )

    private fun canonical() = LinkAccountPickerState(
        payload = Success(
            LinkAccountPickerState.Payload(
                title = returningNetworkingUserAccountPicker.title,
                accounts = partnerAccountList(),
                addNewAccount = returningNetworkingUserAccountPicker.addNewAccount!!,
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                repairAuthorizationEnabled = false,
                stepUpAuthenticationRequired = false,
                defaultCta = returningNetworkingUserAccountPicker.defaultCta,
            )
        ),
    )

    private fun accountSelected() = LinkAccountPickerState(
        selectedAccountId = partnerAccountList().first().first.id,
        payload = Success(
            LinkAccountPickerState.Payload(
                title = returningNetworkingUserAccountPicker.title,
                accounts = partnerAccountList(),
                addNewAccount = returningNetworkingUserAccountPicker.addNewAccount!!,
                accessibleData = accessibleCallout(),
                businessName = "Random business",
                consumerSessionClientSecret = "secret",
                repairAuthorizationEnabled = false,
                stepUpAuthenticationRequired = false,
                defaultCta = returningNetworkingUserAccountPicker.defaultCta,
            )
        ),
    )

    private fun partnerAccountList() = listOf(
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id1",
            name = "With balance",
            balanceAmount = 1000,
            status = FinancialConnectionsAccount.Status.ACTIVE,
            displayableAccountNumbers = "1234",
            currency = "USD",
            _allowSelection = true,
            allowSelectionMessage = "",
            subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
            supportedPaymentMethodTypes = emptyList()
        ) to NetworkedAccount(
            allowSelection = true,
            id = "id1",
            caption = null
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id2",
            name = "With balance disabled",
            balanceAmount = 1000,
            _allowSelection = false,
            allowSelectionMessage = "Disconnected",
            subcategory = FinancialConnectionsAccount.Subcategory.SAVINGS,
            supportedPaymentMethodTypes = emptyList()
        ) to NetworkedAccount(
            allowSelection = false,
            id = "id2",
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id3",
            name = "No balance",
            displayableAccountNumbers = "1234",
            subcategory = FinancialConnectionsAccount.Subcategory.CREDIT_CARD,
            _allowSelection = true,
            allowSelectionMessage = "",
            supportedPaymentMethodTypes = emptyList()
        ) to NetworkedAccount(
            allowSelection = true,
            id = "id3",
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id4",
            name = "No balance disabled",
            displayableAccountNumbers = "1234",
            subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
            _allowSelection = false,
            allowSelectionMessage = "Disconnected",
            supportedPaymentMethodTypes = emptyList()
        ) to NetworkedAccount(
            allowSelection = false,
            id = "id4",
        ),
        PartnerAccount(
            authorization = "Authorization",
            category = FinancialConnectionsAccount.Category.CASH,
            id = "id5",
            name = "Very long institution that is already linked",
            displayableAccountNumbers = "1234",
            linkedAccountId = "linkedAccountId",
            _allowSelection = true,
            subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
            supportedPaymentMethodTypes = emptyList()
        ) to NetworkedAccount(
            allowSelection = true,
            id = "id5",
        ),
    )

    private fun accessibleCallout() = AccessibleDataCalloutModel(
        businessName = "My business",
        permissions = listOf(
            FinancialConnectionsAccount.Permissions.PAYMENT_METHOD,
            FinancialConnectionsAccount.Permissions.BALANCES,
            FinancialConnectionsAccount.Permissions.OWNERSHIP,
            FinancialConnectionsAccount.Permissions.TRANSACTIONS
        ),
        isStripeDirect = true,
        isNetworking = true,
        dataPolicyUrl = ""
    )
}
