/* ownCloud Android Library is available under MIT license
*   Copyright (C) 2020 ownCloud GmbH.
*
*   Permission is hereby granted, free of charge, to any person obtaining a copy
*   of this software and associated documentation files (the "Software"), to deal
*   in the Software without restriction, including without limitation the rights
*   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*   copies of the Software, and to permit persons to whom the Software is
*   furnished to do so, subject to the following conditions:
*
*   The above copyright notice and this permission notice shall be included in
*   all copies or substantial portions of the Software.
*
*   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
*   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
*   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
*   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
*   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
*   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
*   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
*   THE SOFTWARE.
*
*/

package com.owncloud.android.lib.resources.status

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.RedirectChainHandler
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.HttpScheme.HTTPS_SCHEME
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

internal class StatusRequester {
    private fun getGetMethod(url: String): GetMethod {
        return GetMethod(URL(url)).apply {
            setReadTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            setConnectionTimeout(TRY_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
        }
    }

    data class RequestResult(
        val getMethod: GetMethod,
        val status: Int,
        val result: RemoteOperationResult<OwnCloudVersion>,
        val redirectedToUnsecureLocation: Boolean
    )

    fun requestStatus(baseLocation: String, client: OwnCloudClient): RequestResult {
        var currentLocation = baseLocation + OwnCloudClient.STATUS_PATH
        var status: Int

        val getMethod = getGetMethod(currentLocation)
        val redirectChainHandler = RedirectChainHandler()
        getMethod.addRedirectChainHandler(redirectChainHandler)

        status = client.executeHttpMethod(getMethod)
        val result =
            if (status.isSuccess()) RemoteOperationResult<OwnCloudVersion>(RemoteOperationResult.ResultCode.OK)
            else RemoteOperationResult(getMethod)

        return RequestResult(getMethod, status, result, redirectChainHandler.hasBeenRedirectedUnsecureLocation)
    }

    private fun Int.isSuccess() = this == HttpConstants.HTTP_OK

    fun handleRequestResult(
        requestResult: RequestResult,
        baseUrl: String
    ): RemoteOperationResult<OwnCloudVersion> {
        if (!requestResult.status.isSuccess())
            return RemoteOperationResult(requestResult.getMethod)

        val respJSON = JSONObject(requestResult.getMethod.getResponseBodyAsString() ?: "")
        if (!respJSON.getBoolean(NODE_INSTALLED))
            return RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED)

        val ocVersion = OwnCloudVersion(respJSON.getString(NODE_VERSION))
        // the version object will be returned even if the version is invalid, no error code;
        // every app will decide how to act if (ocVersion.isVersionValid() == false)
        val result =
            if (requestResult.redirectedToUnsecureLocation) {
                RemoteOperationResult<OwnCloudVersion>(RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION)
            } else {
                if (baseUrl.startsWith(HTTPS_SCHEME)) RemoteOperationResult(
                    RemoteOperationResult.ResultCode.OK_SSL
                )
                else RemoteOperationResult(RemoteOperationResult.ResultCode.OK_NO_SSL)
            }
        result.data = ocVersion
        return result
    }

    companion object {
        /**
         * Maximum time to wait for a response from the server when the connection is being tested,
         * in MILLISECONDs.
         */
        private const val TRY_CONNECTION_TIMEOUT: Long = 5000
        private const val NODE_INSTALLED = "installed"
        private const val NODE_VERSION = "version"
    }
}
