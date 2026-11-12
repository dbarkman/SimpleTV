package com.redmesastudios.simpletv

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.pow

// Which decoration the home screen shows on a date. Holidays come in packs, and a household
// picks its packs in home.txt (holiday_packs=us,christian,…). Each holiday shows over a
// stretch of days; where two overlap, the shorter one wins its days, so a one-day fun day
// pops up inside a longer stretch. The Seasons pack's days (the first day of each season)
// show only when no other holiday does.

enum class Pack(val key: String) {
    UsNational("us"),
    Christian("christian"),
    World("world"),
    FunDays("fun"),
    Seasons("seasons"),
    Jewish("jewish"),
    Muslim("muslim"),
    Indian("indian"),
}

val DefaultPacks = setOf(Pack.UsNational, Pack.Christian, Pack.World, Pack.FunDays, Pack.Seasons)

// "us, christian, fun" → those packs; blank → the default ones. Unknown names are skipped.
fun parsePacks(text: String): Set<Pack> {
    if (text.isBlank()) return DefaultPacks
    val keys = text.split(',').map { it.trim().lowercase() }
    return Pack.entries.filter { it.key in keys }.toSet()
}

enum class Holiday(val pack: Pack) {
    NewYear(Pack.UsNational),
    MlkDay(Pack.UsNational),
    SuperBowl(Pack.UsNational),
    Valentines(Pack.UsNational),
    PresidentsDay(Pack.UsNational),
    StPatricks(Pack.UsNational),
    MothersDay(Pack.UsNational),
    MemorialDay(Pack.UsNational),
    Juneteenth(Pack.UsNational),
    FathersDay(Pack.UsNational),
    FourthOfJuly(Pack.UsNational),
    LaborDay(Pack.UsNational),
    GrandparentsDay(Pack.UsNational),
    Halloween(Pack.UsNational),
    ElectionDay(Pack.UsNational),
    VeteransDay(Pack.UsNational),
    Thanksgiving(Pack.UsNational),
    Christmas(Pack.UsNational),

    Epiphany(Pack.Christian),
    AshWednesday(Pack.Christian),
    Easter(Pack.Christian),
    GoodFriday(Pack.Christian),
    Pentecost(Pack.Christian),

    LunarNewYear(Pack.World),
    EarthDay(Pack.World),
    CincoDeMayo(Pack.World),
    Oktoberfest(Pack.World),
    DiaDeLosMuertos(Pack.World),

    PopcornDay(Pack.FunDays),
    GroundhogDay(Pack.FunDays),
    PizzaDay(Pack.FunDays),
    PiDay(Pack.FunDays),
    AprilFools(Pack.FunDays),
    BitcoinPizzaDay(Pack.FunDays),
    DonutDay(Pack.FunDays),
    EmojiDay(Pack.FunDays),
    IceCreamDay(Pack.FunDays),
    SmoresDay(Pack.FunDays),
    DogDay(Pack.FunDays),
    CheeseburgerDay(Pack.FunDays),
    PirateDay(Pack.FunDays),
    CoffeeDay(Pack.FunDays),
    TacoDay(Pack.FunDays),
    CatDay(Pack.FunDays),

    FirstDayOfSpring(Pack.Seasons),
    FirstDayOfSummer(Pack.Seasons),
    FirstDayOfFall(Pack.Seasons),
    FirstDayOfWinter(Pack.Seasons),

    Purim(Pack.Jewish),
    Passover(Pack.Jewish),
    RoshHashanah(Pack.Jewish),
    YomKippur(Pack.Jewish),
    Sukkot(Pack.Jewish),
    Hanukkah(Pack.Jewish),

    Ramadan(Pack.Muslim),
    EidAlFitr(Pack.Muslim),
    EidAlAdha(Pack.Muslim),

    Holi(Pack.Indian),
    Navratri(Pack.Indian),
    Diwali(Pack.Indian),
}

// The holidays with a drawn scene so far (HomeScene.kt); the others leave their days to these.
val DrawnHolidays = setOf(
    Holiday.NewYear, Holiday.MlkDay, Holiday.SuperBowl, Holiday.Valentines, Holiday.PresidentsDay,
    Holiday.Epiphany, Holiday.LunarNewYear, Holiday.PopcornDay, Holiday.GroundhogDay,
    Holiday.Halloween, Holiday.ElectionDay, Holiday.VeteransDay, Holiday.Thanksgiving, Holiday.Christmas,
)

