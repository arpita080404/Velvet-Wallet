package com.velvetwallet.app.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.velvetwallet.app.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun export(context: Context, expenses: List<Expense>, fileName: String = "velvet_wallet_export.csv"): Result<String> {
        return runCatching {
            val csv = buildCsv(expenses)
            val stream = openOutputStream(context, fileName)
            stream.use { it.write(csv.toByteArray()) }
            "Exported ${expenses.size} transactions to Downloads/$fileName"
        }
    }

    private fun buildCsv(expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Category,Type,Amount,Notes,Recurring")
        expenses.forEach { e ->
            val date = dateFormat.format(Date(e.date))
            val notes = e.notes.replace(",", ";").replace("\n", " ")
            sb.appendLine("$date,\"${e.title}\",${e.category},${e.type},${e.amount},\"$notes\",${e.isRecurring}")
        }
        return sb.toString()
    }

    private fun openOutputStream(context: Context, fileName: String): OutputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Could not create file in Downloads")
            context.contentResolver.openOutputStream(uri)
                ?: throw Exception("Could not open output stream")
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            FileOutputStream(File(dir, fileName))
        }
    }
}
