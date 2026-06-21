package com.vduczz.navigationcomponent

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.vduczz.navigationcomponent.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // -------------------------
        // INIT navController
        navController = (supportFragmentManager
            .findFragmentById(binding.navHostFragment.id)
                as NavHostFragment).navController

        // -------------------------
        // SETUP BottomNavigationView + NavController
        binding.bottomNav.setupWithNavController(navController)

        // -------------------------
        // SETUP ToolBar + NavController
        //  + top-level destination -> hide Up button
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.profileFragment,
                R.id.loginFragment, R.id.registerFragment
            )
        )
        //  + toolbar + navcontroller
        setSupportActionBar(binding.toolBar)
//        setupActionBarWithNavController(navController, appBarConfig)
        setupActionBarWithNavController(navController, appBarConfig)
        // Tắt title mặc định của ActionBar
//        supportActionBar?.setDisplayShowTitleEnabled(false)


        // -------------------------
        // OBSERVE destination CHANGED
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentGraphId = destination.parent?.id
            when (currentGraphId) {
                R.id.auth_graph -> {
                    binding.toolBar.visibility = View.GONE
                    binding.bottomNav.visibility = View.GONE
                }

                R.id.home_graph -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.toolBar.visibility =
                        if (destination.id != R.id.profileFragment) View.VISIBLE
                        else View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}