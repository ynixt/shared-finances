package com.ynixt.sharedfinances.resources.services.mfa

import com.ynixt.sharedfinances.domain.services.mfa.TotpService
import org.apache.commons.codec.binary.Base32
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TotpServiceImpl : TotpService {
    private val base32 = Base32()
    private val digitFormat = "%06d"

    override fun generateNewSecret(): String {
        val key = KeyGenerators.secureRandom(20).generateKey()
        return base32.encodeToString(key)
    }

    override fun verifyRaw(
        rawSecret: String,
        code: String,
    ): Boolean {
        val secretBytes = base32.decode(rawSecret)
        val currentInterval = Instant.now().epochSecond / 30

        for (i in -1..1) {
            if (calculateCode(secretBytes, currentInterval + i) == code.trim()) {
                return true
            }
        }
        return false
    }

    private fun calculateCode(
        secret: ByteArray,
        interval: Long,
    ): String {
        val data = ByteBuffer.allocate(8).putLong(interval).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0xf
        val binary =
            ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        return String.format(digitFormat, binary % 1_000_000)
    }
}
