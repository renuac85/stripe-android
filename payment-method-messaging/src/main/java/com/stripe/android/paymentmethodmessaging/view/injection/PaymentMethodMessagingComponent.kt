package com.stripe.android.paymentmethodmessaging.view.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingView
import com.stripe.android.paymentmethodmessaging.view.PaymentMethodMessagingViewModel
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PaymentMethodMessagingModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessagingComponent {
    val viewModel: PaymentMethodMessagingViewModel

    fun inject(factory: PaymentMethodMessagingViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: PaymentMethodMessagingView.Configuration): Builder

        @BindsInstance
        fun isSystemDarkTheme(isSystemDarkTheme: Boolean): Builder

        @BindsInstance
        fun lifecycleScope(lifecycleScope: CoroutineScope): Builder

        fun build(): PaymentMethodMessagingComponent
    }
}
