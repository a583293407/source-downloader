package io.github.shoaky.sourcedownloader.core.component

import com.fasterxml.jackson.module.kotlin.convertValue
import io.github.shoaky.sourcedownloader.core.processor.SourceProcessor
import io.github.shoaky.sourcedownloader.sdk.Properties
import io.github.shoaky.sourcedownloader.sdk.SourceItemPointer
import io.github.shoaky.sourcedownloader.sdk.component.*
import io.github.shoaky.sourcedownloader.util.jackson.yamlMapper
import kotlin.jvm.optionals.getOrElse

interface ComponentManager {

    fun createComponent(name: String, componentType: ComponentType, props: Properties)

    fun getAllProcessor(): List<SourceProcessor>

    fun getComponent(name: String): ComponentWrapper<SdComponent>?

    fun getAllComponent(): List<ComponentWrapper<SdComponent>>

    fun getSupplier(type: ComponentType): ComponentSupplier<*>

    fun getSuppliers(): List<ComponentSupplier<*>>

    fun registerSupplier(vararg sdComponentSuppliers: ComponentSupplier<*>)

    fun getAllComponentNames(): Set<String>

    fun getAllTrigger(): List<Trigger> {
        return getAllComponent().map { it.component }.filterIsInstance<Trigger>()
    }

    fun getAllSource(): List<Source<SourceItemPointer>> {
        return getAllComponent().map { it.component }.filterIsInstance<Source<SourceItemPointer>>()
    }

    fun getAllDownloader(): List<Downloader> {
        return getAllComponent().map { it.component }.filterIsInstance<Downloader>()
    }

    fun getAllMover(): List<FileMover> {
        return getAllComponent().map { it.component }.filterIsInstance<FileMover>()
    }

    fun getAllProvider(): List<VariableProvider> {
        return getAllComponent().map { it.component }.filterIsInstance<VariableProvider>()
    }

    fun getComponentDescriptions(): List<ComponentDescription>

    fun destroy(instanceName: String)
}

object DescriptionLoader {

    private const val DESCRIPTION_FILE = "sd-description.yaml"
    private const val COMPONENT = "component"
    private const val INSTANCE = "instance"

    fun load(): Descriptor {
        val classLoader = Thread.currentThread().contextClassLoader
        val node = classLoader.resources(DESCRIPTION_FILE).map {
            yamlMapper.readTree(it)
        }.reduce { acc, op ->
            val readerForUpdating = yamlMapper.readerForUpdating(acc)
            readerForUpdating.readValue(op)
        }.getOrElse { yamlMapper.nullNode() }
        val component = node[COMPONENT]
        val instance = node[INSTANCE]
        return Descriptor(
            yamlMapper.convertValue(instance),
            yamlMapper.convertValue(component),
        )
    }
}

data class Descriptor(
    val instance: List<InstanceDescription> = emptyList(),
    val component: List<ComponentDescription> = emptyList()
)

data class InstanceDescription(
    val id: String,
    override val name: String = id,
    override val description: String,
    val properties: List<PropertyDescriptor> = emptyList(),
) : CommonDescriptor(name, description, null)

data class ComponentDescription(
    val id: String,
    val description: String? = null,
    val properties: List<PropertyDescriptor> = emptyList(),
    val variables: List<VariableDescriptor> = emptyList(),
    var types: List<ComponentTypeDescriptor> = emptyList(),
    var rules: List<RuleDescriptor> = emptyList(),
)

data class ComponentTypeDescriptor(
    val topType: ComponentTopType,
    val subType: String,
)

data class PropertyDescriptor(
    override val name: String,
    override val description: String? = null,
    override val example: String? = null,
    val required: Boolean = true
) : CommonDescriptor(name, description, example)

data class VariableDescriptor(
    override val name: String,
    override val description: String,
    override val example: String? = null
) : CommonDescriptor(name, description, example)

open class CommonDescriptor(
    open val name: String,
    open val description: String? = null,
    open val example: String? = null
)