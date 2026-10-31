package com.closetstudio.app.data

import android.content.Context
import com.closetstudio.core.model.ProjectCodec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.SavedProject
import java.io.File
import java.util.UUID

/**
 * Local JSON file-backed repository for saving, creating, opening, and deleting projects,
 * persisting all room measurements, door settings, corner angles, and locks (`candados`).
 */
class ProjectStorage(context: Context) {

    private val storageDir = File(context.filesDir, "closet_studio_projects").apply { mkdirs() }
    private val indexFile = File(storageDir, "projects.json")
    private val activeMetaFile = File(storageDir, "active_project_id.txt")
    private val languageFile = File(storageDir, "app_language.txt")

    fun getLanguageCode(): String {
        if (!languageFile.exists()) return "ES"
        return runCatching { languageFile.readText().trim().uppercase().ifEmpty { "ES" } }
            .getOrDefault("ES")
    }

    fun setLanguageCode(code: String) {
        runCatching { languageFile.writeText(code.trim().uppercase()) }
    }

    fun loadAllProjects(): List<SavedProject> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            ProjectCodec.decodeProjects(indexFile.readText())
                .sortedByDescending { it.updatedAtEpochMs }
        }.getOrDefault(emptyList())
    }

    fun getActiveProjectId(): String? {
        if (!activeMetaFile.exists()) return null
        return runCatching { activeMetaFile.readText().trim().takeIf { it.isNotEmpty() } }.getOrNull()
    }

    fun setActiveProjectId(id: String?) {
        runCatching {
            if (id == null) {
                activeMetaFile.delete()
            } else {
                activeMetaFile.writeText(id)
            }
        }
    }

    fun saveProject(
        id: String?,
        name: String,
        room: RoomSpec,
    ): SavedProject {
        val all = loadAllProjects().toMutableList()
        val now = System.currentTimeMillis()
        val cleanName = name.trim().ifEmpty { "Proyecto sin título" }
        val existingIdx = if (id != null) all.indexOfFirst { it.id == id } else -1

        val saved = if (existingIdx >= 0) {
            all[existingIdx].copy(
                name = cleanName,
                updatedAtEpochMs = now,
                room = room,
            ).also { all[existingIdx] = it }
        } else {
            SavedProject(
                id = id ?: UUID.randomUUID().toString(),
                name = cleanName,
                updatedAtEpochMs = now,
                room = room,
            ).also { all.add(0, it) }
        }

        persistAll(all)
        setActiveProjectId(saved.id)
        return saved
    }

    fun deleteProject(id: String): List<SavedProject> {
        val remaining = loadAllProjects().filterNot { it.id == id }
        persistAll(remaining)
        if (getActiveProjectId() == id) {
            setActiveProjectId(remaining.firstOrNull()?.id)
        }
        return remaining
    }

    private fun persistAll(projects: List<SavedProject>) {
        runCatching {
            indexFile.writeText(ProjectCodec.encodeProjects(projects))
        }
    }
}

