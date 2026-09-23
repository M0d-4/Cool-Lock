package com.mod4.cool_lock.logic

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/** Downloads Cool-Lock's own update APK via DownloadManager and hands it to the system installer. */
object ApkDownloader {

    /**
     * Emits download progress (0..100, or -1 while size is unknown) and completes once the file
     * has finished downloading. Throws if the download fails.
     */
    fun download(context: Context, url: String, fileName: String = "cool-lock-update.apk"): Flow<Int> = callbackFlow {
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "").apply { mkdirs() }
        val destFile = File(downloadDir, fileName)
        if (destFile.exists()) destFile.delete()

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Cool-Lock update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            .setDestinationUri(Uri.fromFile(destFile))
            .setAllowedOverMetered(true)

        val downloadId = manager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return
                val query = DownloadManager.Query().setFilterById(downloadId)
                manager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            trySend(100)
                            close()
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            close(IllegalStateException("Download failed"))
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED
        )

        // Poll progress in the background while DownloadManager does the actual work
        var polling = true
        val pollThread = Thread {
            while (polling) {
                try {
                    manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val soFarIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val soFar = if (soFarIdx >= 0) cursor.getLong(soFarIdx) else 0L
                            val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L
                            val pct = if (total > 0) ((soFar * 100) / total).toInt() else -1
                            trySend(pct)
                        }
                    }
                    Thread.sleep(400)
                } catch (e: Exception) {
                    // channel likely closed already; stop polling
                    polling = false
                }
            }
        }
        pollThread.isDaemon = true
        pollThread.start()

        awaitClose {
            polling = false
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w("CoolLockUpdate", "Receiver already unregistered", e)
            }
        }
    }

    /** Launches the system package installer for a downloaded APK. */
    fun installApk(context: Context, fileName: String = "cool-lock-update.apk") {
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "")
        val apkFile = File(downloadDir, fileName)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /** Whether this device requires the user to grant "install unknown apps" before installApk() will work. */
    fun needsInstallPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()
    }
}
