package dk.bison.wt

import android.app.Application
import android.util.Log
import dk.bison.wt.api.ApiProxy
import dk.bison.wt.api.makeApiProxy
import dk.bison.wt.kstack.KStack
import dk.bison.wt.kstack.kLog

/**
 * Created by bison on 20-05-2017.
 */
class App : Application()
{
    companion object {
        private lateinit var proxy : ApiProxy
        private lateinit var _instance : App

        fun apiProxy() : ApiProxy
        {
            return proxy
        }

        fun instance() : App
        {
            return _instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        proxy = makeApiProxy()

        KStack.setLogFunction { tag, msg -> Log.e(tag, msg) }
        KStack.init(this, "BmZHmoKuU99A5ZnOByOiRxMVSmAWC2yBz3OW", "yw9go00oCWt6zPhfbdjRYXiHRWmkQZQSuRke", true)
        KStack.setTranslationClass(Translation::class.java)
        Log.e("debug", "debug is now ${KStack.debug}")
        /*
        KStack.appOpen({ success ->
            kLog(KStack.TAG, "appOpen success = $success")
        })
        */

        //KStack.appOpen()
    }
}