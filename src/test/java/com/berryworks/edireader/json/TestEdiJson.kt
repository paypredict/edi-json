package com.berryworks.edireader.json

import java.io.File

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 12/26/2018.
 */
fun main(args: Array<String>) {
    val json =
        File(args[0])
            .reader()
            .use { reader ->
                EdiToJson()
                    .asJson(reader)
            }
    println(json)
}

