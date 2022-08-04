package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class UpgradeAccountViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val productDetails = subscriptionManager.observeProductDetails().map { productDetailsState ->
        if (productDetailsState is ProductDetailsState.Loaded) {
            val product = productDetailsState.productDetails
                .find { detail -> detail.productId == SubscriptionManager.TEST_FREE_TRIAL_PRODUCT_ID }
            val subscription = product?.let { Subscription.fromProductDetails(it) }
            val trialPhase = subscription?.trialSubscriptionPhase
            val trialString = subscription?.numFreeThenPricePerPeriod(context.resources)
            val productState = if (trialPhase != null && trialString != null) {
                ProductState.ProductWithTrial(
                    featureLabel = context.resources.getString(
                        LR.string.profile_feature_try_trial,
                        trialPhase.periodValue(context.resources)
                    ),
                    price = trialString
                )
            } else {
                subscription?.recurringSubscriptionPhase?.priceSlashPeriod(context.resources)?.let { price ->
                    ProductState.ProductWithoutTrial(
                        featureLabel = context.resources.getString(LR.string.profile_feature_requires),
                        price = price,
                    )
                }
            }
            Optional.of(productState)
        } else {
            Optional.empty()
        }
    }

    val productState: LiveData<Optional<ProductState>> =
        LiveDataReactiveStreams.fromPublisher(productDetails)

    sealed class ProductState {
        abstract val featureLabel: String
        abstract val price: String
        abstract val buttonLabel: Int
        data class ProductWithTrial(
            override val featureLabel: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_start_free_trial
        ) : ProductState()

        data class ProductWithoutTrial(
            override val featureLabel: String,
            override val price: String,
            @StringRes override val buttonLabel: Int = LR.string.profile_upgrade_to_plus
        ) : ProductState()
    }
}
