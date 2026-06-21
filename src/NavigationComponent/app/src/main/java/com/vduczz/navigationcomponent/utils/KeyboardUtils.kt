package com.vduczz.navigationcomponent.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object KeyboardUtils {
    fun hideKeyboard(view: View, clearFocus: Boolean = true) {
        hideKeyboard(view)
        if (clearFocus) view.clearFocus()
    }

    fun hideKeyboard(activity: Activity, clearFocus: Boolean = true) {
        val currentFocusedView = activity.currentFocus
        if (currentFocusedView != null)
            hideKeyboard(currentFocusedView, clearFocus)
        else {
            // nếu không có view nào đang focus thì
            // chỉ ẩn phím dựa trên window token của root view
            val decorView = activity.window.decorView
            hideKeyboard(decorView)
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}