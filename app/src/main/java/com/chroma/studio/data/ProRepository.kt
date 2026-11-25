package com.chroma.studio.data

import com.chroma.studio.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

enum class ProBadgeTier(val label: String) {
    LUXURY("Luxury"),
    SIGNATURE("Signature"),
    STANDARD("Standard")
}

data class ProBadgePriceTier(
    val key: String,
    val label: String,
    val tier: ProBadgeTier,
    val priceUsd: Int,
    val priceInr: Int,
    val tagline: String
)

val ProBadgeCatalog: List<ProBadgePriceTier> = listOf(
    // Luxury & Metallic ($24 - $29)
    ProBadgePriceTier("gold", "Gold Premium", ProBadgeTier.LUXURY, 29, 2499, "Imperial 24K radiant shimmer seal"),
    ProBadgePriceTier("roseGold", "Rose Gold", ProBadgeTier.LUXURY, 29, 2499, "Brushed champagne rose shimmer"),
    ProBadgePriceTier("chroma", "Chroma Prism", ProBadgeTier.LUXURY, 29, 2499, "Signature iridescent prism edition"),
    ProBadgePriceTier("silver", "Silver Exec", ProBadgeTier.LUXURY, 24, 1999, "Titanium metallic executive seal"),
    ProBadgePriceTier("ruby", "Ruby Elite", ProBadgeTier.LUXURY, 24, 1999, "Deep faceted crimson ruby finish"),
    ProBadgePriceTier("crimson", "Crimson Crown", ProBadgeTier.LUXURY, 24, 1999, "Royal Velvet crown crest seal"),

    // Signature Colors ($15)
    ProBadgePriceTier("emerald", "Emerald Official", ProBadgeTier.SIGNATURE, 15, 1299, "Studio official emerald tone"),
    ProBadgePriceTier("amethyst", "Amethyst Trust", ProBadgeTier.SIGNATURE, 15, 1299, "Rich royal amethyst tone"),
    ProBadgePriceTier("deepPurple", "Deep Purple VIP", ProBadgeTier.SIGNATURE, 15, 1299, "VIP studio collector violet"),
    ProBadgePriceTier("sapphire", "Sapphire Auth", ProBadgeTier.SIGNATURE, 15, 1299, "Deep ultramarine sapphire"),
    ProBadgePriceTier("bronze", "Bronze Pioneer", ProBadgeTier.SIGNATURE, 15, 1299, "Founding creator bronze alloy"),

    // Standard Verified ($9)
    ProBadgePriceTier("blue", "Standard Blue", ProBadgeTier.STANDARD, 9, 799, "Classic verified creator blue"),
    ProBadgePriceTier("ocean", "Ocean Sentinel", ProBadgeTier.STANDARD, 9, 799, "Bright coastal sky seal"),
    ProBadgePriceTier("cyan", "Corporate Cyan", ProBadgeTier.STANDARD, 9, 799, "Crisp studio cyan blue"),
    ProBadgePriceTier("teal", "Teal Authentic", ProBadgeTier.STANDARD, 9, 799, "Balanced studio teal tone"),
    ProBadgePriceTier("forest", "Forest Secure", ProBadgeTier.STANDARD, 9, 799, "Natural verdant creator tone"),
    ProBadgePriceTier("navy", "Navy Seal", ProBadgeTier.STANDARD, 9, 799, "Executive midnight navy"),
    ProBadgePriceTier("midnight", "Midnight Core", ProBadgeTier.STANDARD, 9, 799, "Stealth obsidian core seal"),
    ProBadgePriceTier("spaceGray", "Space Gray Auth", ProBadgeTier.STANDARD, 9, 799, "Minimal matte titanium gray"),
    ProBadgePriceTier("slate", "Slate Authority", ProBadgeTier.STANDARD, 9, 799, "Clean architectural slate"),
    ProBadgePriceTier("charcoal", "Charcoal Stealth", ProBadgeTier.STANDARD, 9, 799, "Carbon monochrome seal")
)

