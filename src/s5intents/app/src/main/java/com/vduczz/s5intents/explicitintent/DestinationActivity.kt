package com.vduczz.s5intents.explicitintent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivityDestinationExplicitBinding

class DestinationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DATA = "extra_string"

        fun newIntent(context: Context, extraData: String?): Intent =
            /**
             * Explicit intent: Intent(context, destination)
             */
            Intent(context, DestinationActivity::class.java).apply {
                // attach data belong to Intent
                putExtra(EXTRA_DATA, extraData)
            }
    }

    private lateinit var binding: ActivityDestinationExplicitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDestinationExplicitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // read data from intent
        intent.getStringExtra(EXTRA_DATA)?.let {
            binding.tvExtraData.text = it
        }
    }
}