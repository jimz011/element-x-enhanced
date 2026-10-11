/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.messages.impl.timeline.di.TimelineItemPresenterFactory
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemVideoContent
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.coroutines.awaitCancellation

data class AnimatedVideoState(val filePath: String? = null)

@AssistedInject
class AnimatedVideoPresenter(
    private val matrixClient: MatrixClient,
    @Assisted private val content: TimelineItemVideoContent,
) : Presenter<AnimatedVideoState> {
    @AssistedFactory
    fun interface Factory : TimelineItemPresenterFactory<TimelineItemVideoContent, AnimatedVideoState> {
        override fun create(content: TimelineItemVideoContent): AnimatedVideoPresenter
    }

    @Composable
    override fun present(): AnimatedVideoState {
        var filePath by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(content.mediaSource) {
            matrixClient.matrixMediaLoader.downloadMediaFile(content.mediaSource, content.mimeType, content.filename)
                .getOrNull()?.use { mediaFile ->
                    filePath = mediaFile.path()
                    // Keep the decrypted temporary file alive only while this timeline item is composed.
                    awaitCancellation()
                }
        }
        return AnimatedVideoState(filePath)
    }
}
