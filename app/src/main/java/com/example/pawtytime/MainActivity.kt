package com.example.pawtytime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import coil.load
import coil.transform.CircleCropTransformation
import coil.size.Scale


class MainActivity : AppCompatActivity() {
    private var suppressNavCallback = false
    lateinit var bottomNav : MaterialButtonToggleGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val profileBtn = findViewById<ImageButton>(R.id.profile_btn)
        val notifsBtn =findViewById<ImageButton>(R.id.notifs_btn)
        val inboxBtn = findViewById<ImageButton>(R.id.inbox_btn)
        val logoBtn = findViewById<ImageButton>(R.id.app_logo)


        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("profileUrl")
                    if (!url.isNullOrBlank()) {
                        profileBtn.load(url) {
                            placeholder(R.drawable.ic_profile)
                            error(R.drawable.ic_profile)
                            crossfade(true)
                            transformations(CircleCropTransformation())
                            scale(Scale.FILL)
                        }
                    } else {
                        profileBtn.setImageResource(R.drawable.ic_profile)
                    }
                }
        }



        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.check(R.id.nav_home)
        bottomNav.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (suppressNavCallback) return@addOnButtonCheckedListener

            when (checkedId) {
                R.id.nav_home -> loadFragment(HomeScreen())
                R.id.nav_events -> loadFragment(EventsScreen())
                R.id.nav_map -> loadFragment(MapScreen())
                R.id.nav_shop -> loadFragment(ShopScreen())
            }
        }

        profileBtn.setOnClickListener{
            val dropdownView = layoutInflater.inflate(R.layout.profile_dropdown, null)
            val popupWindow = PopupWindow(dropdownView, WRAP_CONTENT, WRAP_CONTENT, true)
            dropdownView.elevation = 10f

            val dropdownAvatar = dropdownView.findViewById<ImageView>(R.id.dropdownAvatar)
            val nameText = dropdownView.findViewById<TextView>(R.id.tvProfileName)

            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val first = doc.getString("firstName") ?: ""
                        val last = doc.getString("lastName") ?: ""
                        val username = doc.getString("username") ?: ""
                        val profileUrl = doc.getString("profileUrl")

                        val fullName = "$first $last".trim()
                        val displayName = when {
                            fullName.isNotBlank() -> fullName
                            username.isNotBlank() -> username
                            else -> user.email ?: "Pawty User"
                        }

                        nameText.text = displayName

                        if (!profileUrl.isNullOrBlank()) {
                            dropdownAvatar.load(profileUrl) {
                                placeholder(R.drawable.ic_profile)
                                error(R.drawable.ic_profile)
                                crossfade(true)
                                transformations(CircleCropTransformation())
                                scale(Scale.FILL)
                            }
                        } else {
                            dropdownAvatar.setImageResource(R.drawable.ic_profile)
                        }
                    }
            }

            popupWindow.showAsDropDown(profileBtn, 0, 10)

            dropdownView.findViewById<LinearLayout>(R.id.settingsBtn).setOnClickListener{
                bottomNav.clearChecked()
                loadFragment(AccountSettings())
                popupWindow.dismiss()
            }

            val profileOptions = dropdownView.findViewById<LinearLayout>(R.id.profile_options)
            val viewProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.hidden_profile)
            val changeIcon = dropdownView.findViewById<ImageView>(R.id.profile_icon)

            profileOptions.setOnClickListener{

                val isShowing = viewProfileOptions.visibility == View.VISIBLE
                viewProfileOptions.visibility = if (isShowing) View.GONE else View.VISIBLE

                val openedIcon = if (isShowing) R.drawable.add  else R.drawable.minus
                changeIcon.setImageResource(openedIcon)
            }

            val petProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.pet_profile_options)
            val viewPetProfileOptions = dropdownView.findViewById<LinearLayout>(R.id.hidden_pet_profile)
            val changePetIcon = dropdownView.findViewById<ImageView>(R.id.pet_profile_icon)

            petProfileOptions.setOnClickListener{
                val isShowing = viewPetProfileOptions.visibility == View.VISIBLE
                viewPetProfileOptions.visibility = if (isShowing) View.GONE else View.VISIBLE

                val openedIcon = if (isShowing) R.drawable.add else R.drawable.minus
                changePetIcon.setImageResource(openedIcon)
            }

            dropdownView.findViewById<LinearLayout>(R.id.go_to_view_profile).setOnClickListener{
                bottomNav.clearChecked()
                loadFragment(ProfileView())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_edit_profile).setOnClickListener{
                bottomNav.clearChecked()
                loadFragment(ProfileEdit())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_view_pet_profile).setOnClickListener{
                bottomNav.clearChecked()
                loadFragment(petProfileView())
                popupWindow.dismiss()
            }
            dropdownView.findViewById<LinearLayout>(R.id.go_to_edit_pet_profile).setOnClickListener{
                bottomNav.clearChecked()
                loadFragment(PetProfileEdit())
                popupWindow.dismiss()
            }

            dropdownView.findViewById<LinearLayout>(R.id.calendarBtn).setOnClickListener {
                bottomNav.clearChecked()
                loadFragment(CalendarScreen())
                popupWindow.dismiss()
            }

            dropdownView.findViewById<LinearLayout>(R.id.logoutBtn).setOnClickListener {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                popupWindow.dismiss()
            }

        }

        logoBtn.setOnClickListener{
            bottomNav.clearChecked()
            loadFragment(HomeScreen())
        }

        notifsBtn.setOnClickListener{
            bottomNav.clearChecked()
            loadFragment(Notifications())
        }

        inboxBtn.setOnClickListener{
            bottomNav.clearChecked()
            loadFragment(Inbox())

        }

        handleIntent(intent)
    }
    fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
            .addToBackStack(null)
        transaction.commit()
    }

    private fun handleIntent(intent: Intent?) {
        val openTab = intent?.getStringExtra("open_tab")

        val profileUid = intent?.getStringExtra("open_profile_uid")
        if (!profileUid.isNullOrBlank()) {
            loadFragment(ProfileView.newInstance(profileUid))
            return
        }

        if (openTab == "map") {
            val centerLat = intent.getDoubleExtra("center_lat", Double.NaN)
            val centerLng = intent.getDoubleExtra("center_lng", Double.NaN)

            val mapFragment = if (!centerLat.isNaN() && !centerLng.isNaN()) {
                MapScreen.newCentered(centerLat, centerLng)
            } else {
                MapScreen()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, mapFragment)
                .commit()

            suppressNavCallback = true
            bottomNav.check(R.id.nav_map)
            suppressNavCallback = false
            return
        } else {
            bottomNav.check(R.id.nav_home)
            loadFragment(HomeScreen())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}

