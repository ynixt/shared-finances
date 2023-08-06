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
    var lang: String
) : AuditedEntity(), UserDetails {
    @OneToMany(mappedBy = "user")
    var creditCards: MutableSet<CreditCard>? = null

    @ManyToMany(mappedBy = "users")
    var groups: MutableSet<Group>? = null

    @OneToMany(mappedBy = "user")
    var bankAccounts: MutableSet<BankAccount>? = null

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

    companion object {
        const val DEFAULT_LANG = "en"
    }
}

class UserPrincipal(val user: User) : Principal {
    override fun getName(): String {
        return user.email
    }
}
