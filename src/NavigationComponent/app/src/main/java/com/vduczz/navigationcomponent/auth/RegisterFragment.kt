package com.vduczz.navigationcomponent.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.vduczz.navigationcomponent.R
import com.vduczz.navigationcomponent.databinding.FragmentRegisterBinding
import com.vduczz.navigationcomponent.utils.KeyboardUtils

class RegisterFragment : Fragment() {

    companion object {
        private const val MIN_USERNAME_LENGTH = 6
    }

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
    }

    private fun setupUi() {

        // navigate to register fragment
        binding.btnToLogin.setOnClickListener {
            navigateToLoginWithCurrentUsername()
        }

        binding.btnSubmit.setOnClickListener {
            KeyboardUtils.hideKeyboard(requireActivity())
            val username = binding.edtUsername.text.toString()
            if (username.isBlank()) {
                binding.tilUsername.error = "Username is required!"
            } else if (username.length < MIN_USERNAME_LENGTH) {
                binding.tilUsername.error =
                    "Username must be at least $MIN_USERNAME_LENGTH characters!"
            } else {
                binding.tilUsername.error = null
                Toast.makeText(
                    this.context,
                    "Register successfully! Please login.",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToLoginWithCurrentUsername()
            }
        }

        binding.edtUsername.doOnTextChanged { _, _, _, _ ->
            binding.tilUsername.error = null
        }
    }

    private fun navigateToLoginWithCurrentUsername() {
        // RegisterFragmentDirections -> navigate + gửi kèm data trong action
        //  -> phía nhận  val arg:...FragmentArg by navArgs()
        val action = RegisterFragmentDirections.actionRegisterToLogin(
            username = binding.edtUsername.text?.toString()
        )
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_left
                exit = R.anim.slide_out_right
            }
            launchSingleTop = true
            popUpTo(R.id.loginFragment) {
                inclusive = true
            }
        }

        findNavController().navigate(
            action, options
        )
    }

}