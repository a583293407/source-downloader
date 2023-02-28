package xyz.shoaky.sourcedownloader.core

interface ProcessingStorage {

    fun saveRenameTask(content: ProcessingContent)
    fun findRenameContent(name: String, renameTimesThreshold: Int): List<ProcessingContent>
    fun deleteById(id: String)
    fun findByNameAndHash(processorName: String, itemHashing: String): ProcessingContent?
}