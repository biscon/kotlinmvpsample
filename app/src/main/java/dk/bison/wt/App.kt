package dk.bison.wt

import android.app.Application
import dk.bison.wt.api.ApiProxy
import dk.bison.wt.api.makeApiProxy

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
    }
}