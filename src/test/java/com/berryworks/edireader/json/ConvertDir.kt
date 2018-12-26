package com.berryworks.edireader.json

import java.io.File
import java.io.Writer
import java.util.logging.Level
import java.util.logging.Logger
import javax.json.*
import javax.json.stream.JsonGenerator

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 12/26/2018.
 */
fun main(args: Array<String>) {
    val log = Logger.getLogger("ConvertDir.kt")

    val transform =
        args.contains("--transform")

    val srcDir =
        File(".")

    val outDir =
        srcDir
            .canonicalFile
            .let { it.resolveSibling("${it.name}-com.berryworks.edireader.json") }
            .also { it.mkdir() }

    val ediToJson =
        EdiToJson().apply {
            isAnnotated = true
            isFormatting = true
        }
    for (srcFile in srcDir.walk()) {
        if (!srcFile.isFile) continue
        log.info(srcFile.path)
        srcFile.reader().use { reader ->
            val jsonObject =
                try {
                    ediToJson.asJson(reader).parse()
                } catch (e: Exception) {
                    log.log(Level.WARNING, "$srcFile error:", e)
                    null
                }

            jsonObject?.also { ediJson ->
                val outJsons: List<JsonObject> =
                    when (transform) {
                        true -> ediJson.transform()
                        else -> listOf(ediJson)
                    }

                outJsons.forEach { outJson ->
                    val outFile =
                        if (transform)
                            outDir.resolve("${outJson._id} (${srcFile.name}).json") else
                            outDir.resolve("${srcFile.name}.json")

                    outFile.writer().use { writer ->
                        outJson.write(writer)
                    }
                }
            }
        }
    }
}

private val JsonObject._id: String
    get() = getString("_id", "")

private fun _id(st: JsonObject, ISA: JsonObject): String {
    val ISA_09_Date = ISA.getString("ISA_09_Date", "")
    val ISA_10_Time = ISA.getString("ISA_10_Time", "")
    val ISA_13_InterchangeControlNumber = ISA.getString("ISA_13_InterchangeControlNumber", "")
    val ST_01_TransactionSetIdentifierCode = st.getString("ST_01_TransactionSetIdentifierCode", "")
    return "$ISA_09_Date-$ISA_10_Time-$ISA_13_InterchangeControlNumber-$ST_01_TransactionSetIdentifierCode"
}

private fun JsonObject.transform(): List<JsonObject> {
    val interchanges =
        this["interchanges"] as? JsonArray ?: return emptyList()
    val result = mutableListOf<JsonObject>()
    for (isa in interchanges) {
        if (isa !is JsonObject) continue
        val ISA = isa.copyScalars()
        val functional_groups =
            isa["functional_groups"] as? JsonArray ?: continue
        for (gs in functional_groups) {
            if (gs !is JsonObject) continue
            val GS = gs.copyScalars()
            val transactions =
                gs["transactions"] as? JsonArray ?: continue
            for (st in transactions) {
                if (st !is JsonObject) continue
                val item = Json.createObjectBuilder()
                item.add("_id", _id(st, ISA))
                item.add("ISA", ISA)
                item.add("GS", GS)
                st.forEach { name, value ->
                    item.add(name, value)
                }

                result += item.build()
            }
        }
    }
    return result
}

private fun JsonObject.copyScalars(): JsonObject =
    Json.createObjectBuilder()
        .also { result ->
            for ((key, value) in this) {
                if (value.valueType == JsonValue.ValueType.OBJECT) continue
                if (value.valueType == JsonValue.ValueType.ARRAY) continue
                result.add(key, value)
            }
        }
        .build()


private val jsonWriterFactoryPP: JsonWriterFactory by lazy {
    Json.createWriterFactory(mapOf(JsonGenerator.PRETTY_PRINTING to true))
}

private fun String.parse(): JsonObject =
    Json.createReader(reader()).readObject()

private fun JsonObject.write(writer: Writer) {
    jsonWriterFactoryPP.createWriter(writer).write(this)
}

