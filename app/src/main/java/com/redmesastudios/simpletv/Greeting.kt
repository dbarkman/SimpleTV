package com.redmesastudios.simpletv

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// What the greeting says on a holiday that has its own words, instead of "Good morning".
// Holidays that last several days (Valentine's, St. Patrick's, Easter week, Mother's Day and
// Memorial Day weekends) say it only on the day itself.
fun holidayGreeting(holiday: Holiday?, day: java.time.LocalDate): String? = when (holiday) {
    Holiday.Valentines -> if (day.monthValue == 2 && day.dayOfMonth == 14) "Happy Valentine's Day!" else null
    Holiday.PresidentsDay -> "Happy Presidents' Day"
    Holiday.StPatricks -> if (day.monthValue == 3 && day.dayOfMonth == 17) "Happy St. Patrick's Day!" else null
    Holiday.PizzaDay -> "Happy Pizza Day!"
    Holiday.Easter -> if (day == easter(day.year)) "Happy Easter!" else null
    Holiday.EarthDay -> "Happy Earth Day!"
    Holiday.BitcoinPizzaDay -> "Happy Bitcoin Pizza Day!"
    Holiday.Pentecost -> "Happy Pentecost"
    Holiday.DonutDay -> "Happy Donut Day!"
    Holiday.Juneteenth -> "Happy Juneteenth!"
    Holiday.FathersDay -> if (day.dayOfWeek == java.time.DayOfWeek.SUNDAY) "Happy Father's Day!" else null
    Holiday.FirstDayOfSummer -> "Hello, summer!"
    Holiday.EmojiDay -> "Happy Emoji Day!"
    Holiday.IceCreamDay -> "Happy Ice Cream Day!"
    Holiday.SmoresDay -> "Happy S'mores Day!"
    Holiday.DogDay -> "Happy Dog Day!"
    Holiday.CheeseburgerDay -> "Happy Cheeseburger Day!"
    Holiday.PirateDay -> "Ahoy, matey!"
    Holiday.FirstDayOfFall -> "Hello, fall!"
    Holiday.Oktoberfest -> "Happy Oktoberfest!"
    Holiday.CoffeeDay -> "Happy Coffee Day!"
    Holiday.TacoDay -> "Happy Taco Day!"
    Holiday.CatDay -> "Happy Cat Day!"
    Holiday.Halloween -> if (day.monthValue == 10 && day.dayOfMonth == 31) "Happy Halloween!" else null
    Holiday.ElectionDay -> if (day == electionDay(day.year)) "It's Election Day" else null
    Holiday.DiaDeLosMuertos -> "Día de los Muertos"
    Holiday.FirstDayOfWinter -> "Hello, winter!"
    Holiday.LaborDay -> if (day.dayOfWeek == java.time.DayOfWeek.MONDAY) "Happy Labor Day!" else null
    Holiday.GrandparentsDay -> if (day.dayOfWeek == java.time.DayOfWeek.SUNDAY) "Happy Grandparents Day!" else null
    Holiday.FourthOfJuly -> if (day.monthValue == 7 && day.dayOfMonth == 4) "Happy Fourth of July!" else null
    Holiday.CincoDeMayo -> "Happy Cinco de Mayo!"
    Holiday.MothersDay -> if (day.dayOfWeek == java.time.DayOfWeek.SUNDAY) "Happy Mother's Day!" else null
    Holiday.MemorialDay -> if (day.dayOfWeek == java.time.DayOfWeek.MONDAY) "Honoring the fallen" else null
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
