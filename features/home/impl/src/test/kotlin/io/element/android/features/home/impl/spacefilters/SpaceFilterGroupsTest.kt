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

class SpaceFilterGroupsTest {
    @Test
    fun `each level-1 filter is grouped under the level-0 filter it directly follows`() {
        val work = aSpaceServiceFilter(displayName = "Work", roomId = RoomId("!work:example.com"), level = 0)
        val projects = aSpaceServiceFilter(displayName = "Projects", roomId = RoomId("!projects:example.com"), level = 1)
        val personal = aSpaceServiceFilter(displayName = "Personal", roomId = RoomId("!personal:example.com"), level = 0)

        val groups = buildSpaceFilterGroups(listOf(work, projects, personal))

        assertThat(groups).hasSize(2)
        assertThat(groups[0].topLevel).isEqualTo(work)
        assertThat(groups[0].children).containsExactly(projects)
        assertThat(groups[1].topLevel).isEqualTo(personal)
        assertThat(groups[1].children).isEmpty()
    }

    @Test
    fun `a top level filter with no children in the list gets an empty children group`() {
        val solo = aSpaceServiceFilter(displayName = "Solo", level = 0)

        val groups = buildSpaceFilterGroups(listOf(solo))

        assertThat(groups).hasSize(1)
        assertThat(groups.single().children).isEmpty()
    }

    @Test
    fun `an empty filter list produces no groups`() {
        assertThat(buildSpaceFilterGroups(emptyList())).isEmpty()
    }
}
