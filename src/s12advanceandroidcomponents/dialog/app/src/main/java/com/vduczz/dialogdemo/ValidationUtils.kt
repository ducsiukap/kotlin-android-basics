package com.vduczz.dialogdemo

object ValidationUtils {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    private const val PASSWORD_MIN_LENGTH = 6

    fun isValidEmail(email: String): String? = if (!EMAIL_REGEX.matches(email)) {
        "Invalid email!"
    } else {
        null
    }

    fun isValidPassword(password: String): String? = if (password.length < PASSWORD_MIN_LENGTH) {
        "Password too short!"
    } else {
        null
    }
}