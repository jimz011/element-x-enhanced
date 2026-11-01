/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

sealed interface PreferencesRootEvent {
    data object OnVersionInfoClick : PreferencesRootEvent
    data object ToggleOtherAccountsExpanded : PreferencesRootEvent
    data class SetTheme(val theme: ThemeOption) : PreferencesRootEvent

    /** Sets the accent color of the app, as an ARGB int, or `null` to go back to the default one. */
    data class SetAccentColor(val color: Int?) : PreferencesRootEvent
}
