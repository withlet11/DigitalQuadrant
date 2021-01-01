/*
 * TabAdapter.kt
 *
 * Copyright 2020 Yasuhiro Yamakawa <withlet11@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.withlet11.digitalquadrant

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabAdapter (fragment: FragmentActivity, enabled: Boolean) : FragmentStateAdapter(fragment) {
    private var listener1: QuadrantFragment? = null
    private var listener2: QuadrantFragment? = null

    var isAutoHoldEnabled = enabled
        set(value) {
            listener1?.isAutoHoldEnabledChanged(value)
            listener2?.isAutoHoldEnabledChanged(value)
            field = value
        }

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> CameralessQuadrantFragment().also {
                listener1 = it
            }
            else -> CameraQuadrantFragment().also {
                listener2 = it
            }
        }.apply {
            arguments = Bundle().apply {
                putBoolean("IS_AUTOHOLD_ENABLED", isAutoHoldEnabled)
            }
        }

    override fun getItemCount(): Int = 2
}
