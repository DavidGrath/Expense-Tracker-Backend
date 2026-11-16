package com.davidgrath.expensetrackerbackend.utils

import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.CATEGORY
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER_LOCATION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.google.gson.Gson
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.floor
import kotlin.math.min


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
    val hash = getSha256AsBytes(inputStream)
    val hashText = String.format("%064x", BigInteger(1, hash))
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

fun getSha256AsBytes(inputStream: InputStream): ByteArray {

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
    LOGGER.info("getSha256: read {} bytes", total)
    return hash
}

fun sha256ToString(bytes: ByteArray): String {
    val hashText = String.format("%064x", BigInteger(1, bytes))
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

fun uuidToBytes(uuid: UUID): ByteArray {
    val msb = uuid.mostSignificantBits
    val lsb = uuid.leastSignificantBits
    val bytes = ByteArray(16)
    bytes[0] = (msb shr (7 * 8) and 0xFF).toByte()
    bytes[1] = (msb shr (6 * 8) and 0xFF).toByte()
    bytes[2] = (msb shr (5 * 8) and 0xFF).toByte()
    bytes[3] = (msb shr (4 * 8) and 0xFF).toByte()
    bytes[4] = (msb shr (3 * 8) and 0xFF).toByte()
    bytes[5] = (msb shr (2 * 8) and 0xFF).toByte()
    bytes[6] = (msb shr (1 * 8) and 0xFF).toByte()
    bytes[7] = (msb shr (0 * 8) and 0xFF).toByte()

    bytes[8] = (lsb shr (7 * 8) and 0xFF).toByte()
    bytes[9] = (lsb shr (6 * 8) and 0xFF).toByte()
    bytes[10] = (lsb shr (5 * 8) and 0xFF).toByte()
    bytes[11] = (lsb shr (4 * 8) and 0xFF).toByte()
    bytes[12] = (lsb shr (3 * 8) and 0xFF).toByte()
    bytes[13] = (lsb shr (2 * 8) and 0xFF).toByte()
    bytes[14] = (lsb shr (1 * 8) and 0xFF).toByte()
    bytes[15] = (lsb shr (0 * 8) and 0xFF).toByte()
    return bytes
}

fun bytesToUUID(bytes: ByteArray): UUID {
//    return UUID.nameUUIDFromBytes(bytes)
    var high = 0L
    for(i in 0..7) {
        val num = (bytes[i].toLong() and 0xFF) shl ((7-i) * 8)
        high = high or num
    }
    var low = 0L
    for(i in 0..7) {
        val num = (bytes[i+8].toLong() and 0xFF) shl ((7-i) * 8)
        low = low or num
    }
    val uuid = UUID(high, low)
    return uuid
}

fun binaryIndexSearchPrevious(sortKeys: IntArray, previous: Int, current: Int): Int? {
    return null
}

//TODO Test cases: range filled up, first half of range filled up, range mostly filled up except for 1 free slot, half range mostly filled up except for 1 free slot
fun binaryIndexSearchNext(sortKeys: IntArray, current: Int, next: Int): Int? {
    return null
}

fun interface Mover<T: Comparable<T>> {
    fun moveSortKey(id: T, sortKey: Short)
}


class DatabaseMover<T: Comparable<T>>: Mover<T> {
    override fun moveSortKey(id: T, sortKey: Short) {
        //UPDATE TABLE SET SORT_KEY = :sortKey WHERE ID = :id
    }
}

/**
 * It's quite inconvenient to reorganize elements if it's impossible to have a gap between each one, therefore it
 * throws an exception when [maxIndexOfCollection] / the size of the elements, [idToKeyMap] <= 2
 */
fun <T: Comparable<T>> getNextSortKey(maxSortKey: Short, idToKeyMap: Map<T, Short>, mover: Mover<T>, maxIndexOfCollection: BigInteger = BigInteger(Short.MAX_VALUE.toString()), preferredSpacingFactor: Short = 1_000): Short {
    require(preferredSpacingFactor > 1)
    if(idToKeyMap.isEmpty()) {
        return preferredSpacingFactor.toShort()
    }
    val sparseness = BigDecimal(maxIndexOfCollection).setScale(8)/BigDecimal(idToKeyMap.size)
    LOGGER.debug("Sparseness: {}; {}", sparseness, idToKeyMap.size)
    if(sparseness <= BigDecimal(2)) {
        throw RuntimeException("Collection is not sparse enough")
    }

    var next = if(maxIndexOfCollection - BigInteger(maxSortKey.toString()) < BigInteger(preferredSpacingFactor.toString())) {
        maxIndexOfCollection.toShort()
    } else {
        (maxSortKey + preferredSpacingFactor).toShort()
    }
    LOGGER.debug("Next: {}", next)

    if(next - maxSortKey <= preferredSpacingFactor) {
        LOGGER.info("Gap to next key is too small. Rebalancing")

        val (spacingFactor, newMax) = rebalance(preferredSpacingFactor, maxIndexOfCollection, idToKeyMap, mover)
        return (newMax + spacingFactor).toShort()
    }
    return next
}

/**
 * TODO I think I can also rebalance by using row_number * preferredSpacingFactor at the repository level, but for now we use this
 * The most important part is that it keeps the relative ordering between elements. It should only make the gaps between them even
 * @return The chosen spacing factor used and the last sort key generated
 */
fun <T: Comparable<T>> rebalance(preferredSpacingFactor: Short, maxIndexOfCollection: BigInteger, sortKeyMap: Map<T, Short>, mover: Mover<T>): Pair<Short, Short> {

    require(maxIndexOfCollection.compareTo(BigInteger.ONE)  > 0)
    //for i in 1..existingSortKeys.size if existingSortKeys[i] > maxIndex throe IllegalArgumentException


    val currentOccupantCount = sortKeyMap.size

    val copy = sortKeyMap.toMap()
    copy.entries.sortedByDescending { it.value }.onEachIndexed {i, (id, sortKey) ->
        val index = BigInteger(i.toString())
        mover.moveSortKey(id, (maxIndexOfCollection-(index)).toShort())
    }
    val spacingFactor = floor(maxIndexOfCollection.toDouble() / currentOccupantCount).toInt().toShort()
    val chosenSpacingFactor = min(preferredSpacingFactor.toInt(), spacingFactor.toInt()).toShort()
    val newCopy = sortKeyMap.toMap()
    var newMax: Short = -1
    newCopy.entries.sortedBy { it.value }.onEachIndexed {i, (id, sortKey) ->
        val newPos = (i+1) * chosenSpacingFactor
        mover.moveSortKey(id, newPos.toShort())
        newMax = newPos.toShort() //In reality, only the last assignment is needed
    }
    return chosenSpacingFactor to newMax
}

/**
 * @param slots assumed to be sorted
 * @param max It is assumed that 1 is the starting point, so the range 1..max is searched
 */
fun findEarliestOccupancySlot(slots: ShortArray, max: Int): Short? {
    //TODO singular gap m such that 1<m<n and 1<=n<max is filled up
    require(max >= 1) {
        "Max must be positive"
    }
    val expanded = BooleanArray(max)
    if(slots.size > max) {
        throw IllegalArgumentException("List too large")
    }
    for(i in slots) {
        if(i !in 1..max) {
            throw IllegalArgumentException("Element out of bounds")
        }
        expanded[i - 1] = true
    }
    for(i in 1..max) {
        if(!expanded[i-1]) {
            return i.toShort()
        }
    }
    return null
}

/**
 * TODO move to StatisticsRepository once I've tested and reworked the other methods
 */
fun <T: Record> applyFilters(sStep: SelectConditionStep<T>, statisticsFilter: StatisticsFilterDb): SelectConditionStep<T> {
    var selectStep = sStep
    if(statisticsFilter.fromDate != null) {
        selectStep = selectStep.and(TRANSACTION.DATED_AT.greaterOrEqual(statisticsFilter.fromDate))
    }
    if(statisticsFilter.toDate != null) {
        selectStep = selectStep.and(TRANSACTION.DATED_AT.lessOrEqual(statisticsFilter.toDate))
    }
    if(statisticsFilter.accountIds.isNotEmpty() && statisticsFilter.currencyCode == null) {
        selectStep = selectStep.and(ACCOUNT.ID.`in`(statisticsFilter.accountIds))
    }
    if(statisticsFilter.currencyCode != null) {
        selectStep = selectStep.and(ACCOUNT.CURRENCY_CODE.`eq`(statisticsFilter.currencyCode.toString()))
    }
    if(statisticsFilter.weekDays.isNotEmpty()) {
        val weekdaysInts = statisticsFilter.weekDays.map { it.value }
        selectStep = selectStep.and(DSL.isoDayOfWeek(TRANSACTION.DATED_AT).`in`(weekdaysInts))
    }
    if(statisticsFilter.categoryIds.isNotEmpty()) {
        selectStep = selectStep.and(CATEGORY.ID.`in`(statisticsFilter.categoryIds))
    }
    if(statisticsFilter.transactionModes.isNotEmpty()) {
        selectStep = selectStep.and(TRANSACTION.MODE.`in`(statisticsFilter.transactionModes))
    }
    if(statisticsFilter.sellerIds.isNotEmpty() || statisticsFilter.sellerLocationIds.isNotEmpty()) {
        var condition = SELLER.ID.`in`(statisticsFilter.sellerIds)
        if(statisticsFilter.sellerLocationIds.isNotEmpty()) {
            condition = condition.or(SELLER_LOCATION.ID.`in`(statisticsFilter.sellerLocationIds))
        }
        selectStep = selectStep.and(condition)
    }
    return selectStep
}

val invalidRegex = Regex("^[\\p{Z}\\p{M}\\p{Cf}]+$")
val invalidRegexControl = Regex("\\p{Cc}") //TODO ZWJ, ZWNJ, SOFT HYPHEN, consider a sanitisation pipeline
val invalidRegexControlExceptCrLfTab = Regex("[\\p{Cc}&&[^\\r\\n\\t]]")
fun validateSearchString(queryString: String): Boolean {
    return validateGenericSingleLineString(queryString)
}

fun validateGenericSingleLineString(string: String): Boolean {
//    val trimmed = string.trim() //TODO Sanitisation should be considered separately, I think
    if(string.isBlank()) {
        return false
    }
    if(string.matches(invalidRegex)) {
        return false
    }
    if(string.contains(invalidRegexControl)) {
        return false
    }
    return true
}