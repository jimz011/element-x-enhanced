/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccentColorDialogTest {
    @Test
    fun `parseHexColor accepts six digits with or without a hash`() {
        assertThat(parseHexColor("#0DBD8B")).isEqualTo(0xFF0DBD8B.toInt())
        assertThat(parseHexColor("0dbd8b")).isEqualTo(0xFF0DBD8B.toInt())
        assertThat(parseHexColor("  #FFFFFF ")).isEqualTo(0xFFFFFFFF.toInt())
    }

    @Test
    fun `parseHexColor rejects anything that is not exactly six hex digits`() {
        assertThat(parseHexColor("")).isNull()
        assertThat(parseHexColor("#FFF")).isNull()
        assertThat(parseHexColor("#0DBD8B1")).isNull()
        assertThat(parseHexColor("#GGGGGG")).isNull()
        assertThat(parseHexColor("#+0DBD8")).isNull()
    }

    @Test
    fun `toHexColorString drops the alpha channel and round trips`() {
        assertThat(0xFF0DBD8B.toInt().toHexColorString()).isEqualTo("#0DBD8B")
        assertThat(0x800000FF.toInt().toHexColorString()).isEqualTo("#0000FF")
        accentColorPresets.forEach { (_, argb) ->
            assertThat(parseHexColor(argb.toHexColorString())).isEqualTo(argb)
        }
    }
}
