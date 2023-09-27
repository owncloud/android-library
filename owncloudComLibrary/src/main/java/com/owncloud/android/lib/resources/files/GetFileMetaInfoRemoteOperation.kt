package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils.allPropSet
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

class GetFileMetaInfoRemoteOperation(val fileId: String) : RemoteOperation<String>() {
    private val stringUrl = "${client.baseUri}$META_PATH$fileId"

    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        return try {
            val propFindMethod = PropfindMethod(URL(stringUrl), 0, allPropSet)

            val status = client.executeHttpMethod(propFindMethod)
            if (isSuccess(status)) RemoteOperationResult<String>(RemoteOperationResult.ResultCode.OK)
            else RemoteOperationResult<String>(propFindMethod)
        } catch (e: Exception) {
            Timber.e(e, "Could not get actuall (or redirected) base URL from base url (/).")
            RemoteOperationResult<String>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        private const val META_PATH = "/remote.php/dav/meta/"
    }
}