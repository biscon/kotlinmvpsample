package dk.bison.wt.kstack

import android.content.Context
import android.support.v7.app.AlertDialog
import dk.bison.wt.kstack.appopen.AppOpenSettings
import dk.bison.wt.kstack.appopen.AppUpdate
import dk.bison.wt.kstack.providers.HttpCacheProvider
import dk.bison.wt.kstack.providers.HttpClientProvider
import dk.bison.wt.kstack.store.JsonStore
import dk.bison.wt.kstack.store.PrefJsonStore
import dk.bison.wt.kstack.translate.TranslationManager
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by bison on 22-05-2017.
 */

// log function definiton, yay typedef is back :D
typealias LogFunction = (tag : String, msg : String) -> Unit

enum class UpdateType {
    UPDATE,
    FORCE_UPDATE,
    CHANGELOG,
    NOTHING
}

enum class StoreId
{
    LANGUAGES,
    TRANSLATIONS
}

// data/model class for storing information about available languages
data class Language (var id : Int, var name : String, var locale : String, var direction : String)

typealias VersionControlCallback = (type : UpdateType, builder : AlertDialog.Builder?) -> Unit
typealias AppOpenCallback = (success: Boolean) -> Unit

/*
    Default implementation of internal logger
*/
internal var kLog : LogFunction = fun (tag, msg) {
    println("$tag : $msg")
}

object KStack {
    val TAG = "KStack"
    private lateinit var appContext : Context
    var appId : String = ""
    var appKey : String = ""
    var isInitialized : Boolean = false
    private lateinit var backendManager : BackendManager
    private val translationManager = TranslationManager()
    private var updateJob : Job? = null
    private lateinit var jsonStore : JsonStore
    private lateinit var appOpenSettings : AppOpenSettings
    var jsonLanguages : JSONObject? = null
    var jsonTranslations : JSONObject? = null
    val localeLanguageMap : MutableMap<String, Language> = HashMap()
    var appUpdate : AppUpdate? = null

    // Properties with custom setters/getters ---------------------------------------------------
    var currentLocale : String? = null
        set(value) {
            field = value
            kLog(TAG, "Current locale set to $value")
        }

    var debug : Boolean = false
        set(value) {
            if(!isInitialized)
                throw IllegalStateException("init() was not called")
            field = value
            backendManager.client = HttpClientProvider.provideHttpClient(HttpCacheProvider.provideCache(appContext), value)
        }

    val deviceLocale: String
        get() {
            return Locale.getDefault().toLanguageTag()
        }


    fun init(appContext : Context, appId : String, appKey : String, debug : Boolean = false)
    {
        if(isInitialized)
            return
        this.appContext = appContext
        this.appId = appId
        this.appKey = appKey
        kLog(TAG, "AppId = $appId, AppKey = $appKey")
        backendManager = BackendManager(HttpClientProvider.provideHttpClient(HttpCacheProvider.provideCache(appContext), debug))
        jsonStore = PrefJsonStore(appContext)
        appOpenSettings = AppOpenSettings(appContext)
        isInitialized = true
        this.debug = debug

        updateCache()
        async(CommonPool)
        {
            updateJob?.join()
            // build map of available nstack languages indexed by language tag
            if(jsonLanguages != null)
                buildLocaleLanguageMap(jsonLanguages!!)
            // if device language matches a downloaded or cached language, update translation class
            if(jsonTranslations != null)
                updateTranslationClass(jsonTranslations!!)
        }

        kLog(TAG, "Just ran updateCacheAsync")

    }

    private fun buildLocaleLanguageMap(languages: JSONObject) {
        val lang_array = languages.getJSONArray("data")
        repeat(lang_array.length()) { i ->
            val json_lang : JSONObject = lang_array.getJSONObject(i)
            val lang : Language = Language(json_lang.getInt("id"), json_lang.getString("name"), json_lang.getString("locale"), json_lang.getString("direction"))
            localeLanguageMap[lang.locale] = lang
        }
        kLog(TAG, "Language map = $localeLanguageMap")
    }

    private fun updateTranslationClass(translations : JSONObject)
    {
        val locale = currentLocale ?: deviceLocale
        kLog(TAG, "Attempting to update translation class with locale $locale")
        val data = translations.getJSONObject("data")
        val iterator = data.keys()
        while (iterator.hasNext()) {
            var langTag = iterator.next()
            kLog(TAG, langTag)

            if(locale.toLowerCase().contentEquals(langTag.toLowerCase()))
            {
                kLog(TAG, "Found matching locale in stored translations, overriding baked in language with $langTag")
                translationManager.parseTranslations(data.getJSONObject(langTag))
            }
        }
    }

    private fun parseAndSave(key : String, response : Response) : JSONObject?
    {
        if(response.isSuccessful)
        {
            try {
                val obj : JSONObject = JSONObject(response.body()?.string())
                jsonStore.save(key, obj, {
                    kLog(TAG, "Saved $key to JsonStore")
                })
                return obj
            }
            catch (e : JSONException)
            {
                e.printStackTrace()
            }
        }
        return null
    }

    fun updateCache() {
        updateJob = launch(CommonPool) {
            jsonLanguages = jsonStore.loadDeferred(StoreId.LANGUAGES.name).await()
            jsonTranslations = jsonStore.loadDeferred(StoreId.TRANSLATIONS.name).await()

            if(jsonLanguages == null)
            {
                jsonLanguages = parseAndSave(StoreId.LANGUAGES.name, backendManager.getAllLanguagesAsync().await())
            }
            if(jsonTranslations == null)
            {
                jsonTranslations = parseAndSave(StoreId.TRANSLATIONS.name, backendManager.getAllTranslationsAsync().await())
            }
            kLog(TAG, "jsonLanguages = ${jsonLanguages?.toString(4) ?: "null"}")
            kLog(TAG, "jsonTranslations = ${jsonTranslations?.toString(4) ?: "null"}")
        }
    }

    fun appOpen(callback: AppOpenCallback = {})
    {
        if(!isInitialized)
            throw IllegalStateException("init() was not called")
        async(CommonPool)
        {
            // if Update job is still running, wait for it
            updateJob?.join()
            val response = backendManager.postAppOpen(appOpenSettings, currentLocale ?: deviceLocale).await()
            if(response.isSuccessful)
                parseAppOpenResponse(response)
            launch(UI)
            {
                callback(response.isSuccessful)
            }
        }
    }

    private fun parseAppOpenResponse(response: Response) {
        try {
            val obj : JSONObject = JSONObject(response.body()?.string())
            val data : JSONObject = obj.getJSONObject("data")
            // set current locale
            currentLocale = obj.getJSONObject("meta").getJSONObject("language").getString("locale")
            kLog(TAG, "AppOpen response = ${data.toString(4)}")
            val translate : JSONObject? = data.getJSONObject("translate")
            if(translate != null)
                translationManager.parseTranslations(translate)
            if(data.has("update"))
                appUpdate = AppUpdate(data.getJSONObject("update"))
        }
        catch (e : JSONException)
        {
            e.printStackTrace()
        }
    }

    fun versionControl(callback : VersionControlCallback)
    {
        // we launch this async in case we need to wait for the updateJob to complete
        async(CommonPool)
        {
            // if Update job is still running, wait for it
            updateJob?.join()
            //delay(5000L)
            launch(UI)
            {

            }
        }
    }

    fun setLogFunction(fnc : LogFunction)
    {
        kLog = fnc
    }

    fun setTranslationClass(translationClass : Class<*>)
    {
        translationManager.setTranslationClass(translationClass)
    }

    fun translate(view: Any)
    {
        translationManager.translate(view)
    }
}