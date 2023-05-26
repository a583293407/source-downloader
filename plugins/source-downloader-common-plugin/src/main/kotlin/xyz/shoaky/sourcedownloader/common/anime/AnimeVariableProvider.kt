package xyz.shoaky.sourcedownloader.common.anime

import org.slf4j.LoggerFactory
import xyz.shoaky.sourcedownloader.external.anilist.AnilistClient
import xyz.shoaky.sourcedownloader.external.anilist.Search
import xyz.shoaky.sourcedownloader.external.bangumi.BgmTvApiClient
import xyz.shoaky.sourcedownloader.external.bangumi.SearchSubjectRequest
import xyz.shoaky.sourcedownloader.sdk.PatternVariables
import xyz.shoaky.sourcedownloader.sdk.SourceFile
import xyz.shoaky.sourcedownloader.sdk.SourceItem
import xyz.shoaky.sourcedownloader.sdk.SourceItemGroup
import xyz.shoaky.sourcedownloader.sdk.component.VariableProvider
import xyz.shoaky.sourcedownloader.sdk.util.replaces
import java.nio.file.Path

class AnimeVariableProvider(
    private val bgmTvApiClient: BgmTvApiClient,
    private val anilistClient: AnilistClient
) : VariableProvider {

    override fun createSourceGroup(sourceItem: SourceItem): SourceItemGroup {
        val create = create(sourceItem)
        return AnimeSourceGroup(create)
    }

    override fun support(item: SourceItem): Boolean = true

    private fun create(sourceItem: SourceItem): PatternVariables {
        val title = extractTitle(sourceItem)

        val hasJp = hasLanguage(title, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA)
        val hasChinese = hasLanguage(title, Character.UnicodeScript.HAN)
        if (hasJp || hasChinese.not()) {
            val response = anilistClient.execute(Search(title)).body()
            if (response.errors.isNotEmpty()) {
                return PatternVariables.EMPTY
            }
            val anime = response.data.page.medias.firstOrNull()
            if (anime == null) {
                log.warn("searching anime: $title no result")
            }
            return Anime(
                romajiName = anime?.title?.romaji,
                nativeName = anime?.title?.native
            )
        }
        val body = bgmTvApiClient.execute(SearchSubjectRequest(title)).body()
        val subjectItem = body.list.firstOrNull()
        if (subjectItem == null) {
            log.warn("searching anime: $title no result")
            return PatternVariables.EMPTY
        }

        val response = anilistClient.execute(Search(subjectItem.name)).body()
        if (response.errors.isNotEmpty()) {
            return Anime(
                nativeName = subjectItem.name
            )
        }
        val media = response.data.page.medias.firstOrNull()
        if (media == null) {
            log.warn("searching anime: $title no result")
        }

        return Anime(
            romajiName = media?.title?.romaji,
            nativeName = subjectItem.name
        )
    }

    private fun extractTitle(sourceItem: SourceItem): String {
        val text = sourceItem.title.replaces(
            listOf("(", "【", "（"), "["
        ).replaces(
            listOf(")", "】", "）"), "]"
        ).replaces(
            listOf("~", "！", "～", "SP", "TV", "-",
                "S01", "Season 1", "Season 01",
                "BDBOX", "BD-BOX", "+"
            ), ""
        )
            .replace(Regex("S(\\d+)"), "Season $1")
            .replace(Regex("\\[.*?]"), "")
            .trim()

        if (text.length > 12) {
            val sp = listOf("/", "|").firstOrNull {
                text.contains(it)
            } ?: return text

            // 优先选择日语，最后是中文尽可能用anilist搜索
            return text.split(sp)
                .map { TitleScore(it) }
                .maxBy { it.score }.title
        }
        return text
    }

    private fun hasLanguage(text: String, vararg unicode: Character.UnicodeScript): Boolean {
        return text.codePoints().anyMatch {
            unicode.contains(Character.UnicodeScript.of(it))
        }
    }

}

private class TitleScore(
    val title: String,
) {

    val score: Int = byLanguage(title)

    private fun byLanguage(title: String): Int {
        return title.codePoints().map {
            when (Character.UnicodeScript.of(it)) {
                Character.UnicodeScript.HAN -> 1
                Character.UnicodeScript.HIRAGANA -> 3
                Character.UnicodeScript.KATAKANA -> 3
                else -> 1
            }
        }.sum()
    }
}

private val log = LoggerFactory.getLogger(AnimeSourceGroup::class.java)

internal class AnimeSourceGroup(
    private val shared: PatternVariables
) : SourceItemGroup {

    override fun sharedPatternVariables(): PatternVariables {
        return shared
    }

    override fun sourceFiles(paths: List<Path>): List<SourceFile> {
        return paths.map { SourceFile.EMPTY }
    }

}

internal data class Anime(
    val romajiName: String? = null,
    val nativeName: String? = null
) : PatternVariables