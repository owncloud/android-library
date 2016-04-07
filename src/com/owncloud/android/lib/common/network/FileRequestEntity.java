/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.lib.common.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.methods.RequestEntity;


/**
 * A RequestEntity that represents a File.
 * 
 */
public class FileRequestEntity implements RequestEntity, ProgressiveDataTransferer {

    protected final FileChannel mChannel;
    protected final String mRemotePath;
    protected final String mContentType;
    protected Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();

    @Deprecated
    public FileRequestEntity(final File file, final String contentType) {
        super();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            mChannel = raf.getChannel();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File may not be null");
        }
        this.mContentType = contentType;
        this.mRemotePath = file.getAbsolutePath();
    }

    public FileRequestEntity(final FileChannel channel, final String contentType, final String remotePath) {
        super();
        if (channel == null) {
            throw new IllegalArgumentException("File channel may not be null");
        }
        this.mChannel = channel;
        this.mRemotePath = remotePath;
        this.mContentType = contentType;
    }
    
    @Override
    public long getContentLength() {
        try {
            return mChannel.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }
    
    @Override
    public void addDatatransferProgressListeners(Collection<OnDatatransferProgressListener> listeners) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.addAll(listeners);
        }
    }
    
    @Override
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }
    
    
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        ByteBuffer tmp = ByteBuffer.allocate(4096);
        int readResult = 0;

        Iterator<OnDatatransferProgressListener> it = null;
        long transferred = 0;
        long size = mChannel.size();
        if (size == 0) size = -1;
        try {
            while ((readResult = mChannel.read(tmp)) >= 0) {
                try {
                    out.write(tmp.array(), 0, readResult);
                } catch (IOException io) {
                    // work-around try catch to filter exception in writing
                    throw new WriteException(io);
                }
                tmp.clear();
                transferred += readResult;
                synchronized (mDataTransferListeners) {
                    it = mDataTransferListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onTransferProgress(readResult, transferred, size, mRemotePath);
                    }
                }
            }

        } catch (IOException io) {
            // any read problem will be handled as if the file is not there
            if (io instanceof FileNotFoundException) {
                throw io;
            } else {
                FileNotFoundException fnf = new FileNotFoundException("Exception reading source file");
                fnf.initCause(io);
                throw fnf;
            }

        } catch (WriteException we) {
            throw we.getWrapped();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static class WriteException extends Exception {
        IOException mWrapped;

        WriteException(IOException wrapped) {
            mWrapped = wrapped;
        }

        public IOException getWrapped() {
            return mWrapped;
        }
    }

}
