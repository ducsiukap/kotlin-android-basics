package com.vduczz.dialogdemo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.dialogdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val ERROR_STATE = "error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setupUi(savedInstanceState)
    }

    private fun setupUi(savedState: Bundle?) {
        val error = savedState?.getString(ERROR_STATE)
        if (error != null) {
            binding.tvError.text = error
            binding.tvError.visibility = View.VISIBLE
        } else {
            binding.tvError.visibility = View.GONE
        }

        setupListeners()
        setupFragmentResultListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val error = binding.tvError.text.toString()
        outState.putString(ERROR_STATE, error)
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            var error: String? = null

            // validate
            val email = binding.etEmail.text.toString()
            error = ValidationUtils.isValidEmail(email)
                ?: ValidationUtils.isValidPassword(binding.etPassword.text.toString())
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                binding.tvError.visibility = View.GONE
                binding.tvError.text = ""

                Toast.makeText(
                    this,
                    "Login successfully with email: $email",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // show dialog
        binding.btnRegister.setOnClickListener {
            val registerDialog = RegisterDialog()

            // dialog.show(fragmentManager, tag)
            registerDialog.show(supportFragmentManager, RegisterDialog.TAG)
        }

        // show confirm dialog
        binding.btnGuestLogin.setOnClickListener {
            ConfirmCancelDialog
                .newInstance(null, "Register as a Guest?")
                .show(supportFragmentManager, ConfirmCancelDialog.TAG)

        }
    }

    private fun setupFragmentResultListeners() {
        // register fragment result listener
        supportFragmentManager.setFragmentResultListener(
            RegisterDialog.REGISTER_REQUEST_KEY, this
        ) { _, bundle ->
            val email = bundle.getString(RegisterDialog.REGISTER_RESULT_EMAIL)
            val password = bundle.getString(RegisterDialog.REGISTER_RESULT_PASSWORD)

            Toast.makeText(
                this,
                "Register successfully with email: $email, password: $password",
                Toast.LENGTH_SHORT
            ).show()

            binding.etEmail.setText(email)
            binding.etPassword.setText(password)
        }

        // confirm fragment
        supportFragmentManager.setFragmentResultListener(
            ConfirmCancelDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val isConfirm: Boolean = bundle.getBoolean(ConfirmCancelDialog.RESULT_KEY, false)
            if (isConfirm) {
                Toast.makeText(
                    this,
                    "Logged in with Guest account!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}