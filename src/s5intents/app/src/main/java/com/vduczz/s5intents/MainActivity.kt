package com.vduczz.s5intents

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vduczz.s5intents.databinding.ActivityMainBinding
import com.vduczz.s5intents.activityresult.SourceActivity as ActivityResultSource
import com.vduczz.s5intents.explicitintent.SourceActivity as ExplicitSource
import com.vduczz.s5intents.implicitintent.SourceActivity as ImplicitSource

class MainActivity : AppCompatActivity() {
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

        binding.btnStartExplicitIntent.setOnClickListener {
            val intent = Intent(
                this, ExplicitSource::class.java
            )
            startActivity(intent)
        }

        binding.btnStartImplicitIntent.setOnClickListener {
            val intent = Intent(
                this, ImplicitSource::class.java
            )
            startActivity(intent)
        }

        binding.btnStartResultIntent.setOnClickListener {
            val intent = Intent(
                this, ActivityResultSource::class.java
            )
            startActivity(intent)
        }
    }

}