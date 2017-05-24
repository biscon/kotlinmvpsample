package dk.bison.wt.kstack

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request

class BackendManager(var client: OkHttpClient) {
    val TAG = "BackendManager"

    fun getAllLanguagesAsync() = async(CommonPool) {
        val request = Request.Builder()
                .url("https://nstack.io/api/v1/translate/mobile/languages")
                .build()
        val response = client.newCall(request).execute()
        response
    }

    fun getAllTranslationsAsync() = async(CommonPool) {
        val request = Request.Builder()
                .url("https://nstack.io/api/v1/translate/mobile/keys?all=true&flat=false")
                .build()
        val response = client.newCall(request).execute()
        response
    }

    fun test()
    {
        async(CommonPool) {
            val lang = getAllLanguagesAsync()
            lang.await()
        }
    }
}
