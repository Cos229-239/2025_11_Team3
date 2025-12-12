package com.example.pawtytime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton


/**
 * A simple [Fragment] subclass.
 * Use the [pet_edit_add_pets.newInstance] factory method to
 * create an instance of this fragment.
 */
class Pet_edit_add_pets : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pet_edit_add_pets, container, false)

        val backBtn = view.findViewById<ImageButton>(R.id.pet_edit_back_Btn)


        backBtn.setOnClickListener { (activity as? MainActivity)?.loadFragment(PetProfileEdit()) }
        return view
    }


}