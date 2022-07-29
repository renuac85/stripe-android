package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository

internal suspend fun initializeRepositoryAndGetStripeIntent(
    resourceRepository: ResourceRepository,
    stripeIntentRepository: StripeIntentRepository,
    clientSecret: ClientSecret,
    eventReporter: EventReporter,
    merchant_support_async: Boolean
): StripeIntent {
    val value = stripeIntentRepository.get(clientSecret, merchant_support_async)
    resourceRepository.getLpmRepository().update(
        value.intent.paymentMethodTypes,
        value.formUI
    )

    when (resourceRepository.getLpmRepository().serverSpecLoadingState) {
        is LpmRepository.ServerSpecState.ServerNotParsed -> {
            eventReporter.onLpmSpecFailure()
        }
        is LpmRepository.ServerSpecState.ServerParsed,
        is LpmRepository.ServerSpecState.Uninitialized,
        is LpmRepository.ServerSpecState.NoServerSpec -> {
        }
    }

    return value.intent
}
