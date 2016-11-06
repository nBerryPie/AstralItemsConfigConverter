package com.nagisberry.aicc

import com.google.gson.JsonPrimitive

class ColumnConfig(cfg: Map<String, Any>) {
    val num = cfg["column"]?.let {
        it as? Number ?: error("column is not Number")
    }?.toInt() ?: error("column is Not Found")
    val type = cfg["type"]?.let {
        it as? String ?: error("type is not String")
    }?.let { Types.valueOf(it.toUpperCase()) } ?: error("type is not found")
    val default = cfg["default"].let { type.getDefault(it) }
    val option = cfg.filterNot {
        listOf("column", "type", "default").contains(it.key)
    }

    override fun toString() = "{num=$num, type=$type, default=$default, option=$option}"
}