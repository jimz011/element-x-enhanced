/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spacefilters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.libraries.designsystem.atomic.atoms.UnreadIndicatorAtom
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.lazyColumnContentPadding
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceFiltersView(
    state: SpaceFiltersState,
    spaceUnreadCounts: SpaceUnreadCounts,
    onDirectMessagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelecting by rememberUpdatedState(state is SpaceFiltersState.Selecting)
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        confirmValueChange = { sheetValueTarget ->
            // This ensures the hide animation is not cancelled
            when (sheetValueTarget) {
                SheetValue.Expanded -> isSelecting
                else -> true
            }
        }
    )
    LaunchedEffect(isSelecting) {
        if (!isSelecting) {
            sheetState.hide()
        }
    }
    if (sheetState.isVisible || isSelecting) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = sheetState,
            onDismissRequest = {
                if (state is SpaceFiltersState.Selecting) {
                    state.eventSink(SpaceFiltersEvent.Selecting.Cancel)
                }
            },
            scrollable = false,
            contentWindowInsets = { scaffoldScrollableContentInsets },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                if (state is SpaceFiltersState.Selecting) {
                    SpaceFiltersBottomSheetContent(
                        filters = state.visibleFilters,
                        searchQuery = state.searchQuery,
                        selectedFilter = state.selectedFilter,
                        spaceUnreadCounts = spaceUnreadCounts,
                        onSelectFilter = { filter ->
                            state.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(filter))
                        },
                        onAllChatsClick = {
                            state.eventSink(SpaceFiltersEvent.Selecting.ClearSelection)
                        },
                        onDirectMessagesClick = {
                            state.eventSink(SpaceFiltersEvent.Selecting.ClearSelection)
                            onDirectMessagesClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceFiltersBottomSheetContent(
    filters: ImmutableList<SpaceServiceFilter>,
    searchQuery: TextFieldState,
    selectedFilter: SpaceServiceFilter?,
    spaceUnreadCounts: SpaceUnreadCounts,
    onSelectFilter: (SpaceServiceFilter) -> Unit,
    onAllChatsClick: () -> Unit,
    onDirectMessagesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The fixed destinations and the collapsible grouping only make sense while browsing the
    // hierarchy; a search instead flattens straight to a simple, ungrouped match list.
    val isSearching = searchQuery.text.isNotBlank()
    val groups = remember(filters) { buildSpaceFilterGroups(filters) }
    // Collapsed by default, like classic. Re-expand the group the current selection lives in so
    // reopening the sheet doesn't hide it. Indices, not room ids, so this never depends on room ids
    // being unique (they always are in practice, but nothing here should assume it).
    var expandedGroups by remember(groups) {
        val selectedGroupIndex = selectedFilter?.let { selected ->
            groups.indexOfFirst { group -> group.children.any { it.spaceRoom.roomId == selected.spaceRoom.roomId } }
        }?.takeIf { it >= 0 }
        mutableStateOf(setOfNotNull(selectedGroupIndex))
    }
    Column(
        modifier = modifier.padding(top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_roomlist_your_spaces),
            style = ElementTheme.typography.fontHeadingSmMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SearchField(
            state = searchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = stringResource(CommonStrings.action_search),
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            contentPadding = lazyColumnContentPadding,
        ) {
            if (isSearching) {
                items(filters) { filter ->
                    SpaceFilterItem(
                        filter = filter,
                        unreadCount = spaceUnreadCounts.countFor(filter),
                        onClick = { onSelectFilter(filter) }
                    )
                }
            } else {
                item(key = "all_chats") {
                    SpaceDestinationItem(
                        icon = CompoundIcons.Home(),
                        label = stringResource(R.string.screen_roomlist_classic_all_chats),
                        unreadCount = spaceUnreadCounts.totalUnreadRoomCount,
                        onClick = onAllChatsClick,
                    )
                }
                item(key = "direct_messages") {
                    SpaceDestinationItem(
                        icon = CompoundIcons.User(),
                        label = stringResource(R.string.screen_roomlist_classic_direct_messages),
                        unreadCount = 0,
                        onClick = onDirectMessagesClick,
                    )
                }
                if (groups.isNotEmpty()) {
                    item(key = "spaces_header") {
                        SpaceSectionHeader(text = stringResource(R.string.screen_roomlist_classic_section_spaces))
                    }
                }
                groups.forEachIndexed { index, group ->
                    item {
                        SpaceGroupRow(
                            group = group,
                            isExpanded = index in expandedGroups,
                            unreadCount = spaceUnreadCounts.countFor(group.topLevel),
                            onToggleExpand = {
                                expandedGroups = if (index in expandedGroups) {
                                    expandedGroups - index
                                } else {
                                    expandedGroups + index
                                }
                            },
                            onClick = { onSelectFilter(group.topLevel) },
                        )
                    }
                    if (index in expandedGroups) {
                        items(group.children) { child ->
                            SpaceFilterItem(
                                filter = child,
                                unreadCount = spaceUnreadCounts.countFor(child),
                                onClick = { onSelectFilter(child) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A top level space together with its direct child spaces (the only two levels the space sheet
 * supports), in the order [filters] gave them -- which is the order [buildSpaceFilterGroups] relies
 * on: every level-1 filter directly follows its level-0 parent.
 */
internal data class SpaceFilterGroup(
    val topLevel: SpaceServiceFilter,
    val children: ImmutableList<SpaceServiceFilter>,
)

internal fun buildSpaceFilterGroups(filters: List<SpaceServiceFilter>): ImmutableList<SpaceFilterGroup> {
    val groups = mutableListOf<SpaceFilterGroup>()
    var index = 0
    while (index < filters.size) {
        val topLevel = filters[index]
        index++
        val children = mutableListOf<SpaceServiceFilter>()
        while (index < filters.size && filters[index].level != 0) {
            children.add(filters[index])
            index++
        }
        groups.add(SpaceFilterGroup(topLevel, children.toImmutableList()))
    }
    return groups.toImmutableList()
}

@Composable
private fun SpaceSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = ElementTheme.typography.fontBodySmMedium,
        color = ElementTheme.colors.textSecondary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** A fixed destination such as "All chats", which is not backed by a space. */
@Composable
private fun SpaceDestinationItem(
    icon: ImageVector,
    label: String,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElementTheme.colors.iconPrimary,
            modifier = Modifier.size(AvatarSize.RoomSelectRoomListItem.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpaceUnreadCountBadge(count = unreadCount)
    }
}

/**
 * A top level space's row in the sheet: tapping the row itself selects the space (and, per
 * [io.element.android.features.home.impl.spacefilters.withRecursiveDescendants], every chat in its
 * child spaces too); the chevron -- shown only when it has children -- independently expands or
 * collapses them, the way classic's space panel does.
 */
@Composable
private fun SpaceGroupRow(
    group: SpaceFilterGroup,
    isExpanded: Boolean,
    unreadCount: Int,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spaceRoom = group.topLevel.spaceRoom
    val supportingText = spaceRoom.canonicalAlias?.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = spaceRoom.getAvatarData(AvatarSize.RoomSelectRoomListItem),
            avatarType = AvatarType.Space(),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spaceRoom.displayName,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SpaceUnreadCountBadge(count = unreadCount)
        if (group.children.isNotEmpty()) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) CompoundIcons.ChevronUp() else CompoundIcons.ChevronDown(),
                    tint = ElementTheme.colors.iconSecondary,
                    contentDescription = if (isExpanded) {
                        stringResource(R.string.a11y_collapse_space)
                    } else {
                        stringResource(R.string.a11y_expand_space)
                    },
                )
            }
        }
    }
}

@Composable
private fun SpaceFilterItem(
    filter: SpaceServiceFilter,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spaceRoom = filter.spaceRoom
    val supportingText = spaceRoom.canonicalAlias?.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Level-based indentation
        Spacer(modifier = Modifier.width((16 * filter.level).dp))
        Avatar(
            avatarData = spaceRoom.getAvatarData(AvatarSize.RoomSelectRoomListItem),
            avatarType = AvatarType.Space(),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spaceRoom.displayName,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SpaceUnreadCountBadge(count = unreadCount)
    }
}

@Composable
private fun SpaceUnreadCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    UnreadIndicatorAtom(
        count = count.toLong(),
        contentDescription = stringResource(R.string.a11y_unread_rooms_count, count),
        modifier = modifier.padding(start = 8.dp),
    )
}

@PreviewsDayNight
@Composable
internal fun SpaceFiltersViewPreview(@PreviewParameter(SpaceFiltersStatePreviewParam::class) state: SpaceFiltersState) = ElementPreview(fillMaxSize = true) {
    SpaceFiltersView(
        state = state,
        spaceUnreadCounts = emptySpaceUnreadCounts(),
        onDirectMessagesClick = {},
    )
}

@PreviewsDayNight
@Composable
internal fun SpaceFiltersViewWithUnreadCountsPreview() = ElementPreview(fillMaxSize = true) {
    SpaceFiltersView(
        state = aSelectingSpaceFiltersState(),
        spaceUnreadCounts = SpaceUnreadCounts(
            totalUnreadRoomCount = 5,
            countsBySpaceRoomId = persistentMapOf(
                RoomId("!space:example.com") to 3, // "Work"
                RoomId("!personal:example.com") to 2, // "Personal"
            ),
        ),
        onDirectMessagesClick = {},
    )
}
