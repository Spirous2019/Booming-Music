/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.component.menu

import android.content.Context
import android.content.ContextWrapper
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu

typealias MenuConsumer = (Menu) -> Unit

fun Context.findAppCompatActivity(): AppCompatActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is AppCompatActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun newPopupMenu(anchor: View, menuRes: Int, menuConsumer: MenuConsumer? = null): PopupMenu {
    return PopupMenu(anchor.context, anchor).apply {
        inflate(menuRes)
        if (menuConsumer != null) {
            menuConsumer(menu)
        }
    }
}

abstract class OnClickMenu : View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    override fun onClick(v: View) {
        val popup = newPopupMenu(v, popupMenuRes) { menu ->
            onPreparePopup(menu)
        }
        
        val activity = v.context.findAppCompatActivity()
        if (activity != null) {
            val dialog = MenuBottomSheetDialogFragment()
                .setMenu(popup.menu) { itemId ->
                    val item = popup.menu.findItem(itemId)
                    if (item != null) {
                        onMenuItemClick(item)
                    }
                }
            setupBottomSheet(dialog)
            dialog.show(activity.supportFragmentManager, MenuBottomSheetDialogFragment.TAG)
        } else {
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    protected open fun setupBottomSheet(dialog: MenuBottomSheetDialogFragment) {}

    protected abstract val popupMenuRes: Int

    protected open fun onPreparePopup(menu: Menu) {}
}