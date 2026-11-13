package com.redmesastudios.simpletv

import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow

// What the backdrop behind the home screen is on a given day. Ordinary days, the first day of
// a season, Thanksgiving (fall) and Christmas (winter) get the season's hills and trees;
// other holidays get a backdrop of their own as one is built, until then the season's.
// Some holidays have several backdrops to choose from; the preview steps through them.

enum class Room { Pizzeria, Classroom, Bakery, ComputerRoom }

sealed interface BackdropKind {
    data class Outdoors(val season: Season) : BackdropKind
    // St. Patrick's: green hills and trees, nothing drifting, whatever the season.
    data object Emerald : BackdropKind
    data class Indoors(val room: Room) : BackdropKind
}

// The backdrops a holiday can use, the chosen one first. Empty: the season's.
fun backdropOptions(holiday: Holiday?): List<BackdropKind> = when (holiday) {
    Holiday.Christmas -> listOf(BackdropKind.Outdoors(Season.Winter))
    Holiday.StPatricks -> listOf(BackdropKind.Emerald)
    Holiday.PizzaDay -> listOf(BackdropKind.Indoors(Room.Pizzeria))
    Holiday.PiDay -> listOf(Room.Classroom, Room.Bakery, Room.ComputerRoom).map { BackdropKind.Indoors(it) }
    else -> emptyList()
}

// variant picks among a holiday's options (the preview's Up and Down); 0 is the chosen one.
fun backdropFor(holiday: Holiday?, day: LocalDate, variant: Int = 0, zone: ZoneId = ZoneId.systemDefault()): BackdropKind {
    val options = backdropOptions(holiday)
    return if (options.isEmpty()) BackdropKind.Outdoors(seasonOn(day, zone)) else options[Math.floorMod(variant, options.size)]
}

// One stop in the holiday preview: a holiday on the day that shows it best, a family
// birthday or anniversary, or an ordinary day in a season.
data class PreviewEntry(val label: String, val day: LocalDate, val holiday: Holiday?, val family: FamilyDayKind? = null)

data class PreviewState(val entries: List<PreviewEntry>, val index: Int = 0, val variant: Int = 0) {
    val entry get() = entries[index]
    fun step(by: Int) = copy(index = Math.floorMod(index + by, entries.size), variant = 0)
    fun turn(by: Int) = copy(variant = variant + by)
}

// The day of a holiday's stretch that best shows it: its big day where it has one, else
// the last day (when a stretch's greeting comes), or the only one.
fun showingDay(holiday: Holiday, stretch: ClosedRange<LocalDate>): LocalDate {
    val year = stretch.start.year
    return when (holiday) {
        Holiday.NewYear -> stretch.start
        Holiday.Halloween -> LocalDate.of(year, 10, 31)
        Holiday.Christmas -> LocalDate.of(year, 12, 25)
        Holiday.FourthOfJuly -> LocalDate.of(year, 7, 4)
        Holiday.LunarNewYear, Holiday.Passover, Holiday.Hanukkah, Holiday.Ramadan,
        Holiday.Sukkot, Holiday.Navratri, Holiday.RoshHashanah, Holiday.EidAlFitr, Holiday.EidAlAdha -> stretch.start
        Holiday.Diwali -> stretch.start.plusDays(2)
        else -> stretch.endInclusive
    }
}

private val OrdinaryDays = listOf(
    MonthDay.of(1, 27) to "A winter day",
    MonthDay.of(4, 28) to "A spring day",
    MonthDay.of(7, 28) to "A summer day",
    MonthDay.of(10, 8) to "A fall day",
)

