/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.lib.common;

import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory.OwnCloudAnonymousCredentials;
import com.owncloud.android.lib.common.http.HttpClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;
import com.owncloud.android.lib.common.utils.RandomUtils;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.owncloud.android.lib.common.http.HttpConstants.AUTHORIZATION_HEADER;
import static com.owncloud.android.lib.common.http.HttpConstants.HTTP_MOVED_PERMANENTLY;

public class OwnCloudClient extends HttpClient {

    public static final String WEBDAV_FILES_PATH_4_0 = "/remote.php/dav/files/";
    public static final String STATUS_PATH = "/status.php";
    private static final String WEBDAV_UPLOADS_PATH_4_0 = "/remote.php/dav/uploads/";
    private static final int MAX_RETRY_COUNT = 2;

    private static int sIntanceCounter = 0;
    private OwnCloudCredentials mCredentials = null;
    private int mInstanceNumber;
    private Uri mBaseUri;
    private OwnCloudAccount mAccount;
    private final ConnectionValidator mConnectionValidator;
    private Object mRequestMutex = new Object();

    // If set to true a mutex will be used to prevent parallel execution of the execute() method
    // if false the execute() method can be called even though the mutex is already aquired.
    // This is used for the ConnectionValidator, which has to be able to execute OperationsWhile all "normal" operations net
    // to be set on hold.
    private final Boolean mSynchronizeRequests;

    private SingleSessionManager mSingleSessionManager = null;

    private boolean mFollowRedirects = false;

    public OwnCloudClient(Uri baseUri,
                          ConnectionValidator connectionValidator,
                          boolean synchronizeRequests,
                          SingleSessionManager singleSessionManager,
                          Context context) {
        super(context);

        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be NULL");
        }
        mBaseUri = baseUri;
        mSynchronizeRequests = synchronizeRequests;
        mSingleSessionManager = singleSessionManager;

        mInstanceNumber = sIntanceCounter++;
        Timber.d("#" + mInstanceNumber + "Creating OwnCloudClient");

        clearCredentials();
        clearCookies();
        mConnectionValidator = connectionValidator;
    }

    public void clearCredentials() {
        if (!(mCredentials instanceof OwnCloudAnonymousCredentials)) {
            mCredentials = OwnCloudCredentialsFactory.getAnonymousCredentials();
        }
    }

    public int executeHttpMethod(HttpBaseMethod method) throws Exception {
        if (mSynchronizeRequests) {
            synchronized (mRequestMutex) {
                return saveExecuteHttpMethod(method);
            }
        } else {
            return saveExecuteHttpMethod(method);
        }
    }

    private int saveExecuteHttpMethod(HttpBaseMethod method) throws Exception {
        int repeatCounter = 0;
        int status;

        if (mFollowRedirects) {
            method.setFollowRedirects(true);
        }

        boolean retry;
        do {
            repeatCounter++;
            retry = false;
            String requestId = RandomUtils.generateRandomUUID();

            // Header to allow tracing requests in apache and ownCloud logs
            Timber.d("Executing in request with id %s", requestId);
            method.setRequestHeader(HttpConstants.OC_X_REQUEST_ID, requestId);
            method.setRequestHeader(HttpConstants.USER_AGENT_HEADER, SingleSessionManager.getUserAgent());
            method.setRequestHeader(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
            if (mCredentials.getHeaderAuth() != null && !mCredentials.getHeaderAuth().isEmpty()) {
                method.setRequestHeader(AUTHORIZATION_HEADER, mCredentials.getHeaderAuth());
            }

            status = method.execute(this);

            if (shouldConnectionValidatorBeCalled(method, status)) {
                retry = mConnectionValidator.validate(this, mSingleSessionManager, getContext()); // retry on success fail on no success
            } else if (method.getFollowPermanentRedirects() && status == HTTP_MOVED_PERMANENTLY) {
                retry = true;
                method.setFollowRedirects(true);
            }

        } while (retry && repeatCounter < MAX_RETRY_COUNT);

        return status;
    }

    private boolean shouldConnectionValidatorBeCalled(HttpBaseMethod method, int status) {

        return mConnectionValidator != null && (
                (!(mCredentials instanceof OwnCloudAnonymousCredentials) &&
                        status == HttpConstants.HTTP_UNAUTHORIZED
                ) || (!mFollowRedirects &&
                        !method.getFollowRedirects() &&
                        status == HttpConstants.HTTP_MOVED_TEMPORARILY
                )
        );
    }

    /**
     * Exhausts a not interesting HTTP response. Encouraged by HttpClient documentation.
     *
     * @param responseBodyAsStream InputStream with the HTTP response to exhaust.
     */
    public void exhaustResponse(InputStream responseBodyAsStream) {
        if (responseBodyAsStream != null) {
            try {
                responseBodyAsStream.close();

            } catch (IOException io) {
                Timber.e(io, "Unexpected exception while exhausting not interesting HTTP response; will be IGNORED");
            }
        }
    }

    public Uri getBaseFilesWebDavUri() {
        return Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0);
    }

    public Uri getUserFilesWebDavUri() {
        return (mCredentials instanceof OwnCloudAnonymousCredentials || mAccount == null)
                ? Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0)
                : Uri.parse(mBaseUri + WEBDAV_FILES_PATH_4_0 + AccountUtils.getUserId(
                        mAccount.getSavedAccount(), getContext()
                )
        );
    }

    public Uri getUploadsWebDavUri() {
        return mCredentials instanceof OwnCloudAnonymousCredentials
                ? Uri.parse(mBaseUri + WEBDAV_UPLOADS_PATH_4_0)
                : Uri.parse(mBaseUri + WEBDAV_UPLOADS_PATH_4_0 + AccountUtils.getUserId(
                        mAccount.getSavedAccount(), getContext()
                )
        );
    }

    public Uri getBaseUri() {
        return mBaseUri;
    }

    /**
     * Sets the root URI to the ownCloud server.
     * <p>
     * Use with care.
     *
     * @param uri
     */
    public void setBaseUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be NULL");
        }
        mBaseUri = uri;
    }

    public final OwnCloudCredentials getCredentials() {
        return mCredentials;
    }

    public void setCredentials(OwnCloudCredentials credentials) {
        if (credentials != null) {
            mCredentials = credentials;
        } else {
            clearCredentials();
        }
    }

    public void setCookiesForBaseUri(List<Cookie> cookies) {
        getOkHttpClient().cookieJar().saveFromResponse(
                HttpUrl.parse(mBaseUri.toString()),
                cookies
        );
    }

    public List<Cookie> getCookiesForBaseUri() {
        return getOkHttpClient().cookieJar().loadForRequest(
                HttpUrl.parse(mBaseUri.toString()));
    }

    public OwnCloudAccount getAccount() {
        return mAccount;
    }

    public void setAccount(OwnCloudAccount account) {
        this.mAccount = account;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.mFollowRedirects = followRedirects;
    }
}
