/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeVideo
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MAX_THUMBNAIL_HEIGHT
import io.element.android.libraries.matrix.ui.media.MAX_THUMBNAIL_WIDTH
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlin.time.Duration

data class TimelineItemVideoContent(
    override val filename: String,
    override val fileSize: Long?,
    override val caption: String?,
    override val formattedCaption: CharSequence?,
    override val htmlCaption: String? = null,
    override val isEdited: Boolean,
    val duration: Duration,
    override val mediaSource: MediaSource,
    val thumbnailSource: MediaSource?,
    val aspectRatio: Float?,
    val blurHash: String?,
    val height: Int?,
    val width: Int?,
    val thumbnailWidth: Int?,
    val thumbnailHeight: Int?,
    override val mimeType: String,
    override val formattedFileSize: String,
    override val fileExtension: String,
    val isAnimated: Boolean = false,
) : TimelineItemEventContentWithAttachment {
    override val type: String = "TimelineItemImageContent"

    val showCaption = caption != null

    val thumbnailMediaRequestData: MediaRequestData
        get() = if (thumbnailSource != null) {
            MediaRequestData(
                source = thumbnailSource,
                kind = MediaRequestData.Kind.Thumbnail(
                    width = thumbnailWidth?.toLong() ?: MAX_THUMBNAIL_WIDTH,
                    height = thumbnailHeight?.toLong() ?: MAX_THUMBNAIL_HEIGHT,
                ),
            )
        } else {
            // Fetch through the session media loader so encrypted videos can also yield a preview frame.
            MediaRequestData(mediaSource, MediaRequestData.Kind.File(filename, mimeType.takeIf { it.isMimeTypeVideo() } ?: MimeTypes.Mp4))
        }
}
