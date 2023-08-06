package com.ynixt.sharedfinances.service.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.user.UserSettingsDto
import com.ynixt.sharedfinances.repository.UserRepository
import com.ynixt.sharedfinances.service.UserService
import jakarta.transaction.Transactional
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service


@Service
class UserServiceImpl(
    private val userRepository: UserRepository, private val firebaseAuth: FirebaseAuth
) : UserService {
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
                lang = User.DEFAULT_LANG
            )
        )
    }

    override fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            userRepository.findByEmail(username) ?: throw UsernameNotFoundException("User not found")
        }
    }

    @Transactional
    override fun updateSettings(user: User, newSettingsDto: UserSettingsDto) {
        user.lang = newSettingsDto.lang
        userRepository.save(user)
    }

    @Transactional
    override fun deleteAccount(user: User) {
        userRepository.deleteById(user.id!!)
        firebaseAuth.deleteUser(user.uid)
    }
}
