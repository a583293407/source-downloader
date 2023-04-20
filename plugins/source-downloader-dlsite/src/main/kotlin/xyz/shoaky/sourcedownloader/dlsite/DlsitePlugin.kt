package xyz.shoaky.sourcedownloader.dlsite

import xyz.shoaky.sourcedownloader.sdk.Plugin
import xyz.shoaky.sourcedownloader.sdk.PluginContext
import xyz.shoaky.sourcedownloader.sdk.PluginDescription

class DlsitePlugin : Plugin {
    override fun init(pluginContext: PluginContext) {
        pluginContext.registerSupplier(DlsiteVariableProviderSupplier)
    }

    override fun destroy(pluginContext: PluginContext) {

    }

    override fun description(): PluginDescription {
        return PluginDescription("IDK", "0.0.1")
    }
}