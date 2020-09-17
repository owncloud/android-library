package com.owncloud.android.lib.common;

import at.bitfire.dav4jvm.exception.HttpException;
import com.owncloud.android.lib.common.SingleSessionManager;
import com.owncloud.android.lib.common.authentication.OwnCloudCredentials;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.HttpBaseMethod;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.utils.RandomUtils;
import okhttp3.HttpUrl;
import timber.log.Timber;

import static com.owncloud.android.lib.common.http.HttpConstants.OC_X_REQUEST_ID;

class RedirectionManager {
    private static final int MAX_REDIRECTIONS_COUNT = 3;

    private final OwnCloudClient ownCloudClient;

    public RedirectionManager(OwnCloudClient client) {
        ownCloudClient = client;
    }

    private int executeRedirectedHttpMethod(HttpBaseMethod method, OwnCloudCredentials credentials) throws Exception {
        boolean repeatWithFreshCredentials;
        int repeatCounter = 0;
        int status;

        do {
            String requestId = RandomUtils.generateRandomUUID();

            // Header to allow tracing requests in apache and ownCloud logs
            Timber.d("Executing in request with id %s", requestId);
            method.setRequestHeader(OC_X_REQUEST_ID, requestId);
            method.setRequestHeader(HttpConstants.USER_AGENT_HEADER, SingleSessionManager.getUserAgent());
            method.setRequestHeader(HttpConstants.PARAM_SINGLE_COOKIE_HEADER, "true");
            method.setRequestHeader(HttpConstants.ACCEPT_ENCODING_HEADER, HttpConstants.ACCEPT_ENCODING_IDENTITY);
            if (credentials.getHeaderAuth() != null) {
                method.setRequestHeader(HttpConstants.AUTHORIZATION_HEADER, credentials.getHeaderAuth());
            }
            status = method.execute();

            repeatWithFreshCredentials = ownCloudClient.checkUnauthorizedAccess(status, repeatCounter);
            if (repeatWithFreshCredentials) {
                repeatCounter++;
            }
        } while (repeatWithFreshCredentials);

        return status;
    }

    private String getLocationFromMethod(HttpBaseMethod method) {
        return method.getResponseHeader(HttpConstants.LOCATION_HEADER) != null
                ? method.getResponseHeader(HttpConstants.LOCATION_HEADER)
                : method.getResponseHeader(HttpConstants.LOCATION_HEADER_LOWER);
    }

    private boolean shouldFollowRedirection(int redirectionsCount, int status) {
        return (redirectionsCount < MAX_REDIRECTIONS_COUNT &&
                (status == HttpConstants.HTTP_MOVED_PERMANENTLY ||
                        status == HttpConstants.HTTP_MOVED_TEMPORARILY ||
                        status == HttpConstants.HTTP_TEMPORARY_REDIRECT));
    }

    private String getDestinationFromMethod(HttpBaseMethod method) {
        return method.getRequestHeader("Destination") != null
                ? method.getRequestHeader("Destination")
                : method.getRequestHeader("destination");
    }

    private String buildDestinationHeader(String location, String destination) {
        final int suffixIndex = location.lastIndexOf(ownCloudClient.getUserFilesWebDavUri().toString());
        final String redirectionBase = location.substring(0, suffixIndex);
        final String destinationPath = destination.substring(ownCloudClient.getBaseUri().toString().length());
        return redirectionBase + destinationPath;
    }

    private int followRedirect(HttpBaseMethod method, String location, String destination) throws Exception {
        if (destination != null) {
            method.setRequestHeader("destination", buildDestinationHeader(location, destination));
        }
        try {
            return executeRedirectedHttpMethod(method, ownCloudClient.getCredentials());
        } catch (HttpException e) {
            if (e.getMessage().contains(Integer.toString(HttpConstants.HTTP_MOVED_TEMPORARILY))) {
                return HttpConstants.HTTP_MOVED_TEMPORARILY;
            } else {
                throw e;
            }
        }
    }

    public RedirectionPath followRedirection(HttpBaseMethod method) throws Exception {
        int redirectionsCount = 0;
        int status = method.getStatusCode();
        RedirectionPath redirectionPath = new RedirectionPath(status, MAX_REDIRECTIONS_COUNT);

        while (shouldFollowRedirection(redirectionsCount, status)) {
            final String location = getLocationFromMethod(method);

            if (location != null) {
                Timber.d("#" + ownCloudClient.getInstanceNumber() + "Location to redirect: " + location);

                redirectionPath.addLocation(location);

                // Release the connection to avoid reach the max number of connections per host
                // due to it will be set a different url
                ownCloudClient.exhaustResponse(method.getResponseBodyAsStream());

                method.setUrl(HttpUrl.parse(location));
                final String destination = getDestinationFromMethod(method);

                status = followRedirect(method, location, destination);
                redirectionPath.addStatus(status);
                redirectionsCount++;

            } else {
                Timber.d(" #" + ownCloudClient.getInstanceNumber() + "No location to redirect!");
                status = HttpConstants.HTTP_NOT_FOUND;
            }
        }
        return redirectionPath;
    }
}
