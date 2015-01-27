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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dragon
 */
public class UsbDeviceFactory {
    private static boolean init;
    
    public static UsbDevice createInstance(long msbits, long lsbits) {
        if (!init) {
            init = true;
            
            System.setSecurityManager(null); 
            
            try {
                try {
                    final File dllFile = File.createTempFile("Logic140", ".dll");
                    final String osname = System.getProperty("os.name");
                    final File jarFile = new File(
                            UsbDeviceFactory.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                    final JarFile jar = new JarFile(jarFile);
                    String resourceNamePrefix = "";
                    String resourceFileName;

                    if (osname.startsWith("Windows ")) {
                        resourceNamePrefix = "win";
                        resourceFileName = "Logic140_winusb.dll";
                    } else if (osname.equals("SunOS")) {
                        resourceNamePrefix = "solaris";
                        resourceFileName = "Logic140_libusb.so";
                    } else if (osname.equals("Linux")) {
                        resourceNamePrefix = "linux";
                        resourceFileName = "Logic140_libusb.so";
                    } else
                        throw new UnsupportedOperationException("Unsupported OS "+osname);

                    final InputStream is = jar.getInputStream(jar.getJarEntry(
                            resourceNamePrefix + System.getProperty("sun.arch.data.model") + '/' + resourceFileName));
                    final byte buf[] = new byte[1024];
                    try (OutputStream os = new FileOutputStream(dllFile)) {
                        int len;
                        while (true) {
                            len = is.read(buf);
                            if (len <= 0)
                                break;
                            os.write(buf, 0, len);
                        }
                    }
                    dllFile.deleteOnExit();
                    System.load(dllFile.getPath());
                }                
                catch (Exception ex) {
                    Logger.getLogger(UsbDeviceFactory.class.getName()).log(Level.SEVERE, "Cannot load libusb library", ex);
                    System.loadLibrary("Logic140_winusb");
                }
            } catch (Throwable err) {
                throw err instanceof RuntimeException ? (RuntimeException) err : new RuntimeException(err);
            }
        }
        return new UsbDevice(msbits, lsbits);
    }
}
