package com.example.myapitest.utils

import com.google.firebase.auth.FirebaseAuth

object AuthUtils {
    
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }
    
    fun getCurrentUser() = FirebaseAuth.getInstance().currentUser
    
    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}