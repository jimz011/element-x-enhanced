/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

object RoomListConfig {
    const val SHOW_INVITE_MENU_ITEM = false
    const val SHOW_REPORT_PROBLEM_MENU_ITEM = false
    // Classic keeps a "Mark all as read" entry in the room list's overflow menu.
    const val SHOW_MARK_ALL_AS_READ_MENU_ITEM = true

    const val HAS_DROP_DOWN_MENU = SHOW_INVITE_MENU_ITEM || SHOW_REPORT_PROBLEM_MENU_ITEM || SHOW_MARK_ALL_AS_READ_MENU_ITEM
}
