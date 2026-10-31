package com.closetstudio.core.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ProjectCodec {
    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(room: RoomSpec): String = json.encodeToString(RoomSpec.serializer(), room)

    fun decode(text: String): RoomSpec = json.decodeFromString(RoomSpec.serializer(), text)

    fun encodeProjects(projects: List<SavedProject>): String =
        json.encodeToString(ListSerializer(SavedProject.serializer()), projects)

    fun decodeProjects(text: String): List<SavedProject> =
        json.decodeFromString(ListSerializer(SavedProject.serializer()), text)
}
