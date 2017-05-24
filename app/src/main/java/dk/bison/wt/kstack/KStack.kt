package dk.bison.wt.kstack

import android.content.Context
import android.support.v7.app.AlertDialog
import dk.bison.wt.kstack.providers.HttpCacheProvider
import dk.bison.wt.kstack.providers.HttpClientProvider
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI

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

typealias VersionControlCallback = (type : UpdateType, builder : AlertDialog.Builder?) -> Unit
typealias AppOpenCallback = (failed : Boolean) -> Unit

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
    private var updateJob : Job? = null

    fun init(appContext : Context, appId : String, appKey : String, debug : Boolean = false)
    {
        if(isInitialized)
            return
        this.appContext = appContext
        this.appId = appId
        this.appKey = appKey
        kLog(TAG, "AppId = $appId, AppKey = $appKey")
        backendManager = BackendManager(HttpClientProvider.provideHttpClient(HttpCacheProvider.provideCache(appContext), debug))
        isInitialized = true
        this.debug = debug

        //backendManager.test()
        updateCache()
        kLog(TAG, "Just ran updateCacheAsync")

    }

    fun updateCache() {
        updateJob = launch(CommonPool) {
            val languages = backendManager.getAllLanguagesAsync()
            val translations = backendManager.getAllTranslationsAsync()
            runBlocking {
                languages.await()
                translations.await()
                kLog(TAG, "Completed fetching data")
            }
        }
    }

    fun appOpen(callback : AppOpenCallback = {} )
    {
        // we launch this async in case we need to wait for the updateJob to complete
        async(CommonPool)
        {
            // if Update job is still running, wait for it
            updateJob?.join()
            //delay(5000L)
            launch(UI)
            {
                callback(true)
            }
        }
    }

    fun setLogFunction(fnc : LogFunction)
    {
        kLog = fnc
    }

    var debug : Boolean = false
        set(value) {
            if(!isInitialized)
                throw IllegalStateException("init() was not called")
            field = value
            backendManager.client = HttpClientProvider.provideHttpClient(HttpCacheProvider.provideCache(appContext), value)
        }
}