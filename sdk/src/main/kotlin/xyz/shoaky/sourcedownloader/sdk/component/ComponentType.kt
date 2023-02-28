package xyz.shoaky.sourcedownloader.sdk.component

import kotlin.reflect.KClass

data class ComponentType(val typeName: String,
    //TODO limit sealed class
                         val klass: KClass<out SdComponent>) {

    companion object {
        fun downloader(type: String) = ComponentType(type, Downloader::class)
        fun source(type: String) = ComponentType(type, Source::class)
        fun fileMover(type: String) = ComponentType(type, FileMover::class)
        fun creator(type: String) = ComponentType(type, SourceContentCreator::class)
    }

    fun fullName() = "${klass.simpleName}:$typeName"

    fun instanceName(name: String) = "${fullName()}:${name}"

}