package dk.bison.wt.kstack

import android.content.Context
import android.support.v7.app.AlertDialog
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

enum class CacheId
{
    LANGUAGES,
    TRANSLATIONS
}

typealias VersionControlCallback = (type : UpdateType, builder : AlertDialog.Builder?) -> Unit
//typealias AppOpenCallback = (success: Boolean) -> Unit

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
    var jsonLanguages : JSONObject? = null
    var jsonTranslations : JSONObject? = null

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
        isInitialized = true
        this.debug = debug

        updateCache()
        async(CommonPool)
        {
            updateJob?.join()
            if(jsonTranslations != null)
                updateTranslationClass(jsonTranslations!!)
        }

        kLog(TAG, "Just ran updateCacheAsync")

    }

    private fun updateTranslationClass(translations : JSONObject)
    {
        val locale = Locale.getDefault()
        kLog(TAG, "Default locale is ${locale.toLanguageTag()}")
        //val sel_locale = locale.toLanguageTag()
        val sel_locale = "en-GB"
        val data = translations.getJSONObject("data")
        val iterator = data.keys()
        while (iterator.hasNext()) {
            var langTag = iterator.next()
            kLog(TAG, langTag)
            if(sel_locale.toLowerCase().contentEquals(langTag.toLowerCase()))
            {
                kLog(TAG, "Found matching locale")
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
            jsonLanguages = jsonStore.loadDeferred(CacheId.LANGUAGES.name).await()
            jsonTranslations = jsonStore.loadDeferred(CacheId.TRANSLATIONS.name).await()

            if(jsonLanguages == null)
            {
                jsonLanguages = parseAndSave(CacheId.LANGUAGES.name, backendManager.getAllLanguagesAsync().await())
            }
            if(jsonTranslations == null)
            {
                jsonTranslations = parseAndSave(CacheId.TRANSLATIONS.name, backendManager.getAllTranslationsAsync().await())
            }
            kLog(TAG, "jsonLanguages = ${jsonLanguages?.toString(4) ?: "null"}")
            kLog(TAG, "jsonTranslations = ${jsonTranslations?.toString(4) ?: "null"}")
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

    var debug : Boolean = false
        set(value) {
            if(!isInitialized)
                throw IllegalStateException("init() was not called")
            field = value
            backendManager.client = HttpClientProvider.provideHttpClient(HttpCacheProvider.provideCache(appContext), value)
        }
}