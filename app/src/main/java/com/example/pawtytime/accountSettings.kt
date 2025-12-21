package com.example.pawtytime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class AccountSettings : Fragment() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy { FirebaseFirestore.getInstance() }

    var user = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account_settings, container, false)

        val logoutBtn = view.findViewById<Button>(R.id.logoutButton)
        val deleteAccountBtn = view.findViewById<Button>(R.id.deleteAccount)

        logoutBtn.setOnClickListener {

        }

        deleteAccountBtn.setOnClickListener{

        }
        return view
    }


    // re-authenticate the users email and password to delete account

    private fun reauthenticateUser( password: String, _complete: (Boolean) -> Unit) {
        if (user != null && user!!.email != null) {

            val credential = EmailAuthProvider
                .getCredential(user!!.email!!, password)

            user!!.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _complete(true)
                    } else {
                        Toast.makeText(
                            context,
                            "Re-authentication Failed, check password and try again",
                            Toast.LENGTH_SHORT
                        ).show()
                        _complete(false)
                    }
                }
            } else {
                Toast.makeText(context, "User not found or email is missing", Toast.LENGTH_SHORT).show()
                _complete(false)
        }
    }

        // delete the actual data from firebase
        private fun deleteAccountData(){

        }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AccountSettings().apply {
            }
    }
}