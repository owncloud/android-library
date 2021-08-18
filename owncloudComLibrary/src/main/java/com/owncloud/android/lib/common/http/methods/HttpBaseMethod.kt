package com.owncloud.android.lib.common.http.methods

import com.owncloud.android.lib.common.http.HttpClient
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

abstract class HttpBaseMethod constructor(url: URL) {
    var httpUrl: HttpUrl = url.toHttpUrlOrNull() ?: throw MalformedURLException()
    var request: Request
    abstract var response: Response

    var followRedirects:Boolean? = true
    var retryOnConnectionFailure:Boolean? = false
    var connectionTimeoutVal:Long? = 10L
    var connectionTimeoutUnit:TimeUnit? = null
    var readTimeoutVal:Long? = null
    var readTimeoutUnit:TimeUnit? = null


    var call: Call? = null


    init {
        request = Request.Builder()
            .url(httpUrl)
            .build()
    }

    @Throws(Exception::class)
    open fun execute(httpClient: HttpClient): Int {
        val okHttpClient = httpClient.okHttpClient.newBuilder().apply {
            retryOnConnectionFailure?.let{ retryOnConnectionFailure(it) }
            followRedirects?.let { followRedirects(it) }
            readTimeoutUnit?.let { unit ->
                readTimeoutVal?.let {readTimeout(it, unit) }
            }
            connectionTimeoutUnit?.let { unit ->
               connectionTimeoutVal?.let{ connectTimeout(it, unit) }
            }
        }.build()

        return onExecute(okHttpClient)
    }

    open fun setUrl(url: HttpUrl) {
        request = request.newBuilder()
            .url(url)
            .build()
    }

    /****************
     *** Requests ***
     ****************/

    fun getRequestHeader(name: String): String? {
        return request.header(name)
    }

    fun getRequestHeadersAsHashMap(): HashMap<String, String?> {
        val headers: HashMap<String, String?> = HashMap()
        val superHeaders: Set<String> = request.headers.names()
        superHeaders.forEach {
            headers[it] = getRequestHeader(it)
        }
        return headers
    }

    open fun addRequestHeader(name: String, value: String) {
        request = request.newBuilder()
            .addHeader(name, value)
            .build()
    }

    /**
     * Sets a header and replace it if already exists with that name
     *
     * @param name  header name
     * @param value header value
     */
    open fun setRequestHeader(name: String, value: String) {
        request = request.newBuilder()
            .header(name, value)
            .build()
    }

    /****************
     *** Response ***
     ****************/
    val statusCode: Int
        get() = response.code

    val statusMessage: String
        get() = response.message

    // Headers
    open fun getResponseHeaders(): Headers? {
        return response.headers
    }

    open fun getResponseHeader(headerName: String): String? {
        return response.header(headerName)
    }

    // Body
    fun getResponseBodyAsString(): String? = response.body?.string()

    open fun getResponseBodyAsStream(): InputStream? {
        return response.body?.byteStream()
    }

    /*************************
     *** Connection Params ***
     *************************/

    //////////////////////////////
    //         Setter
    //////////////////////////////
    // Connection parameters
    /*
    open fun setRetryOnConnectionFailure(retryOnConnectionFailure: Boolean) {
        retryOnConnectionFailureVal = true
    }

     */

    open fun setReadTimeout(readTimeout: Long, timeUnit: TimeUnit) {
        readTimeoutVal = readTimeout
        readTimeoutUnit = timeUnit
    }

    open fun setConnectionTimeout(
        connectionTimeout: Long,
        timeUnit: TimeUnit
    ) {
        connectionTimeoutVal = connectionTimeout
        connectionTimeoutUnit = timeUnit
    }

   /*
    open fun setFollowRedirects(followRedirects: Boolean) {
        followRedirectsVal = followRedirects
    }

    */

    /************
     *** Call ***
     ************/
    open fun abort() {
        call?.cancel()
    }

    open val isAborted: Boolean
        get() = call?.isCanceled() ?: false

    //////////////////////////////
    //         For override
    //////////////////////////////
    @Throws(Exception::class)
    protected abstract fun onExecute(okHttpClient: OkHttpClient): Int
}
