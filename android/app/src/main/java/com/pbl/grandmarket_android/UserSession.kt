package com.pbl.grandmarket_android

import android.content.Context

object UserSession {
    private const val PREF_NAME = "grandmarket_session"
    private const val KEY_USER_ROLE = "user_role"

    fun saveRole(context: Context, role: UserRole) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ROLE, role.value)
            .apply()
    }

    fun getRole(context: Context): UserRole {
        val roleValue = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ROLE, null)
        return UserRole.from(roleValue)
    }
}
