/* ownCloud Android Library is available under MIT license
 *   @author Abel García de Prada
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
package com.owncloud.android.lib.resources.status.responses

import com.owncloud.android.lib.resources.status.RemoteCapability
import com.owncloud.android.lib.resources.status.RemoteCapability.CapabilityBooleanType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CapabilityResponse(
    @Json(name = "version")
    val serverVersion: ServerVersion?,
    val capabilities: Capabilities?
) {
    fun toRemoteCapability(): RemoteCapability = RemoteCapability(
        versionMayor = serverVersion?.major ?: 0,
        versionMinor = serverVersion?.minor ?: 0,
        versionMicro = serverVersion?.micro ?: 0,
        versionString = serverVersion?.string ?: "",
        versionEdition = serverVersion?.edition ?: "",
        corePollinterval = capabilities?.coreCapabilities?.pollinterval ?: 0,
        chunkingVersion = capabilities?.davCapabilities?.chunking ?: "",
        filesSharingApiEnabled = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingApiEnabled),
        filesSharingResharing = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingReSharing),
        filesSharingPublicEnabled = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.enabled),
        filesSharingPublicUpload = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicUpload),
        filesSharingPublicSupportsUploadOnly = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicUploadOnly),
        filesSharingPublicMultiple = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicMultiple),
        filesSharingPublicPasswordEnforced = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicPassword?.enforced),
        filesSharingPublicPasswordEnforcedReadOnly = CapabilityBooleanType.fromBooleanValue(
            capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicPassword?.enforcedFor?.enforcedReadOnly
        ),
        filesSharingPublicPasswordEnforcedReadWrite = CapabilityBooleanType.fromBooleanValue(
            capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicPassword?.enforcedFor?.enforcedReadWrite
        ),
        filesSharingPublicPasswordEnforcedUploadOnly = CapabilityBooleanType.fromBooleanValue(
            capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicPassword?.enforcedFor?.enforcedUploadOnly
        ),
        filesSharingPublicExpireDateEnabled = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicExpireDate?.enabled),
        filesSharingPublicExpireDateDays = capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicExpireDate?.days
            ?: 0,
        filesSharingPublicExpireDateEnforced = CapabilityBooleanType.fromBooleanValue(
            capabilities?.fileSharingCapabilities?.fileSharingPublic?.fileSharingPublicExpireDate?.enforced
        ),
        filesBigFileChunking = CapabilityBooleanType.fromBooleanValue(capabilities?.fileCapabilities?.bigfilechunking),
        filesUndelete = CapabilityBooleanType.fromBooleanValue(capabilities?.fileCapabilities?.undelete),
        filesVersioning = CapabilityBooleanType.fromBooleanValue(capabilities?.fileCapabilities?.versioning),
        filesSharingFederationIncoming = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingFederation?.incoming),
        filesSharingFederationOutgoing = CapabilityBooleanType.fromBooleanValue(capabilities?.fileSharingCapabilities?.fileSharingFederation?.outgoing)
    )
}

@JsonClass(generateAdapter = true)
data class Capabilities(
    @Json(name = "core")
    val coreCapabilities: CoreCapabilities?,
    @Json(name = "files_sharing")
    val fileSharingCapabilities: FileSharingCapabilities?,
    @Json(name = "files")
    val fileCapabilities: FileCapabilities?,
    @Json(name = "dav")
    val davCapabilities: DavCapabilities?
)

@JsonClass(generateAdapter = true)
data class CoreCapabilities(
    val pollinterval: Int?
)

@JsonClass(generateAdapter = true)
data class FileSharingCapabilities(
    @Json(name = "api_enabled")
    val fileSharingApiEnabled: Boolean?,
    @Json(name = "public")
    val fileSharingPublic: FileSharingPublic?,
    @Json(name = "resharing")
    val fileSharingReSharing: Boolean?,
    @Json(name = "federation")
    val fileSharingFederation: FileSharingFederation?
)

@JsonClass(generateAdapter = true)
data class FileSharingPublic(
    val enabled: Boolean?,
    @Json(name = "upload")
    val fileSharingPublicUpload: Boolean?,
    @Json(name = "supports_upload_only")
    val fileSharingPublicUploadOnly: Boolean?,
    @Json(name = "multiple")
    val fileSharingPublicMultiple: Boolean?,
    @Json(name = "password")
    val fileSharingPublicPassword: FileSharingPublicPassword?,
    @Json(name = "expire_date")
    val fileSharingPublicExpireDate: FileSharingPublicExpireDate?
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicPassword(
    val enforced: Boolean?,
    @Json(name = "enforced_for")
    val enforcedFor: FileSharingPublicPasswordEnforced?
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicPasswordEnforced(
    @Json(name = "read_only")
    val enforcedReadOnly: Boolean?,
    @Json(name = "read_write")
    val enforcedReadWrite: Boolean?,
    @Json(name = "upload_only")
    val enforcedUploadOnly: Boolean?
)

@JsonClass(generateAdapter = true)
data class FileSharingPublicExpireDate(
    val enabled: Boolean?,
    val days: Int?,
    val enforced: Boolean?
)

@JsonClass(generateAdapter = true)
data class FileSharingFederation(
    val incoming: Boolean?,
    val outgoing: Boolean?
)

@JsonClass(generateAdapter = true)
data class FileCapabilities(
    val bigfilechunking: Boolean?,
    val undelete: Boolean?,
    val versioning: Boolean?
)

@JsonClass(generateAdapter = true)
data class DavCapabilities(
    val chunking: String?
)

@JsonClass(generateAdapter = true)
data class ServerVersion(
    var major: Int?,
    var minor: Int?,
    var micro: Int?,
    var string: String?,
    var edition: String?
)
