package com.vduczz.dialogdemo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

class ConfirmCancelDialog : DialogFragment() {

    companion object {

        const val TAG = "ConfirmCancelDialog"
        const val REQUEST_KEY = "confirm_request_key"
        const val RESULT_KEY = "confirm_result_key"

        private const val ARG_TITLE = "dialog_title"
        private const val ARG_MESSAGE = "dialog_message"

        fun newInstance(
            title: String?,
            description: String
        ) = ConfirmCancelDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, description)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE)
        val message = arguments?.getString(ARG_MESSAGE) ?: "Confirm?"

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ ->
                // Handle confirm action
                setFragmentResult(REQUEST_KEY, Bundle().apply { putBoolean(RESULT_KEY, true) })
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Handle cancel action
                setFragmentResult(REQUEST_KEY, Bundle().apply { putBoolean(RESULT_KEY, false) })
            }
            .create()
    }
}