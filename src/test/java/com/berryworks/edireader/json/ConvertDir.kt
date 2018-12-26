package com.berryworks.edireader.json

import java.io.File
import java.io.IOException

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 12/26/2018.
 */
fun main(args: Array<String>) {
    val outDir =
        File("com.berryworks.edireader.json")
            .absoluteFile
            .also { if (!it.isDirectory) throw IOException("Out directory $it not found") }

    val ediToJson =
        EdiToJson().apply {
            isAnnotated = true
            isFormatting = true
        }
    for (srcFile in File(".").listFiles()) {
        if (!srcFile.isFile) continue
        val outFile = outDir.resolve(srcFile.name + ".json")
        srcFile.reader().use { reader ->
            outFile.writer().use { writer ->
                try {
                    ediToJson.asJson(reader, writer)
                } catch (e: Exception) {
                    System.err.println("$srcFile error:")
                    e.printStackTrace()
                }
            }
        }
    }
}