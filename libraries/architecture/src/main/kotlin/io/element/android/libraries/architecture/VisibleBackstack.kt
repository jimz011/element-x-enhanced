/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.architecture

import com.bumble.appyx.core.navigation.NavModel
import com.bumble.appyx.core.navigation.NavModelAdapter
import com.bumble.appyx.core.plugin.Destroyable
import com.bumble.appyx.navmodel.backstack.BackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Keeps an extra pane resumed without changing the back stack or its Back behaviour. */
internal class VisibleBackstack<T : Any>(
    private val backstack: BackStack<T>,
    visibleTargets: StateFlow<Set<T>>,
) : NavModel<T, BackStack.State> by backstack, Destroyable {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    override val screenState = combine(backstack.screenState, visibleTargets) { state, targets ->
        val extra = state.offScreen.filter { it.key.navTarget in targets && it.targetState == BackStack.State.STASHED }
        NavModelAdapter.ScreenState(
            onScreenWithVisibleTargetState = state.onScreenWithVisibleTargetState + extra,
            onScreen = state.onScreen + extra,
            offScreen = state.offScreen - extra.toSet(),
        )
    }.stateIn(scope, SharingStarted.Eagerly, backstack.screenState.value)

    override fun destroy() {
        scope.cancel()
        backstack.destroy()
    }
}
