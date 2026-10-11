/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import coil3.fetch.SourceFetchResult
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.media.MatrixMediaLoader
import io.element.android.libraries.matrix.api.media.MediaFile
import io.element.android.libraries.matrix.api.media.MediaSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

class CoilMediaFetcherTest {
    @Test
    fun `file requests retain the mime type for video and animation decoders and close the media handle`() = runTest {
        val path = createTempFile(suffix = ".mp4")
        try {
            val mediaFile = mockk<MediaFile>(relaxed = true) { every { path() } returns path.toString() }
            val loader = mockk<MatrixMediaLoader> {
                coEvery { downloadMediaFile(any(), any(), any(), any()) } returns Result.success(mediaFile)
            }
            for (mimeType in listOf("video/mp4", "image/gif")) {
                val request = MediaRequestData(MediaSource("mxc://server/media"), MediaRequestData.Kind.File("media", mimeType))
                val result = CoilMediaFetcher(loader, request).fetch() as SourceFetchResult
                assertThat(result.mimeType).isEqualTo(mimeType)
                result.source.close()
            }
            verify(exactly = 2) { mediaFile.close() }
        } finally {
            path.deleteIfExists()
        }
    }
}
