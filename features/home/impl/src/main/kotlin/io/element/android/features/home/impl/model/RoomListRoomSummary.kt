/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.model

import androidx.compose.runtime.Immutable
import io.element.android.features.invite.api.InviteData
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.user.DisplayedStatus
import io.element.android.libraries.matrix.ui.model.InviteSender
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class RoomListRoomSummary(
    val id: String,
    val displayType: RoomSummaryDisplayType,
    val roomId: RoomId,
    val name: String?,
    val canonicalAlias: RoomAlias?,
    val numberOfUnreadMessages: Long,
    val numberOfUnreadMentions: Long,
    val numberOfUnreadNotifications: Long,
    val isMarkedUnread: Boolean,
    val timestamp: String?,
    /** Epoch millis backing [timestamp]; null when there is no event yet (e.g. an empty new room). */
    val latestEventTimestampMillis: Long?,
    val latestEvent: LatestEvent,
    val avatarData: AvatarData,
    val userDefinedNotificationMode: RoomNotificationMode?,
    val hasRoomCall: Boolean,
    val activeCallIntent: CallIntent?,
    val isDirect: Boolean,
    val isDm: Boolean,
    val isFavorite: Boolean,
    val isLowPriority: Boolean,
    val inviteSender: InviteSender?,
    val isTombstoned: Boolean,
    val heroes: ImmutableList<AvatarData>,
    val isSpace: Boolean,
    val dmUserStatus: DisplayedStatus?,
) {
    val isHighlighted = userDefinedNotificationMode != RoomNotificationMode.MUTE &&
        (numberOfUnreadNotifications > 0 || numberOfUnreadMentions > 0) ||
        isMarkedUnread

    /**
     * Whether this room shows an unread dot/badge at all. Deliberately excludes
     * [numberOfUnreadMessages]: that field counts every "interesting" event the Rust SDK has not
     * marked as read, independently of whether it would ever notify -- on the SDK version this app is
     * pinned to, that is a *broader* set than genuine notifications (it does not require the event to
     * pass the room's notification settings). The room list's own "Unread" filter chip
     * (`RoomListEntriesDynamicFilterKind.Unread`) only ever checks `numberOfUnreadNotifications` and
     * [isMarkedUnread] -- so keying this off [numberOfUnreadMessages] too made rooms show an unread dot
     * that the "Unreads" filter itself did not consider unread. Mentions still get their own indicator
     * regardless (see MentionIndicatorAtom in RoomSummaryRow), so dropping them from this gate does not
     * lose that signal.
     */
    val hasNewContent = numberOfUnreadNotifications > 0 || isMarkedUnread

    fun toInviteData() = InviteData(
        roomId = roomId,
        roomName = name ?: roomId.value,
        isDm = isDm,
    )
}
