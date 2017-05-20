package dk.bison.wt

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by bison on 20-05-2017.
 * Package level functions
 */

fun add(i1 : Int, i2 : Int) : Int
{
    return i1+i2
}

// extension
fun Date.parseFromISO8601(str : String)
{
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    try {
        this.time = format.parse(str).time;
    }
    catch (e : Exception) {
    }
}