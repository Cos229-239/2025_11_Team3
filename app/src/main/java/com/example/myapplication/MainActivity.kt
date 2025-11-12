package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val profileBtn = findViewById<ImageButton>(R.id.profile_btn)
        val notifsBtn =findViewById<ImageButton>(R.id.notifs_btn)
        val inboxBtn = findViewById<ImageButton>(R.id.inbox_btn)

        loadFragment(HomeScreen())
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)!!
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment(HomeScreen())
                    true
                }
                R.id.events -> {
                    loadFragment(EventsScreen())
                    true
                }
                R.id.map -> {
                    loadFragment(MapScreen())
                    true
                }
                R.id.shop -> {
                    loadFragment(ShopScreen())
                    true
                }
                else -> false
            }
        }
        bottomNav.itemIconTintList = null

        profileBtn.setOnClickListener{

        }

        notifsBtn.setOnClickListener{
            loadFragment(Notifications())
        }

        inboxBtn.setOnClickListener{
            loadFragment(Inbox())
        }



    }
    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
        transaction.commit()
    }
}