// The holiday to show on a date: among the packs' holidays that have a scene (drawn), the
// shortest stretch covering the date. Ties go to the one listed first.
fun holidayOn(
    date: LocalDate,
    packs: Set<Pack> = DefaultPacks,
    drawn: Set<Holiday> = Holiday.entries.toSet(),
    zone: ZoneId = ZoneId.systemDefault(),
): Holiday? {
    val showing = Holiday.entries
        .filter { it.pack in packs && it in drawn }
        .mapNotNull { holiday ->
            (date.year - 1..date.year).asSequence()
                .flatMap { holiday.stretchesIn(it, zone) }
                .firstOrNull { date in it }
                ?.let { holiday to ChronoUnit.DAYS.between(it.start, it.endInclusive) }
        }
    val (seasonDays, others) = showing.partition { it.first.pack == Pack.Seasons }
    return others.ifEmpty { seasonDays }.minByOrNull { it.second }?.first
}

// The stretches of days a holiday shows that start in this year.
fun Holiday.stretchesIn(year: Int, zone: ZoneId = ZoneId.systemDefault()): List<ClosedRange<LocalDate>> {
    fun date(month: Int, day: Int) = LocalDate.of(year, month, day)
    fun day(on: LocalDate) = listOf(on..on)
    fun days(from: LocalDate, through: LocalDate) = listOf(from..through)
    return when (this) {
        Holiday.NewYear -> days(date(12, 31), date(12, 31).plusDays(1))
        Holiday.MlkDay -> day(nthWeekday(year, Month.JANUARY, DayOfWeek.MONDAY, 3))
        Holiday.SuperBowl -> day(nthWeekday(year, Month.FEBRUARY, DayOfWeek.SUNDAY, 2))
        Holiday.Valentines -> days(date(2, 7), date(2, 14))
        Holiday.PresidentsDay -> day(nthWeekday(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3))
        Holiday.StPatricks -> days(date(3, 14), date(3, 17))
        Holiday.MothersDay -> nthWeekday(year, Month.MAY, DayOfWeek.SUNDAY, 2).let { days(it.minusDays(2), it) }
        Holiday.MemorialDay -> lastWeekday(year, Month.MAY, DayOfWeek.MONDAY).let { days(it.minusDays(2), it) }
        Holiday.Juneteenth -> day(date(6, 19))
        Holiday.FathersDay -> nthWeekday(year, Month.JUNE, DayOfWeek.SUNDAY, 3).let { days(it.minusDays(2), it) }
        Holiday.FourthOfJuly -> days(date(6, 28), date(7, 5))
        Holiday.LaborDay -> laborDay(year).let { days(it.minusDays(2), it) }
        Holiday.GrandparentsDay -> laborDay(year).plusDays(6).let { days(it.minusDays(2), it) }
        Holiday.Halloween -> days(date(10, 24), date(11, 1))
        Holiday.ElectionDay -> days(date(11, 2), electionDay(year))
        Holiday.VeteransDay -> day(date(11, 11))
        Holiday.Thanksgiving -> days(electionDay(year).plusDays(1), thanksgiving(year))
        Holiday.Christmas -> days(thanksgiving(year).with(TemporalAdjusters.next(DayOfWeek.SATURDAY)), date(12, 30))

        Holiday.Epiphany -> day(date(1, 6))
        Holiday.AshWednesday -> day(easter(year).minusDays(46))
        Holiday.Easter -> easter(year).let { days(it.minusDays(7), it) }
        Holiday.GoodFriday -> day(easter(year).minusDays(2))
        Holiday.Pentecost -> day(easter(year).plusDays(49))

        Holiday.LunarNewYear -> LunarNewYears[year]?.let { days(it, it.plusDays(2)) }.orEmpty()
        Holiday.EarthDay -> day(date(4, 22))
        Holiday.CincoDeMayo -> day(date(5, 5))
        Holiday.Oktoberfest -> days(date(9, 24), date(9, 30))
        Holiday.DiaDeLosMuertos -> day(date(11, 2))

        Holiday.PopcornDay -> day(date(1, 19))
        Holiday.GroundhogDay -> day(date(2, 2))
        Holiday.PizzaDay -> day(date(2, 9))
        Holiday.PiDay -> day(date(3, 14))
        Holiday.AprilFools -> day(date(4, 1))
        Holiday.BitcoinPizzaDay -> day(date(5, 22))
        Holiday.DonutDay -> day(nthWeekday(year, Month.JUNE, DayOfWeek.FRIDAY, 1))
        Holiday.EmojiDay -> day(date(7, 17))
        Holiday.IceCreamDay -> day(nthWeekday(year, Month.JULY, DayOfWeek.SUNDAY, 3))
        Holiday.SmoresDay -> day(date(8, 10))
        Holiday.DogDay -> day(date(8, 26))
        Holiday.CheeseburgerDay -> day(date(9, 18))
        Holiday.PirateDay -> day(date(9, 19))
        Holiday.CoffeeDay -> day(date(9, 29))
        Holiday.TacoDay -> day(date(10, 4))
        Holiday.CatDay -> day(date(10, 29))

        Holiday.FirstDayOfSpring -> day(seasonStart(year, Season.Spring, zone))
        Holiday.FirstDayOfSummer -> day(seasonStart(year, Season.Summer, zone))
        Holiday.FirstDayOfFall -> day(seasonStart(year, Season.Fall, zone))
        Holiday.FirstDayOfWinter -> day(seasonStart(year, Season.Winter, zone))

        // Jewish holidays begin at sundown; these are the daytime dates.
        Holiday.Purim -> day(passover(year).minusDays(30))
        Holiday.Passover -> passover(year).let { days(it, it.plusDays(7)) }
        Holiday.RoshHashanah -> roshHashanah(year).let { days(it, it.plusDays(1)) }
        Holiday.YomKippur -> day(roshHashanah(year).plusDays(9))
        Holiday.Sukkot -> roshHashanah(year).plusDays(14).let { days(it, it.plusDays(6)) }
        Holiday.Hanukkah -> hanukkah(year).let { days(it, it.plusDays(7)) }

        Holiday.Ramadan -> hijriStarts(year, 9).map { it..it.plusMonthsHijri(1).minusDays(1) }
        Holiday.EidAlFitr -> hijriStarts(year, 10).map { it..it.plusDays(2) }
        Holiday.EidAlAdha -> hijriStarts(year, 12, day = 10).map { it..it.plusDays(2) }

        Holiday.Holi -> Holis[year]?.let { day(it) }.orEmpty()
        Holiday.Navratri -> Navratris[year]?.let { days(it, it.plusDays(9)) }.orEmpty()
        Holiday.Diwali -> Diwalis[year]?.let { days(it.minusDays(2), it.plusDays(2)) }.orEmpty()
    }
}

