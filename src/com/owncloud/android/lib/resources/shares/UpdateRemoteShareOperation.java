/* ownCloud Android Library is available under MIT license
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.shares;

import android.net.Uri;
import android.util.Pair;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


/**
 * Updates parameters of an existing Share resource, known its remote ID.
 * <p/>
 * Allow updating several parameters, triggering a request to the server per parameter.
 */

public class UpdateRemoteShareOperation extends RemoteOperation {

    private static final String TAG = GetRemoteShareOperation.class.getSimpleName();

    private static final String PARAM_NAME = "name";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_EXPIRATION_DATE = "expireDate";
    private static final String PARAM_PERMISSIONS = "permissions";
    private static final String PARAM_PUBLIC_UPLOAD = "publicUpload";
    private static final String FORMAT_EXPIRATION_DATE = "yyyy-MM-dd";
    private static final String ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ENTITY_CHARSET = "UTF-8";


    /**
     * Identifier of the share to update
     */
    private long mRemoteId;

    /**
     * Password to set for the public link
     */
    private String mPassword;

    /**
     * Expiration date to set for the public link
     */
    private long mExpirationDateInMillis;

    /**
     * Access permissions for the file bound to the share
     */
    private int mPermissions;

    /**
     * Upload permissions for the public link (only folders)
     */
    private Boolean mPublicUpload;
    private String mName;


    /**
     * Constructor. No update is initialized by default, need to be applied with setters below.
     *
     * @param remoteId Identifier of the share to update.
     */
    public UpdateRemoteShareOperation(long remoteId) {
        mRemoteId = remoteId;
        mPassword = null;               // no update
        mExpirationDateInMillis = 0;    // no update
        mPublicUpload = null;
        mPermissions = OCShare.DEFAULT_PERMISSION;
    }


    /**
     * Set name to update in Share resource. Ignored by servers previous to version 10.0.0
     *
     * @param name     Name to set to the target share.
     *                 Empty string clears the current name.
     *                 Null results in no update applied to the name.
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Set password to update in Share resource.
     *
     * @param password Password to set to the target share.
     *                 Empty string clears the current password.
     *                 Null results in no update applied to the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }


    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis Expiration date to set to the target share.
     *                               A negative value clears the current expiration date.
     *                               Zero value (start-of-epoch) results in no update done on
     *                               the expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }


    /**
     * Set permissions to update in Share resource.
     *
     * @param permissions Permissions to set to the target share.
     *                    Values <= 0 result in no update applied to the permissions.
     */
    public void setPermissions(int permissions) {
        mPermissions = permissions;
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload  Upload permission to set to the target share.
     *                      Null results in no update applied to the upload permission.
     */
    public void setPublicUpload(Boolean publicUpload) {
        mPublicUpload = publicUpload;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status;

        /// prepare array of parameters to update
        List<Pair<String, String>> parametersToUpdate = new ArrayList<>();
        if (mName != null) {
            parametersToUpdate.add(new Pair<>(PARAM_NAME, mName));
        }
        
        if (mPassword != null) {
            parametersToUpdate.add(new Pair<>(PARAM_PASSWORD, mPassword));
        }
        if (mExpirationDateInMillis < 0) {
            // clear expiration date
            parametersToUpdate.add(new Pair<>(PARAM_EXPIRATION_DATE, ""));

        } else if (mExpirationDateInMillis > 0) {
            // set expiration date
            DateFormat dateFormat = new SimpleDateFormat(FORMAT_EXPIRATION_DATE, Locale.GERMAN);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.setTimeInMillis(mExpirationDateInMillis);
            String formattedExpirationDate = dateFormat.format(expirationDate.getTime());
            parametersToUpdate.add(new Pair<>(PARAM_EXPIRATION_DATE, formattedExpirationDate));

        } // else, ignore - no update

        if (mPublicUpload != null) {
            parametersToUpdate.add(new Pair<>(PARAM_PUBLIC_UPLOAD, Boolean.toString(mPublicUpload)));
        }

        // IMPORTANT: permissions parameter needs to be updated after mPublicUpload parameter,
        // otherwise they would be set always as 1 (READ) in the server when mPublicUpload was updated
        if (mPermissions > 0) {
            // set permissions
            parametersToUpdate.add(new Pair<>(PARAM_PERMISSIONS, Integer.toString(mPermissions)));
        }

        /// perform required PUT requests
        PutMethod put = null;
        String uriString;

        try {
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH.substring(1));
            uriBuilder.appendEncodedPath(Long.toString(mRemoteId));
            uriString = uriBuilder.build().toString();

            for (Pair<String, String> parameter : parametersToUpdate) {
                if (put != null) {
                    put.releaseConnection();
                }
                put = new PutMethod(uriString);
                put.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
                put.setRequestEntity(new StringRequestEntity(
                        parameter.first + "=" + parameter.second,
                        ENTITY_CONTENT_TYPE,
                        ENTITY_CHARSET
                ));

                status = client.executeMethod(put);

                if (status == HttpStatus.SC_OK) {
                    String response = put.getResponseBodyAsString();

                    // Parse xml response
                    ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                            new ShareXMLParser()
                    );
                    parser.setOwnCloudVersion(client.getOwnCloudVersion());
                    parser.setServerBaseUri(client.getBaseUri());
                    result = parser.parse(response);

                } else {
                    result = new RemoteOperationResult(false, put);
                }
                if (!result.isSuccess() &&
                    !PARAM_NAME.equals(parameter.first)
                        // fail in "name" parameter will be ignored; requires OCX, will fail
                        // fails in previous versions
                    ) {
                    break;
                }
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while updating remote share ", e);
            if (put != null) {
                put.releaseConnection();
            }

        } finally {
            if (put != null) {
                put.releaseConnection();
            }
        }
        return result;
    }
}
