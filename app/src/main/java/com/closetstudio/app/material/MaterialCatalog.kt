package com.closetstudio.app.material

import android.content.Context

enum class TextureCategory(val folder: String, val displayNameKey: String) {
    MELAMINE("textures/melamines", "Melaminas"),
    WALL("textures/walls", "Paredes"),
    FLOOR("textures/floors", "Pisos"),
}

data class TextureItem(
    val id: String,
    val displayName: String,
    val assetPath: String,
    val category: TextureCategory,
)

object MaterialCatalog {
    /**
     * Converts a raw file name like "mdf-carvalho_ebano.jpg" or "piso_rovere_madera.png"
     * into a clean, human-readable commercial title like "Carvalho Ébano".
     */
    fun formatDisplayName(fileName: String): String {
        val baseName = fileName.substringBeforeLast('.')
            .removePrefix("mdf-")
            .removePrefix("mdf_")
            .removePrefix("piso_")
            .removePrefix("pared_")
            .replace('_', ' ')
            .replace('-', ' ')
        return baseName.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Discovers all image files dynamically in the specified asset directory.
     * Zero boilerplate: any new .jpg, .jpeg, or .png dropped into the folder
     * is automatically listed and available in the app.
     */
    fun loadTextures(context: Context, category: TextureCategory): List<TextureItem> {
        return try {
            val fileNames = context.assets.list(category.folder) ?: emptyArray()
            fileNames
                .filter { name ->
                    val lower = name.lowercase()
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                }
                .sorted()
                .map { name ->
                    TextureItem(
                        id = name,
                        displayName = formatDisplayName(name),
                        assetPath = "${category.folder}/$name",
                        category = category,
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
