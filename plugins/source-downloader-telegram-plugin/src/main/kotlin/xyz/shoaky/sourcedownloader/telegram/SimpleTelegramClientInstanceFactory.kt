package xyz.shoaky.sourcedownloader.telegram

import it.tdlight.client.*
import it.tdlight.jni.TdApi
import xyz.shoaky.sourcedownloader.sdk.InstanceFactory
import xyz.shoaky.sourcedownloader.sdk.Properties
import java.net.URI
import java.nio.file.Path

class SimpleTelegramClientInstanceFactory(
    private val applicationDataPath: Path
) : InstanceFactory<SimpleTelegramClient> {

    override fun create(props: Properties): SimpleTelegramClient {
        val config = props.parse<ClientConfig>()

        val settings = TDLibSettings.create(APIToken(config.apiId, config.apiHash))
        val clientFactory = SimpleTelegramClientFactory()
        val client = clientFactory.builder(settings)
            .build(AuthenticationSupplier.qrCode())
        // 不清楚这个path需不需要apiId区分
        if (config.databasePath.isAbsolute) {
            settings.databaseDirectoryPath = config.databasePath
        } else {
            settings.databaseDirectoryPath = applicationDataPath.resolve(config.databasePath)
        }
        settings.downloadedFilesDirectoryPath = config.downloadPath

        val proxy = config.proxy
        if (proxy != null) {
            val type = when (proxy.scheme) {
                "http" -> TdApi.ProxyTypeHttp()
                "https" -> TdApi.ProxyTypeHttp()
                "socks5" -> TdApi.ProxyTypeSocks5()
                else -> throw IllegalArgumentException("Unknown proxy type: ${proxy.scheme}")
            }
            client.send(TdApi.AddProxy(proxy.host, proxy.port, true, type)) {}
        }
        return client
    }

    data class ClientConfig(
        val apiId: Int,
        val apiHash: String,
        val databasePath: Path,
        val downloadPath: Path,
        val proxy: URI?,
    )

}