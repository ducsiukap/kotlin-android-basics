package com.vduczz.s5intents.implicitintent

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivitySourceImplicitBinding

class SourceActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySourceImplicitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySourceImplicitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
            insets
        }

        binding.btnStartCall.setOnClickListener {
            val phone = binding.etPhone.text.toString()

            if (phone.isEmpty()) {
                binding.etPhone.error = "Please enter a phone number"
                binding.etPhone.requestFocus()
                return@setOnClickListener
            }

            /**
             * Implicit intent:
             *
             * Syntax:
             *      Intent(action, data)
             * where:
             *  - action: The general action to be performed
             *  - data: The data to operate on, expressed as a Uri
             *
             *  eg:
             *      action: ACTION_CALL/ACTION_DIAL - data: "tel:$phone".toUri()
             *      action: ACTION_VIEW             - data: "https://...", ...
             *      action: ACTION_SENDTO           - data: "mailto:$email".toUri()
             *      action: ACTION_SEND
             */
            val uri = "tel:$phone".toUri()
            val intent = Intent(Intent.ACTION_DIAL, uri)
            // ACTION_CALL cần permission

            /**
             * Intent Flag : Flags kiểm soát cách Activity
             * được thêm vào Back Stack — tương tự launchMode
             * nhưng linh hoạt hơn vì set per-call (trong code) thay vì cố định trong Manifest.
             */
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

            // send intent
            startActivity(intent)
        }

        binding.btnSendText.setOnClickListener {
            val text = binding.etText.text.toString()
            if (text.isEmpty()) {
                binding.etText.error = "Please enter a text"
                binding.etText.requestFocus()
                return@setOnClickListener
            }

            // send text
            val intent = Intent(Intent.ACTION_SEND).apply {
                // specify type:
                type = "text/plain"

                // put extras
                putExtra(Intent.EXTRA_TEXT, text)

                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Intent.createChooser() -> create ui to select app handle intent
            startActivity(Intent.createChooser(intent, "Share text via"))

            // nếu không có Activity nào có thể handle
            // -> throw ActivityNotFoundException

            // solution 1:
            // API 33+ dùng resolveActivity với ResolveInfoFlags
            //            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //                if (intent.resolveActivity(packageManager) != null) {
            //                    startActivity(intent)
            //                }
            //            } else {
            //                if (intent.resolveActivity(packageManager) != null) {
            //                    startActivity(intent)
            //                } else {
            //                    Toast.makeText(this, "Không có app xử lý được", Toast.LENGTH_SHORT).show()
            //                }
            //            }

            // solution 2: try-catch
            // Cách gọn hơn — dùng try-catch
            //            try {
            //                startActivity(intent)
            //            } catch (e: ActivityNotFoundException) {
            //                Toast.makeText(this, "Không tìm thấy app phù hợp", Toast.LENGTH_SHORT).show()
            //            }
        }
    }
}