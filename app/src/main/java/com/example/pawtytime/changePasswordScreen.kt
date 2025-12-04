package com.example.pawtytime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChangePasswordScreen.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChangePasswordScreen : Fragment() {
    // TODO: Rename and change types of parameters


    // private members

    private lateinit var currentPass: EditText
    private lateinit var newPass: EditText
    private lateinit var confirmPass: EditText
    private lateinit var confirmChangedPass: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_change_password_screen, container, false)

        currentPass = view.findViewById<EditText>(R.id.currentPassword)
        newPass = view.findViewById<EditText>(R.id.newPassword)
        confirmPass = view.findViewById<EditText>(R.id.confirmPassword)
        confirmChangedPass = view.findViewById<Button>(R.id.confirmChangedPassword)


        confirmChangedPass.setOnClickListener {
            handlePasswordChange()
        }
        return view
    }


    private fun handlePasswordChange(){
        val current = currentPass.text.toString()
        val new = newPass.text.toString()
        val confirm = confirmPass.text.toString()

        if(newPass.length() < 6)
        {
            Toast.makeText(requireContext(), "Password must be at least 6 characters" ,Toast.LENGTH_SHORT).show()
            return
        }
        if (new != confirm){
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return
        val credential = EmailAuthProvider.getCredential(email,current)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(new)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Password Updated", Toast.LENGTH_SHORT)
                            .show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Password update failed",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Re-authentification Failed", Toast.LENGTH_SHORT).show()
            }

    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment changePasswordScreen.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChangePasswordScreen().apply {

            }
    }
}