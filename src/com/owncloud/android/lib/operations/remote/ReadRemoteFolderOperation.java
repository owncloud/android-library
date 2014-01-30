/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
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

package com.owncloud.android.lib.operations.remote;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.webdav.WebdavEntry;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.RemoteFile;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;

/**
 * Remote operation performing the read of remote file or folder in the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class ReadRemoteFolderOperation extends RemoteOperation {

	private static final String TAG = ReadRemoteFolderOperation.class.getSimpleName();

	private String mRemotePath;
	private ArrayList<Object> mFolderAndFiles;
	
	/**
     * Constructor
     * 
     * @param remotePath		Remote path of the file. 
     */
	public ReadRemoteFolderOperation(String remotePath) {
		mRemotePath = remotePath;
	}

	/**
     * Performs the read operation.
     * 
     * @param   client      Client object to communicate with the remote ownCloud server.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
        PropFindMethod query = null;
        
        try {
            // remote request 
            query = new PropFindMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath),
                    DavConstants.PROPFIND_ALL_PROP,
                    DavConstants.DEPTH_1);
            int status = client.executeMethod(query);

            // check and process response
            if (isMultiStatus(status)) {
            	// get data from remote folder 
            	MultiStatus dataInServer = query.getResponseBodyAsMultiStatus();
            	readData(dataInServer, client);
            	
            	// Result of the operation
            	result = new RemoteOperationResult(true, status, query.getResponseHeaders());
            	// Add data to the result
            	if (result.isSuccess()) {
            		result.setData(mFolderAndFiles);
            	}
            } else {
                // synchronization failed
                client.exhaustResponse(query.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, status, query.getResponseHeaders());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            

        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods
            if (result.isSuccess()) {
                Log.i(TAG, "Synchronized "  + mRemotePath + ": " + result.getLogMessage());
            } else {
                if (result.isException()) {
                    Log.e(TAG, "Synchronized " + mRemotePath  + ": " + result.getLogMessage(), result.getException());
                } else {
                    Log.e(TAG, "Synchronized " + mRemotePath + ": " + result.getLogMessage());
                }
            }
            
        }
        return result;
	}

    public boolean isMultiStatus(int status) {
        return (status == HttpStatus.SC_MULTI_STATUS); 
    }

    /**
     *  Read the data retrieved from the server about the contents of the target folder 
     *  
     * 
     *  @param dataInServer     Full response got from the server with the data of the target 
     *                          folder and its direct children.
     *  @param client           Client instance to the remote server where the data were 
     *                          retrieved.  
     *  @return                
     */
    private void readData(MultiStatus dataInServer, OwnCloudClient client) {   	
        mFolderAndFiles = new ArrayList<Object>();
        
        // parse data from remote folder 
        WebdavEntry we = new WebdavEntry(dataInServer.getResponses()[0], client.getWebdavUri().getPath());
        mFolderAndFiles.add(fillOCFile(we));
        
        // loop to update every child
        RemoteFile remoteFile = null;
        for (int i = 1; i < dataInServer.getResponses().length; ++i) {
            /// new OCFile instance with the data from the server
            we = new WebdavEntry(dataInServer.getResponses()[i], client.getWebdavUri().getPath());                        
            remoteFile = fillOCFile(we);
            mFolderAndFiles.add(remoteFile);
        }
        
    }
    
    /**
     * Creates and populates a new {@link RemoteFile} object with the data read from the server.
     * 
     * @param we        WebDAV entry read from the server for a WebDAV resource (remote file or folder).
     * @return          New OCFile instance representing the remote resource described by we.
     */
    private RemoteFile fillOCFile(WebdavEntry we) {
        RemoteFile file = new RemoteFile(we.decodedPath());
        file.setCreationTimestamp(we.createTimestamp());
        file.setLength(we.contentLength());
        file.setMimeType(we.contentType());
        file.setModifiedTimestamp(we.modifiedTimestamp());
        file.setEtag(we.etag());
        return file;
    }
}
