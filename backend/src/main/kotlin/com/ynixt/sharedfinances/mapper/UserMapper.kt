package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.user.CurrentUserDto
import com.ynixt.sharedfinances.model.dto.user.UserDto
import org.mapstruct.Mapper

@Mapper(uses = [BankAccountMapper::class])
interface UserMapper {
    fun toDto(user: User): UserDto
    fun toCurrentUserDto(user: User): CurrentUserDto
}
