package com.vduczz.androidkotlin.s5_intent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vduczz.androidkotlin.R
import com.vduczz.androidkotlin.databinding.S5IntentFilterDemoBinding

class IntentFilterDemoActivity : AppCompatActivity() {

    private lateinit var binding: S5IntentFilterDemoBinding

    companion object {
        private val LOG_TAG = IntentFilterDemoActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = S5IntentFilterDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // handle incoming intent
        handleIncomingIntent()
    }

    fun handleIncomingIntent() {
        if (intent.action == Intent.ACTION_SEND) {
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    binding.tvSharedText.setTextColor(getColor(R.color.green))
                    binding.tvSharedText.text = sharedText
                    Log.i(LOG_TAG, "shared text: $sharedText")
                }

                else -> {
                    Log.i(LOG_TAG, "unknown type: ${intent.type}")
                    binding.tvSharedText.text = "Error - Invalid Intent type: ${intent.type}"
                    binding.tvSharedText.setTextColor(getColor(R.color.red))
                }
            }
        }
    }
}