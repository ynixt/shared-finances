package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.VersionResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.info.BuildProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(
    name = "Version",
    description = "Backend version",
)
@RequestMapping("/open/version")
class VersionController(
    private val buildProperties: BuildProperties,
) {
    @Operation(summary = "Get version")
    @GetMapping
    suspend fun getVersion(): ResponseEntity<VersionResponseDto> =
        ResponseEntity.ok(VersionResponseDto(version = buildProperties.version ?: "unknown"))
}
