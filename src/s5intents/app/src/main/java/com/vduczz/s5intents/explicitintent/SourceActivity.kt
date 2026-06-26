package com.vduczz.s5intents.explicitintent

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivitySourceExplicitBinding

class SourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceExplicitBinding

    companion object {
        private const val EXTRA_DATA = "edt_extra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySourceExplicitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        savedInstanceState?.getString(EXTRA_DATA)?.let {
            binding.etExtraData.setText(it)
        }

        binding.btnToDestActivity.setOnClickListener {
            val intent = DestinationActivity
                .newIntent(this, binding.etExtraData.text.toString())

            // use intent to start a service/activity, ...
            // startService(intent)
            startActivity(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(EXTRA_DATA, binding.etExtraData.text.toString())
    }

}