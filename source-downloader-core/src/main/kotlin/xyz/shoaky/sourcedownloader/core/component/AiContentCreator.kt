package xyz.shoaky.sourcedownloader.core.component

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.plexpt.chatgpt.ChatGPT
import com.plexpt.chatgpt.entity.chat.ChatCompletion
import com.plexpt.chatgpt.entity.chat.Message
import xyz.shoaky.sourcedownloader.sdk.*
import xyz.shoaky.sourcedownloader.sdk.component.ComponentProps
import xyz.shoaky.sourcedownloader.sdk.component.ComponentType
import xyz.shoaky.sourcedownloader.sdk.component.SdComponentSupplier
import xyz.shoaky.sourcedownloader.sdk.component.SourceContentCreator
import xyz.shoaky.sourcedownloader.sdk.util.Jackson
import java.nio.file.Path
import kotlin.io.path.name

// NOTE 先这样简单写后面如果有更好的ai再支持多个
class AiContentCreator(
    val config: Config
) : SourceContentCreator {

    private val chatGPT: ChatGPT = ChatGPT.builder()
        .apiKeyList(config.apiKeys)
        .timeout(900)
        .apiHost(config.apiHost) // 反向代理地址
        .build()
        .init()

    private val role = Message.ofSystem("""
            你现在是一个文件解析器，从文件名中解析信息
            需要的信息有:${config.resolveVariables}
            返回json，不要有其他会干扰json解析的字符
        """.trimIndent())

    override fun createSourceGroup(sourceItem: SourceItem): SourceItemGroup {

        return AiSourceGroup(chatGPT, role, sourceItem)
    }

    data class Config(
        val apiKeys: List<String>,
        val resolveVariables: List<String> = emptyList(),
        val type: String = "openai",
        val apiHost: String = "https://api.openai.com/",
    )
}

private class AiSourceGroup(
    val chatGPT: ChatGPT,
    val role: Message,
    val sourceItem: SourceItem,
) : SourceItemGroup {
    override fun sourceFiles(paths: List<Path>): List<SourceFile> {
        return paths.map { path ->
            val chatCompletion: ChatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
                .messages(listOf(role, Message.of("${sourceItem.title} ${path.name}")))
                .maxTokens(2000)
                .temperature(0.9)
                .build()
            val response = chatGPT.chatCompletion(chatCompletion)
            val first = response.choices.map {
                it.message
            }
            val map = Jackson.fromJson(first.first().content, jacksonTypeRef<Map<String, String>>())
            UniversalSourceFile(MapPatternVariables(map))
        }
    }
}

object AiContentCreatorSupplier : SdComponentSupplier<AiContentCreator> {
    override fun apply(props: ComponentProps): AiContentCreator {
        return AiContentCreator(props.parse())
    }

    override fun supplyTypes(): List<ComponentType> {
        return listOf(
            ComponentType.creator("ai"),
            ComponentType.creator("gpt")
        )
    }

    override fun getComponentClass(): Class<AiContentCreator> {
        return AiContentCreator::class.java
    }

}