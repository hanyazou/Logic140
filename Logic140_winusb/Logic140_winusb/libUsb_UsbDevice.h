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
#include <jni.h>
/* Header for class libUsb_UsbDevice */

#ifndef _Included_libUsb_UsbDevice
#define _Included_libUsb_UsbDevice
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     libUsb_UsbDevice
 * Method:    open0
 * Signature: (JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_libUsb_UsbDevice_open0
  (JNIEnv *, jobject, jlong, jlong);

/*
 * Class:     libUsb_UsbDevice
 * Method:    close0
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_libUsb_UsbDevice_close0
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     libUsb_UsbDevice
 * Method:    controlRequest
 * Signature: (BB)B
 */
JNIEXPORT jbyte JNICALL Java_libUsb_UsbDevice_controlRequest0
  (JNIEnv *, jobject, jbyteArray, jbyte, jbyte);

/*
 * Class:     libUsb_UsbDevice
 * Method:    fillBuffer
 * Signature: ([B)V
 */
JNIEXPORT jint JNICALL Java_libUsb_UsbDevice_fillBuffer0
  (JNIEnv *, jobject, jbyteArray, jobject);

#ifdef __cplusplus
}
#endif
#endif
