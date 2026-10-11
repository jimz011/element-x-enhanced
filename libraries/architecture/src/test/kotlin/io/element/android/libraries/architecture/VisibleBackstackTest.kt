/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.architecture

import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.pop
import com.bumble.appyx.navmodel.backstack.operation.push
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class VisibleBackstackTest {
    @Test
    fun `unfolding makes home visible without changing history and folding hides it again`() {
        val backstack = BackStack(initialElement = "home", savedStateMap = null)
        val visibleTargets = MutableStateFlow(emptySet<String>())
        val display = VisibleBackstack(backstack, visibleTargets)
        try {
            backstack.push("room")
            backstack.onTransitionFinished(backstack.elements.value.map { it.key })
            val originalElements = backstack.elements.value
            visibleTargets.value = setOf("home")
            assertThat(display.screenState.value.onScreen.map { it.key.navTarget }).containsExactly("room", "home")
            assertThat(display.screenState.value.onScreenWithVisibleTargetState.map { it.key.navTarget }).containsExactly("room", "home")
            assertThat(backstack.elements.value).isEqualTo(originalElements)
            visibleTargets.value = emptySet()
            assertThat(display.screenState.value.onScreen.map { it.key.navTarget }).containsExactly("room")
            backstack.pop()
            backstack.onTransitionFinished(backstack.elements.value.map { it.key })
            assertThat(display.screenState.value.onScreen.map { it.key.navTarget }).containsExactly("home")
        } finally {
            display.destroy()
        }
    }

    @Test
    fun `destroyed rooms are never revived by extra visibility`() {
        val backstack = BackStack(initialElement = "home", savedStateMap = null)
        val visibleTargets = MutableStateFlow(setOf("room"))
        val display = VisibleBackstack(backstack, visibleTargets)
        try {
            backstack.push("room")
            backstack.onTransitionFinished(backstack.elements.value.map { it.key })
            backstack.pop()
            backstack.onTransitionFinished(backstack.elements.value.map { it.key })
            assertThat(display.screenState.value.onScreen.map { it.key.navTarget }).containsExactly("home")
            assertThat(backstack.elements.value.map { it.key.navTarget }).containsExactly("home")
        } finally {
            display.destroy()
        }
    }
}
