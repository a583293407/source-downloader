package xyz.shoaky.sourcedownloader.component.supplier

import xyz.shoaky.sourcedownloader.component.provider.SeasonProvider
import xyz.shoaky.sourcedownloader.sdk.Properties
import xyz.shoaky.sourcedownloader.sdk.SdComponentSupplier
import xyz.shoaky.sourcedownloader.sdk.component.ComponentType

object SeasonProviderSupplier : SdComponentSupplier<SeasonProvider> {
    override fun apply(props: Properties): SeasonProvider {
        return SeasonProvider
    }

    override fun supplyTypes(): List<ComponentType> {
        return listOf(
            ComponentType.provider("season")
        )
    }

}