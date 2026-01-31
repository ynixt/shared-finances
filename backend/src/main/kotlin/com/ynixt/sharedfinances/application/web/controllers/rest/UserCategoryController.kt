package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.EditCategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
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
@RequestMapping("/categories")
@Tag(
    name = "Categories",
    description = "Operations related to all categories that belongs to logged user",
)
class UserCategoryController(
    private val userCategoryService: UserCategoryService,
    private val categoryDtoMapper: CategoryDtoMapper,
) {
    @Operation(summary = "Get all user categories")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestParam onlyRoot: Boolean = true,
        @RequestParam mountChildren: Boolean = true,
        @RequestParam query: String? = null,
        pageable: Pageable,
    ): Page<CategoryDto> =
        userCategoryService
            .findAllCategories(
                principalToken.principal.id,
                onlyRoot = onlyRoot,
                mountChildren = mountChildren,
                query = query,
                pageable,
            ).map(categoryDtoMapper::toDto)

    @Operation(summary = "Get user category by id")
    @GetMapping("/{id}")
    suspend fun findCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestParam mountChildren: Boolean = true,
    ): ResponseEntity<CategoryDto> =
        userCategoryService
            .findCategory(
                userId = principalToken.principal.id,
                id = id,
                mountChildren = mountChildren,
            ).let { category ->
                ResponseEntity.ofNullable(category?.let { categoryDtoMapper.toDto(it) })
            }

    @Operation(summary = "Create a new user category")
    @PostMapping
    suspend fun newCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewCategoryDto,
    ): CategoryDto =
        userCategoryService
            .newCategory(
                principalToken.principal.id,
                categoryDtoMapper.fromNewDtoToNewRequest(body),
            ).let(categoryDtoMapper::toDto)

    @Operation(summary = "Create a many user categories")
    @PostMapping("/bulk")
    suspend fun newCategories(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: List<NewCategoryDto>,
    ): ResponseEntity<Unit> {
        body.forEach { cat ->
            userCategoryService
                .newCategory(
                    principalToken.principal.id,
                    categoryDtoMapper.fromNewDtoToNewRequest(cat),
                )
        }

        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Edit user category by id")
    @PutMapping("/{id}")
    suspend fun editCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditCategoryDto,
    ): ResponseEntity<CategoryDto> =
        userCategoryService
            .editCategory(
                userId = principalToken.principal.id,
                id = id,
                categoryDtoMapper.fromEditDtoToEditRequest(body),
            ).let { category ->
                ResponseEntity.ofNullable(category?.let { categoryDtoMapper.toDto(it) })
            }

    @Operation(summary = "Delete user category by id")
    @DeleteMapping("/{id}")
    suspend fun deleteCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        userCategoryService
            .deleteCategory(
                userId = principalToken.principal.id,
                id = id,
            ).let { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
