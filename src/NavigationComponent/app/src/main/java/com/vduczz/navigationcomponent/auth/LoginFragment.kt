package com.vduczz.navigationcomponent.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.vduczz.navigationcomponent.AuthGraphDirections
import com.vduczz.navigationcomponent.R
import com.vduczz.navigationcomponent.databinding.FragmentLoginBinding
import com.vduczz.navigationcomponent.home.SharedViewModel
import com.vduczz.navigationcomponent.utils.KeyboardUtils

class LoginFragment : Fragment() {

    private val vm: SharedViewModel by activityViewModels()

    companion object {
        private const val MIN_USERNAME_LENGTH = 6
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val args: LoginFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
    }

    private fun setupUi() {
        // take passed argument
        binding.edtUsername.setText(args.username ?: "")

        // navigate to register fragment
        binding.btnToRegister.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginToRegister()
            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                }
                launchSingleTop = true
            }

            findNavController().navigate(
                action, options
            )
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
                    "Login success with username=$username",
                    Toast.LENGTH_SHORT
                ).show()

                vm.setUser(username)

                // navigate + clear
                val action = AuthGraphDirections.actionAuthToHome()
                val options = navOptions {
                    anim {
                        enter = R.anim.from_transparent
                        exit = R.anim.to_transparent
                    }

                    popUpTo(R.id.auth_graph) {
                        inclusive = true
                    }
                }

                findNavController().navigate(action, options)
            }
        }

        binding.edtUsername.doOnTextChanged { _, _, _, _ ->
            binding.tilUsername.error = null
        }

        binding.edtUsername.imeOptions = EditorInfo.IME_ACTION_GO
        binding.edtUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                KeyboardUtils.hideKeyboard(requireActivity())
                binding.btnSubmit.performClick()
                true
            } else
                false

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}