/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2019 ownCloud GmbH.
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.owncloud.android.lib.common.accounts.AccountTypeUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.io.IOException;

public class OwnCloudClientFactory {

    final private static String TAG = OwnCloudClientFactory.class.getSimpleName();

    /**
     * Creates a OwnCloudClient setup for an ownCloud account
     * <p>
     * Do not call this method from the main thread.
     *
     * @param account         The ownCloud account
     * @param appContext      Android application context
     * @param currentActivity Caller {@link Activity}
     * @return A OwnCloudClient object ready to be used
     * @throws AuthenticatorException     If the authenticator failed to get the authorization
     *                                    token for the account.
     * @throws OperationCanceledException If the authenticator operation was cancelled while
     *                                    getting the authorization token for the account.
     * @throws IOException                If there was some I/O error while getting the
     *                                    authorization token for the account.
     * @throws AccountNotFoundException   If 'account' is unknown for the AccountManager
     */
    public static OwnCloudClient createOwnCloudClient(Account account, Context appContext,
                                                      Activity currentActivity)
            throws OperationCanceledException, AuthenticatorException, IOException,
            AccountNotFoundException {
        Uri baseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        // TODO avoid calling to getUserData here
        boolean isOauth2 =
                am.getUserData(account, AccountUtils.Constants.KEY_SUPPORTS_OAUTH2) != null;
        boolean isSamlSso =
                am.getUserData(account, AccountUtils.Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null;
        OwnCloudClient client = createOwnCloudClient(baseUri, appContext, !isSamlSso);

        String username = AccountUtils.getUsernameForAccount(account);
        if (isOauth2) {    // TODO avoid a call to getUserData here
            AccountManagerFuture<Bundle> future = am.getAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypeAccessToken(account.type),
                    null,
                    currentActivity,
                    null,
                    null);

            Bundle result = future.getResult();
            String accessToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            if (accessToken == null) {
                throw new AuthenticatorException("WTF!");
            }
            client.setCredentials(
                    OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken)
            );

        } else if (isSamlSso) {    // TODO avoid a call to getUserData here
            AccountManagerFuture<Bundle> future = am.getAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type),
                    null,
                    currentActivity,
                    null,
                    null);

            Bundle result = future.getResult();
            String accessToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            if (accessToken == null) {
                throw new AuthenticatorException("WTF!");
            }
            client.setCredentials(
                    OwnCloudCredentialsFactory.newSamlSsoCredentials(username, accessToken)
            );

        } else {
            AccountManagerFuture<Bundle> future = am.getAuthToken(
                    account,
                    AccountTypeUtils.getAuthTokenTypePass(account.type),
                    null,
                    currentActivity,
                    null,
                    null
            );

            Bundle result = future.getResult();
            String password = result.getString(AccountManager.KEY_AUTHTOKEN);
            OwnCloudVersion version = AccountUtils.getServerVersionForAccount(account, appContext);
            client.setCredentials(
                    OwnCloudCredentialsFactory.newBasicCredentials(
                            username,
                            password,
                            (version != null && version.isPreemptiveAuthenticationPreferred())
                    )
            );
        }

        // Restore cookies
        AccountUtils.restoreCookies(account, client, appContext);

        return client;
    }

    /**
     * Creates a OwnCloudClient to access a URL and sets the desired parameters for ownCloud
     * client connections.
     *
     * @param uri     URL to the ownCloud server; BASE ENTRY POINT, not WebDavPATH
     * @param context Android context where the OwnCloudClient is being created.
     * @return A OwnCloudClient object ready to be used
     */
    public static OwnCloudClient createOwnCloudClient(Uri uri, Context context,
                                                      boolean followRedirects) {
        OwnCloudClient client = new OwnCloudClient(uri);

        client.setFollowRedirects(followRedirects);

        client.setContext(context);

        return client;
    }
}