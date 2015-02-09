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
#include "pch.h"
#include "libUsb_UsbDevice.h"

#include <stdio.h>

#define EMULATION 0

/*
* Class:     libUsb_UsbDevice
* Method:    open
* Signature: ()V
*/
JNIEXPORT jbyteArray JNICALL Java_libUsb_UsbDevice_open0
(JNIEnv *env, jobject obj, jlong msbits, jlong lsbits) {
#if !EMULATION
	DEVICE_DATA           deviceData;
	HRESULT               hr;
	USB_DEVICE_DESCRIPTOR deviceDesc;
	BOOL                  bResult;
	BOOL                  noDevice;
	ULONG                 lengthReceived;

	(void)obj;

	//
	// Find a device connected to the system that has WinUSB installed using our
	// INF
	//
	hr = OpenDevice(msbits, lsbits, &deviceData, &noDevice);

	if (FAILED(hr)) {
		const char *msg;
		jclass clazz;
		if (noDevice) {
			msg = "Device not connected or driver not installed\n";
			clazz = env->FindClass("java/io/FileNotFoundException");
		}
		else {
			printf(_T("Failed looking for device, HRESULT 0x%x\n"), hr);
			msg = "Failed looking for device";
			clazz = env->FindClass("java/io/IOException");
		}

		env->ThrowNew(clazz, msg);
		return 0;
	}

	//
	// Get device descriptor
	//
	bResult = WinUsb_GetDescriptor(deviceData.WinusbHandle,
		USB_DEVICE_DESCRIPTOR_TYPE,
		0,
		0,
		(PBYTE)&deviceDesc,
		sizeof(deviceDesc),
		&lengthReceived);

	if (FALSE == bResult || lengthReceived != sizeof(deviceDesc)) {

		printf(_T("Error among LastError %d or lengthReceived %d\n"),
			FALSE == bResult ? GetLastError() : 0,
			lengthReceived);
		env->ThrowNew(env->FindClass("java/io/IOException"), "Cannot get USB device descriptor");
		CloseDevice(&deviceData);
		return 0;
	}

	//
	// Print a few parts of the device descriptor
	//
	printf(_T("Device found: VID_%04X&PID_%04X; bcdUsb %04X\n"),
		deviceDesc.idVendor,
		deviceDesc.idProduct,
		deviceDesc.bcdUSB);

	jbyteArray res = env->NewByteArray(sizeof(deviceData));
	env->SetByteArrayRegion(res, 0, sizeof(deviceData), (jbyte*)&deviceData);

	return res;
#else
	(void)env;
	(void)obj;
	(void)msbits;
	(void)lsbits;
	return env->NewByteArray(0);
#endif
}

/*
* Class:     libUsb_UsbDevice
* Method:    close
* Signature: ()V
*/
JNIEXPORT void JNICALL Java_libUsb_UsbDevice_close0
(JNIEnv *env, jobject obj, jbyteArray descriptor) {
#if !EMULATION
	DEVICE_DATA           deviceData;
	(void)obj;
	if (descriptor != 0) {
		env->GetByteArrayRegion(descriptor, 0, sizeof(deviceData), (jbyte*)&deviceData);
		CloseDevice(&deviceData);
	}
#else
	(void)env;
	(void)obj;
	(void)descriptor;
#endif
}

/*
* Class:     libUsb_UsbDevice
* Method:    controlRequest
* Signature: (BB)B
*/
JNIEXPORT jbyte JNICALL Java_libUsb_UsbDevice_controlRequest0
(JNIEnv *env, jobject obj, jbyteArray descriptor, jbyte request, jbyte value) {
	(void)obj;
#if !EMULATION
	DEVICE_DATA           deviceData;
	WINUSB_SETUP_PACKET setup = { 0x80 /* in, std, whole device */, request, value, 0, 1 };
	jbyte res;
	ULONG resLength;
	env->GetByteArrayRegion(descriptor, 0, sizeof(deviceData), (jbyte*)&deviceData);
	if (WinUsb_ControlTransfer(deviceData.WinusbHandle, setup, (UCHAR*)&res, 1, &resLength, NULL))
		return res;
	env->ThrowNew(env->FindClass("java/io/IOException"), "control transfer function failed");
	return 0;
#else
	(void)env;
	(void)value;
	(void)descriptor;
	return request == 0x22 || request == 0x23 ? 0x00 : request == 0x50 ? 0x21 : request;
#endif
}

#if EMULATION
static char num = 0;
#endif
/*
* Class:     libUsb_UsbDevice
* Method:    fillBuffer
* Signature: ([B)V
*/
JNIEXPORT jint JNICALL Java_libUsb_UsbDevice_fillBuffer0
(JNIEnv *env, jobject obj, jbyteArray descriptor, jobject buf) {
#if !EMULATION
	DEVICE_DATA           deviceData;
	ULONG resLength;
#endif
	UCHAR *p;
	(void)obj;
	p = (UCHAR*)env->GetDirectBufferAddress(buf);
	if (p == NULL)
		env->ThrowNew(env->FindClass("java/io/IOException"), "cannot get buffer address or not a direct buffer");
#if !EMULATION
	env->GetByteArrayRegion(descriptor, 0, sizeof(deviceData), (jbyte*)&deviceData);
	if (WinUsb_ReadPipe(deviceData.WinusbHandle, 0x82, p, (ULONG)env->GetDirectBufferCapacity(buf), &resLength, NULL))
		return (jlong)resLength;
	printf(_T("LastError %d\n"), GetLastError());
	env->ThrowNew(env->FindClass("java/io/IOException"), "read function failed");
	return 0;
#else
	(void)descriptor;
	num++;
	for (int i = (ULONG)env->GetDirectBufferCapacity(buf); --i >= 0; )
		p[i] = num != 3 && num != 5 ? 1 : (UCHAR)(0x155 >> (((i + ((i & 1)*10)) >> 8) & 1));
	Sleep(100);
	return (jint)env->GetDirectBufferCapacity(buf);
#endif
}
