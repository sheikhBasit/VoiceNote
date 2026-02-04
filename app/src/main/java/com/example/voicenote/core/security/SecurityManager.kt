package com.example.voicenote.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.voicenote.data.remote.UserDTO
import com.google.gson.Gson
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecurityManager(context: Context) {

    private val gson = Gson()
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_USER_TOKEN = "user_session_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DATA = "user_profile_data"
        private const val KEY_NAME = "biometric_key"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_BYPASSED = "biometric_bypassed"
        private const val KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled"
    }

    fun getSessionToken(): String? {
        return sharedPreferences.getString(KEY_USER_TOKEN, null)
    }

    fun saveSessionToken(token: String) {
        sharedPreferences.edit().putString(KEY_USER_TOKEN, token).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserData(): UserDTO? {
        val json = sharedPreferences.getString(KEY_USER_DATA, null) ?: return null
        return try {
            gson.fromJson(json, UserDTO::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserData(user: UserDTO) {
        val json = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_USER_DATA, json).apply()
        saveUserEmail(user.email)
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun hasBypassedOnce(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_BYPASSED, false)
    }

    fun setBypassedOnce(bypassed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_BYPASSED, bypassed).apply()
    }

    fun isFloatingButtonEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_FLOATING_BUTTON_ENABLED, true)
    }

    fun setFloatingButtonEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FLOATING_BUTTON_ENABLED, enabled).apply()
    }

    fun generateNewToken(): String {
        val token = UUID.randomUUID().toString()
        saveSessionToken(token)
        return token
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_DATA)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_BIOMETRIC_BYPASSED)
            .apply()
    }

    fun getInitializedCipher(): Cipher {
        val cipher = Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        )
        val secretKey = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
            }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