fun nthWeekday(year: Int, month: Month, weekday: DayOfWeek, n: Int): LocalDate =
    LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, weekday))

fun lastWeekday(year: Int, month: Month, weekday: DayOfWeek): LocalDate =
    LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(weekday))

fun laborDay(year: Int): LocalDate = nthWeekday(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1)

// The Tuesday after the first Monday in November.
fun electionDay(year: Int): LocalDate = LocalDate.of(year, Month.NOVEMBER, 2).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))

fun thanksgiving(year: Int): LocalDate = nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4)

// Western Easter (the anonymous Gregorian computus).
fun easter(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = (h + l - 7 * m + 114) % 31 + 1
    return LocalDate.of(year, month, day)
}

// The Hebrew calendar's arithmetic (Reingold & Dershowitz, Calendrical Calculations).
// Days are counted from 1 January of year 1 (day 1), the book's "fixed" dates.
private const val HebrewEpoch = -1_373_427L
private const val FixedDayOf1970 = 719_163L

private fun hebrewElapsedDays(year: Int): Long {
    val months = Math.floorDiv(235L * year - 234, 19L)
    val parts = 12_084L + 13_753L * months
    val day = 29L * months + Math.floorDiv(parts, 25_920L)
    return if (Math.floorMod(3 * (day + 1), 7L) < 3) day + 1 else day
}

private fun hebrewNewYearDelay(year: Int): Long {
    val before = hebrewElapsedDays(year - 1)
    val start = hebrewElapsedDays(year)
    val after = hebrewElapsedDays(year + 1)
    return when {
        after - start == 356L -> 2
        start - before == 382L -> 1
        else -> 0
    }
}

