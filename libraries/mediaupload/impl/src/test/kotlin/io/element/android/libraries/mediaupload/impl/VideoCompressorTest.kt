/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaupload.impl

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class VideoCompressorTest : RobolectricTest() {
    private lateinit var transformerThread: Thread
    private val transformerDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "video-transformer").also { transformerThread = it }
    }.asCoroutineDispatcher()
    private val transformer = mockk<Transformer>(relaxed = true)
    private val listener = slot<Transformer.Listener>()
    private val started = CompletableDeferred<Unit>()
    private var completeOnStart = false

    @Before
    fun setUp() {
        Dispatchers.setMain(transformerDispatcher)
        mockkConstructor(Transformer.Builder::class)
        every { anyConstructed<Transformer.Builder>().addListener(capture(listener)) } answers { self as Transformer.Builder }
        every { anyConstructed<Transformer.Builder>().build() } returns transformer
        every { transformer.start(any<Composition>(), any<String>()) } answers {
            assertTransformerThread()
            started.complete(Unit)
            if (completeOnStart) {
                listener.captured.onCompleted(firstArg(), ExportResult.Builder().build())
            }
        }
        every { transformer.getProgress(any()) } answers {
            assertTransformerThread()
            Transformer.PROGRESS_STATE_NOT_STARTED
        }
        every { transformer.cancel() } answers { assertTransformerThread() }
    }

    @After
    fun tearDown() {
        unmockkConstructor(Transformer.Builder::class)
        Dispatchers.resetMain()
        transformerDispatcher.close()
    }

    @Test
    fun `completion collected on a background thread cleans up on the transformer thread`() = runBlocking<Unit>(Dispatchers.Default) {
        completeOnStart = true
        val result = compressor().compress(Uri.EMPTY, VideoCompressionPreset.STANDARD)
            .filterIsInstance<VideoTranscodingEvent.Completed>()
            .first()

        assertThat(result.file.extension).isEqualTo("mp4")
        verify(exactly = 1) { transformer.cancel() }
        result.file.delete()
    }

    @Test
    fun `cancellation from a background thread stops the transformer on its thread`() = runBlocking(Dispatchers.Default) {
        val job = launch {
            compressor().compress(Uri.EMPTY, VideoCompressionPreset.STANDARD).collect()
        }
        started.await()
        job.cancelAndJoin()

        verify(exactly = 1) { transformer.cancel() }
    }

    private fun compressor() = VideoCompressor(InstrumentationRegistry.getInstrumentation().context)

    private fun assertTransformerThread() {
        assertThat(Thread.currentThread()).isSameInstanceAs(transformerThread)
    }
}
