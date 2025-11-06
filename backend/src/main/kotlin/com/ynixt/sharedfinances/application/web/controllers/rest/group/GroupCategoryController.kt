package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.EditCategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapPage
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
import reactor.core.publisher.Mono
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
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestParam onlyRoot: Boolean = true,
        @RequestParam mountChildren: Boolean = true,
        @RequestParam query: String? = null,
        pageable: Pageable,
    ): Mono<Page<CategoryDto>> =
        groupCategoryService
            .findAllCategories(
                userId = principalToken.principal.id,
                groupId = groupId,
                onlyRoot = onlyRoot,
                mountChildren = mountChildren,
                query = query,
                pageable,
            ).mapPage(categoryDtoMapper::toDto)

    @Operation(summary = "Get group category by id")
    @GetMapping("/{id}")
    fun findCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
        @RequestParam mountChildren: Boolean = true,
    ): Mono<ResponseEntity<CategoryDto>> =
        groupCategoryService
            .findCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
                mountChildren = mountChildren,
            ).map {
                ResponseEntity.ofNullable(categoryDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Create a new group category")
    @PostMapping
    fun newCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: NewCategoryDto,
    ): Mono<CategoryDto> =
        groupCategoryService
            .newCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                categoryDtoMapper.fromNewDtoToNewRequest(body),
            ).map(categoryDtoMapper::toDto)

    @Operation(summary = "Create a many group categories")
    @PostMapping("/bulk")
    fun newCategories(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: List<NewCategoryDto>,
    ): Mono<ResponseEntity<Unit>> =
        Mono
            .`when`(
                body.map { cat ->
                    groupCategoryService
                        .newCategory(
                            userId = principalToken.principal.id,
                            groupId = groupId,
                            categoryDtoMapper.fromNewDtoToNewRequest(cat),
                        )
                },
            ).then(Mono.fromCallable { ResponseEntity.noContent().build<Unit>() })
            .defaultIfEmpty(ResponseEntity.badRequest().build())

    @Operation(summary = "Edit group category by id")
    @PutMapping("/{id}")
    fun editCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
        @RequestBody body: EditCategoryDto,
    ): Mono<ResponseEntity<CategoryDto>> =
        groupCategoryService
            .editCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
                categoryDtoMapper.fromEditDtoToEditRequest(body),
            ).map {
                ResponseEntity.ofNullable(categoryDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Delete group category by id")
    @DeleteMapping("/{id}")
    fun deleteCategory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupCategoryService
            .deleteCategory(
                userId = principalToken.principal.id,
                groupId = groupId,
                id = id,
            ).map { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
