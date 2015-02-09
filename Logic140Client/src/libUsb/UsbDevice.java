/*
 * Copyright (c) 2015, Andrey Petushkov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package libUsb;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author Andrey Petushkov
 */
public class UsbDevice {
    private long msbits;
    private long lsbits;
    private byte[] descriptor;

    UsbDevice(long msbits, long lsbits) {
        this.msbits = msbits;
        this.lsbits = lsbits;
    }
    
    public void open() throws IOException {
        descriptor = open0(msbits, lsbits);
    }
    
    public void close() {
        try {
            close0(descriptor);
        }
        catch (Throwable ignored) {
        }
    }
    
    public byte controlRequest(byte request, byte value) throws IOException {
        return controlRequest0(descriptor, request, value);
    }
    
    public void fillBuffer(ByteBuffer buf) throws IOException {
        if (fillBuffer0(descriptor, buf) < buf.capacity())
            throw new IOException("cannot get full buffer or buffer size mismatch");
    }
    
    private native byte[] open0(long msbits, long lsbits) throws IOException;
    private native void close0(byte[] descriptor);
    private native byte controlRequest0(byte[] descriptor, byte request, byte value) throws IOException;
    private native int fillBuffer0(byte[] descriptor, ByteBuffer buf) throws IOException;
}
