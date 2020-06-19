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

package com.owncloud.android.lib.common.http.methods.webdav;

import at.bitfire.dav4jvm.Property;
import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.exception.DavException;
import kotlin.Unit;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Propfind calls wrapper
 *
 * @author David González Verdugo
 */
public class PropfindMethod extends DavMethod {

    // request
    private final int mDepth;
    private final Property.Name[] mPropertiesToRequest;

    // response
    private final List<Response> mMembers;
    private Response mRoot;

    public PropfindMethod(URL url, int depth, Property.Name[] propertiesToRequest) {
        super(url);
        mDepth = depth;
        mPropertiesToRequest = propertiesToRequest;
        mMembers = new ArrayList<>();
        mRoot = null;
    }

    @Override
    public int onExecute() throws IOException, DavException {
        mDavResource.propfind(mDepth, mPropertiesToRequest,
                (Response response, Response.HrefRelation hrefRelation) -> {
                    switch (hrefRelation) {
                        case MEMBER:
                            mMembers.add(response);
                            break;
                        case SELF:
                            mRoot = response;
                            break;
                        case OTHER:
                        default:
                    }
                    return Unit.INSTANCE;
                }, response -> {
                    mResponse = response;
                    return Unit.INSTANCE;
                });

        return getStatusCode();
    }

    public int getDepth() {
        return mDepth;
    }

    public List<Response> getMembers() {
        return mMembers;
    }

    public Response getRoot() {
        return mRoot;
    }
}