val ProBenefitsList: List<String> = listOf(
    "Custom designed verified badge across Profile, Feed, Flash, Search & Chat",
    "Oscellion Pro Pass status & Pro identity highlight",
    "Priority discovery & creator credibility signal",
    "Full benefit activation within 7 days of payment confirmation"
)

fun resolveProBadgePricing(tone: String?): ProBadgePriceTier {
    val clean = tone?.trim()?.lowercase().orEmpty()
    return ProBadgeCatalog.firstOrNull { it.key.lowercase() == clean }
        ?: ProBadgeCatalog.first { it.key == "blue" }
}

data class ProVerificationRequest(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val contactEmail: String = "",
    val fullName: String = "",
    val category: String = "",
    val portfolioUrl: String? = null,
    val reason: String? = null,
    val badgeTone: String = "blue",
    val badgeLabel: String = "Standard Blue",
    val badgeTier: String = "STANDARD",
    val priceUsd: Int = 9,
    val priceInr: Int = 799,
    val currency: String = "USD",
    val status: String = "submitted",
    val paymentReference: String? = null,
    val paymentQrPayload: String? = null,
    val paymentQrImageUrl: String? = null,
    val paymentAmountLabel: String? = null,
    val paymentInstructions: String? = null,
    val paymentProofRef: String? = null,
    val adminNote: String? = null,
    val emailDeliveryStatus: String? = null,
    val emailSentTo: String? = null,
    val submittedAt: Long = 0L,
    val reviewedAt: Long? = null,
    val paymentSentAt: Long? = null,
    val emailSentAt: Long? = null,
    val paymentSubmittedAt: Long? = null,
    val expectedActivationBy: Long? = null,
    val activatedAt: Long? = null,
    val reviewedBy: String? = null,
    val updatedAt: Long = 0L
) {
    val isActiveWorkflow: Boolean
        get() = status in setOf("submitted", "under_review", "payment_sent", "payment_submitted")
}

object ProRepository {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private const val COLLECTION_PRO_REQUESTS = "pro_requests"
    private const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L

