package com.stripe.android.financialconnections.utils

import android.webkit.URLUtil
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import javax.inject.Inject

internal class ClickHandler @Inject constructor(
    private val uriUtils: UriUtils,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger
) {
    suspend fun handle(
        uri: String,
        pane: Pane,
        onNetworkUrlClick: (String) -> Unit,
        clickActions: Map<String, () -> Unit>

    ) {
        // if clicked uri contains an eventName query param, track click event.
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(
                Click(
                    eventName = eventName,
                    pane = pane
                )
            )
        }

        when {
            URLUtil.isNetworkUrl(uri) -> onNetworkUrlClick(uri)
            else ->
                clickActions
                    // get the first action that matches the uri.
                    .firstNotNullOfOrNull { (url, action) ->
                        action.takeIf { uriUtils.compareSchemeAuthorityAndPath(url, uri) }
                    }
                    // An actionable url has been found: Trigger action.
                    ?.let { action -> action() }
                    ?: run {
                        // No actionable url found on [clickActions]: Log error.
                        eventTracker.logError(
                            logger = logger,
                            pane = pane,
                            error = IllegalArgumentException("Unknown clickable URL: $uri"),
                            extraMessage = "Error resolving clickable URL"
                        )
                    }
        }
    }
}
