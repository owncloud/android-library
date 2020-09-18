package com.owncloud.android.lib.common.http.methods

import com.owncloud.android.lib.resources.status.HttpScheme
import com.owncloud.android.lib.resources.status.HttpScheme.HTTPS_SCHEME
import com.owncloud.android.lib.resources.status.HttpScheme.HTTP_SCHEME
import okhttp3.Interceptor
import okhttp3.Response

class RedirectChainHandler : Interceptor {
    private val _redirectChain = arrayListOf<String>()

    val redirectChain: List<String>
        get() = _redirectChain

    /**
     * Is only true if http and https requests where in the request chain.
     */
    val hasBeenRedirectedUnsecureLocation: Boolean
        get() {
            var containsHttpsRequests = false
            var containsHttpRequests = false
            for(url in _redirectChain) {
                when {
                    url.startsWith(HTTPS_SCHEME) -> containsHttpsRequests = true
                    url.startsWith("$HTTP_SCHEME://") -> containsHttpRequests = true
                }

            }
            return containsHttpRequests && containsHttpsRequests
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        _redirectChain.add(chain.request().url.toString())
        return chain.proceed(chain.request())
    }
}


