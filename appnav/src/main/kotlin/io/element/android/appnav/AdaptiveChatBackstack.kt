/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bumble.appyx.core.navigation.transition.JumpToEndTransitionHandler
import com.bumble.appyx.navmodel.backstack.BackStack
import io.element.android.libraries.architecture.appyx.SafeChildren

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun LoggedInFlowNode.AdaptiveChatBackstack() {
    val elements by backstack.elements.collectAsState()
    val active = elements.lastOrNull { it.targetState == BackStack.State.ACTIVE }?.key?.navTarget
    val hasHome = elements.any { it.key.navTarget == LoggedInFlowNode.NavTarget.Home }
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val density = LocalDensity.current
    var listFraction by rememberSaveable { mutableStateOf<Float?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() },
    ) {
        val hinge = adaptiveInfo.windowPosture.hingeList.firstOrNull { it.isVertical && (it.isSeparating || it.isOccluding) }
        val panes = calculateChatPaneLayout(
            width = maxWidth,
            height = maxHeight,
            preferredListFraction = listFraction,
            hingeOccluding = hinge?.isOccluding ?: false,
            tabletop = adaptiveInfo.windowPosture.isTabletop,
            hingeLeft = hinge?.let { with(density) { (it.bounds.left - origin.x).toDp() } },
            hingeRight = hinge?.let { with(density) { (it.bounds.right - origin.x).toDp() } },
        ).takeIf { hasHome && (active == LoggedInFlowNode.NavTarget.Home || active is LoggedInFlowNode.NavTarget.Room) }

        SideEffect {
            additionalVisibleTargets?.value = if (panes != null) setOf(LoggedInFlowNode.NavTarget.Home) else emptySet()
        }
        DisposableEffect(Unit) {
            onDispose { additionalVisibleTargets?.value = emptySet() }
        }

        if (panes != null && active == LoggedInFlowNode.NavTarget.Home) {
            ChatDetailPlaceholder(
                modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = panes.detailStart).width(maxWidth - panes.detailStart).fillMaxHeight(),
            )
        }
        val transitionHandler = if (panes != null) {
            remember { JumpToEndTransitionHandler<LoggedInFlowNode.NavTarget, BackStack.State>() }
        } else {
            rememberLoggedInFlowTransitionHandler(backstack)
        }
        SafeChildren(navModel = displayedBackstack, modifier = Modifier.fillMaxSize(), transitionHandler = transitionHandler) {
            children<LoggedInFlowNode.NavTarget> { child, descriptor ->
                val isHome = descriptor.element == LoggedInFlowNode.NavTarget.Home
                val paneModifier = when {
                    panes == null -> Modifier.fillMaxSize()
                    isHome -> Modifier.width(panes.listWidth).fillMaxHeight()
                    else -> Modifier.absoluteOffset(x = panes.detailStart).width(maxWidth - panes.detailStart).fillMaxHeight()
                }
                Box(modifier = paneModifier.align(AbsoluteAlignment.TopLeft)) { child(Modifier.fillMaxSize()) }
            }
        }
        if (panes != null && (panes.detailStart == panes.listWidth || hinge?.isOccluding != true)) {
            ChatPaneDivider(
                fraction = panes.listWidth / maxWidth,
                fractionRange = (280.dp / maxWidth)..((maxWidth - 320.dp) / maxWidth),
                onFractionChange = { listFraction = it },
                onDrag = { delta ->
                    val currentWidth = listFraction?.let { maxWidth * it } ?: panes.listWidth
                    listFraction = resizedChatListFraction(maxWidth, currentWidth, with(density) { delta.toDp() })
                },
                modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = panes.listWidth - 24.dp),
            )
        }
    }
}
