package dk.bison.wt.kstack.store

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by bison on 24-05-2017.
 */
class PrefJsonStore(context : Context) : JsonStore {
    val prefs : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun save(key: String, obj: JSONObject, callback: SaveCallback) {
        async(CommonPool) {
            val success = prefs.edit().putString(key, obj.toString()).commit()
            launch(UI) {
                callback(success)
            }
        }
    }

    override fun load(key: String): JSONObject? {
        try {
            val json_data: String? = prefs.getString(key, null)
            val json_obj = JSONObject(json_data)
            return json_obj
        }
        catch (e : JSONException)
        {
            return null
        }
    }

    override fun loadDeferred(key: String): Deferred<JSONObject?> {
        val d  = async(CommonPool)
        {
            load(key)
        }
        return d
    }

    override fun loadCallback(key: String, callback: LoadCallback) {
        async(CommonPool)
        {
            val obj : JSONObject? = load(key)
            launch(UI)
            {
                callback(obj)
            }
        }
    }

}