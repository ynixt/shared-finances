package com.ynixt.sharedfinances.config.security

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.IOException

@Configuration
class FirebaseAuthConfig {
    @Value("classpath:service-account.json")
    lateinit var serviceAccount: Resource

    @Bean
    fun firebaseAuth(): FirebaseAuth {
        var options: FirebaseOptions

        try {
            options =
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.inputStream))
                    .build()
        } catch (ex: IOException) {
            options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
        }

        val firebaseApp = FirebaseApp.initializeApp(options)
        return FirebaseAuth.getInstance(firebaseApp)
    }
}
