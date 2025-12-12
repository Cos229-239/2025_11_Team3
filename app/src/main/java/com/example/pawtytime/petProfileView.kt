package com.example.pawtytime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton


class petProfileView : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout for this fragment
        return inflater.inflate(R.layout.fragment_pet_profile_view, container, false)
    }

    //  Hook up the Schedule Playdate button
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSchedulePlaydate =
            view.findViewById<MaterialButton>(R.id.btnSchedulePlaydate)

        // TEMP VALUES just so it compiles & runs.
        // Replace later with real data from Firebase / pet object.
        val petId: String? = null
        val petName: String = param1 ?: "Your Pet"
        val petAge: Int = param2?.toIntOrNull() ?: -1

        btnSchedulePlaydate.setOnClickListener {
            val ctx = requireContext()
            val intent = Intent(ctx, SchedulePlaydateActivity::class.java).apply {
                putExtra(SchedulePlaydateActivity.EXTRA_PET_ID, petId)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_NAME, petName)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_AGE, petAge)
            }
            startActivity(intent)
        }
    }


}