// Everything the household's TV can show over the next year, soonest first: each drawn
// holiday in its packs, a birthday and an anniversary if it has any, and an ordinary day in
// each season.
fun previewEntries(
    today: LocalDate,
    packs: Set<Pack>,
    familyDays: List<FamilyDay>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<PreviewEntry> {
    fun next(monthDay: MonthDay): LocalDate = monthDay.atYear(today.year).let { if (it < today) monthDay.atYear(today.year + 1) else it }
    val holidays = Holiday.entries.filter { it in DrawnHolidays && it.pack in packs }.mapNotNull { holiday ->
        (today.year - 1..today.year + 1).asSequence()
            .flatMap { holiday.stretchesIn(it, zone) }
            .filter { it.endInclusive >= today }
            .minByOrNull { it.start }
            ?.let { PreviewEntry(holidayName(holiday), showingDay(holiday, it), holiday) }
    }
    val family = FamilyDayKind.entries.mapNotNull { kind ->
        familyDays.filter { it.kind == kind }.minByOrNull { next(it.day) }
            ?.let { PreviewEntry(if (kind == FamilyDayKind.Birthday) "A birthday" else "An anniversary", next(it.day), null, kind) }
    }
    val ordinary = OrdinaryDays.map { (day, label) -> PreviewEntry(label, next(day), null) }
    return (holidays + family + ordinary).sortedBy { it.day }
}

fun holidayName(holiday: Holiday): String = when (holiday) {
    Holiday.NewYear -> "New Year's"
    Holiday.MlkDay -> "Martin Luther King Jr. Day"
    Holiday.SuperBowl -> "Game day"
    Holiday.Valentines -> "Valentine's Day"
    Holiday.PresidentsDay -> "Presidents' Day"
    Holiday.StPatricks -> "St. Patrick's Day"
    Holiday.MothersDay -> "Mother's Day"
    Holiday.FathersDay -> "Father's Day"
    Holiday.FourthOfJuly -> "Fourth of July"
    Holiday.GrandparentsDay -> "Grandparents Day"
    Holiday.ElectionDay -> "Election Day"
    Holiday.VeteransDay -> "Veterans Day"
    Holiday.AshWednesday -> "Ash Wednesday"
    Holiday.GoodFriday -> "Good Friday"
    Holiday.LunarNewYear -> "Lunar New Year"
    Holiday.EarthDay -> "Earth Day"
    Holiday.CincoDeMayo -> "Cinco de Mayo"
    Holiday.DiaDeLosMuertos -> "Día de los Muertos"
    Holiday.PopcornDay -> "Popcorn Day"
    Holiday.GroundhogDay -> "Groundhog Day"
    Holiday.PizzaDay -> "Pizza Day"
    Holiday.PiDay -> "Pi Day"
    Holiday.AprilFools -> "April Fools' Day"
    Holiday.BitcoinPizzaDay -> "Bitcoin Pizza Day"
    Holiday.FirstDayOfSpring -> "First day of spring"
    Holiday.FirstDayOfSummer -> "First day of summer"
    Holiday.FirstDayOfFall -> "First day of fall"
    Holiday.FirstDayOfWinter -> "First day of winter"
    Holiday.RoshHashanah -> "Rosh Hashanah"
    Holiday.YomKippur -> "Yom Kippur"
    Holiday.EidAlFitr -> "Eid al-Fitr"
    Holiday.EidAlAdha -> "Eid al-Adha"
    else -> holiday.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
}

private val PreviewDate = DateTimeFormatter.ofPattern("EEE, MMM d")

// The strip along the bottom while previewing.
fun previewCaption(state: PreviewState): String {
    val entry = state.entry
    val options = backdropOptions(entry.holiday).size
    val backdrop = if (options > 1) " · Backdrop ${Math.floorMod(state.variant, options) + 1} of $options (▲ ▼)" else ""
    return "Preview · ${entry.label} · ${entry.day.format(PreviewDate)}$backdrop · ◀ ▶ to change · Back to leave"
}

// A date to show the scene for instead of today (adb's scene_date), or the holiday preview
// started from Settings. Pressing Home ends either.
object ScenePreview {
    val date = MutableStateFlow<LocalDate?>(null)
    val state = MutableStateFlow<PreviewState?>(null)

    fun stop() {
        date.value = null
        state.value = null
    }
}
