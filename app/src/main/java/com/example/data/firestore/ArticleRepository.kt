package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.ArticleLanguage
import com.example.data.model.ArticleTone
import com.example.data.model.BlogArticle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ArticleRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to access saved articles.")

    fun observeArticles(userId: String): Flow<List<BlogArticle>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/articles"
        emitAll(
            db.collection("users").document(userId).collection("articles")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BlogArticle::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveArticle(article: BlogArticle): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/articles"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("articles").document()
            docPath = docRef.path

            val safeLanguage = ArticleLanguage.fromLabel(article.language).label
            val safeTone = ArticleTone.fromLabel(article.tone).label

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "topicTitle" to article.topicTitle.trim().take(300).ifBlank { "Untitled Topic" },
                "category" to article.category.trim().take(80).ifBlank { "All Trends" },
                "seoTitle" to article.seoTitle.trim().take(300).ifBlank { "Untitled Article" },
                "metaDescription" to article.metaDescription.trim().take(600),
                "language" to safeLanguage,
                "targetWordCount" to article.targetWordCount.coerceIn(1000L, 5000L),
                "actualWordCount" to article.actualWordCount.coerceIn(0L, 15000L),
                "tone" to safeTone,
                "plainTextContent" to article.plainTextContent.trim().take(120000).ifBlank { " " },
                "bloggerHtmlContent" to article.bloggerHtmlContent.trim().take(250000).ifBlank { "<p></p>" },
                "heroImageAlt" to article.heroImageAlt.trim().take(400),
                "heroImageCaption" to article.heroImageCaption.trim().take(500),
                "inlineImageAlt" to article.inlineImageAlt.trim().take(400),
                "inlineImageCaption" to article.inlineImageCaption.trim().take(500),
                "sources" to article.sources.take(20),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (article.heroImageBase64.isNotBlank()) {
                payload["heroImageBase64"] = article.heroImageBase64.take(220000)
            }
            if (article.inlineImageBase64.isNotBlank()) {
                payload["inlineImageBase64"] = article.inlineImageBase64.take(220000)
            }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateArticleImages(
        articleId: String,
        heroImageBase64: String? = null,
        inlineImageBase64: String? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/articles/$articleId"
        return try {
            val uid = requireUserId()
            require(articleId.isNotBlank()) { "Article ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("articles").document(articleId)
            docPath = docRef.path

            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!heroImageBase64.isNullOrBlank()) {
                updates["heroImageBase64"] = heroImageBase64.take(220000)
            }
            if (!inlineImageBase64.isNullOrBlank()) {
                updates["inlineImageBase64"] = inlineImageBase64.take(220000)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteArticle(articleId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/articles/$articleId"
        return try {
            val uid = requireUserId()
            require(articleId.isNotBlank()) { "Article ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("articles").document(articleId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
