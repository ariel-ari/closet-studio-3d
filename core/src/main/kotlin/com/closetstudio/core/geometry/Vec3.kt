package com.closetstudio.core.geometry

/** Immutable point/vector in the millimetre domain. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)

    val length: Float get() = kotlin.math.sqrt(x * x + y * y + z * z)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UNIT_X = Vec3(1f, 0f, 0f)
        val UNIT_Y = Vec3(0f, 1f, 0f)
        val UNIT_Z = Vec3(0f, 0f, 1f)
    }
}

/** Axis aligned bounding box, used for culling and for framing the camera. */
data class Aabb(val min: Vec3, val max: Vec3) {
    val size: Vec3 get() = max - min
    val center: Vec3 get() = (min + max) * 0.5f

    companion object {
        val EMPTY = Aabb(
            Vec3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            Vec3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
        )
    }
}
