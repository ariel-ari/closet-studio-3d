package com.closetstudio.app.ui

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class RoomGestureModeTest {
    @Test
    fun `unlocked closet hit starts container movement`() {
        var hitTestCalled = false

        val dragTarget = resolveRoomDragTarget(
            isContainerMovementLocked = false,
            canDragPartitionAt = { false },
            canDragContainerAt = {
                hitTestCalled = true
                true
            },
        )

        assertTrue(dragTarget == RoomDragTarget.CONTAINER)
        assertTrue(hitTestCalled)
    }

    @Test
    fun `locked closet hit stays available for camera orbit`() {
        var hitTestCalled = false

        var partitionHitTestCalled = false
        val dragTarget = resolveRoomDragTarget(
            isContainerMovementLocked = true,
            canDragPartitionAt = {
                partitionHitTestCalled = true
                false
            },
            canDragContainerAt = {
                hitTestCalled = true
                true
            },
        )

        assertTrue(dragTarget == RoomDragTarget.CAMERA)
        assertFalse(hitTestCalled)
        assertTrue(partitionHitTestCalled)
    }

    @Test
    fun `locked closet movement still allows internal partition drag`() {
        var containerHitTestCalled = false

        val dragTarget = resolveRoomDragTarget(
            isContainerMovementLocked = true,
            canDragPartitionAt = { true },
            canDragContainerAt = {
                containerHitTestCalled = true
                true
            },
        )

        assertTrue(dragTarget == RoomDragTarget.PARTITION)
        assertFalse(containerHitTestCalled)
    }

    @Test
    fun `partition drag keeps priority over container movement`() {
        var hitTestCalled = false

        val dragTarget = resolveRoomDragTarget(
            isContainerMovementLocked = false,
            canDragPartitionAt = { true },
            canDragContainerAt = {
                hitTestCalled = true
                true
            },
        )

        assertTrue(dragTarget == RoomDragTarget.PARTITION)
        assertFalse(hitTestCalled)
    }
}
