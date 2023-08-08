package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.GroupInviteMapper
import com.ynixt.sharedfinances.mapper.GroupMapper
import com.ynixt.sharedfinances.model.dto.TransactionValuesGroupChartDto
import com.ynixt.sharedfinances.model.dto.group.*
import com.ynixt.sharedfinances.model.dto.groupinvite.GroupInviteDto
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import com.ynixt.sharedfinances.service.GroupInviteService
import com.ynixt.sharedfinances.service.GroupService
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/group")
class GroupController(
    private val groupService: GroupService,
    private val securityService: SecurityService,
    private val groupMapper: GroupMapper,
    private val transactionService: TransactionService,
    private val groupInviteService: GroupInviteService,
    private val groupInviteMapper: GroupInviteMapper
) {
    @GetMapping("with-users")
    fun listAllWithUsers(authentication: Authentication): List<GroupWithUserDto> {
        val user = securityService.authenticationToUser(authentication)!!
        return groupMapper.toGroupWithUserDtoList(groupService.listAllWithUsers(user))!!
    }

    @GetMapping
    fun listGroups(authentication: Authentication): List<GroupDto> {
        val user = securityService.authenticationToUser(authentication)!!
        return groupService.listGroupAsGroupDto(user)
    }

    @GetMapping("summary/{groupId}")
    fun getGroupSummary(
        authentication: Authentication,
        @PathVariable("groupId") groupId: Long,
        @RequestParam("minDate", required = false) minDate: LocalDate?,
        @RequestParam("maxDate", required = false) maxDate: LocalDate?,
        @RequestParam categoriesId: List<Long>?
    ): GroupSummaryDto {
        val user = securityService.authenticationToUser(authentication)!!
        return groupService.getGroupSummary(
            user, groupId, minDate = minDate, maxDate = maxDate, categoriesId = categoriesId
        )
    }

    @GetMapping("{id}/view")
    fun viewGroup(authentication: Authentication, @PathVariable id: Long): GroupViewDto? {
        val user = securityService.authenticationToUser(authentication)!!
        return groupService.getOneAsViewDto(user, id)
    }

    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<GroupDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(groupMapper.toDto(groupService.getOne(user, id)))
    }

    @PutMapping("{id}")
    fun update(
        authentication: Authentication, @PathVariable id: Long, @RequestBody updateDto: UpdateGroupDto
    ): GroupDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupMapper.toDto(groupService.updateGroup(user, id, updateDto))!!
    }

    @PostMapping
    fun newGroup(authentication: Authentication, @RequestBody newDto: NewGroupDto): GroupDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupMapper.toDto(groupService.newGroup(user, newDto))!!
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable("id") id: Long) {
        val user = securityService.authenticationToUser(authentication)!!

        return groupService.delete(user, id)
    }

    @GetMapping("{groupId}/transactions")
    fun listTransactions(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestParam minDate: LocalDate?,
        @RequestParam maxDate: LocalDate?,
        @RequestParam categoriesId: List<Long>?,
        pageable: Pageable
    ): Page<TransactionDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionService.findAllIncludeGroupAndCategoriesAsTransactionDto(
            bankAccountId = null,
            groupId = groupId,
            creditCardId = null,
            user = user,
            minDate = minDate,
            maxDate = maxDate,
            categoriesId = categoriesId,
            pageable = pageable
        )
    }

    @GetMapping("{groupId}/chart")
    fun getChartByGroupId(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestParam minDate: LocalDate?,
        @RequestParam maxDate: LocalDate?,
        @RequestParam categoriesId: List<Long>?
    ): TransactionValuesGroupChartDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupService.getChartByGroupId(
            user = user, groupId = groupId, minDate = minDate, maxDate = maxDate, categoriesId = categoriesId
        )
    }

    @PostMapping("{groupId}/invite")
    fun generateInvite(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): GroupInviteDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupInviteMapper.toDto(groupInviteService.generateInvite(user, groupId))!!
    }

    @GetMapping("invite/{code}")
    fun useInvite(
        authentication: Authentication,
        @PathVariable code: String,
    ): Long? {
        val user = securityService.authenticationToUser(authentication)!!

        return groupInviteService.useInvite(user, code)
    }
}
