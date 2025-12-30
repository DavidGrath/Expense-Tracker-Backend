package com.davidgrath.expensetrackerbackend.utils

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest


private val LOGGER = LoggerFactory.getLogger("com.davidgrath.expensetrackerbackend.Utils")


val CORE_CATEGORIES = setOf(
    "food",
    "rent",
    "utilities",
    "transportation",
    "healthcare",
    "debt",
    "childcare",
    "household",
    "entertainment",
    "self_care",
    "clothing",
    "education",
    "gifts_and_donations",
    "emergency",
    "savings",
    "fitness",
    "miscellaneous",
)

val CATEGORY_IDS_ICONS_DEFAULT = mapOf(
    "food" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:restaurant",
    "rent" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:home",
    "utilities" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:construction",
    "transportation" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:directions_car",
    "healthcare" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:medical_services",
    "debt" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:credit_card",
    "childcare" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:child_care",
    "household" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:household_supplies",
    "entertainment" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:tv",
    "self_care" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:self_care",
    "clothing" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:checkroom",
    "education" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:book",
    "gifts_and_donations" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:featured_seasonal_and_gifts",
    "emergency" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:emergency",
    "savings" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:savings",
    "fitness" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:fitness_center",
    "miscellaneous" to "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category",
)

fun getSha256(inputStream: InputStream): String {

        val bufSize = DEFAULT_BUFFER_SIZE
        val bufferedStream = inputStream.buffered()
        val md = MessageDigest.getInstance("SHA256")
        var array = ByteArray(bufSize)
        var len = 0
        var total = 0L //TODO UOM/UCOM
        while (len >= 0) {
            len = bufferedStream.read(array)
            if (len >= 0) {
                total += len
                md.update(array, 0, len)
            }
        }
        val hash = md.digest()
        val hashText = String.format("%064x", BigInteger(1, hash))
        LOGGER.info("getSha256: read {} bytes", total)
        LOGGER.info("getSha256: hash: {}", hashText)
        return hashText

}

fun getSha256(byteArray: ByteArray): String {

    val bufSize = DEFAULT_BUFFER_SIZE

    val md = MessageDigest.getInstance("SHA256")

    var len = byteArray.size

    md.update(byteArray, 0, len)
    val hash = md.digest()
    val hashText = String.format("%064x", BigInteger(1, hash))
    LOGGER.info("getSha256: read {} bytes", len)
    LOGGER.info("getSha256: hash: {}", hashText)
    return hashText

}


fun file(file: File, vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(file, fullPath)
}

val nonExistentSymbols = hashSetOf(
    "antigravity",
    "arrows_left_right_circle",
    "arrows_up_down_circle",
    "b_circle",
    "bookmark_stacks",
    "cards_stack",
    "chevron_line_up",
    "circle_circle",
    "computer_sound",
    "dashboard_2_edit",
    "dashboard_2_gear",
    "detection_and_zone_off",
    "eyebrow",
    "format_image_back",
    "format_image_break_left",
    "format_image_break_right",
    "format_image_front",
    "format_image_inline_left",
    "format_image_inline_right",
    "game_bumper_left",
    "game_bumper_right",
    "game_button_l",
    "game_button_l1",
    "game_button_l2",
    "game_button_r",
    "game_button_r1",
    "game_button_r2",
    "game_button_zl",
    "game_button_zr",
    "game_stick_l3",
    "game_stick_left",
    "game_stick_r3",
    "game_stick_right",
    "game_trigger_left",
    "game_trigger_right",
    "gamepad_circle_down",
    "gamepad_circle_left",
    "gamepad_circle_right",
    "gamepad_circle_up",
    "gamepad_down",
    "gamepad_left",
    "gamepad_right",
    "gamepad_up",
    "graph_8",
    "hourglass_check",
    "lips",
    "mic_gear",
    "mobile_unlock",
    "music_note_2",
    "notification_audio",
    "notification_audio_off",
    "passport",
    "person_text",
    "rectangle_add",
    "square_circle",
    "sticker",
    "sticker_add",
    "thermometer_alert",
    "triangle_circle",
    "undereye",
    "video_template",
    "voice_chat_off",
    "watch_lock",
    "widget_menu",
    "x_circle",
    "y_circle"
)

fun loadMaterialSymbolsIcons(classLoader: ClassLoader): Set<String> {

        val gson = Gson()
        val inputStreamReader = classLoader.getResourceAsStream("material_metadata.json").bufferedReader()
        val materialMetadata = gson.fromJson(inputStreamReader, MaterialMetadata::class.java)
        inputStreamReader.close()
        val symbolsOutlined = materialMetadata.icons.filter {
            !it.unsupported_families.contains(MaterialMetadata.MaterialIconFamily.MaterialSymbolsOutlined)
        }.filter {
            it.name !in nonExistentSymbols
        }.map {
            it.name
        }.toHashSet()
        return symbolsOutlined

}