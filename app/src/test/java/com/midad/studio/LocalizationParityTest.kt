package com.midad.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app ships two locales by hand, so nothing but this test stops the Arabic catalogue
 * from drifting away from English. It also guards the other direction: identical string
 * counts were reported as evidence of localisation, but duplicates or a missing format
 * argument would still ship.
 */
class LocalizationParityTest {

    private val resourceRoot: File = findResourceRoot()

    private fun findResourceRoot(): File {
        val candidates = listOf(File("src/main/res"), File("app/src/main/res"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not find src/main/res from " + File(".").absolutePath)
    }

    private fun localeFiles(dir: String): List<File> =
        resourceRoot.resolve(dir)
            .listFiles { file -> file.name.startsWith("strings_") && file.name.endsWith(".xml") }
            ?.sortedBy { it.name }
            ?: emptyList()

    private fun entries(file: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result = LinkedHashMap<String, String>()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            // translatable="false" keys are allowed to exist only in the default locale.
            if (element.getAttribute("translatable").equals("false", ignoreCase = true)) continue
            val name = element.getAttribute("name")
            result[name] = element.textContent
        }
        return result
    }

    private fun formatArguments(value: String): List<String> {
        val pattern = Regex("%(?:\\d+\\\$)?[a-zA-Z]")
        return pattern.findAll(value).map { it.value }.sorted().toList()
    }

    @Test
    fun everyLocaleFileExists() {
        val english = localeFiles("values").map { it.name }.toSet()
        val arabic = localeFiles("values-ar").map { it.name }.toSet()
        assertTrue("no English string files found", english.isNotEmpty())
        assertEquals("Arabic and English string file sets differ", english, arabic)
    }

    @Test
    fun bothLocalesDefineTheSameKeys() {
        val failures = StringBuilder()
        for (file in localeFiles("values")) {
            val arabicFile = resourceRoot.resolve("values-ar").resolve(file.name)
            if (!arabicFile.isFile) {
                failures.append("missing Arabic ").append(file.name).append('\n')
                continue
            }
            val english = entries(file)
            val arabic = entries(arabicFile)
            for (name in english.keys - arabic.keys) failures.append(file.name).append(": ar missing ").append(name).append('\n')
            for (name in arabic.keys - english.keys) failures.append(file.name).append(": ar extra ").append(name).append('\n')
            for (name in arabic.keys) {
                if (arabic[name].isNullOrBlank()) failures.append(file.name).append(": ar blank ").append(name).append('\n')
            }
        }
        assertTrue(failures.toString(), failures.isEmpty())
    }

    @Test
    fun formatArgumentsMatchBetweenLocales() {
        val failures = StringBuilder()
        for (file in localeFiles("values")) {
            val arabicFile = resourceRoot.resolve("values-ar").resolve(file.name)
            if (!arabicFile.isFile) continue
            val english = entries(file)
            val arabic = entries(arabicFile)
            for ((name, englishValue) in english) {
                val arabicValue = arabic[name] ?: continue
                val englishArgs = formatArguments(englishValue)
                val arabicArgs = formatArguments(arabicValue)
                if (englishArgs != arabicArgs) {
                    failures.append(file.name).append(": ").append(name)
                        .append(" en=").append(englishArgs).append(" ar=").append(arabicArgs).append('\n')
                }
            }
        }
        assertTrue(failures.toString(), failures.isEmpty())
    }

    @Test
    fun keysAreUniqueAcrossFiles() {
        val seen = HashMap<String, String>()
        val duplicates = StringBuilder()
        for (file in localeFiles("values")) {
            for (name in entries(file).keys) {
                val previous = seen.put(name, file.name)
                if (previous != null) {
                    duplicates.append(name).append(" in ").append(previous).append(" and ").append(file.name).append('\n')
                }
            }
        }
        assertTrue(duplicates.toString(), duplicates.isEmpty())
    }
}
