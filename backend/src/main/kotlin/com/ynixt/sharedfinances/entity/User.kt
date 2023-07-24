package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.security.Principal

@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val uid: String,
    val email: String,
    val name: String,
    val photoUrl: String?,
) : AuditedEntity(), UserDetails {
    @OneToMany(mappedBy = "user")
    var creditCards: MutableList<CreditCard>? = null

    @ManyToMany(mappedBy = "users")
    var groups: MutableList<Group>? = null

    @OneToMany(mappedBy = "user")
    var bankAccounts: MutableList<BankAccount>? = null

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
}

class UserPrincipal(val user: User) : Principal {
    override fun getName(): String {
        return user.email
    }
}
