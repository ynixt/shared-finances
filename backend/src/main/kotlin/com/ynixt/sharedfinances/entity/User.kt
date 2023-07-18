package com.ynixt.sharedfinances.entity

import com.ynixt.sharedfinances.dto.UserDto
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val uid: String,
    val email: String,
    val name: String,
    val photoUrl: String?,
) : DatabaseEntity(), UserDetails {
    override fun getAuthorities(): List<GrantedAuthority> {
        return listOf()
    }

    override fun getPassword(): String {
        return ""
    }

    override fun getUsername(): String {
        return email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    fun toUserDto(): UserDto {
        return UserDto(
            id = id,
            email = email,
            name = name,
            photoUrl = photoUrl
        )
    }
}