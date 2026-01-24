package com.example.voicenote.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Interceptor to inject HMAC-SHA256 signature and timestamp.
 * Signature = HMAC_SHA256(secret, method + path + query + timestamp + body_hash)
 */
class HmacInterceptor(private val secretKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        
        val url = originalRequest.url
        val path = url.encodedPath
        val query = url.encodedQuery ?: ""
        val method = originalRequest.method
        
        // 1. Get Body Hash (Security hardening)
        val bodyHash = originalRequest.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            SHA256(buffer.readByteArray())
        } ?: ""

        // 2. Construct Message: METHOD + PATH + QUERY + TIMESTAMP + BODY_HASH
        val message = "$method$path$query$timestamp$bodyHash"
        val signature = calculateHmac(message, secretKey)

        // 3. Build Request with Headers
        val signedRequest = originalRequest.newBuilder()
            .header("X-Device-Signature", signature)
            .header("X-Device-Timestamp", timestamp)
            .build()

        return chain.proceed(signedRequest)
    }

    private fun calculateHmac(message: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(message.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun SHA256(data: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }
}
