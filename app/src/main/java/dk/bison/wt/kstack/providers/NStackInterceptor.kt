package dk.bison.wt.kstack.providers

/**
 * Created by joso on 08/10/15.
 */
class NStackInterceptor : okhttp3.Interceptor {

    @Throws(java.io.IOException::class)
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder()
                //Commented this out because it was causing issues with the cached languageHeader
                //.header("Accept-Language", NStack.getStack().getSelectedLanguageHeader())
                .header("X-Application-Id", dk.bison.wt.kstack.KStack.appId)
                .header("X-Rest-Api-Key", dk.bison.wt.kstack.KStack.appKey)
                .build()

        return chain.proceed(newRequest)
    }

}
