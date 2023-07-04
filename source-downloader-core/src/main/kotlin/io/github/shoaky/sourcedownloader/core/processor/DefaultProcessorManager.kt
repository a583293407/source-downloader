package io.github.shoaky.sourcedownloader.core.processor

import io.github.shoaky.sourcedownloader.SourceDownloaderApplication.Companion.log
import io.github.shoaky.sourcedownloader.component.supplier.*
import io.github.shoaky.sourcedownloader.core.ObjectContainer
import io.github.shoaky.sourcedownloader.core.ProcessingStorage
import io.github.shoaky.sourcedownloader.core.ProcessorConfig
import io.github.shoaky.sourcedownloader.core.component.ComponentManager
import io.github.shoaky.sourcedownloader.sdk.Properties
import io.github.shoaky.sourcedownloader.sdk.SourceItemPointer
import io.github.shoaky.sourcedownloader.sdk.component.*

class DefaultProcessorManager(
    private val processingStorage: ProcessingStorage,
    private val componentManager: ComponentManager,
    private val objectContainer: ObjectContainer,
) : ProcessorManager {

    @Suppress("UNCHECKED_CAST")
    override fun createProcessor(config: ProcessorConfig): SourceProcessor {
        val processorBeanName = processorBeanName(config.name)
        if (objectContainer.contains(processorBeanName)) {
            throw ComponentException.processorExists("Processor ${config.name} already exists")
        }
        val source = objectContainer.get(config.sourceInstanceName(), Source::class.java) as Source<SourceItemPointer>
        val downloader = objectContainer.get(config.downloaderInstanceName(), Downloader::class.java)

        val providers = config.providerInstanceNames().map {
            objectContainer.get(it, VariableProvider::class.java)
        }
        val mover = objectContainer.get(config.moverInstanceName(), FileMover::class.java)
        val resolver = objectContainer.get(config.fileResolverInstanceName(), ItemFileResolver::class.java)
        val processor = SourceProcessor(
            config.name,
            config.source.id,
            source,
            providers,
            resolver,
            downloader,
            mover,
            config.savePath,
            config.options,
            processingStorage,
        )

        val mutableListOf = mutableListOf(
            config.source.getComponentType(Source::class),
            config.downloader.getComponentType(Downloader::class),
            config.fileMover.getComponentType(FileMover::class),
        )
        mutableListOf.addAll(
            config.variableProviders.map { it.getComponentType(VariableProvider::class) }
        )

        val cps = mutableListOf(source, downloader, mover, resolver)
        cps.addAll(providers)
        mutableListOf.forEach {
            check(it, cps, config)
        }

        val fileFilters = config.options.fileContentFilters.map {
            val instanceName = it.getInstanceName(FileContentFilter::class)
            objectContainer.get(instanceName, FileContentFilter::class.java)
        }.toTypedArray()
        processor.addFileFilter(*fileFilters)

        val itemFilters = config.options.sourceItemFilters.map {
            val instanceName = it.getInstanceName(FileContentFilter::class)
            objectContainer.get(instanceName, FileContentFilter::class.java)
        }.toTypedArray()
        processor.addFileFilter(*itemFilters)

        initOptions(config.options, processor)

        objectContainer.put(processorBeanName, processor)
        log.info("处理器初始化完成:$processor")

        val task = processor.safeTask()
        config.triggerInstanceNames().map {
            objectContainer.get(it, Trigger::class.java)
        }.forEach {
            it.addTask(task)
        }
        return processor
    }

    override fun getProcessor(name: String): SourceProcessor? {
        val processorBeanName = processorBeanName(name)
        return if (objectContainer.contains(processorBeanName)) {
            objectContainer.get(processorBeanName, SourceProcessor::class.java)
        } else {
            null
        }
    }

    private fun processorBeanName(name: String): String {
        if (name.startsWith("Processor-")) {
            return name
        }
        return "Processor-$name"
    }

    private fun initOptions(options: ProcessorConfig.Options, processor: SourceProcessor) {
        if (options.itemExpressionExclusions.isNotEmpty() || options.itemExpressionInclusions.isNotEmpty()) {
            processor.addItemFilter(ExpressionItemFilterSupplier.expressions(
                options.itemExpressionExclusions,
                options.itemExpressionInclusions
            ))
        }
        if (options.contentExpressionExclusions.isNotEmpty() || options.contentExpressionInclusions.isNotEmpty()) {
            processor.addContentFilter(ExpressionSourceContentFilterSupplier.expressions(
                options.contentExpressionExclusions,
                options.contentExpressionInclusions
            ))
        }
        if (options.fileExpressionExclusions.isNotEmpty() || options.fileExpressionInclusions.isNotEmpty()) {
            processor.addFileFilter(ExpressionFileFilterSupplier.expressions(
                options.fileExpressionExclusions,
                options.fileExpressionInclusions
            ))
        }
        val runAfterCompletion = options.runAfterCompletion
        runAfterCompletion.forEach {
            val instanceName = it.getInstanceName(RunAfterCompletion::class)
            objectContainer.get(instanceName, RunAfterCompletion::class.java)
                .also { completion -> processor.addRunAfterCompletion(completion) }
        }
        processor.scheduleRenameTask(options.renameTaskInterval)
        if (options.deleteEmptyDirectory) {
            val deleteEmptyDirectory = DeleteEmptyDirectorySupplier.apply(Properties.EMPTY)
            processor.addRunAfterCompletion(deleteEmptyDirectory)
        }
        if (options.touchItemDirectory) {
            val touchItemDirectory = TouchItemDirectorySupplier.apply(Properties.EMPTY)
            processor.addRunAfterCompletion(touchItemDirectory)
        }
        options.fileTaggers.forEach {
            val instanceName = it.getInstanceName(FileTagger::class)
            objectContainer.get(instanceName, FileTagger::class.java)
                .also { tagger -> processor.addTagger(tagger) }
        }
    }

    // TODO 重构这一校验，目标通过组件的描述对象
    // TODO 第二个参数应该给组件的描述对象
    private fun check(componentType: ComponentType, components: List<SdComponent>, config: ProcessorConfig) {
        val supplier = componentManager.getSupplier(componentType)
        val compatibilities = supplier.rules().groupBy { it.type }

        val componentTypeMapping = components.groupBy {
            ComponentTopType.fromClass(it::class)
        }.flatMap { (key, value) ->
            key.map { it to value }
        }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.flatten().distinct() }

        for (rules in compatibilities) {
            val typeComponents = componentTypeMapping[rules.key] ?: emptyList()
            if (typeComponents.isEmpty()) {
                continue
            }
            var exception: Exception? = null
            val allow = rules.value.any { rule ->
                components.map {
                    try {
                        rule.verify(it)
                        return@map true
                    } catch (ex: ComponentException) {
                        exception = ex
                        return@map false
                    }
                }.any()
            }
            if (allow.not()) {
                exception?.let {
                    throw ComponentException.compatibility("Processor:${config.name} ${it.message}")
                }
            }
        }
    }

    override fun getProcessors(): List<SourceProcessor> {
        return objectContainer.getObjectsOfType(SourceProcessor::class.java).values.toList()
    }

    override fun destroy(processorName: String) {
        val processorBeanName = processorBeanName(processorName)
        if (objectContainer.contains(processorBeanName).not()) {
            throw ComponentException.processorMissing("Processor $processorName not exists")
        }

        val processor = objectContainer.get(processorBeanName, SourceProcessor::class.java)
        val safeTask = processor.safeTask()
        componentManager.getAllTrigger().forEach {
            it.removeTask(safeTask)
        }
        objectContainer.remove(processorBeanName)
    }

    override fun getAllProcessorNames(): Set<String> {
        return objectContainer.getObjectsOfType(SourceProcessor::class.java).keys
    }
}