package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.dto.user.UserDto
import com.ynixt.sharedfinances.entity.User
import org.mapstruct.Mapper
import org.springframework.stereotype.Component

@Mapper
@Component
interface UserMapper {
    fun toDto(user: User): UserDto
}