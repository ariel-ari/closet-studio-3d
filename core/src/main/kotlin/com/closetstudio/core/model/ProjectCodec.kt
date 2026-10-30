package com.closetstudio.core.model

import kotlinx.serialization.json.Json

/**
 * Single source of truth for project persistence. Explicit and lenient enough
 * to survive older files gaining fields later.
 */
object ProjectCodec {
    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(project: ClosetProject): String = json.encodeToString(ClosetProject.serializer(), project)

    fun decode(text: String): ClosetProject = json.decodeFromString(ClosetProject.serializer(), text)
}
