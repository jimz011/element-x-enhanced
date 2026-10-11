/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.OnLifecycleEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/** The header above each classic room list section, e.g. "Favourites". */
@Composable
fun ClassicSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = ElementTheme.typography.fontBodyMdMedium,
        color = ElementTheme.colors.textSecondary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun ClassicQuickAccessView(
    rooms: ImmutableList<RoomListRoomSummary>,
    onRoomClick: (RoomListRoomSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rooms.isEmpty()) return

    // Classic always shows this row starting from the most recent chat, so a back action landing back
    // on the home screen -- from a room, from Settings, anywhere -- resets it to the start rather than
    // leaving it wherever the user last scrolled it. The home screen's own Appyx node is what gets
    // paused/resumed by that backstack navigation, which is what ON_RESUME here is observing.
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            coroutineScope.launch { lazyListState.scrollToItem(0) }
        }
    }

    Column(modifier = modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.screen_roomlist_classic_quick_access),
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = rooms,
                key = { it.roomId.value },
            ) { room ->
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onRoomClick(room) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(
                        avatarData = room.avatarData,
                        avatarType = if (room.isSpace) {
                            AvatarType.Space(isTombstoned = room.isTombstoned)
                        } else {
                            AvatarType.Room(
                                heroes = room.heroes,
                                isTombstoned = room.isTombstoned,
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = room.name ?: room.roomId.value,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
