package com.ynixt.sharedfinances.application.config

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import javax.imageio.ImageIO

@Component
class ImageIoPluginsInit {
    @PostConstruct
    fun init() {
        ImageIO.scanForPlugins()
    }
}
