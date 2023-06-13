package io.github.shoaky.sourcedownloader.core

import io.github.shoaky.sourcedownloader.sdk.component.ComponentTopType

interface ComponentConfigStorage {

    fun getAllComponentConfig(): Map<String, List<ComponentConfig>>
}

interface ConfigOperator {

    fun save(type: String, componentConfig: ComponentConfig)

    fun save(name: String, processorConfig: ProcessorConfig)

    fun deleteComponent(topType: ComponentTopType, type: String, name: String)
    fun deleteProcessor(name: String)
}