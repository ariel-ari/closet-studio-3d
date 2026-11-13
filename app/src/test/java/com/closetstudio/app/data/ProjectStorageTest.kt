package com.closetstudio.app.data

import com.closetstudio.core.model.RoomSpec
import java.nio.file.Files
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class ProjectStorageTest {
    @Test
    fun `saveProject reports an index write failure`() {
        val root = Files.createTempDirectory("project-storage-test").toFile()
        try {
            root.resolve("projects.json").mkdir()
            val storage = ProjectStorage.forStorageDirectory(root)

            assertFailsWith<Exception> {
                storage.saveProject(id = null, name = "Test", room = RoomSpec())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
