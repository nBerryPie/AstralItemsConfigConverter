package com.nagisberry.aicc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.poi.ss.usermodel.*
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths

object Main {

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    val dir: String = System.getProperty("user.dir")

    fun main() {
        val config = Paths.get(dir, "config.json").toFile().reader().let {
            gson.fromJson<Map<String, Map<String, Map<String, Any>>>>(it)
        }.mapValues { it.value.mapValues { it.value.let(::ColumnConfig) } }
        val main = config["main"] ?: error("main is Not Found")
        Paths.get(dir, "items.xlsx").toFile().let(WorkbookFactory::create).let { workbook ->
            val items = workbook["main"]?.rowIterator()?.asSequence()?.filter { it.rowNum != 0 }?.map { row ->
                (main["file"]?.let {
                    row[it.num]?.stringCellValue
                } ?: error("file column is Not Found")) to JsonObject().apply {
                    main.filterKeys { it != "file" }.forEach {
                        add(it.key, it.value.let { cfg ->
                            row[cfg.num]?.let { cfg.type(it, cfg.option) } ?: cfg.default
                        })
                    }
                }
            }?.groupBy { it.first }?.mapValues {
                it.value.map { it.second }
            } ?: error("Main Sheet is Not Found")
            val elements = config.filterNot { it.key.equals("main", true) }.map {
                it.key to (workbook[it.key]?.rowIterator()?.asSequence()?.filter {
                    it.rowNum != 0
                }?.map { row ->
                    it.value.let {
                        (it["id"]?.let {
                            row[it.num]?.stringCellValue
                        } ?: error("id column is Not Found")) to JsonObject().apply {
                            it.filterKeys { it != "id" }.forEach {
                                add(it.key, it.value.let { cfg ->
                                    row[cfg.num]?.let { cfg.type(it, cfg.option) } ?: cfg.default
                                })
                            }
                        }
                    }
                }?.toMap() ?: error("${it.key} Sheet is Not Found"))
            }.toMap()
            items.map {
                Paths.get(dir, "output", "${it.key}.json") to JsonArray().apply {
                    it.value.map {
                        it.apply {
                            elements.map { element ->
                                element.value[get("id").asString]?.let {
                                    element.key to it
                                }
                            }.filterNotNull().forEach { add(it.first, it.second) }
                        }
                    }.forEach { add(it) }
                }
            }.forEach { pair ->
                pair.first.apply {
                    if (Files.notExists(this.parent)) {
                        Files.createDirectories(this.parent)
                    }
                    if (Files.notExists(this)) {
                        Files.createFile(this)
                    }
                }.toFile().bufferedWriter().use {
                    gson.toJson(pair.second, it)
                }
            }
        }
    }

    fun List<String>.toJsonArray() = JsonArray().apply { this@toJsonArray.forEach { add(it) } }

    inline fun <reified T: Any> Gson.fromJson(json: Reader): T = this.fromJson(json, T::class.java)

    operator fun Workbook.get(name: String): Sheet? = getSheet(name)
    operator fun Sheet.get(rowNum: Int): Row? = getRow(rowNum)
    operator fun Row.get(cellNum: Int): Cell? = getCell(cellNum)
}

fun main(args: Array<String>) {
    Main.main()
}