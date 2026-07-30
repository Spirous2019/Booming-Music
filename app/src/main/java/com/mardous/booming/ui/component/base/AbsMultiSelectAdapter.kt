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

package com.mardous.booming.ui.component.base

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MenuRes
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.R

abstract class AbsMultiSelectAdapter<VH : RecyclerView.ViewHolder, I>(
    private val activity: FragmentActivity, @MenuRes protected var menuRes: Int
) : RecyclerView.Adapter<VH>(), ActionMode.Callback {

    var actionMode: ActionMode? = null
        private set
    val isInQuickSelectMode: Boolean
        get() = actionMode != null
    private val checked: MutableList<I> = ArrayList()

    private var internalCabMenu: Menu? = null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val popup = androidx.appcompat.widget.PopupMenu(activity, activity.window.decorView)
        mode.menuInflater.inflate(menuRes, popup.menu)
        internalCabMenu = popup.menu

        val moreItem = menu.add(Menu.NONE, R.id.action_cab_more, Menu.NONE, R.string.action_more)
        moreItem.setIcon(R.drawable.ic_more_vert_24dp)
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_cab_more) {
            showSelectionBottomSheet()
            return true
        }
        return false
    }

    private fun showSelectionBottomSheet() {
        val cabMenu = internalCabMenu ?: return
        val checkAllItem = cabMenu.findItem(R.id.action_multi_select_adapter_check_all)
        if (checkAllItem != null) {
            if (checked.size >= itemCount && itemCount > 0) {
                checkAllItem.title = activity.getString(R.string.deselect_all_title)
            } else {
                checkAllItem.title = activity.getString(R.string.select_all_title)
            }
        }

        val dialog = com.mardous.booming.ui.component.menu.MenuBottomSheetDialogFragment()
        dialog.setMenu(cabMenu) { selectedItemId ->
            if (selectedItemId == R.id.action_multi_select_adapter_check_all) {
                if (checked.size >= itemCount && itemCount > 0) {
                    clearChecked()
                    updateCab()
                } else {
                    checkAll()
                }
            } else {
                val menuItem = cabMenu.findItem(selectedItemId)
                if (menuItem != null) {
                    onMultipleItemAction(menuItem, ArrayList(checked))
                    actionMode?.finish()
                    clearChecked()
                }
            }
        }
        dialog.show(activity.supportFragmentManager, com.mardous.booming.ui.component.menu.MenuBottomSheetDialogFragment.TAG)
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        clearChecked()
        actionMode = null
        internalCabMenu = null
        onBackPressedCallback.remove()
    }

    private fun checkAll() {
        if (actionMode != null) {
            checked.clear()
            for (i in 0 until itemCount) {
                val identifier = getIdentifier(i)
                if (identifier != null) {
                    checked.add(identifier)
                }
            }
            notifyDataSetChanged()
            updateCab()
        }
    }

    protected abstract fun getIdentifier(position: Int): I?

    protected open fun getName(item: I): String? {
        return item.toString()
    }

    protected fun isChecked(identifier: I): Boolean {
        return checked.contains(identifier)
    }

    protected abstract fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>)

    protected fun toggleChecked(position: Int): Boolean {
        val identifier = getIdentifier(position) ?: return false
        if (!checked.remove(identifier)) {
            checked.add(identifier)
        }
        notifyItemChanged(position)
        updateCab()
        return true
    }

    private fun clearChecked() {
        checked.clear()
        notifyDataSetChanged()
    }

    private fun updateCab() {
        if (actionMode == null) {
            actionMode = activity.startActionMode(this)
            activity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
        val size = checked.size
        when {
            size <= 0 -> actionMode?.finish()
            size == 1 -> actionMode?.title = getName(checked.single())
            else -> actionMode?.title = activity.getString(R.string.x_selected, size)
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (actionMode != null) {
                actionMode?.finish()
                remove()
            }
        }
    }
}