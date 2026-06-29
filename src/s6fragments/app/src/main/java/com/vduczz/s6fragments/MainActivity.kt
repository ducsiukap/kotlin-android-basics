package com.vduczz.s6fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.vduczz.s6fragments.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // navController
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)

//            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // khởi tạo navController từ NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)
                as NavHostFragment
        navController = navHostFragment.navController

        // bind bottom nav
        binding.bottomNav.setupWithNavController(navController)

        // bind action bar (toolBar)
        setSupportActionBar(binding.toolBar)
        // + root fragment -> hide Up button
        val appBarConfig = AppBarConfiguration(
            setOf(R.id.home_fragment, R.id.profile_fragment)
        )
        setupActionBarWithNavController(navController, appBarConfig)

        // observe destination change
        setupDestinationChangeObserver()

    }

    // handle click Up button
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // setup destination change observe
    fun setupDestinationChangeObserver() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = "S6 Fragments" // fixed actionbar title

            // destination.id -> current fragment/destination id
            val graphId = destination.parent?.id // NavGraph id (root of current graph)
            when {
                graphId == R.id.home_graph -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    binding.toolBar.subtitle = "Home"
                }

                graphId == null || destination.id == R.id.profile_fragment -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                    binding.toolBar.subtitle = "Profile"
                }

                else -> {
                    binding.bottomNav.visibility = android.view.View.GONE
                    binding.toolBar.subtitle = "Home"
                }
            }

        }
    }
}