    private fun readLongOrNull(value: Any?): Long? = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is Timestamp -> value.seconds * 1000L + value.nanoseconds / 1_000_000L
        is Date -> value.time
        else -> null
    }

    private fun DocumentSnapshot.toProRequest(): ProVerificationRequest {
        val pricing = resolveProBadgePricing(getString("badgeTone"))
        return ProVerificationRequest(
            id = id,
            uid = getString("uid").orEmpty(),
            username = getString("username").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            avatarUrl = getString("avatarUrl"),
            contactEmail = getString("contactEmail").orEmpty(),
            fullName = getString("fullName").orEmpty(),
            category = getString("category").orEmpty(),
            portfolioUrl = getString("portfolioUrl"),
            reason = getString("reason"),
            badgeTone = getString("badgeTone") ?: pricing.key,
            badgeLabel = getString("badgeLabel") ?: pricing.label,
            badgeTier = getString("badgeTier") ?: pricing.tier.name,
            priceUsd = readLongOrNull(get("priceUsd"))?.toInt() ?: pricing.priceUsd,
            priceInr = readLongOrNull(get("priceInr"))?.toInt() ?: pricing.priceInr,
            currency = getString("currency") ?: "USD",
            status = getString("status") ?: "submitted",
            paymentReference = getString("paymentReference"),
            paymentQrPayload = getString("paymentQrPayload"),
            paymentQrImageUrl = getString("paymentQrImageUrl"),
            paymentAmountLabel = getString("paymentAmountLabel"),
            paymentInstructions = getString("paymentInstructions"),
            paymentProofRef = getString("paymentProofRef"),
            adminNote = getString("adminNote"),
            emailDeliveryStatus = getString("emailDeliveryStatus"),
            emailSentTo = getString("emailSentTo"),
            submittedAt = readLongOrNull(get("submittedAt")) ?: 0L,
            reviewedAt = readLongOrNull(get("reviewedAt")),
            paymentSentAt = readLongOrNull(get("paymentSentAt")),
            emailSentAt = readLongOrNull(get("emailSentAt")),
            paymentSubmittedAt = readLongOrNull(get("paymentSubmittedAt")),
            expectedActivationBy = readLongOrNull(get("expectedActivationBy")),
            activatedAt = readLongOrNull(get("activatedAt")),
            reviewedBy = getString("reviewedBy"),
            updatedAt = readLongOrNull(get("updatedAt")) ?: 0L
        )
    }

    fun observeUserProRequests(uid: String): Flow<List<ProVerificationRequest>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection(COLLECTION_PRO_REQUESTS)
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ProRepository", "Failed listening to pro_requests", error)
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        try {
                            doc.toProRequest()
                        } catch (e: Exception) {
                            android.util.Log.e("ProRepository", "Error mapping pro request ${doc.id}", e)
                            null
                        }
                    }
                    ?.sortedByDescending { it.submittedAt }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ProRepository", "Failed listening to user profile", error)
                    return@addSnapshotListener
                }
                val profile = snap?.toObject(UserProfile::class.java)?.let {
                    if (it.uid.isBlank()) it.copy(uid = snap.id) else it
                }
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitProRequest(
        uid: String,
        profile: UserProfile?,
        badgeTier: ProBadgePriceTier,
        fullName: String,
        contactEmail: String,
        category: String,
        portfolioUrl: String,
        reason: String
    ): Result<ProVerificationRequest> = runCatching {
        val now = System.currentTimeMillis()
        val ref = db.collection(COLLECTION_PRO_REQUESTS).document()
        val cleanEmail = contactEmail.trim()
        val cleanFullName = fullName.trim()
        val cleanCategory = category.trim().ifBlank { "Creator" }
        val cleanPortfolio = portfolioUrl.trim()
        val cleanReason = reason.trim()
        val username = profile?.username?.ifBlank { "creator" } ?: "creator"
        val displayName = profile?.displayName?.ifBlank { cleanFullName } ?: cleanFullName

        val payload = hashMapOf<String, Any?>(
            "id" to ref.id,
            "uid" to uid,
            "username" to username,
            "displayName" to displayName,
            "avatarUrl" to (profile?.avatarUrl ?: ""),
            "contactEmail" to cleanEmail,
            "fullName" to cleanFullName,
            "category" to cleanCategory,
            "portfolioUrl" to cleanPortfolio,
            "reason" to cleanReason,
            "badgeTone" to badgeTier.key,
            "badgeLabel" to badgeTier.label,
            "badgeTier" to badgeTier.tier.name,
            "priceUsd" to badgeTier.priceUsd,
            "priceInr" to badgeTier.priceInr,
            "currency" to "USD",
            "status" to "submitted",
            "submittedAt" to now,
            "updatedAt" to now
        )
        ref.set(payload).await()
        ProVerificationRequest(
            id = ref.id,
            uid = uid,
            username = username,
            displayName = displayName,
            avatarUrl = profile?.avatarUrl,
            contactEmail = cleanEmail,
            fullName = cleanFullName,
            category = cleanCategory,
            portfolioUrl = cleanPortfolio,
            reason = cleanReason,
            badgeTone = badgeTier.key,
            badgeLabel = badgeTier.label,
            badgeTier = badgeTier.tier.name,
            priceUsd = badgeTier.priceUsd,
            priceInr = badgeTier.priceInr,
            currency = "USD",
            status = "submitted",
            submittedAt = now,
            updatedAt = now
        )
    }

    suspend fun submitPaymentProof(
        requestId: String,
        paymentProofRef: String
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val cleanRef = paymentProofRef.trim()
        require(cleanRef.isNotBlank()) { "Transaction ID / UTR / Payment reference is required." }

        db.collection(COLLECTION_PRO_REQUESTS).document(requestId)
            .update(
                mapOf(
                    "status" to "payment_submitted",
                    "paymentProofRef" to cleanRef,
                    "paymentSubmittedAt" to now,
                    "expectedActivationBy" to (now + SEVEN_DAYS_MS),
                    "updatedAt" to now
                )
            )
            .await()
    }
}
