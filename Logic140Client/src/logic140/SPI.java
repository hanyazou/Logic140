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
package logic140;

/**
 *
 * @author Andrey Petushkov
 */
public class SPI {
    private static Data.DataIterator data;
    private static int sclk;
    private static int sclkV;
    private static int mosi;
    private static int miso;
    private static int cs;
    private static boolean isInit;
    private static boolean cpol;
    private static boolean cpha;
    
    interface SPIEventHandler {
        void spiError(int sample, String msg);
        void spiData(int sample, int mosi, int miso, int ss);
    }
    
    static void setData(Data dataRef) {
        data = dataRef.new DataIterator();
    }
    
    static void init(int sclk, int mosi, int miso, int cs, boolean cpol, boolean cpha) {
        if (sclk < 0 || sclk >= 8 ||
                ((mosi < 0 || mosi >= 8) && (miso < 0 || miso >= 8))) {
            Main.error("Invalid SPI configuration. Need at least SCLK and either of MOSI or MISO specified", false);
            isInit = false;
        }
        SPI.sclk = 1 << sclk;
        SPI.mosi = 1 << mosi;
        SPI.miso = 1 << miso;
        SPI.cs = 1 << cs;
        SPI.cpol = cpol;
        SPI.cpha = cpha;
        sclkV = cpol ? 0 : sclk;
        isInit = true;
    }
    
    static void deinit() {
        isInit = false;
    }
    
    static void reset() {
        if (!isInit)
            return;
        data.init(0);
    }
    
    static void parse(SPIEventHandler handler) {
        if (!isInit || !data.hasMoreData())
            return;
        
        int startSample = data.getCurrentSample();
        int[] hp = new int[15]; // lengths of SCLK half-periods during one byte of data
        
        nextDataPacket:
        for (; data.hasMoreData(); data.skipToNextPacket()) {
            // the algorithm:
            // 1. get data packet
            // 2. search for CE
            // 3. while CE search for SCLK
            // 4. check if SCLK valid
            // 5. check for enough clock signals, consider moving to next packet if no gap
            // 5.a. if not enough samples and there is a gap, skip to next packet, goto step 1.
            // if failed on 2, 3 or 4 - go to next data packet and step 1 (if exists) or stop at the end (exit)
            // if failed on 5 - stop on current position to continue when more data if added (exit)
            // 6. parse data at SCLK ticks, verify data consistency

            // step 1.
            byte[] d = data.getData2();
            int i = data.getDataStart();
            
            nextDataByte:
            while (true) {
                // step 2.
                while (i < d.length && !isCS(d[i]))
                    i++;
                if (i == d.length)
                    continue nextDataPacket;

                // step 3.
                while (i < d.length && isCS(d[i]) && !isSCLK(d[i]))
                    i++;
                if (i == d.length || !isCS(d[i]))
                    continue nextDataPacket;

                // step 4.
                // check by searching next 8 periods of SCLK
                startSample = data.getCurrentSample();
                boolean switchedPackets = false;
                boolean currentSclk = true;
                int prevSample = startSample;
                int meanHalfPeriod = 0;
                for (int x = 0; x < 15 && i < d.length && isCS(d[i]); x++) {
                    hp[i] = data.getCurrentSample() - prevSample;
                    prevSample = data.getCurrentSample();
                    meanHalfPeriod += hp[i];
                    while (isSCLK(d[i]) == currentSclk) {
                        if (++i == d.length) {
                            if (data.getLostDataLength() > 0)
                                continue nextDataPacket;
                            if (!data.hasNextPacket()) {
                                if (!switchedPackets)
                                    data.backInTime(startSample);
                                else
                                    data.skipToNextPacket();
                                break nextDataPacket;
                            }
                            data.skipToNextPacket();
                            switchedPackets = true;
                            i = 0;
                        }
                    }
                    currentSclk = isSCLK(d[i]);
                }
                // found all 16 fronts, check consistency of periods
                meanHalfPeriod /= 15;
                if (meanHalfPeriod < 2) {
                    handler.spiError(startSample, "SCLK too high");
                    continue nextDataPacket;
                }
                for (int x = 0; x < 15; x++)
                    if ((hp[i] < meanHalfPeriod/1.2 && hp[i] < meanHalfPeriod-3) ||
                            (hp[i] > meanHalfPeriod*1.2 && hp[i] > meanHalfPeriod+3)) {
                        handler.spiError(startSample, "SCLK uneven");
                        continue nextDataPacket;
                    }
                System.out.println("found spi byte at "+startSample);
                if (++i == d.length) {
                    continue nextDataPacket;
                }
            }
        }
    }
    
    private static boolean isCS(byte d) {
        return (d & cs) == 0; // CS active low
    }
    
    private static boolean isSCLK(byte d) {
        return (d & sclk) == sclkV;
    }
}
