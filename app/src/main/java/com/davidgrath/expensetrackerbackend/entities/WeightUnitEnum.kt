package com.davidgrath.expensetrackerbackend.entities

enum class WeightUnitEnum(val stringVal: String) {
    Gram("g"),
    Pound("[lb_av]"),
    Ounce("[oz_av]")
}

enum class VolumeUnitEnum(val stringVal: String) {
    Liter("l"),
    FluidOunce("[foz_us]"),
    Gallon("[gal_us]")
}

enum class UnitPrefixEnum(val stringVal: String) {
    Milli("m"),
    Centi("c"),
    Kilo("k"),
    Mega("M")
}