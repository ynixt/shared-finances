package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.EditCategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/categories")
@Tag(
    name = "Categories",
    description = "Operations related to all categories that belongs to a group",
)
class GroupCategoryController(
    private val groupCategoryService: GroupCategoryService,
    private val categoryDtoMapper: CategoryDtoMapper,
) {
    @Operation(summary = "Get all group categories")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestParam onlyRoot: Boolean = true,
        @RequestParam mountChildren: Boolean = true,
        @RequestParam query: String? = null,
        pageable: Pageable,
    ): Page<CategoryDto> =
        groupCategoryService
            .findAllCategories(
                userId = principalToken.principal.id,
                groupId = groupId,
                onlyRoot = onlyRoot,
                mountChildren = mountChildren,
                query = query,
                pageable,
            ).map(categoryDtoMapper::toDto)

    @Operation(summary = "Get group category by id")
    @GetMapping("/{id}")
    suspend fun findCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
        @RequestParam mountChildren: Boolean = true,
    ): ResponseEntity<CategoryDto> =
        groupCategoryService
            .findCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
                mountChildren = mountChildren,
            ).let { category ->
                ResponseEntity.ofNullable(category?.let { categoryDtoMapper.toDto(it) })
            }

    @Operation(summary = "Create a new group category")
    @PostMapping
    suspend fun newCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: NewCategoryDto,
    ): ResponseEntity<CategoryDto> =
        groupCategoryService
            .newCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                categoryDtoMapper.fromNewDtoToNewRequest(body),
            ).let { category ->
                ResponseEntity.ofNullable(category?.let { categoryDtoMapper.toDto(it) })
            }

    @Operation(summary = "Create a many group categories")
    @PostMapping("/bulk")
    suspend fun newCategories(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: List<NewCategoryDto>,
    ): ResponseEntity<Unit> =
        body
            .forEach { cat ->
                groupCategoryService
                    .newCategory(
                        userId = principalToken.principal.id,
                        groupId = groupId,
                        categoryDtoMapper.fromNewDtoToNewRequest(cat),
                    )
            }.let {
                ResponseEntity.noContent().build<Unit>()
            }

    @Operation(summary = "Edit group category by id")
    @PutMapping("/{id}")
    suspend fun editCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: EditCategoryDto,
    ): ResponseEntity<CategoryDto> =
        groupCategoryService
            .editCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
                categoryDtoMapper.fromEditDtoToEditRequest(body),
            ).let { category ->
                ResponseEntity.ofNullable(category?.let { categoryDtoMapper.toDto(it) })
            }

    @Operation(summary = "Delete group category by id")
    @DeleteMapping("/{id}")
    suspend fun deleteCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        groupCategoryService
            .deleteCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
            ).let { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
