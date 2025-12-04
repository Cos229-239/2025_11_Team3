package com.example.pawtytime

import java.io.Serializable

data class PersonProfile(
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String?,
    val location: String?,
    val profileUrl: String?,
    val idFrontUrl: String?,
    val idBackUrl: String?,
    val bio: String?
) : Serializable