package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.TransactionCategoryMapper
import com.ynixt.sharedfinances.model.dto.transactioncategory.NewUserTransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.UpdateUserTransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.UserTransactionCategoryDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user-transaction-category")
class UserTransactionCategoryController(
    private val securityService: SecurityService,
    private val transactionCategoryService: TransactionCategoryService,
    private val transactionCategoryMapper: TransactionCategoryMapper
) {
    @PostMapping
    fun newCategory(
        authentication: Authentication, @RequestBody newDto: NewUserTransactionCategoryDto
    ): UserTransactionCategoryDto {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionCategoryMapper.toDtoUser(transactionCategoryService.newUserCategory(user, newDto))!!
    }

    @PutMapping("{id}")
    fun updateCategory(
        authentication: Authentication, @PathVariable id: Long, @RequestBody updateDto: UpdateUserTransactionCategoryDto
    ): UserTransactionCategoryDto {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionCategoryMapper.toDtoUser(transactionCategoryService.updateUserCategory(user, id, updateDto))!!
    }

    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<UserTransactionCategoryDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(
            transactionCategoryMapper.toDtoUser(
                transactionCategoryService.getOneUserCategory(
                    user, id
                )
            )
        )
    }

    @GetMapping
    fun list(authentication: Authentication): List<UserTransactionCategoryDto> {
        val user = securityService.authenticationToUser(authentication)!!
        
        return transactionCategoryService.findAllUserCategoriesAsUserTransactionCategoryDto(
            user
        )
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable id: Long) {
        val user = securityService.authenticationToUser(authentication)!!

        transactionCategoryService.deleteOneUserCategory(user, id)
    }
}
