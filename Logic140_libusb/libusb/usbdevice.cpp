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
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <libusb.h>
#include "libUsb_UsbDevice.h"

#define ARRAYSIZEOF(a) (sizeof(a)/sizeof(*(a)))
#define ULONG unsigned long
#define UCHAR unsigned char
#define Sleep(a) usleep((a)*1000)

#define EMULATION 0
//#define DEBUG

#define USB_PRODUCT_ID 0x8312
#define USB_VENDOR_ID  0x8312
#define USB_CONTROL_REQUEST_TYPE_IN 0x80

struct ctx {
	libusb_context *usbctx;
	libusb_device_handle* devhdl;
};

/*
* Class:     libUsb_UsbDevice
* Method:    open
* Signature: ()V
*/
JNIEXPORT jbyteArray JNICALL Java_libUsb_UsbDevice_open0
(JNIEnv *env, jobject obj, jlong msbits, jlong lsbits) {
#if !EMULATION
	libusb_context *usbctx;
	libusb_device_handle* devhdl;
	int res;

	(void)obj;

	/*
	 * initialize
	 */
	res = libusb_init(&usbctx);
	if (res != 0)
		env->ThrowNew(env->FindClass("java/io/IOException"), "Can't initialize libusb.");
	libusb_set_debug(usbctx, 3); 

#ifdef DEBUG
	/*
	 * enumeration (not needed, just information)
	 */
	ssize_t ndevs;
	libusb_device **list;
	struct libusb_device_descriptor desc;
	ndevs = libusb_get_device_list(usbctx, &list);
	printf("%d usb device%s found,\n", (int)ndevs, 1 < ndevs ? "s" : "");
	for (int i = 0; i < ndevs; i++) {
		libusb_get_device_descriptor(list[i], &desc);
		printf("%2d: %04x:%04x\n", i, desc.idVendor, desc.idProduct);
		if (desc.idVendor == USB_VENDOR_ID && desc.idProduct == USB_PRODUCT_ID) {
			struct libusb_config_descriptor* config;
			libusb_get_active_config_descriptor(list[i], &config);
			for (int i = 0; i < config->interface->num_altsetting; i++) {
				const struct libusb_interface_descriptor* iface = &config->interface->altsetting[i];
				printf("  bNumEndpoints=%d\n", iface->bNumEndpoints);
				for (int i = 0; i < iface->bNumEndpoints; i++) {
					const struct libusb_endpoint_descriptor* epd = &iface->endpoint[i];
					printf("    [%d]bEndpointAddess=0x%02x\n", i, epd->bEndpointAddress);
				}
			}
			libusb_free_config_descriptor(config);
		}
	}
	libusb_free_device_list(list, 1 /* unref devices in the list */);
#endif // DEBUG

	/*
	 * open device
	 */
	devhdl = libusb_open_device_with_vid_pid(usbctx, USB_VENDOR_ID, USB_PRODUCT_ID);
	if (devhdl == NULL)
		env->ThrowNew(env->FindClass("java/io/IOException"), "Can't open the USB device.");
	res = libusb_claim_interface(devhdl, 0);
	if (res < 0)
		env->ThrowNew(env->FindClass("java/io/IOException"), "Can't claim interface 0.");

	struct ctx ctx;
	ctx.usbctx = usbctx;
	ctx.devhdl = devhdl;
	jbyteArray ar = env->NewByteArray(sizeof(ctx));
	env->SetByteArrayRegion(ar, 0, sizeof(ctx), (jbyte*)&ctx);

	return ar;
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
	struct ctx ctx;

	(void)obj;
	if (descriptor != 0) {
		env->GetByteArrayRegion(descriptor, 0, sizeof(ctx), (jbyte*)&ctx);
		libusb_close(ctx.devhdl);
		libusb_exit(ctx.usbctx);
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
	struct ctx ctx;
	int res;
	unsigned char data_in;

	env->GetByteArrayRegion(descriptor, 0, sizeof(ctx), (jbyte*)&ctx);

	res = libusb_control_transfer(ctx.devhdl,
				USB_CONTROL_REQUEST_TYPE_IN,
				request,
				value,
				0,		// the index field for the setup packet
				&data_in,
				1,		// the length field for the setup packet. The data buffer should be at least this size.
				0);		// TIMEOUT_MS
#ifdef DEBUG
	printf("libusb_control_transfer(%02x, %02x): len=%d, res=%02x\n", request & 0xff, value & 0xff, res, data_in & 0xff);
#endif // DEBUG
	if (res != 1)
		env->ThrowNew(env->FindClass("java/io/IOException"), "control transfer function failed");

	return (jbyte)(data_in & 0xff);
#else
	(void)env;
	(void)value;
	(void)descriptor;
#ifdef DEBUG
	printf("libusb_control_transfer(%02x, %02x)=%d, data_in=%02x\n", request & 0xff, value & 0xff, 1, (request == 0x22 || request == 0x23 ? value : request == 0x50 ? 0x21 : request) & 0xff);
#endif // DEBUG
	return request == 0x22 || request == 0x23 ? value : request == 0x50 ? 0x21 : request;
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
	struct ctx ctx;
	int res;
	int transferred;
#endif
	UCHAR *p;
	(void)obj;
	p = (UCHAR*)env->GetDirectBufferAddress(buf);
	if (p == NULL)
		env->ThrowNew(env->FindClass("java/io/IOException"), "cannot get buffer address or not a direct buffer");
#if !EMULATION
	env->GetByteArrayRegion(descriptor, 0, sizeof(ctx), (jbyte*)&ctx);
	res = libusb_bulk_transfer(ctx.devhdl,
				0x2 | LIBUSB_ENDPOINT_IN,	// end point address
				p,
				(ULONG)env->GetDirectBufferCapacity(buf),
				&transferred,
				0);							// timeout (unlimited)
#ifdef DEBUG
	printf("libusb_bulk_transfer(length=%d): res=%d, transferred=%d\n", (int)env->GetDirectBufferCapacity(buf), res, transferred);
#endif // DEBUG
	if (res != 0)
		env->ThrowNew(env->FindClass("java/io/IOException"), "read function failed");
	return (jint)transferred;
#else
	(void)descriptor;
	num++;
	for (int i = (ULONG)env->GetDirectBufferCapacity(buf); --i >= 0; )
		p[i] = (UCHAR)(0x155 >> (((i + ((i & 1)*10)) >> 8) & 1));
	Sleep(100);
	return (jint)env->GetDirectBufferCapacity(buf);
#endif
}
