package com.nagisberry.aicc

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.apache.poi.ss.usermodel.Cell

enum class  Types(
        private val toValue: (Cell, Map<String, Any>) -> JsonElement,
        private val toDefault: (Any?) -> JsonElement
) {
    STRING({ cell, option ->
        cell.stringCellValue.let(::JsonPrimitive)
    }, { ((it as? String?) ?: "").let(::JsonPrimitive) }),
    INT({ cell, option ->
        cell.numericCellValue.toInt().let(::JsonPrimitive)
    }, { ((it as? Number?) ?: 0).toInt().let(::JsonPrimitive) }),
    ARRAY_STRING({ cell, option ->
        JsonArray().apply {
            cell.stringCellValue.split(option["delimiter"] as? String ?: ",").forEach {
                add(it)
            }
        }
    }, { JsonArray().apply { ((it as? String?) ?: "").forEach { add(it) } } });

    operator fun invoke(cell: Cell, option: Map<String, Any>) = toValue(cell, option)
    fun getDefault(default: Any?) = toDefault(default)
}