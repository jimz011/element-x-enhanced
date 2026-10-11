/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.room

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.room.join.JoinRule
import io.element.android.libraries.matrix.api.room.powerlevels.RoomPowerLevels
import io.element.android.libraries.matrix.api.room.tombstone.SuccessorRoom
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList

data class RoomInfo(
    val id: RoomId,
    /** The room's name from the room state event if received from sync, or one that's been computed otherwise. */
    val name: String?,
    /** Room name as defined by the room state event only. */
    val rawName: String?,
    val topic: String?,
    val avatarUrl: String?,
    val isPublic: Boolean?,
    val isDirect: Boolean,
    val isDm: Boolean,
    val isEncrypted: Boolean?,
    val joinRule: JoinRule?,
    val isSpace: Boolean,
    val isFavorite: Boolean,
    val canonicalAlias: RoomAlias?,
    val alternativeAliases: ImmutableList<RoomAlias>,
    val currentUserMembership: CurrentUserMembership,
    /**
     * Member who invited the current user to a room that's in the invited
     * state.
     *
     * Can be missing if the room membership invite event is missing from the
     * store.
     */
    val inviter: RoomMember?,
    val activeMembersCount: Long,
    val invitedMembersCount: Long,
    val joinedMembersCount: Long,
    val roomPowerLevels: RoomPowerLevels?,
    val highlightCount: Long,
    val notificationCount: Long,
    val userDefinedNotificationMode: RoomNotificationMode?,
    val hasRoomCall: Boolean,
    val activeRoomCallParticipants: ImmutableList<UserId>,
    val isMarkedUnread: Boolean,
    /**
     * "Interesting" messages received in that room, independently of the
     * notification settings.
     */
    val numUnreadMessages: Long,
    /**
     * Events that will notify the user, according to their
     * notification settings.
     */
    val numUnreadNotifications: Long,
    /**
     * Events causing mentions/highlights for the user, according to their
     * notification settings.
     */
    val numUnreadMentions: Long,
    /**
     * At most one non-self member of a DM, or empty otherwise -- see [allHeroes] for the
     * unrestricted list this is derived from.
     */
    val heroes: ImmutableList<MatrixUser>,
    /**
     * Every non-self member the SDK reports as a "hero" of the room, with no restriction on count.
     * [heroes] is deliberately narrowed to at most one entry for UI code that needs a single,
     * unambiguous DM partner (e.g. avatar/title), which throws the rest away -- some bridges
     * (observed with Signal) also join a "self-puppet" ghost representing our own account on that
     * network into a DM alongside the real contact, so [heroes] alone can't reliably identify the
     * other party there. Code that needs to consider every candidate (e.g. resolving a DM's real
     * contact for local search) should use this instead. Defaults to mirror [heroes] for callers,
     * such as most test fixtures, that only construct a single-hero DM.
     */
    val allHeroes: ImmutableList<MatrixUser> = heroes,
    val pinnedEventIds: ImmutableList<EventId>,
    val creators: ImmutableList<UserId>,
    val historyVisibility: RoomHistoryVisibility,
    val successorRoom: SuccessorRoom?,
    val roomVersion: String?,
    val privilegedCreatorRole: Boolean,
    val isLowPriority: Boolean,
    val activeCallIntentConsensus: CallIntentConsensus,
    val fullyReadEventId: EventId?,
) {
    val aliases: List<RoomAlias>
        get() = listOfNotNull(canonicalAlias) + alternativeAliases
}
