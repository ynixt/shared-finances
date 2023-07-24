package com.ynixt.sharedfinances.service

import com.google.firebase.auth.FirebaseToken
import com.ynixt.sharedfinances.entity.User
import org.springframework.security.core.userdetails.UserDetailsService


interface UserService {
    fun getForCurrentUser(id: Long): User?
    fun createUserIfNotExists(firebaseToken: FirebaseToken): User
    fun userDetailsService(): UserDetailsService
}