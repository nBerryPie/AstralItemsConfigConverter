package com.nagisberry.aicc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.poi.ss.usermodel.*
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths

val gson = GsonBuilder().setPrettyPrinting().create()

val items: MutableMap<String, MutableList<JsonObject>> = mutableMapOf()

fun main(args: Array<String>) {
    val config = Paths.get(System.getProperty("user.dir"), "config.json").toFile().reader().let {
        gson.fromJson<Map<String, Map<String, Int>>>(it).mapValues { it.value.mapValues { it.value.toInt() } }
    }
    Paths.get(System.getProperty("user.dir"), "items.xlsx").toFile().let(WorkbookFactory::create).let { workbook ->
        workbook["main"]?.rowIterator()?.asSequence()?.filter { it.rowNum != 0 }?.forEach {
            JsonObject().apply {
                addProperty("id", it[config["main"]?.get("id") ?: 0]?.stringCellValue ?: "")
                addProperty("material", it[config["main"]?.get("material") ?: 1]?.stringCellValue ?: "STICK")
                addProperty("damage", it[config["main"]?.get("damage") ?: 2]?.numericCellValue?.toInt() ?: 0)
                addProperty("name", it[config["main"]?.get("name") ?: 3]?.stringCellValue ?: "Unnamed")
                add("description", it[config["main"]?.get("description") ?: 4]?.stringCellValue?.split("\n")?.toJsonArray() ?: JsonArray())
                addProperty("rarity", it[config["main"]?.get("rarity") ?: 5]?.numericCellValue?.toInt() ?: 0)
                add("types", it[config["main"]?.get("types") ?: 6]?.stringCellValue?.split(",")?.toJsonArray() ?: JsonArray())
            }.let { item -> addItem(it[config["main"]?.get("file") ?: 7]?.stringCellValue ?: "default", item) }
        } ?: error("Main Sheet is Not Found")
        //val elements = config.filterNot { it.key.equals("main", true) }.map { cfg ->
        //    cfg.key to (workbook[cfg.key]?.rowIterator()?.asSequence()?.filter { it.rowNum != 0 }?.map { row ->
        //        row[cfg.value["id"] ?: 0]?.stringCellValue to cfg.value.filter { it.key != "id" }.let {
        //            JsonObject().apply {
        //                it.forEach { addProperty(it.key, row[it.value]?.stringCellValue ?: "") }
        //            }
        //        }
        //    }?.toMap() ?: error("${cfg.key} Sheet is Not Found"))
        //}.toMap()
        items.map {
            JsonArray().apply {
                it.value.forEach {
                    add(it)
                }
            } to Paths.get(System.getProperty("user.dir"), "output", "${it.key}.json")
        }.forEach { pair ->
            pair.second.apply {
                if (Files.notExists(this.parent)) Files.createDirectories(this.parent)
                if (Files.notExists(this)) Files.createFile(this)
            }.toFile().bufferedWriter().use {
                gson.toJson(pair.first, it)
            }
        }
    }
}

fun addItem(fileName: String, item: JsonObject) {
    items.getOrPut(fileName) { mutableListOf() } += item
}

fun List<String>.toJsonArray() = JsonArray().apply { forEach { add(it) } }

inline fun <reified T: Any> Gson.fromJson(json: Reader): T = this.fromJson(json, T::class.java)

operator fun Workbook.get(name: String): Sheet? = getSheet(name)
operator fun Sheet.get(rowNum: Int): Row? = getRow(rowNum)
operator fun Row.get(cellNum: Int): Cell? = getCell(cellNum)