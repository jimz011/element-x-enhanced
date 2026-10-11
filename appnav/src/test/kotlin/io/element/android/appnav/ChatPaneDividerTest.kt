/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.appnav

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class ChatPaneDividerTest : RobolectricTest() {
    @Test
    fun `drag reports physical right and left movement even in RTL`() = runAndroidComposeUiTest<ComponentActivity> {
        var totalDelta = 0f
        setContent {
            ElementPreview {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ChatPaneDivider(0.4f, 0.3f..0.7f, {}, { totalDelta += it })
                    }
                }
            }
        }
        val handle = onNodeWithContentDescription("Resize chat list and conversation")
        handle.performTouchInput { swipe(center, center + Offset(100f, 0f)) }
        runOnIdle { assertThat(totalDelta).isGreaterThan(0f) }
        runOnIdle { totalDelta = 0f }
        handle.performTouchInput { swipe(center, center - Offset(100f, 0f)) }
        runOnIdle { assertThat(totalDelta).isLessThan(0f) }
    }

    @Test
    fun `accessibility resize action clamps the fraction to the allowed range`() = runAndroidComposeUiTest<ComponentActivity> {
        var selectedFraction = 0.4f
        setContent {
            ElementPreview {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ChatPaneDivider(0.4f, 0.3f..0.7f, { selectedFraction = it }, {})
                }
            }
        }
        onNodeWithContentDescription("Resize chat list and conversation").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle { assertThat(selectedFraction).isEqualTo(0.7f) }
    }
}
