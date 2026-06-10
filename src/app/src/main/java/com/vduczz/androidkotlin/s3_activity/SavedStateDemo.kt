package com.vduczz.androidkotlin.s3_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vduczz.androidkotlin.R
import com.vduczz.androidkotlin.databinding.S3SavedInstanceStateDemoBinding
import kotlin.properties.Delegates

// Activity -> extends AppCompatActivity / ComponentActivity
class SavedStateDemo : AppCompatActivity() {

    private lateinit var binding: S3SavedInstanceStateDemoBinding

    // states
    private var unsavedCount by Delegates.notNull<Int>()
    private var savedCount by Delegates.notNull<Int>()

    // input
    private lateinit var unsavedTxt: String

    // state's key
    companion object {
        private const val COUNT_KEY = "count"
    }

    // saved state before destroy
    override fun onSaveInstanceState(outState: Bundle) {
        // put saved data
        outState.putInt(COUNT_KEY, savedCount)
        super.onSaveInstanceState(outState)
    }

    // override onCreate() callback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = S3SavedInstanceStateDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        unsavedCount = savedInstanceState?.getInt("unsavedStateInt") ?: 0 // always 0
        // restore saved state
        savedCount = savedInstanceState?.getInt(COUNT_KEY) ?: 0 // saved or 0

        setupUI()
    }

    private fun updateUnsavedCount() {
        binding.tvUnsavedCount.text = getString(R.string.tv_unsaved_count, unsavedCount)
    }

    private fun updateSavedCount() {
        binding.tvSavedCount.text = getString(R.string.tv_saved_count, savedCount)
    }

    private fun setupUI() {
        updateUnsavedCount()
        updateSavedCount()

        binding.btnUnsaveCount.setOnClickListener {
            ++unsavedCount
            updateUnsavedCount()
        }

        binding.btnSavedCount.setOnClickListener {
            savedCount++
            updateSavedCount()
        }
    }

}