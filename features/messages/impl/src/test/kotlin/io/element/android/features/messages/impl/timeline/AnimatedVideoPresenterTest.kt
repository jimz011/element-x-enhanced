/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.components.event.AnimatedVideoPresenter
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemVideoContent
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaFile
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class AnimatedVideoPresenterTest : RobolectricTest() {
    @Test
    fun `the decrypted animation file stays alive until the item leaves composition`() = runTest {
        val mediaFile = mockk<MediaFile>(relaxed = true) { every { path() } returns "/tmp/animation.mp4" }
        val loader = mockk<MatrixMediaLoader> {
            coEvery { downloadMediaFile(any(), any(), any(), any()) } returns Result.success(mediaFile)
        }
        val client = mockk<MatrixClient> { every { matrixMediaLoader } returns loader }
        val presenter = AnimatedVideoPresenter(client, aTimelineItemVideoContent().copy(isAnimated = true))

        presenter.test {
            val state = consumeItemsUntilPredicate { it.filePath != null }.last()
            assertThat(state.filePath).isEqualTo("/tmp/animation.mp4")
            verify(exactly = 0) { mediaFile.close() }
        }
        verify(exactly = 1) { mediaFile.close() }
    }

    @Test
    fun `a failed download leaves the thumbnail available instead of starting playback`() = runTest {
        val loader = mockk<MatrixMediaLoader> {
            coEvery { downloadMediaFile(any(), any(), any(), any()) } returns Result.failure(IllegalStateException())
        }
        val client = mockk<MatrixClient> { every { matrixMediaLoader } returns loader }
        AnimatedVideoPresenter(client, aTimelineItemVideoContent().copy(isAnimated = true)).test {
            assertThat(awaitItem().filePath).isNull()
        }
    }
}
