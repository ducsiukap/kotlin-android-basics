package com.vduczz.s4activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s4activities.databinding.ActivityMainBinding

class SavedStateDemoActivity : AppCompatActivity() {

    /** Log class - logging
     *
     * Syntax:
     *      Log.d(TAG, msg)
     *  or
     *      Log.d(TAG, msg, throwable)
     *
     * Logging types:
     *      + Log.v(): verbose  -> for detail information, debug
     *      + Log.d(): Debug    -> for debugging in development
     *      + Log.i(): Info     -> information, not an error/exception
     *      + Log.w(): Warning  -> warn message, may be an error but not too serious
     *      + Log.e(): Error    -> serious error
     */
    companion object {
        private val LOG_TAG = SavedStateDemoActivity::class.java.simpleName

        // Activity State keys
        private const val COUNT_STATE = "count"
        private const val IS_NEW = "is_new"
    }

    // activity states
    private var savedCount: Int = 0
    private var unsavedCount: Int = 0

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // query saved state
        savedCount = savedInstanceState?.getInt(COUNT_STATE) ?: 0

        val isFirstReached = savedInstanceState?.getBoolean(IS_NEW) ?: true
        Log.i(
            LOG_TAG,
            if (isFirstReached) "create $LOG_TAG with savedCount=$savedCount, unsavedCount=$unsavedCount"
            else "re-create $LOG_TAG with savedCount=$savedCount, unsavedCount=$unsavedCount"
        )

        setupUi()
    }

    // saved state before configuration changes,
    // such as rotate screen, change language, theme, multi-window mode, ...
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(COUNT_STATE, savedCount)
        outState.putBoolean(IS_NEW, false)

        /**
         * Note: chỉ cần lưu những state của activity / or important states
         *
         * Một số View như EditText, CheckBox, ...
         * TỰ ĐỘNG save state khi recreate
         */
    }

    private fun setupUi() {
        binding.tvSavedCount.text = "savedCount: $savedCount"
        binding.tvUnsavedCount.text = "unsavedCount: $unsavedCount"

        binding.btnIncreaseSavedCount.setOnClickListener {
            savedCount++
            binding.tvSavedCount.text = "savedCount: $savedCount"
        }

        binding.btnIncreaseUnsavedCount.setOnClickListener {
            unsavedCount++
            binding.tvUnsavedCount.text = "unsavedCount: $unsavedCount"
        }
    }
}