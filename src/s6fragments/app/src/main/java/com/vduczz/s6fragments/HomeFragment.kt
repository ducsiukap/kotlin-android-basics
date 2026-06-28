package com.vduczz.s6fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.vduczz.s6fragments.databinding.FragmentCommonBinding
import kotlin.random.Random

// to create a fragment class, can extend:
//  - no-arg constructor + Binding.inflate(inflater, container, false) in onCreateView()
//  - constructor with fragment's layout param + Binding.bind(view) in onViewCreated()

class HomeFragment : Fragment() { // no-arg constructor

    private var _binding: FragmentCommonBinding? = null
    private val binding get() = _binding!!

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

            // chuyển fragment từ fragment con
            //  + cách 1: định nghĩa hàm load ở activity
            //      -> mọi thao tác chuyển fragment đều gọi tới
            //      // nhược: tight-coupling
            (requireActivity() as? MainActivity)?.loadFragment(
                // nếu fragment nhận argument đầu vào
                HomeDetailFragment.newInstance(detailId = Random.nextInt()),
                //  -> nên để nó tự kiểm soát cách đưa đầu vào đó vào argument
                //      và cung cấp api để khởi tạo instance của nó

                true
            )
            //  + cách 2: parentFragmentManager.beginTransaction() ....
            //      // không phụ thuộc Activity nhưng cần biết container id
            //  + cách 3: Activity implement Navigator, fragment call navigator.goTo(...)
            //      // ưu: loose-coupling, dễ unit test
            /**
             * // INTERFACE
             * public interface Navigator {
             *     void navigateTo(Fragment fragment,
             *                     boolean addToBackStack);
             * }
             *
             * // CLASS
             * public class MainActivity
             *         extends AppCompatActivity
             *         implements Navigator {
             *  @Override void navigateTo(...) {...}
             * }
             *
             * // FRAGMENT
             * Navigator navigator;
             * @Override
             * public void onAttach(@NonNull Context context) {
             *     super.onAttach(context);
             *     if(context instanceof Navigator)
             *         navigator = (Navigator) context;
             * }
             * // calling
             * navigator.navigateTo(
             *         new DetailFragment(),
             *         true
             * );
             */
        }
    }

}