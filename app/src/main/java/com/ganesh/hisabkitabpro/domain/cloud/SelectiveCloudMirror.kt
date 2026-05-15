package com.ganesh.hisabkitabpro.domain.cloud

import android.util.Log
import com.ganesh.hisabkitabpro.auth.AuthDataMerger
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore mirror for the Multi-User Shared Khata cloud layer.
 *
 * This class mirrors ONLY the approved lightweight tables:
 * - Business_Profiles
 * - Customers
 * - Transactions
 * - Partner_Access
 * - Staff access/status only
 *
 * Inventory, app settings, OCR cache, reminders, staff payroll/attendance, and
 * local-only feature state intentionally never pass through this class.
 *
 * All writes are fire-and-forget. Firestore's local persistence queues them
 * while offline, so local Room writes stay zero-lag and continue to be the
 * source of truth for billing / ledger math.
 */
@Singleton
class SelectiveCloudMirror @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataMerger: AuthDataMerger,
    private val cloudBusinessIdentity: CloudBusinessIdentity
) {

    fun mirrorBusinessProfile(profile: BusinessProfile) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_BUSINESS_PROFILES)
            .document(profile.userId.ifBlank { "default_profile" })
            .set(profile.toCloudMap(businessId))
            .addOnFailureListener { Log.w(TAG, "Business profile mirror failed", it) }
    }

    fun mirrorCustomer(customer: Customer) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_CUSTOMERS)
            .document(customer.id.toString())
            .set(customer.toCloudMap(businessId))
            .addOnFailureListener { Log.w(TAG, "Customer mirror failed", it) }
    }

    fun mirrorTransaction(transaction: Transaction) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_TRANSACTIONS)
            .document(transaction.txnRef)
            .set(transaction.toCloudMap(businessId))
            .addOnFailureListener { Log.w(TAG, "Transaction mirror failed", it) }
    }

    fun grantPartnerAccess(
        partnerUserId: String,
        role: PartnerRole = PartnerRole.PARTNER
    ) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_PARTNER_ACCESS)
            .document(partnerUserId)
            .set(
                mapOf(
                    "business_id" to businessId,
                    "user_id" to partnerUserId,
                    "role" to role.name,
                    "granted_by_user_id" to currentUserId(),
                    "server_timestamp" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { Log.w(TAG, "Partner access mirror failed", it) }
    }

    fun mirrorStaffAccessRights(staff: StaffEntity) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_STAFF_ACCESS)
            .document(staff.id)
            .set(staff.toAccessCloudMap(businessId))
            .addOnFailureListener { Log.w(TAG, "Staff access mirror failed", it) }
    }

    suspend fun syncStaffAccessRights(staff: StaffEntity) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_STAFF_ACCESS)
            .document(staff.id)
            .set(staff.toAccessCloudMap(businessId))
            .await()
    }

    suspend fun deleteStaffAccessRights(staffId: String) {
        if (!hasSignedInUser()) return
        val businessId = cloudBusinessIdentity.currentBusinessId()
        ensureOwnerAccess(businessId)
        businesses()
            .document(businessId)
            .collection(COLLECTION_STAFF_ACCESS)
            .document(staffId)
            .delete()
            .await()
    }

    /**
     * Deletes all mirrored Shared-Khata documents for [businessId] under
     * `/businesses/{businessId}/…` (profiles, customers, transactions, access lists),
     * then removes the business root document. Intended only for explicit account /
     * data deletion flows while the user is authenticated.
     */
    suspend fun purgeMirroredBusinessData(businessId: String): Result<Unit> = runCatching {
        val normalized = businessId.trim()
        require(normalized.isNotBlank()) { "businessId blank" }
        if (!hasSignedInUser()) {
            error("Not signed in — cannot verify cloud deletion")
        }
        val root = businesses().document(normalized)
        deleteEntireSubcollection(root.collection(COLLECTION_BUSINESS_PROFILES))
        deleteEntireSubcollection(root.collection(COLLECTION_CUSTOMERS))
        deleteEntireSubcollection(root.collection(COLLECTION_TRANSACTIONS))
        deleteEntireSubcollection(root.collection(COLLECTION_PARTNER_ACCESS))
        deleteEntireSubcollection(root.collection(COLLECTION_STAFF_ACCESS))
        root.delete().await()
    }

    private suspend fun deleteEntireSubcollection(collection: CollectionReference) {
        while (true) {
            val snapshot = collection.limit(PURGE_QUERY_CHUNK).get().await()
            if (snapshot.isEmpty) break
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    /**
     * Optimized customer ledger listener for live shared-khata surfaces.
     *
     * Uses a narrow per-customer query:
     * `/businesses/{businessId}/transactions`
     *   `.whereEqualTo("customer_id", customerId)`
     *   `.orderBy("server_timestamp", DESC)`
     *   `.limit(limit)`
     *
     * SCAFFOLDING (not a runtime caller yet) — part of the Multi-User Shared
     * Khata Engine surface. Reserved for the upcoming Live Web Dashboard /
     * partner real-time balance widget. Preserved intentionally per the
     * "Preserve Working Systems" directive — do NOT remove during cleanup
     * passes; R8 will strip it from release if still unused.
     */
    fun listenCustomerTransactions(
        customerId: Long,
        limit: Long = DEFAULT_LEDGER_LIMIT,
        onSnapshot: (List<Map<String, Any?>>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): ListenerRegistration {
        val businessId = cloudBusinessIdentity.currentBusinessId()
        return businesses()
            .document(businessId)
            .collection(COLLECTION_TRANSACTIONS)
            .whereEqualTo("customer_id", customerId)
            .orderBy("server_timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onSnapshot(snapshot?.documents?.map { it.data.orEmpty() }.orEmpty())
            }
    }

    /**
     * Customer summary (single document) listener for live balance widgets.
     *
     * SCAFFOLDING (not a runtime caller yet) — part of the Multi-User Shared
     * Khata Engine surface. Reserved for the upcoming Live Web Dashboard /
     * partner real-time balance widget. Preserved intentionally per the
     * "Preserve Working Systems" directive — do NOT remove during cleanup
     * passes; R8 will strip it from release if still unused.
     */
    fun listenCustomerSummary(
        customerId: Long,
        onSnapshot: (Map<String, Any?>?) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): ListenerRegistration {
        val businessId = cloudBusinessIdentity.currentBusinessId()
        return businesses()
            .document(businessId)
            .collection(COLLECTION_CUSTOMERS)
            .document(customerId.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onSnapshot(snapshot?.data)
            }
    }

    private fun businesses() = firestore.collection(COLLECTION_BUSINESSES)

    private fun ensureOwnerAccess(businessId: String) {
        val uid = currentUserId().takeIf { it != "anonymous" } ?: return
        businesses()
            .document(businessId)
            .set(
                mapOf(
                    "business_id" to businessId,
                    "owner_user_id" to uid,
                    "server_timestamp" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        businesses()
            .document(businessId)
            .collection(COLLECTION_PARTNER_ACCESS)
            .document(uid)
            .set(
                mapOf(
                    "business_id" to businessId,
                    "user_id" to uid,
                    "role" to PartnerRole.OWNER.name,
                    "granted_by_user_id" to uid,
                    "server_timestamp" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
    }

    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: authDataMerger.currentStampedUid()
            ?: "anonymous"

    private fun hasSignedInUser(): Boolean = currentUserId() != "anonymous"

    private fun baseCloudMap(businessId: String): Map<String, Any?> = mapOf(
        "business_id" to businessId,
        "created_by_user_id" to currentUserId(),
        "server_timestamp" to FieldValue.serverTimestamp()
    )

    private fun BusinessProfile.toCloudMap(businessId: String): Map<String, Any?> =
        baseCloudMap(businessId) + mapOf(
            "user_id" to userId,
            "business_name" to businessName,
            "owner_name" to ownerName,
            "phone" to phone,
            "email" to email,
            "address" to address,
            "gst_number" to gstNumber,
            "pan_number" to panNumber,
            "upi_id" to upiId,
            "logo_url" to logoUrl,
            "business_category" to businessCategory,
            "operating_hours" to operatingHours,
            "website_url" to websiteUrl,
            "instagram_url" to instagramUrl,
            "facebook_url" to facebookUrl,
            "linkedin_url" to (linkedInUrl ?: ""),
            "youtube_url" to (youtubeUrl ?: ""),
            "twitter_url" to (twitterUrl ?: ""),
            "whatsapp_business_url" to (whatsAppBusinessUrl ?: ""),
            "google_business_profile_url" to (googleBusinessProfileUrl ?: ""),
            "signature_image_path" to signatureImagePath,
            "latitude" to latitude,
            "longitude" to longitude,
            "location_locked_at" to locationLockedAt,
            "map_link" to mapLink,
            "local_created_at" to createdAt,
            "local_updated_at" to updatedAt
        )

    private fun Customer.toCloudMap(businessId: String): Map<String, Any?> =
        baseCloudMap(businessId) + mapOf(
            "customer_id" to id,
            "name" to name,
            "phone" to phone,
            "address" to address,
            "balance_paise" to balanceCache,
            "is_deleted" to isDeleted,
            "is_blocked" to isBlocked,
            "credit_limit_paise" to creditLimit,
            "local_created_at" to createdAt,
            "local_updated_at" to updatedAt
        )

    private fun Transaction.toCloudMap(businessId: String): Map<String, Any?> =
        baseCloudMap(businessId) + mapOf(
            "transaction_id" to id,
            "txn_ref" to txnRef,
            "unique_hash" to uniqueHash,
            "customer_id" to customerId,
            "amount_paise" to amount,
            "type" to type.name,
            "note" to note,
            "bill_id" to billId,
            "payment_method" to paymentMethod,
            "invoice_no" to invoiceNo,
            "is_deleted" to isDeleted,
            "is_edited" to isEdited,
            "local_created_at" to createdAt,
            "local_updated_at" to updatedAt
        )

    private fun StaffEntity.toAccessCloudMap(businessId: String): Map<String, Any?> =
        baseCloudMap(businessId) + mapOf(
            "staff_id" to id,
            "display_name" to name,
            "role" to role,
            "permissions" to permissions,
            "designation" to designation,
            "is_active" to isActive,
            "is_deleted" to (isDeleted != 0),
            "local_updated_at" to updatedAt
        )

    enum class PartnerRole {
        OWNER,
        PARTNER,
        VIEWER
    }

    companion object {
        private const val TAG = "SelectiveCloudMirror"
        private const val DEFAULT_LEDGER_LIMIT = 500L
        private const val PURGE_QUERY_CHUNK = 400L

        const val COLLECTION_BUSINESSES = "businesses"
        const val COLLECTION_BUSINESS_PROFILES = "business_profiles"
        const val COLLECTION_CUSTOMERS = "customers"
        const val COLLECTION_TRANSACTIONS = "transactions"
        const val COLLECTION_PARTNER_ACCESS = "partner_access"
        const val COLLECTION_STAFF_ACCESS = "staff_access"
    }
}
