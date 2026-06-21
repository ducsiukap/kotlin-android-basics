package com.vduczz.navigationcomponent.home.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vduczz.navigationcomponent.databinding.FragmentHomeBinding

class ListItemFragment : Fragment() {


    private var isScrollingDown = true;
    private lateinit var adapter: ItemAdapter

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvListItem.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ItemAdapter(
            listOf(
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4",
                "item 1", "item 2", "item 3", "item 4"
            )
        ) { item ->
            val action = ListItemFragmentDirections.actionListItemToDetailItem(item)
            findNavController().navigate(action)
        }

        binding.rvListItem.adapter = adapter

        binding.rvListItem.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    adapter.isScrollingDown = (dy > 0)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}