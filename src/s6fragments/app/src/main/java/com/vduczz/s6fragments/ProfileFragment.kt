package com.vduczz.s6fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.vduczz.s6fragments.databinding.FragmentCommonBinding


class ProfileFragment : Fragment(R.layout.fragment_common) {
    // constructor with layout as an argument

    private var _binding: FragmentCommonBinding? = null
    private val binding get() = _binding!!

    // onCreateView() inflate layout automatically

    // _binding initialized in onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding.bind(..) in onViewCreated()
        _binding = FragmentCommonBinding.bind(view)

        binding.tvDescription.text = "Profile Fragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}