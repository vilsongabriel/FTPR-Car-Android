package com.example.myapitest.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.*

object FirebaseStorageUtils {
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    suspend fun uploadImage(imageUri: Uri): String {
        return try {
            val imageRef: StorageReference = storageRef
                .child("car_images/${UUID.randomUUID()}.jpg")

            imageRef.putFile(imageUri).await()

            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            throw Exception("Erro ao fazer upload da imagem: ${e.message}")
        }
    }
}