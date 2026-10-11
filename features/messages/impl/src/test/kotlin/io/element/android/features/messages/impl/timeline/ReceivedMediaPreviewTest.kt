/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.factories.event.TimelineItemContentMessageFactory
import io.element.android.features.messages.impl.timeline.factories.event.isAnimatedVideoJson
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemVideoContent
import io.element.android.features.messages.impl.utils.FakeTextPillificationHelper
import io.element.android.features.messages.test.timeline.FakeHtmlConverterProvider
import io.element.android.libraries.androidutils.filesize.FakeFileSizeFormatter
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.featureflag.test.FakeFeatureFlagService
import io.element.android.libraries.htmlrenderer.test.FakeHtmlMessageParser
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.permalink.FakePermalinkParser
import io.element.android.libraries.matrix.test.timeline.aProfileDetails
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import io.element.android.libraries.mediaviewer.test.util.FileExtensionExtractorWithoutValidation
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ReceivedMediaPreviewTest : RobolectricTest() {
    private val original = MediaSource("mxc://server/original")
    private val thumbnail = MediaSource("mxc://server/thumbnail")

    @Test
    fun `a video without a supplied thumbnail downloads the original for frame extraction`() {
        val content = aTimelineItemVideoContent(mediaSource = original)
        assertThat(content.thumbnailMediaRequestData).isEqualTo(
            MediaRequestData(original, MediaRequestData.Kind.File(content.filename, MimeTypes.Mp4))
        )
    }

    @Test
    fun `a video with a supplied thumbnail uses it without downloading the full video`() {
        val content = aTimelineItemVideoContent(mediaSource = original, thumbnailMediaSource = thumbnail)
        assertThat(content.thumbnailMediaRequestData.source).isEqualTo(thumbnail)
        assertThat(content.thumbnailMediaRequestData.kind).isInstanceOf(MediaRequestData.Kind.Thumbnail::class.java)
    }

    @Test
    fun `video events with missing mime information still select the video decoder`() {
        val content = aTimelineItemVideoContent(mediaSource = original).copy(mimeType = MimeTypes.OctetStream)
        assertThat(content.thumbnailMediaRequestData.kind).isEqualTo(MediaRequestData.Kind.File(content.filename, MimeTypes.Mp4))
    }

    @Test
    fun `a gif with generic mime information loads the animation rather than a static thumbnail`() {
        val content = aTimelineItemImageContent(filename = "animation.GIF").copy(
            mimeType = MimeTypes.OctetStream,
            mediaSource = original,
            thumbnailSource = thumbnail,
        )
        assertThat(content.thumbnailMediaRequestData).isEqualTo(
            MediaRequestData(original, MediaRequestData.Kind.File(content.filename, MimeTypes.Gif))
        )
    }

    @Test
    fun `still images continue to prefer a supplied thumbnail`() {
        val content = aTimelineItemImageContent().copy(mediaSource = original, thumbnailSource = thumbnail)
        assertThat(content.thumbnailMediaRequestData.source).isEqualTo(thumbnail)
        assertThat(content.thumbnailMediaRequestData.kind).isInstanceOf(MediaRequestData.Kind.Thumbnail::class.java)
    }

    @Test
    fun `a gif received as a file attachment is rendered as an animated image`() = runTest {
        val factory = TimelineItemContentMessageFactory(
            FakeFileSizeFormatter(),
            FileExtensionExtractorWithoutValidation(),
            FakeHtmlConverterProvider(),
            FakePermalinkParser(),
            FakeTextPillificationHelper(),
            FakeHtmlMessageParser(),
            FakeFeatureFlagService(),
        )
        val result = factory.create(
            MessageContent("animation.gif", null, false, null, FileMessageType("animation.gif", null, null, original, null)),
            A_USER_ID,
            aProfileDetails(),
            null,
        ) as TimelineItemImageContent
        assertThat(result.mediaSource).isEqualTo(original)
        assertThat(result.mimeType).isEqualTo(MimeTypes.Gif)
        assertThat(result.thumbnailMediaRequestData.kind).isInstanceOf(MediaRequestData.Kind.File::class.java)
    }

    @Test
    fun `bridge gif flags enable animation but autoplay alone does not turn a video into a gif`() {
        assertThat(isAnimatedVideoJson("""{"content":{"info":{"fi.mau.gif":true}}}""")).isTrue()
        assertThat(isAnimatedVideoJson("""{"content":{"info":{"fi.mau.discord.gifv":true}}}""")).isTrue()
        assertThat(isAnimatedVideoJson("""{"content":{"m.new_content":{"info":{"fi.mau.gif":true}}}}""")).isTrue()
        assertThat(isAnimatedVideoJson("""{"content":{"info":{"fi.mau.autoplay":true}}}""")).isFalse()
        assertThat(isAnimatedVideoJson("""{"content":{"info":{"fi.mau.gif":false}}}""")).isFalse()
        assertThat(isAnimatedVideoJson("""{"content":{"info":{"fi.mau.gif":"true"}}}""")).isFalse()
        assertThat(isAnimatedVideoJson("invalid json")).isFalse()
        assertThat(isAnimatedVideoJson(null)).isFalse()
    }
}
