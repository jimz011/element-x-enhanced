/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import org.junit.Test

class SpaceFilterAggregationTest {
    @Test
    fun `a top level space gains the flattened descendants of its child spaces`() {
        val directRoom = RoomId("!direct:example.com")
        val childSpaceId = RoomId("!childSpace:example.com")
        val nestedRoom = RoomId("!nested:example.com")

        val topLevel = aSpaceServiceFilter(
            roomId = RoomId("!top:example.com"),
            level = 0,
            // As per the Rust SDK, a level-0 filter's own descendants are its direct children only,
            // which for a subspace child is the subspace's own room id, not the rooms inside it.
            descendants = listOf(directRoom, childSpaceId),
        )
        val childSpace = aSpaceServiceFilter(
            roomId = childSpaceId,
            level = 1,
            // Level-1 filters are already fully flattened by the SDK.
            descendants = listOf(nestedRoom),
        )
        val otherTopLevel = aSpaceServiceFilter(roomId = RoomId("!other:example.com"), level = 0)

        val result = topLevel.withRecursiveDescendants(listOf(topLevel, childSpace, otherTopLevel))

        assertThat(result.descendants).containsExactly(directRoom, childSpaceId, nestedRoom)
    }

    @Test
    fun `a level 1 filter is returned unchanged, it is already fully flattened`() {
        val childSpace = aSpaceServiceFilter(level = 1, descendants = listOf(RoomId("!nested:example.com")))

        val result = childSpace.withRecursiveDescendants(listOf(childSpace))

        assertThat(result).isSameInstanceAs(childSpace)
    }

    @Test
    fun `a top level space with no children in the list is returned unchanged`() {
        val topLevel = aSpaceServiceFilter(level = 0, descendants = listOf(RoomId("!direct:example.com")))

        val result = topLevel.withRecursiveDescendants(listOf(topLevel))

        assertThat(result).isSameInstanceAs(topLevel)
    }

    @Test
    fun `children of a different top level space are not mixed in`() {
        val firstDirectRoom = RoomId("!firstDirect:example.com")
        val firstChildSpaceId = RoomId("!firstChild:example.com")
        val firstNestedRoom = RoomId("!firstNested:example.com")
        val secondNestedRoom = RoomId("!secondNested:example.com")

        val first = aSpaceServiceFilter(roomId = RoomId("!first:example.com"), level = 0, descendants = listOf(firstDirectRoom, firstChildSpaceId))
        val firstChild = aSpaceServiceFilter(roomId = firstChildSpaceId, level = 1, descendants = listOf(firstNestedRoom))
        val second = aSpaceServiceFilter(roomId = RoomId("!second:example.com"), level = 0, descendants = listOf(RoomId("!secondChild:example.com")))
        val secondChild = aSpaceServiceFilter(roomId = RoomId("!secondChild:example.com"), level = 1, descendants = listOf(secondNestedRoom))

        val result = first.withRecursiveDescendants(listOf(first, firstChild, second, secondChild))

        assertThat(result.descendants).containsExactly(firstDirectRoom, firstChildSpaceId, firstNestedRoom)
    }

    @Test
    fun `duplicate room ids between the parent and a child are not repeated`() {
        val sharedRoom = RoomId("!shared:example.com")
        val childSpaceId = RoomId("!child:example.com")

        val topLevel = aSpaceServiceFilter(roomId = RoomId("!top:example.com"), level = 0, descendants = listOf(sharedRoom, childSpaceId))
        val childSpace = aSpaceServiceFilter(roomId = childSpaceId, level = 1, descendants = listOf(sharedRoom))

        val result = topLevel.withRecursiveDescendants(listOf(topLevel, childSpace))

        assertThat(result.descendants).containsExactly(sharedRoom, childSpaceId)
    }
}
