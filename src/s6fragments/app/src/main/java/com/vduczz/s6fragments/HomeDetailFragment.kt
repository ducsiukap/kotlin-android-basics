package com.vduczz.s6fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.vduczz.s6fragments.databinding.FragmentCommonBinding

class HomeDetailFragment : Fragment(R.layout.fragment_common) {

    private var _binding: FragmentCommonBinding? = null
    private val binding get() = _binding!!

    // take passed argument
    private val args: HomeDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // take passed data
        val detailId = args.detailId

        _binding = FragmentCommonBinding.bind(view)
        binding.tvDescription.text = "Home Detail Fragment - passed=$detailId"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}