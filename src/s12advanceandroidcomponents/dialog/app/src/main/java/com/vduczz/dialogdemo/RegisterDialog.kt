package com.vduczz.dialogdemo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.vduczz.dialogdemo.databinding.DialogRegisterAccountBinding

class RegisterDialog : DialogFragment() {

    private var _binding: DialogRegisterAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())


        _binding = DialogRegisterAccountBinding.inflate(
            // inflate trên layoutInflater của Activity
            // để đồng bộ theme với activity
            requireActivity().layoutInflater
        )

        val dialog = dialogBuilder
            .setTitle("Register Account")
            .setView(binding.root)
            .setPositiveButton("Register", null)
            .setNegativeButton("Cancel") { _, _ ->
                // Handle cancel button click
                dismiss()
            }
            .create()

        // disable auto-dismiss + validate
        // khi click positive button
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                var error: String? = null

                // validate email
                val email = binding.etEmail.text.toString()
                error = ValidationUtils.isValidEmail(email)
                if (error != null) {
                    binding.tilEmail.error = error
                    binding.etEmail.requestFocus()
                    return@setOnClickListener
                }

                // validate password
                val password = binding.etPassword.text.toString()
                error = ValidationUtils.isValidPassword(password)
                if (error != null) {
                    binding.tilPassword.error = error
                    binding.etPassword.requestFocus()
                    return@setOnClickListener
                }

                // validate PASS
                // return result / call view model
                setFragmentResult(
                    REGISTER_REQUEST_KEY,       // request key
                    Bundle().apply {                // result / data
                        putString(REGISTER_RESULT_EMAIL, email)
                        putString(REGISTER_RESULT_PASSWORD, password)
                    }
                )
                // manual dismiss -> disable auto-dismiss
                dismiss()
            }
        }

        return dialog
    }

    companion object {
        const val REGISTER_REQUEST_KEY = "register_rk"
        const val REGISTER_RESULT_EMAIL = "result_email"
        const val REGISTER_RESULT_PASSWORD = "result_password"
        const val TAG = "RegisterDialog"
    }

}