package com.example.pawtytime

import java.io.Serializable

data class PersonProfile(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String? = null,
    val location: String? = null,
    val profileUrl: String? = null,
    val idFrontUrl: String? = null,
    val idBackUrl: String? = null,
    val bio: String? = null,
    val profileTypes: List<String> = emptyList()
): Serializable