private fun hebrewNewYear(hebrewYear: Int): LocalDate =
    LocalDate.ofEpochDay(HebrewEpoch + hebrewElapsedDays(hebrewYear) + hebrewNewYearDelay(hebrewYear) - FixedDayOf1970)

// 1 Tishri of the Hebrew year that begins in this Gregorian year.
fun roshHashanah(year: Int): LocalDate = hebrewNewYear(year + 3761)

// 15 Nisan: 163 days before the next 1 Tishri, whatever the year's length.
fun passover(year: Int): LocalDate = roshHashanah(year).minusDays(163)

// 25 Kislev, after a 30-day Tishri and a Cheshvan of 29 days, or 30 in a "complete" year.
fun hanukkah(year: Int): LocalDate {
    val start = roshHashanah(year)
    val length = ChronoUnit.DAYS.between(start, roshHashanah(year + 1))
    val cheshvan = if (length % 10 == 5L) 30L else 29L
    return start.plusDays(30 + cheshvan + 24)
}

// The days of a Hijri month (Umm al-Qura, as java.time has it) that start in this year.
private fun hijriStarts(year: Int, month: Int, day: Int = 1): List<LocalDate> = try {
    val first = HijrahDate.from(LocalDate.of(year, 1, 1)).get(ChronoField.YEAR)
    (first..first + 1)
        .map { LocalDate.from(HijrahDate.of(it, month, day)) }
        .filter { it.year == year }
} catch (e: java.time.DateTimeException) {
    emptyList()
}

private fun LocalDate.plusMonthsHijri(months: Long): LocalDate = LocalDate.from(HijrahDate.from(this).plus(months, ChronoUnit.MONTHS))

// Holidays that follow the Chinese or Hindu calendars, which java.time doesn't have. Lunar
// New Year comes from the macOS Chinese calendar; the Hindu dates from published calendars
// (Drik Panchang). Add years as they're published.
private val LunarNewYears = listOf(
    "2026-02-17", "2027-02-06", "2028-01-26", "2029-02-13", "2030-02-03", "2031-01-23", "2032-02-11",
    "2033-01-31", "2034-02-19", "2035-02-08", "2036-01-28", "2037-02-15", "2038-02-04", "2039-01-24",
    "2040-02-12", "2041-02-01", "2042-01-22",
).byYear()
private val Holis = listOf("2026-03-04", "2027-03-22", "2028-03-11").byYear()
private val Navratris = listOf("2026-10-11", "2027-09-30", "2028-09-20", "2029-10-09", "2030-09-28").byYear()
private val Diwalis = listOf("2026-11-08", "2027-10-29", "2028-10-17", "2029-11-05").byYear()

private fun List<String>.byYear(): Map<Int, LocalDate> = map(LocalDate::parse).associateBy { it.year }

enum class Season { Winter, Spring, Summer, Fall }

// The season on a date: each starts at its equinox or solstice, as a date where the TV is.
fun seasonOn(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Season {
    val year = date.year
    return when {
        date >= seasonStart(year, Season.Winter, zone) -> Season.Winter
        date >= seasonStart(year, Season.Fall, zone) -> Season.Fall
        date >= seasonStart(year, Season.Summer, zone) -> Season.Summer
        date >= seasonStart(year, Season.Spring, zone) -> Season.Spring
        else -> Season.Winter
    }
}

// The date of the equinox or solstice that starts a season, from Meeus's mean-instant
// polynomials (Astronomical Algorithms, table 27.B): within about half an hour, so the date
// is wrong only when the instant falls right at local midnight.
fun seasonStart(year: Int, season: Season, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    val y = (year - 2000) / 1000.0
    val terms = when (season) {
        Season.Spring -> doubleArrayOf(2451623.80984, 365242.37404, 0.05169, -0.00411, -0.00057)
        Season.Summer -> doubleArrayOf(2451716.56767, 365241.62603, 0.00325, 0.00888, -0.00030)
        Season.Fall -> doubleArrayOf(2451810.21715, 365242.01767, -0.11575, 0.00337, 0.00078)
        Season.Winter -> doubleArrayOf(2451900.05952, 365242.74049, -0.06223, -0.00823, 0.00032)
    }
    val julianDay = terms.withIndex().sumOf { (power, term) -> term * y.pow(power) }
    val millis = ((julianDay - 2440587.5) * 86_400_000).toLong()
    return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
