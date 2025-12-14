package com.example.pawtytime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ViewPetProfileActivity : AppCompatActivity(R.layout.activity_view_pet_profile) {

    companion object {
        const val EXTRA_OWNER_UID = "extra_owner_uid"
        const val EXTRA_PET_ID = "extra_pet_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val ownerUid = intent.getStringExtra(EXTRA_OWNER_UID).orEmpty()
            val petId = intent.getStringExtra(EXTRA_PET_ID).orEmpty()

            val frag = petProfileView().apply {
                arguments = Bundle().apply {
                    putString(petProfileView.ARG_OWNER_UID, ownerUid)
                    putString(petProfileView.ARG_PET_ID, petId)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.petProfileContainer, frag)
                .commit()
        }
    }
}
