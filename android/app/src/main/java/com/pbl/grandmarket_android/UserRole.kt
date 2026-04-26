package com.pbl.grandmarket_android

enum class UserRole(val value: String) {
    SELLER("SELLER"),
    BUYER("BUYER");

    companion object {
        fun from(value: String?): UserRole {
            return values().firstOrNull { it.value == value } ?: BUYER
        }
    }
}
