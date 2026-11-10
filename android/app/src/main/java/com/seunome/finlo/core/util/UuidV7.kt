package com.seunome.finlo.core.util

import java.security.SecureRandom
import java.util.UUID

object UuidV7 {
    private val random = SecureRandom()

    fun generate(timestampMs: Long = System.currentTimeMillis()): String {
        val randBytes = ByteArray(10)
        random.nextBytes(randBytes)

        val timeHigh = ((timestampMs ushr 16) and 0xFFFFFFFFL)
        val timeMid = (timestampMs and 0xFFFFL)

        val randA = ((randBytes[0].toInt() and 0x0F) shl 8) or (randBytes[1].toInt() and 0xFF)
        val verRandA = 0x7000 or randA

        val varRandBHi = ((randBytes[2].toInt() and 0x3F) or 0x80)
        val randBLow = ByteArray(7)
        System.arraycopy(randBytes, 3, randBLow, 0, 7)

        val msb = (timeHigh shl 32) or (timeMid shl 16) or (verRandA.toLong() and 0xFFFFL)

        var lsb = (varRandBHi.toLong() and 0xFFL) shl 56
        for (i in 0 until 7) {
            lsb = lsb or ((randBLow[i].toLong() and 0xFFL) shl ((6 - i) * 8))
        }

        return UUID(msb, lsb).toString()
    }
}
