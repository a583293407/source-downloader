package xyz.shoaky.sourcedownloader.core.processor

import org.junit.jupiter.api.Test
import xyz.shoaky.sourcedownloader.sdk.*
import xyz.shoaky.sourcedownloader.sdk.component.VariableProvider
import xyz.shoaky.sourcedownloader.sourceItem
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.assertEquals

class VariableProvidersAggregationTest {

    @Test
    fun test_vote() {
        val p1 = CustomProvider(
            mapOf("season" to "01", "test1" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "03"))
                )
            )
        )

        val p2 = CustomProvider(
            mapOf("season" to "01", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "03"))
                )
            )
        )

        val p3 = CustomProvider(
            mapOf("season" to "02", "test3" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "03", "test" to "111"))
                )
            )
        )

        val aggregation = VariableProvidersAggregation(listOf(p1, p2, p3))
        val sourceItemGroup = aggregation.aggrVariables(sourceItem(), VariableConflictStrategy.VOTE)
        val variables = sourceItemGroup.sharedPatternVariables().variables()
        assertEquals(4, variables.size)
        assertEquals("01", variables["season"])

        val sourceFiles = sourceItemGroup.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))

        assertEquals("01", sourceFiles[0].patternVariables().variables()["episode"])
        assertEquals("02.5", sourceFiles[1].patternVariables().variables()["episode"])
        assertEquals("03", sourceFiles[2].patternVariables().variables()["episode"])
        assertEquals(2, sourceFiles[2].patternVariables().variables().size)
        assertEquals("111", sourceFiles[2].patternVariables().variables()["test"])
    }


    @Test
    fun test_accuracy() {
        val p1 = CustomProvider(
            mapOf("season" to "01", "test1" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            0
        )

        val p2 = CustomProvider(
            mapOf("season" to "01", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "test1" to "test1"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            1
        )

        val p3 = CustomProvider(
            mapOf("season" to "02", "test3" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "03", "test" to "111"))
                )
            ),
            2
        )

        val p4 = CustomProvider(
            mapOf("season" to "01", "test4" to "4"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "dddd" to "4444"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            1
        )

        val aggregation = VariableProvidersAggregation(listOf(p1, p2, p3, p4))
        val sourceItemGroup = aggregation.aggrVariables(sourceItem(), VariableConflictStrategy.ACCURACY)
        val sharedVariables = sourceItemGroup.sharedPatternVariables().variables()

        assertEquals(5, sharedVariables.size)
        assertEquals("02", sharedVariables["season"])
        assertEquals("2", sharedVariables["test3"])

        val sourceFiles = sourceItemGroup.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))

        assertEquals("01", sourceFiles[0].patternVariables().variables()["episode"])
        assertEquals("03", sourceFiles[2].patternVariables().variables()["episode"])
        assertEquals(3, sourceFiles[0].patternVariables().variables().size)
    }

    @Test
    fun test_smart_given_2providers() {
        val p1 = CustomProvider(
            mapOf("season" to "01", "test1" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            0
        )

        val p2 = CustomProvider(
            mapOf("season" to "02", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "key2" to "test2"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            1
        )

        val aggregation1 = VariableProvidersAggregation(listOf(p1, p2))
        val sourceItemGroup1 = aggregation1.aggrVariables(sourceItem(), VariableConflictStrategy.SMART)
        val sharedVariables1 = sourceItemGroup1.sharedPatternVariables().variables()
        assertEquals(3, sharedVariables1.size)
        assertEquals("02", sharedVariables1["season"])

        val sourceFiles1 = sourceItemGroup1.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))
        assertEquals(2, sourceFiles1[0].patternVariables().variables().size)
        assertEquals("test2", sourceFiles1[0].patternVariables().variables()["key2"])
        assertEquals("02.5", sourceFiles1[1].patternVariables().variables()["episode"])


        // change order
        val aggregation2 = VariableProvidersAggregation(listOf(p2, p1))
        val sourceItemGroup2 = aggregation2.aggrVariables(sourceItem(), VariableConflictStrategy.SMART)
        val sharedVariables2 = sourceItemGroup2.sharedPatternVariables().variables()
        assertEquals(3, sharedVariables2.size)
        assertEquals("02", sharedVariables2["season"])

        val sourceFiles2 = sourceItemGroup2.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))
        assertEquals(2, sourceFiles2[0].patternVariables().variables().size)
        assertEquals("test2", sourceFiles2[0].patternVariables().variables()["key2"])
        assertEquals("02.5", sourceFiles2[1].patternVariables().variables()["episode"])

    }


    @Test
    fun test_smart_given_4providers() {
        val p1 = CustomProvider(
            mapOf("season" to "00", "test1" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            0
        )

        val p2 = CustomProvider(
            mapOf("season" to "01", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "key2" to "test2"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04"))
                )
            ),
            2
        )

        val p3 = CustomProvider(
            mapOf("season" to "02", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "key2" to "test2"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04", "key4.1" to "5"))
                )
            ),
            2
        )

        val p4 = CustomProvider(
            mapOf("season" to "02", "test2" to "2"),
            listOf(
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "01", "key2" to "test2"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "02.5"))
                ),
                UniversalSourceFile(
                    MapPatternVariables(mapOf("episode" to "04", "key4" to "2", "key4.1" to "4"))
                )
            ),
            1
        )

        val aggregation1 = VariableProvidersAggregation(listOf(p1, p3, p2, p4))
        val sourceItemGroup1 = aggregation1.aggrVariables(sourceItem(), VariableConflictStrategy.SMART)
        val sharedVariables1 = sourceItemGroup1.sharedPatternVariables().variables()
        assertEquals(3, sharedVariables1.size)
        assertEquals("02", sharedVariables1["season"])

        val sourceFiles1 = sourceItemGroup1.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))
        assertEquals(2, sourceFiles1[0].patternVariables().variables().size)
        assertEquals(3, sourceFiles1[2].patternVariables().variables().size)
        assertEquals("5", sourceFiles1[2].patternVariables().variables()["key4.1"])

        // change order
        val aggregation2 = VariableProvidersAggregation(listOf(p4, p3, p1, p2))
        val sourceItemGroup2 = aggregation2.aggrVariables(sourceItem(), VariableConflictStrategy.SMART)
        val sharedVariables2 = sourceItemGroup2.sharedPatternVariables().variables()
        assertEquals(3, sharedVariables2.size)
        assertEquals("02", sharedVariables2["season"])

        val sourceFiles2 = sourceItemGroup2.sourceFiles(listOf(
            Path(""), Path(""), Path("")
        ))
        assertEquals(2, sourceFiles2[0].patternVariables().variables().size)
        assertEquals(3, sourceFiles2[2].patternVariables().variables().size)
        assertEquals("5", sourceFiles2[2].patternVariables().variables()["key4.1"])

    }
}

private class CustomProvider(
    val variables: Map<String, String>,
    val files: List<UniversalSourceFile>,
    override val accuracy: Int = 2
) : VariableProvider {

    override fun createSourceGroup(sourceItem: SourceItem): SourceItemGroup {
        return object : SourceItemGroup {
            override fun sourceFiles(paths: List<Path>): List<SourceFile> {
                return files
            }

            override fun sharedPatternVariables(): PatternVariables {
                return MapPatternVariables(variables)
            }
        }
    }

    override fun support(item: SourceItem): Boolean = true

}