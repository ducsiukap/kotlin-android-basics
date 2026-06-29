package com.vduczz.s6fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.vduczz.s6fragments.databinding.FragmentCommonBinding
import kotlin.random.Random

// to create a fragment class, can extend:
//  - no-arg constructor + Binding.inflate(inflater, container, false) in onCreateView()
//  - constructor with fragment's layout param + Binding.bind(view) in onViewCreated()

class HomeFragment : Fragment() { // no-arg constructor

    private var _binding: FragmentCommonBinding? = null
    private val binding get() = _binding!!

    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Binding.inflate(...) in onCreateView()
        _binding = FragmentCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // set _binding = null to avoid memory leak
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDescription.text = "Home Fragment"

        val btn = Button(requireContext())
        btn.text = "To Home Detail"
        btn.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.root.addView(btn)
        btn.setOnClickListener {
            // navigation
            // sageargs -> action
            val action = HomeFragmentDirections.actionHomeToDetail(
                Random.nextInt(0, 100)
            )
            // navOptions -> anim, backstack, ...
            val navOptions = NavOptions.Builder()
                .apply {
                    // animation
                    setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
                    setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
                    setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
                    setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)

                    // singleTop
                    setLaunchSingleTop(true)

                    // backstack...
                    // setPopUpTo(R.id.home_graph, inclusive = true)
                }.build()
            navController.navigate(action, navOptions)
        }
    }

}