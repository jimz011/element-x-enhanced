/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Immutable
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The sections Element Classic splits the room list into, in the order it displays them. [titleRes]
 * is null for [Rooms]: that is the default, catch-all bucket (every ordinary chat, DM or group room
 * alike, mixed together by recency like classic's own "All chats" list), so it never gets a header --
 * only the two special-cased buckets, Invites and Low priority, do.
 */
enum class ClassicRoomSection(val titleRes: Int?) {
    Invites(R.string.screen_roomlist_filter_invites),
    Rooms(null),
    LowPriority(R.string.screen_roomlist_classic_section_low_priority),
}

@Immutable
data class ClassicRoomGroup(
    val section: ClassicRoomSection,
    val rooms: ImmutableList<RoomListRoomSummary>,
)

/**
 * Classic assigns each room to exactly one section, and the order of these checks is what decides
 * ties: an invite is always an invite, and low priority always sinks to the bottom even for a
 * favourited room. DMs are not split into their own section here -- that was this fork's own
 * addition, not something classic's default "All chats" view does; the People/Rooms distinction is a
 * filter chip choice, not automatic grouping. A favourited room shows in its normal section too --
 * the Favourites filter chip above the list is the only favourites entry point.
 */
internal fun RoomListRoomSummary.classicSection(): ClassicRoomSection = when {
    displayType == RoomSummaryDisplayType.INVITE -> ClassicRoomSection.Invites
    // Placeholders carry no metadata yet, so they trail the Rooms section as loading skeletons.
    displayType == RoomSummaryDisplayType.PLACEHOLDER -> ClassicRoomSection.Rooms
    isLowPriority -> ClassicRoomSection.LowPriority
    else -> ClassicRoomSection.Rooms
}

/**
 * Groups [summaries] into the classic sections, preserving the order the room list gave us within
 * each section -- which is why an unfiltered "All chats" list ends up sorted by recency: that is the
 * order the room list data source itself returns rooms in. Empty sections are dropped.
 */
internal fun buildClassicRoomGroups(summaries: List<RoomListRoomSummary>): ImmutableList<ClassicRoomGroup> {
    if (summaries.isEmpty()) return persistentListOf()
    val roomsBySection = summaries.groupBy { it.classicSection() }
    return ClassicRoomSection.entries
        .mapNotNull { section ->
            roomsBySection[section]
                ?.takeIf { it.isNotEmpty() }
                ?.let { rooms -> ClassicRoomGroup(section = section, rooms = rooms.toImmutableList()) }
        }
        .toImmutableList()
}
