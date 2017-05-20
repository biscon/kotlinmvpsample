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

        fun apiProxy() : ApiProxy
        {
            return proxy
        }
    }

    override fun onCreate() {
        super.onCreate()
        proxy = makeApiProxy()
    }
}