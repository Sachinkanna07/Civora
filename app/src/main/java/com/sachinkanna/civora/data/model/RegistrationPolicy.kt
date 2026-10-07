package com.sachinkanna.civora.data.model

/** Public onboarding has no role parameter. Privileged provisioning is server-only. */
fun validateStudentRegistration(name: String, email: String, password: String): UserRole {
    require(name.trim().length in 2..100)
    require(
        email.trim().length <= 254 && email.trim().matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
    )
    require(password.length >= 8)
    return UserRole.STUDENT
}
