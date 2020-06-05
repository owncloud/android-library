/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.lib.resources.users.services.implementation

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.users.GetRemoteUserAvatarOperation
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation
import com.owncloud.android.lib.resources.users.RemoteAvatarData
import com.owncloud.android.lib.resources.users.RemoteUserInfo
import com.owncloud.android.lib.resources.users.services.UserService

class OCUserService(override val client: OwnCloudClient) : UserService {
    override fun getUserInfo(): RemoteOperationResult<RemoteUserInfo> =
        GetRemoteUserInfoOperation().execute(client)

    override fun getUserQuota(): RemoteOperationResult<GetRemoteUserQuotaOperation.RemoteQuota> =
        GetRemoteUserQuotaOperation().execute(client)

    override fun getUserAvatar(avatarDimension: Int): RemoteOperationResult<RemoteAvatarData> =
        GetRemoteUserAvatarOperation(avatarDimension).execute(client)

}
