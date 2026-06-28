package com.vduczz.s6fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.vduczz.s6fragments.databinding.FragmentCommonBinding

class HomeDetailFragment : Fragment(R.layout.fragment_common) {

    companion object {
        private const val ARG_DETAIL_ID = "arg_detail_id"

        fun newInstance(detailId: Int) = HomeDetailFragment().apply {
            // Fragment.arguments là một Bundle
            // được Android lưu và khôi phục tự động qua các configuration change
            // — đây là cơ chế chính thức để truyền data vào Fragment.
            arguments = Bundle().apply {
                putInt(ARG_DETAIL_ID, detailId)
            }
        }
    }

    private var _binding: FragmentCommonBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // take passed data
        val detailId = arguments?.getInt(ARG_DETAIL_ID)
        if (detailId == null) parentFragmentManager.popBackStack()

        _binding = FragmentCommonBinding.bind(view)
        binding.tvDescription.text = "Home Detail Fragment - passed=$detailId"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}