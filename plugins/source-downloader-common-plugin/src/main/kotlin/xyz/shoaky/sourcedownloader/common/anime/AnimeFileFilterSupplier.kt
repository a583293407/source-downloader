package xyz.shoaky.sourcedownloader.common.anime

import xyz.shoaky.sourcedownloader.sdk.Properties
import xyz.shoaky.sourcedownloader.sdk.SdComponentSupplier
import xyz.shoaky.sourcedownloader.sdk.component.ComponentType

object AnimeFileFilterSupplier : SdComponentSupplier<AnimeFileFilter> {
    override fun apply(props: Properties): AnimeFileFilter {
        return AnimeFileFilter
    }

    override fun supplyTypes(): List<ComponentType> {
        return listOf(
            ComponentType.fileFilter("anime")
        )
    }

    override fun autoCreateDefault(): Boolean = true
}