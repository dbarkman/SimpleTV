package com.redmesastudios.simpletv

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// What the greeting says on a holiday that has its own words, instead of "Good morning".
// Valentine's week and St. Patrick's say it only on the day itself.
fun holidayGreeting(holiday: Holiday?, day: java.time.LocalDate): String? = when (holiday) {
    Holiday.Valentines -> if (day.monthValue == 2 && day.dayOfMonth == 14) "Happy Valentine's Day!" else null
    Holiday.PresidentsDay -> "Happy Presidents' Day"
    Holiday.StPatricks -> if (day.monthValue == 3 && day.dayOfMonth == 17) "Happy St. Patrick's Day!" else null
    Holiday.PizzaDay -> "Happy Pizza Day!"
    Holiday.PiDay -> "Happy Pi Day!"
    Holiday.FirstDayOfSpring -> "Hello, spring!"
    Holiday.AprilFools -> "Happy April Fools' Day!"
    Holiday.Epiphany -> "Happy Epiphany"
    Holiday.LunarNewYear -> "Happy Lunar New Year!"
    Holiday.PopcornDay -> "Happy Popcorn Day!"
    Holiday.GroundhogDay -> "Happy Groundhog Day!"
    Holiday.NewYear -> "Happy New Year!"
    Holiday.MlkDay -> "Honoring Dr. King"
    Holiday.SuperBowl -> "Happy game day!"
    Holiday.VeteransDay -> "Thank you, veterans"
    else -> null
}
