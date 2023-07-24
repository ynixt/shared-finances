package com.ynixt.sharedfinances.service.impl

import com.google.firebase.auth.FirebaseToken
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.repository.UserRepository
import com.ynixt.sharedfinances.service.UserService
import jakarta.transaction.Transactional
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service


@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {
    override fun getForCurrentUser(id: Long): User? {
        return userRepository.findCurrentUserOneById(id)
    }

    @Transactional
    override fun createUserIfNotExists(firebaseToken: FirebaseToken): User {
        return userRepository.findByUid(firebaseToken.uid) ?: userRepository.save(
            User(
                uid = firebaseToken.uid,
                name = firebaseToken.name,
                email = firebaseToken.email,
                photoUrl = firebaseToken.picture,
            )
        )
    }

    override fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            userRepository.findByEmail(username) ?: throw UsernameNotFoundException("User not found")
        }
    }
}