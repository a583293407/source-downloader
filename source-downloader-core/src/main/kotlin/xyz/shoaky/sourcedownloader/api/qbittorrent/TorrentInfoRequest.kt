package xyz.shoaky.sourcedownloader.api.qbittorrent

import com.fasterxml.jackson.core.type.TypeReference
import xyz.shoaky.sourcedownloader.sdk.api.HttpMethod

/**
 * https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)#torrent-management
 */
class TorrentInfoRequest(
    val filter: String? = null,
    val category: String? = null,
    val tag: String? = null,
    val sort: String? = null,
    val reverse: Boolean? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val hashes: String? = null,
) : QbittorrentRequest<List<TorrentInfo>>() {

    override val path: String = "/api/v2/torrents/info"

    override val responseBodyType: TypeReference<List<TorrentInfo>> = object : TypeReference<List<TorrentInfo>>() {}

    override val httpMethod: HttpMethod = HttpMethod.GET

}