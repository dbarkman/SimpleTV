package com.redmesastudios.simpletv

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// What the greeting says on a holiday that has its own words, instead of "Good morning".
fun holidayGreeting(holiday: Holiday?): String? = when (holiday) {
    Holiday.NewYear -> "Happy New Year!"
    Holiday.MlkDay -> "Honoring Dr. King"
    Holiday.SuperBowl -> "Happy game day!"
    Holiday.VeteransDay -> "Thank you, veterans"
    else -> null
}
