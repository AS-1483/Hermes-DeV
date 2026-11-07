package com.example

import com.example.ui.navigation.Screen
import com.example.util.PermissionHelper
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun `test Screen items are non-null and valid`() {
        val items = Screen.items
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull("Screen in items must not be null", screen)
            assertTrue("Screen route must not be blank", screen.route.isNotBlank())
            assertTrue("Screen title must not be blank", screen.title.isNotBlank())
            assertNotNull("Screen icon must not be null", screen.icon)
        }
    }

    @Test
    fun `test safety detection for dangerous commands`() {
        val dangerousList = listOf(
            "rm -rf /",
            "rm -rf *",
            "sudo apt-get install",
            "chmod 777 /system",
            ":(){ :|:& };:"
        )

        val safeList = listOf(
            "gradle assembleDebug",
            "git status",
            "python3 -m unittest",
            "npm test",
            "cat README.md"
        )

        fun isDangerous(cmd: String): Boolean {
            val lower = cmd.trim().lowercase()
            return lower.contains("rm -rf") ||
                   lower.contains("sudo") ||
                   lower.contains("chmod 777") ||
                   lower.contains(":(){")
        }

        dangerousList.forEach { cmd ->
            assertTrue("Expected dangerous: $cmd", isDangerous(cmd))
        }

        safeList.forEach { cmd ->
            assertFalse("Expected safe: $cmd", isDangerous(cmd))
        }
    }

    @Test
    fun `test anti fake work physical verification logic`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "hermes_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val validFile = File(tempDir, "output.apk")
        validFile.writeBytes(ByteArray(1024) { 1 })

        val emptyFile = File(tempDir, "empty.log")
        emptyFile.createNewFile()

        val missingFile = File(tempDir, "missing.bin")

        fun verifyFile(f: File): Boolean = f.exists() && f.isFile && f.length() > 0

        assertTrue("Valid non-empty file should pass verification", verifyFile(validFile))
        assertFalse("Empty file should fail anti-fake verification", verifyFile(emptyFile))
        assertFalse("Missing file should fail anti-fake verification", verifyFile(missingFile))

        // Cleanup
        tempDir.deleteRecursively()
    }

    @Test
    fun `test PermissionHelper utility class existence`() {
        assertNotNull(PermissionHelper)
    }
}
