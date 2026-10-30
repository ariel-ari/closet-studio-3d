package com.closetstudio.core.geometry

import com.closetstudio.core.model.PanelSpec
import com.closetstudio.core.model.RoomSpec

/** Material group a generated mesh belongs to. */
enum class Surface {
    /** Melamine board: sides, top, bottom, shelves. */
    CARCASS,
    /** Thin back panel. */
    BACK,
    /** Doors and drawer fronts. */
    FRONT,
    /** Recessed plinth. */
    TOE_KICK,
}

data class Part(val surface: Surface, val mesh: MeshData)

data class ShellGeometry(val parts: List<Part>) {
    fun meshFor(surface: Surface): MeshData? = parts.firstOrNull { it.surface == surface }?.mesh

    fun bounds(): Aabb {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (part in parts) {
            val b = part.mesh.bounds()
            if (b.min.x < minX) minX = b.min.x
            if (b.min.y < minY) minY = b.min.y
            if (b.min.z < minZ) minZ = b.min.z
            if (b.max.x > maxX) maxX = b.max.x
            if (b.max.y > maxY) maxY = b.max.y
            if (b.max.z > maxZ) maxZ = b.max.z
        }
        return Aabb(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
    }
}

/**
 * Generates the empty cabinet envelope.
 *
 * Coordinate system: origin at the back-left floor corner, X towards the right
 * wall, Y up, Z towards the room (front of the closet). Everything in mm.
 *
 * The envelope deliberately omits doors and shelves: those come from the module
 * layout, so the shell can be cached and reused when the interior changes.
 */
object ClosetShell {

    fun build(room: RoomSpec, panels: PanelSpec, uvScaleMm: Float = 600f): ShellGeometry {
        val width = room.clearWidthMm
        val height = room.clearHeightMm
        val depth = room.clearDepthMm
        val t = panels.thicknessMm
        val back = panels.backThicknessMm
        val kick = panels.toeKickHeightMm
        val kickInset = panels.toeKickInsetMm

        require(width > 2f * t) { "room width $width mm is too narrow for $t mm panels" }
        require(height > kick + 2f * t) { "room height $height mm is too low for a toe kick of $kick mm" }
        require(depth > back + t && depth > kickInset + t) {
            "room depth $depth mm cannot fit the back panel and toe kick recess"
        }

        val parts = ArrayList<Part>(4)

        // Left and right sides run from the floor to the top panel.
        val carcass = MeshBuilder(uvScaleMm)
        carcass.addBox(Vec3(0f, 0f, 0f), Vec3(t, height, depth))
        carcass.addBox(Vec3(width - t, 0f, 0f), Vec3(width, height, depth))
        // Top panel spans between the sides.
        carcass.addBox(Vec3(t, height - t, 0f), Vec3(width - t, height, depth))
        // Bottom panel sits above the toe kick so the plinth reads as recessed.
        carcass.addBox(Vec3(t, kick, 0f), Vec3(width - t, kick + t, depth))
        parts += Part(Surface.CARCASS, carcass.build())

        val backBuilder = MeshBuilder(uvScaleMm)
        backBuilder.addBox(Vec3(0f, 0f, 0f), Vec3(width, height, back))
        parts += Part(Surface.BACK, backBuilder.build())

        val kickFront = depth - kickInset
        if (kick > 0f && kickFront - t > back) {
            val kickBuilder = MeshBuilder(uvScaleMm)
            kickBuilder.addBox(Vec3(t, 0f, kickFront - t), Vec3(width - t, kick, kickFront))
            parts += Part(Surface.TOE_KICK, kickBuilder.build())
        }

        return ShellGeometry(parts)
    }
}
