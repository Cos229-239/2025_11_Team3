package com.example.pawtytime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

// TODO: Rename parameter arguments, choose names that match // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class petProfileView : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            petProfileView().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
