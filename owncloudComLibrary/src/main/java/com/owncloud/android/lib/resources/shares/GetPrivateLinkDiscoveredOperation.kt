/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2022 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.lib.resources.shares

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.HttpConstants.LOCATION_WEB_DAV_HEADER
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

class GetPrivateLinkDiscoveredOperation(private val url: String) : RemoteOperation<String>() {

    private fun onResultUnsuccessful(
        status: Int,
        method: GetMethod
    ): RemoteOperationResult<String> {
        Timber.e("Failed response while while getting remote shares ")
        Timber.e("*** status code: $status")
        return RemoteOperationResult(method)
    }

    private fun onRequestSuccessful(response: String?): RemoteOperationResult<String> {
        val result = RemoteOperationResult<String>(RemoteOperationResult.ResultCode.OK)
        Timber.d("Successful response: $response")
        result.data = response
        return result
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        val getMethod = GetMethod(URL(url))

        return try {
            val status = client.executeHttpMethod(getMethod)
            val header = getMethod.getResponseHeader(LOCATION_WEB_DAV_HEADER)

            if (!isSuccess(status)) {
                onResultUnsuccessful(status, getMethod)
            } else {
                onRequestSuccessful(header)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while getting remote shares")
            RemoteOperationResult(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_SEE_OTHER

    companion object {
        //OCS Route
        private const val OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/shares"
    }
}
