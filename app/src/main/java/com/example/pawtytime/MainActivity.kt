package com.example.pawtytime

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {


    lateinit var bottomNav : BottomNavigationView

    @SuppressLint("InflateParams")
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
            val dropdownView = layoutInflater.inflate(R.layout.profile_dropdown, null)
            val popupWindow = PopupWindow(dropdownView, WRAP_CONTENT, WRAP_CONTENT, true)
            dropdownView.elevation = 10f
            popupWindow.showAsDropDown(profileBtn, 0, 10)

            dropdownView.findViewById<LinearLayout>(R.id.settingsBtn).setOnClickListener{
                loadFragment(accountSettings())
                popupWindow.dismiss()
            }

            val profileOptions = dropdownView.findViewById<LinearLayout>(R.id.profile_options)
            val viewProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.hidden_profile)
            val changeIcon = dropdownView.findViewById<ImageView>(R.id.profile_icon)

            profileOptions.setOnClickListener{

                val isShowing = viewProfileOptions.isVisible
                viewProfileOptions.visibility = if (isShowing) View.GONE else View.VISIBLE

                val openedIcon = if (isShowing) R.drawable.add  else R.drawable.minus
                changeIcon.setImageResource(openedIcon)
            }

            val petProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.pet_profile_options)
            val viewPetProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.hidden_pet_profile)
            val changePetIcon = dropdownView.findViewById<ImageView>(R.id.pet_profile_icon)

            petProfileOptions.setOnClickListener{
                val isShowing = viewPetProfileOptions.isVisible
                viewPetProfileOptions.visibility = if (isShowing) View.GONE else View.VISIBLE

                val openedIcon = if (isShowing) R.drawable.add else R.drawable.minus
                changePetIcon.setImageResource(openedIcon)
            }

            dropdownView.findViewById<LinearLayout>(R.id.go_to_view_profile).setOnClickListener{
                loadFragment(profileView())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_edit_profile).setOnClickListener{
                loadFragment(profileEdit())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_view_pet_profile).setOnClickListener{
                loadFragment(petProfileView())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_edit_pet_profile).setOnClickListener{
                loadFragment(petProfileEdit())
                popupWindow.dismiss()
            }

            dropdownView.findViewById<LinearLayout>(R.id.calendarBtn).setOnClickListener {
                loadFragment(calendarScreen())
                popupWindow.dismiss()
            }
            // Added Logout to MainActivity
            dropdownView.findViewById<LinearLayout>(R.id.logoutBtn).setOnClickListener {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                popupWindow.dismiss()
            }

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

