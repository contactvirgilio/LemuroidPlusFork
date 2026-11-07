package com.swordfish.lemuroid.app.shared.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Bundle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.ui.CenterCards
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import com.swordfish.lemuroid.lib.android.RetrogradeActivity
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import javax.inject.Inject

class StorageFrameworkPickerLauncher : RetrogradeActivity() {
    @Inject
    lateinit var directoriesManager: DirectoriesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    this.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    this.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    this.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                }
            try {
                startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
            } catch (e: Exception) {
                showStorageAccessFrameworkNotSupportedDialog()
            }
        }
    }

    private fun showStorageAccessFrameworkNotSupportedDialog() {
        val message = getString(R.string.dialog_saf_not_found, directoriesManager.getInternalRomsDirectory())
        val actionLabel = getString(R.string.ok)
        displayErrorDialog(message, actionLabel) { finish() }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            val newValue = resultData?.data
            if (newValue != null) saveFolder(newValue)
            startLibraryIndexWork()
        }
        finish()
    }

    private val slot: Int
        get() = intent.getIntExtra(EXTRA_SLOT, 1)

    private fun saveFolder(newValue: Uri) {
        val sharedPreferences = SharedPreferencesHelper.getLegacySharedPreferences(this)
        val ownKey = getString(keyResFor(slot))
        val otherKey = getString(keyResFor(if (slot == 1) 2 else 1))

        val currentValue: String? = sharedPreferences.getString(ownKey, null)
        val otherValue: String? = sharedPreferences.getString(otherKey, null)

        if (newValue.toString() == currentValue) return

        if (otherValue != null && overlaps(Uri.parse(otherValue), newValue)) {
            CenterCards.showOrToast(
                this,
                getString(R.string.folders_card_title),
                getString(R.string.folders_card_overlap),
            )
            return
        }

        contentResolver.takePersistableUriPermission(newValue, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Only the folder this one replaces is released: the other folder keeps its access.
        currentValue?.let { releaseUnlessUsed(Uri.parse(it), keep = listOfNotNull(otherValue, newValue.toString())) }

        sharedPreferences.edit().putString(ownKey, newValue.toString()).apply()
    }

    private fun startLibraryIndexWork() {
        LibraryIndexScheduler.scheduleLibrarySync(applicationContext)
    }

    private fun releaseUnlessUsed(
        uri: Uri,
        keep: List<String>,
    ) {
        releaseIfUnused(this, uri, keep)
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1
        private const val EXTRA_SLOT = "slot"

        fun keyResFor(slot: Int): Int =
            if (slot == 2) {
                com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder_2
            } else {
                com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder
            }

        /** Same folder, or one inside the other: the games would be scanned twice. */
        fun overlaps(
            first: Uri,
            second: Uri,
        ): Boolean {
            val a = runCatching { DocumentsContract.getTreeDocumentId(first) }.getOrNull() ?: return false
            val b = runCatching { DocumentsContract.getTreeDocumentId(second) }.getOrNull() ?: return false
            if (first.authority != second.authority) return false
            return a == b || a.startsWith("$b/") || b.startsWith("$a/") ||
                (a.endsWith(":") && b.startsWith(a)) || (b.endsWith(":") && a.startsWith(b))
        }

        private fun releaseIfUnused(
            context: Context,
            uri: Uri,
            keep: List<String>,
        ) {
            if (keep.contains(uri.toString())) return
            runCatching {
                context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        fun pickFolder(
            context: Context,
            slot: Int = 1,
        ) {
            context.startActivity(
                Intent(context, StorageFrameworkPickerLauncher::class.java).putExtra(EXTRA_SLOT, slot),
            )
        }

        /** Forgets the second folder. Its games leave the library on the next scan. */
        fun removeSecondFolder(context: Context) {
            val prefs = SharedPreferencesHelper.getLegacySharedPreferences(context)
            val key = context.getString(keyResFor(2))
            val old = prefs.getString(key, null)
            prefs.edit().remove(key).apply()
            val first = prefs.getString(context.getString(keyResFor(1)), null)
            old?.let { releaseIfUnused(context, Uri.parse(it), listOfNotNull(first)) }
            LibraryIndexScheduler.scheduleLibrarySync(context.applicationContext)
        }
    }
}
