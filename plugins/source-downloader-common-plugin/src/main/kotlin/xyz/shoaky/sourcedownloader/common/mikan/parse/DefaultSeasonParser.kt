package xyz.shoaky.sourcedownloader.common.mikan.parse

object DefaultSeasonParser : ValueParser {
    override val name: String = "default"

    override fun apply(subjectContent: SubjectContent, filename: String): Result {
        return Result(1, Result.Accuracy.LOW)
    }
}