package com.vetpocket.app.ui.scanner

import android.content.Context
import java.io.File

object TessDataHelper {

    /** Копирует файлы обучений из assets в filesDir/tesseract/tessdata, если их там ещё нет. */
    fun ensureTessData(context: Context) {
        val tessDir = File(context.filesDir, "tesseract")
        val tessdataDir = File(tessDir, "tessdata")
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs()
        }

        listOf("rus.traineddata", "eng.traineddata").forEach { fileName ->
            val destFile = File(tessdataDir, fileName)
            if (!destFile.exists()) {
                context.assets.open("tessdata/$fileName").use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}