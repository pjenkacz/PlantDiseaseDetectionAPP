package com.example.slamleaf.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.slamleaf.ui.main.HistoryFragment
import com.example.slamleaf.ui.main.ProfileFragment
import com.example.slamleaf.ui.main.ScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        // domyślny ekran: Scan
        if (savedInstanceState == null) {
            replaceFragment(ScanFragment())
        }



        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> replaceFragment(ScanFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val openTab = intent?.getStringExtra("openTab")
        if (openTab == "history") {
            bottomNav.selectedItemId = R.id.nav_history
            replaceFragment(HistoryFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .commit()
    }
}