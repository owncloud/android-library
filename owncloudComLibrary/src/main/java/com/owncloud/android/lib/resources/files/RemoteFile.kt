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

package com.owncloud.android.lib.resources.files

import android.net.Uri
import android.os.Parcelable
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetETag
import at.bitfire.dav4jvm.property.GetLastModified
import at.bitfire.dav4jvm.property.OCId
import at.bitfire.dav4jvm.property.OCPermissions
import at.bitfire.dav4jvm.property.OCPrivatelink
import at.bitfire.dav4jvm.property.OCSize
import at.bitfire.dav4jvm.property.QuotaAvailableBytes
import at.bitfire.dav4jvm.property.QuotaUsedBytes
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCShareTypes
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareType.Companion.fromValue
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl
import timber.log.Timber
import java.io.File
import java.math.BigDecimal

/**
 * Contains the data of a Remote File from a WebDavEntry
 *
 * The path received must be URL-decoded. Path separator must be File.separator, and it must be the first character in 'path'.
 *
 * @author masensio
 * @author Christian Schabesberger
 * @author Abel García de Prada
 */
@Parcelize
data class RemoteFile(
    var remotePath: String,
    var mimeType: String = "DIR",
    var length: Long = 0,
    var creationTimestamp: Long = 0,
    var modifiedTimestamp: Long = 0,
    var etag: String? = null,
    var permissions: String? = null,
    var remoteId: String? = null,
    var size: Long = 0,
    var quotaUsedBytes: BigDecimal? = null,
    var quotaAvailableBytes: BigDecimal? = null,
    var privateLink: String? = null,
    var owner: String,
    var sharedByLink: Boolean = false,
    var sharedWithSharee: Boolean = false,
) : Parcelable {

    // TODO: Quotas not used. Use or remove them.
    init {
        require(!(remotePath.isEmpty() || !remotePath.startsWith(File.separator))) { "Trying to create a OCFile with a non valid remote path: $remotePath" }
    }

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    val isFolder
        get() = mimeType == MIME_DIR || mimeType == MIME_DIR_UNIX

    companion object {

        const val MIME_DIR = "DIR"
        const val MIME_DIR_UNIX = "httpd/unix-directory"

        fun getRemoteFileFromDav(davResource: Response, userId: String, userName: String): RemoteFile {
            val remotePath = getRemotePathFromUrl(davResource.href, userId)
            val remoteFile = RemoteFile(remotePath = remotePath, owner = userName)
            val properties = davResource.properties

            for (property in properties) {
                when (property) {
                    is CreationDate -> {
                        remoteFile.creationTimestamp = property.creationDate.toLong()
                    }
                    is GetContentLength -> {
                        remoteFile.length = property.contentLength
                    }
                    is GetContentType -> {
                        property.type?.let { remoteFile.mimeType = it }
                    }
                    is GetLastModified -> {
                        remoteFile.modifiedTimestamp = property.lastModified
                    }
                    is GetETag -> {
                        remoteFile.etag = property.eTag
                    }
                    is OCPermissions -> {
                        remoteFile.permissions = property.permission
                    }
                    is OCId -> {
                        remoteFile.remoteId = property.id
                    }
                    is OCSize -> {
                        remoteFile.size = property.size
                    }
                    is QuotaUsedBytes -> {
                        remoteFile.quotaUsedBytes = BigDecimal.valueOf(property.quotaUsedBytes)
                    }
                    is QuotaAvailableBytes -> {
                        remoteFile.quotaAvailableBytes = BigDecimal.valueOf(property.quotaAvailableBytes)
                    }
                    is OCPrivatelink -> {
                        remoteFile.privateLink = property.link
                    }
                    is OCShareTypes -> {
                        val list = property.shareTypes
                        for (i in list.indices) {
                            val shareType = fromValue(list[i].toInt())
                            if (shareType == null) {
                                Timber.d("Illegal share type value: " + list[i])
                                continue
                            }
                            if (shareType == ShareType.PUBLIC_LINK) {
                                remoteFile.sharedByLink = true
                            } else if (shareType == ShareType.USER || shareType == ShareType.FEDERATED || shareType == ShareType.GROUP) {
                                remoteFile.sharedWithSharee = true
                            }
                        }
                    }
                }
            }
            return remoteFile
        }

        /**
         * Retrieves a relative path from a remote file url
         *
         *
         * Example: url:port/remote.php/dav/files/username/Documents/text.txt => /Documents/text.txt
         *
         * @param url    remote file url
         * @param userId file owner
         * @return remote relative path of the file
         */
        private fun getRemotePathFromUrl(url: HttpUrl, userId: String): String {
            val davFilesPath = OwnCloudClient.WEBDAV_FILES_PATH_4_0 + userId
            val absoluteDavPath = Uri.decode(url.encodedPath)
            val pathToOc = absoluteDavPath.split(davFilesPath)[0]
            return absoluteDavPath.replace(pathToOc + davFilesPath, "")
        }
    }
}
