package com.vduczz.s6fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.vduczz.s6fragments.databinding.ActivityMainBinding

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

        val isFirstReached = (savedInstanceState == null)
        setupUi(isFirstReached)

    }

    private fun setupUi(isFirstReached: Boolean) {
        setupToolBar()

        // load fragment only at the first time activity initialized
        // (savedInstanceState = null)
        if (isFirstReached) {
            loadFragment(HomeFragment())
        }

        setupBottomNavigation()
        setupBackStackListener()
    }

    private fun setupToolBar() {
        setSupportActionBar(binding.toolBar)
        supportActionBar?.title = "S6 Fragments"
    }

    private fun setupBottomNavigation() {

        // handle click item on bottom bar
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }

    }

    private fun setupBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment =
                supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (currentFragment != null) {
                onUpdateFragment(currentFragment)
            }
        }

    }

    // to change fragment
    // + at Activity -> supportFragmentManager
    // + at Fragment:
    //      - childFragmentManager -> manager its child fragments
    //      - parentFragmentManager -> refer to host's FragmentManager
    fun loadFragment(fragment: Fragment, isAddToBackStack: Boolean = false) {

        // không commit transaction khi activity đã gọi onSaveInstanceState()
        // thường là khi activity vào background / configuration change / destroy
        // để handle, có thể:
        //      + commitAllowingStateLoss() // không nên dùng
        //  or  + check: isStateSaved
        //  or  + commit trong repeatOnLifecycle(Lifecycle.State.RESUMED){ // commit }
        if (!supportFragmentManager.isStateSaved) {
            val tx = supportFragmentManager.beginTransaction()
                // replace(fragmentContainerId, newFragment) // -> remove current fragment + add newFragment
                // or .add()
                // .remove()
                // .show() / .hide()
                .replace(binding.fragmentContainer.id, fragment)

            // addToBackStack() -> backable ?
            if (isAddToBackStack) tx.addToBackStack(null)

            // commit transaction -> bring transaction into queue
            // FragmentManager will execute the transaction at the next UI loop
            tx.commit()

            if (!isAddToBackStack) // tránh updateUp 2 lần
                onUpdateFragment(fragment)
        }
    }

    private fun onUpdateFragment(fragment: Fragment) {
        // update bottom nav
        if (fragment is HomeFragment || fragment is HomeDetailFragment) {
            binding.bottomNav.menu.findItem(R.id.nav_home).isChecked = true
            binding.toolBar.subtitle = "Home"
        } else if (fragment is ProfileFragment) {
            binding.bottomNav.menu.findItem(R.id.nav_profile).isChecked = true
            binding.toolBar.subtitle = "Profile"
        }

        // update toolBar
        if (isRootFragment(fragment)) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    // handle click up
    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    private fun isRootFragment(fragment: Fragment) =
        (fragment is HomeFragment || fragment is ProfileFragment)

}