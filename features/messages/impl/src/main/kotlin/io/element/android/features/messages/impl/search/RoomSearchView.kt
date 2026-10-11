/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun RoomSearchView(
    state: RoomSearchState,
    onBackClick: () -> Unit,
    onSelectResult: (EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val label = stringResource(R.string.screen_room_search_title)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(label) },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(
                state = state.query,
                placeholder = label,
                modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focusRequester).semantics { contentDescription = label },
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.results, key = { it.eventId.value }) { result ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectResult(result.eventId) }.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(result.sender, style = ElementTheme.typography.fontBodyMdMedium)
                        Text(result.timestamp, style = ElementTheme.typography.fontBodySmRegular, color = ElementTheme.colors.textSecondary)
                        Text(result.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    HorizontalDivider()
                }
                item {
                    when {
                        !state.isAvailable -> Text(stringResource(R.string.screen_room_search_unavailable), modifier = Modifier.padding(16.dp))
                        state.hasError -> {
                            Text(stringResource(R.string.error_room_search), modifier = Modifier.padding(16.dp))
                            TextButton(text = stringResource(CommonStrings.action_retry), onClick = { state.eventSink(RoomSearchEvent.Retry) })
                        }
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        state.query.text.isBlank() -> Text(stringResource(R.string.screen_room_search_hint), modifier = Modifier.padding(16.dp))
                        state.results.isEmpty() -> Text(stringResource(R.string.screen_room_search_no_results), modifier = Modifier.padding(16.dp))
                        state.canLoadMore -> TextButton(
                            text = stringResource(R.string.screen_room_search_load_more),
                            onClick = { state.eventSink(RoomSearchEvent.LoadMore) },
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@PreviewsDayNight
@Composable
internal fun RoomSearchViewPreview(@PreviewParameter(RoomSearchStatePreviewParam::class) state: RoomSearchState) = ElementPreview {
    RoomSearchView(state, onBackClick = {}, onSelectResult = {})
}
