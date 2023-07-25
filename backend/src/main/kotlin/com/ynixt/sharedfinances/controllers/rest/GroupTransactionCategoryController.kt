package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.TransactionCategoryMapper
import com.ynixt.sharedfinances.model.dto.transactioncategory.GroupTransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.NewGroupTransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.UpdateGroupTransactionCategoryDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/group-transaction-category")
class GroupTransactionCategoryController(
    private val securityService: SecurityService,
    private val transactionCategoryService: TransactionCategoryService,
    private val transactionCategoryMapper: TransactionCategoryMapper
) {
    @PostMapping
    fun newCategory(
        authentication: Authentication, @RequestBody newDto: NewGroupTransactionCategoryDto
    ): GroupTransactionCategoryDto {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionCategoryMapper.toDtoGroup(transactionCategoryService.newGroupCategory(user, newDto))!!
    }

    @PutMapping("{id}")
    fun updateCategory(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody updateDto: UpdateGroupTransactionCategoryDto
    ): GroupTransactionCategoryDto {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionCategoryMapper.toDtoGroup(
            transactionCategoryService.updateGroupCategory(
                user,
                id,
                updateDto
            )
        )!!
    }

    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<GroupTransactionCategoryDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(
            transactionCategoryMapper.toDtoGroup(
                transactionCategoryService.getOneGroupCategory(
                    user, id
                )
            )
        )
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable id: Long) {
        val user = securityService.authenticationToUser(authentication)!!

        transactionCategoryService.deleteOneGroupCategory(user, id)
    }
}
