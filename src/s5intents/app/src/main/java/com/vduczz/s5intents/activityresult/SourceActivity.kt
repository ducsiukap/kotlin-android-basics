package com.vduczz.s5intents.activityresult

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivitySourceResultBinding

class SourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySourceResultBinding

    // create launcher
    private val launcher = registerForActivityResult(
        // Contract
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // destination activity thực hiện thành công
            val updatedName =
                result.data?.getStringExtra(DestinationActivity.EXTRA_UPDATED_NAME)
                    ?: return@registerForActivityResult

            val currentName = binding.tvName.text.toString()
            binding.tvName.text = updatedName

            Toast.makeText(
                this,
                "Name updated from $currentName to $updatedName",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // destination activity bị hủy
            // user cancel or error
            Toast.makeText(
                this,
                "Update name canceled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySourceResultBinding.inflate(layoutInflater)
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

        // start activity
        binding.btnChangeName.setOnClickListener {
            val currentName = binding.tvName.text.toString()
            val intent = DestinationActivity.newIntent(this, currentName)
            launcher.launch(intent)
        }
    }
}