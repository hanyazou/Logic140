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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javafx.application.Platform;

/**
 *
 * @author Andrey Petushkov
 */
public class Data {
    private final List<Packet> data = new ArrayList<>();
    private boolean lead;
    private DDS140.Frequency frequency;
    private boolean trim;

    double percentCaptured;
    volatile int totalNumDataSamples;
    volatile int totalNumSamples;
    boolean isLogicAnalyzerMode;

    class Packet {
        final byte[] buf1;
        final byte[] buf2;
        final int trailingGapLength;
        final byte min;
        final byte max;
        final byte minCh1;
        final byte maxCh1;
        final byte minCh2;
        final byte maxCh2;
        final int bufLength;
        
        private Packet(byte[] buf1, byte[] buf2, int bufLength, int trailingGapLength, 
                byte min, byte max, byte minCh1, byte maxCh1, byte minCh2, byte maxCh2) {
            this.buf1 = buf1;
            this.buf2 = buf2;
            this.trailingGapLength = trailingGapLength;
            this.min = min;
            this.max = max;
            this.bufLength = bufLength;
            this.minCh1 = minCh1;
            this.maxCh1 = maxCh1;
            this.minCh2 = minCh2;
            this.maxCh2 = maxCh2;
        }
    }
    
    class DataIterator {
        Packet packet;
        int index;
        int sample;
        int packetSample;
        
        DataIterator() {
        }

        /**
         * Initializes the iterator to start with the provided sample
         * @param startSample
         * @return true if there is any data after requested sample, false
         * if the requested sample is exactly the next sample after the last
         * (or startSample is 0 and there is no data at all)
         */
        boolean init(int startSample) {
            if (startSample > totalNumSamples)
                throw new IndexOutOfBoundsException(""+startSample+" > "+totalNumSamples);
            index = 0;
            packetSample = 0;
            packet = null;
            while (index < data.size()) {
                packet = data.get(index);
                int packetLength = packet.bufLength + packet.trailingGapLength;
                if (startSample < packetLength) {
                    sample = startSample;
                    break;
                }
                startSample -= packetLength;
                packetSample += packetLength;
                if (++index == data.size()) {
                    if (startSample == 0)
                        return false;
                    else
                        throw new RuntimeException();
                }
            }
            return true;
        }
        
        boolean atLostData() {
            return sample >= packet.bufLength;
        }
        
        int getRemainingDataLength() {
            return packet.bufLength - sample;
        }
        
        int getRemainingLostLength() {
            return packet.bufLength + packet.trailingGapLength - sample;
        }
        
        int getLostDataLength() {
            return packet.trailingGapLength;
        }
        
        byte[] getData2() {
            if (atLostData())
                throw new IllegalStateException();
            return packet.buf2;
        }
        
        byte[] getData1() {
            if (atLostData())
                throw new IllegalStateException();
            return packet.buf1;
        }
        
        int getDataStart() {
            if (atLostData())
                throw new IllegalStateException();
            return sample;
        }
        
        boolean hasMoreData() {
            synchronized (data) {
                if (packet == null && index < data.size())
                    packet = data.get(index);
                return packet != null && (index < data.size() || sample < packet.bufLength + packet.trailingGapLength);
            }
        }
        
        boolean hasNextPacket() {
            synchronized (data) {
                return index < data.size() - 1;
            }
        }
        
        private void loadNextPacket() {
            synchronized (data) {
                packetSample += packet.bufLength + packet.trailingGapLength;
                packet = ++index < data.size() ? data.get(index) : null;
                sample = 0;
            }
        }
        
        byte getPacketMin() {
            return packet.min;
        }
        
        byte getPacketMax() {
            return packet.max;
        }
        
        int getPacketMinCh1() {
            return packet.minCh1 & 0xff;
        }
        
        int getPacketMaxCh1() {
            return packet.maxCh1 & 0xff;
        }
        
        int getPacketMinCh2() {
            return packet.minCh2 & 0xff;
        }
        
        int getPacketMaxCh2() {
            return packet.maxCh2 & 0xff;
        }
        
        void skip(int samples) {
            sample += samples;
            if (sample >= packet.bufLength + packet.trailingGapLength) {
                if (sample > packet.bufLength + packet.trailingGapLength)
                    throw new UnsupportedOperationException();
                loadNextPacket();
            }
        }
        
        void skipData() {
            if (sample <= packet.bufLength) {
                if (packet.trailingGapLength > 0)
                    sample = packet.bufLength;
                else 
                    loadNextPacket();
            } else
                throw new IllegalStateException();
        }
        
        void skipLostData() {
            if (sample < packet.bufLength || packet.trailingGapLength == 0)
                throw new IllegalStateException();
            loadNextPacket();
        }

        void skipToNextPacket() {
            loadNextPacket();
        }
        
        int getCurrentSample() {
            return packetSample + sample;
        }
        
        void backInTime(int sample) {
            if (sample < packetSample)
                throw new IllegalArgumentException("Not allowed to get back to different packet");
            this.sample = sample - packetSample;
        }
    }

    void startAcquisition(DDS140.Frequency frequency, boolean isLogicAnalyzerMode, boolean trim) {
        clear();
        this.trim = trim;
        this.frequency = frequency;
        this.isLogicAnalyzerMode = isLogicAnalyzerMode;
    }
    
    void stopAcquisition() {
        if (trim) {
            for (int i = data.size(); --i >= 1; ) {
                Packet p = data.get(i);
                synchronized (data) {
                    if (p.min == p.max)
                        data.remove(i);
                    else
                        break;
                    totalNumSamples -= p.bufLength + p.trailingGapLength;
                    totalNumDataSamples -= p.bufLength;
                }
            }
            Main.controller.updateWaves(totalNumSamples, totalNumSamples);
        }
        if (data.isEmpty())
            Main.error("No data captured (or all data trimmed)", false);
    }
    
    void clear() {
        totalNumSamples = 0;
        totalNumDataSamples = 0;
        lead = true;
        data.clear();
    }
    
    void processData(DDS140.Packet packet) {
        ByteBuffer buf = packet.getBuffer();
        final int packetTrailingGap = packet.getTrailingGapLength();
        byte[] d1 = isLogicAnalyzerMode ? null : new byte[DDS140.SAMPLES_PER_BUFFER];
        byte[] d2 = new byte[DDS140.SAMPLES_PER_BUFFER];
        byte min = (byte)0xff;
        byte max = 0;
        byte minCh1 = (byte)0xff;
        byte maxCh1 = 0;
        byte minCh2 = (byte)0xff;
        byte maxCh2 = 0;
        for (int i = DDS140.SAMPLES_PER_BUFFER; --i >= 0; ) {
            byte val1 = buf.get(i<<1);
            byte val2 = buf.get((i<<1)+1);
            min &= val2;
            max |= val2;
            if (val1 < minCh1)
                minCh1 = val1;
            if (val1 > maxCh1)
                maxCh1 = val1;
            if (val2 < minCh2)
                minCh2 = val2;
            if (val2 > maxCh2)
                maxCh2 = val2;
            if (!isLogicAnalyzerMode)
                d1[i] = val1;
            d2[i] = val2;
        }
        Main.device.releaseDataBuffer(packet);
        if (!lead || !trim || min != max) {
            synchronized (data) {
                data.add(new Packet(d1, d2, d2.length, packetTrailingGap, min, max, minCh1, maxCh1, minCh2, maxCh2));
                totalNumSamples += buf.capacity() / 2 + packetTrailingGap;
                totalNumDataSamples += buf.capacity() / 2;
            }
            lead = false;
            Platform.runLater(() -> Main.controller.updateWaves(totalNumSamples, totalNumSamples));
        }
    }
    
    static Data load(File file) throws IOException {
        Data instance = new Data();
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        instance.clear();
        try {
            instance.totalNumDataSamples = Integer.parseInt(props.getProperty("totalNumDataSamples"));
            instance.totalNumSamples = Integer.parseInt(props.getProperty("totalNumSamples"));
            instance.frequency = DDS140.Frequency.valueOf(props.getProperty("frequency"));
            instance.isLogicAnalyzerMode = !Boolean.parseBoolean(props.getProperty("isOscilloscope"));
            final int numPackets = Integer.parseInt(props.getProperty("numPackets"));
            for (int i = 0; i < numPackets; i++) {
                final int len = Integer.parseInt(props.getProperty("packet-"+i+"-length"));
                instance.data.add(instance.new Packet(
                        (byte[])(instance.isLogicAnalyzerMode ? null : loadBuffer(props, i, len, "1")),
                        loadBuffer(props, i, len, instance.isLogicAnalyzerMode ? "" : "2"),
                        len,
                        Integer.parseInt(props.getProperty("packet-"+i+"-gap")),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-min"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-max"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-minCh1", "80"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-maxCh1", "80"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-minCh2", "80"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-maxCh2", "80"), 16)
                ));
            }
            instance.lead = false;
        }
        catch (IllegalArgumentException | NullPointerException | OutOfMemoryError ex) {
            instance.clear();
            throw new IOException("Corrupt data file", ex);
        }
        return instance;
    }

    private static byte[] loadBuffer(Properties props, int packetIndex, int packetLength, String suffix) throws IOException {
        final byte[] buf = new byte[packetLength];
        final String d = props.getProperty("packet-"+packetIndex+"-data"+suffix);
        if (d.length() != packetLength*3-1)
            throw new IOException("Corrupt data file");
        for (int k = 0; k < packetLength; k++) {
            if (k < packetLength-1 && d.charAt(k*3+2) != ',')
                throw new IOException("Corrupt data file");
            buf[k] = (byte) Integer.parseInt(d.substring(k*3, k*3+2), 16);
        }
        return buf;
    }
    
    void save(File file) throws IOException {
        if (data.isEmpty()) {
            Main.error("No data to save. Please capture some", false);
            return;
        }
        Properties props = new Properties();
        props.setProperty("totalNumDataSamples", Integer.toString(totalNumDataSamples));
        props.setProperty("totalNumSamples", Integer.toString(totalNumSamples));
        props.setProperty("frequency", frequency.toString());
        props.setProperty("numPackets", Integer.toString(data.size()));
        for (int i = 0; i < data.size(); i++)
            serializePacket(i, data.get(i), props);
        try (OutputStream os = new FileOutputStream(file)) {
            props.store(os, "Logic140 captured data");
        }
    }

    private static void serializePacket(int i, Packet p, Properties props) {
        final int len = p.bufLength;
        props.setProperty("packet-"+i+"-length", Integer.toString(len));
        if (p.buf1 != null)
            serializeBuffer(props, p, p.buf1, i, len);
        serializeBuffer(props, p, p.buf2, i, len);
        props.setProperty("packet-"+i+"-gap", Integer.toString(p.trailingGapLength));
        props.setProperty("packet-"+i+"-min", Integer.toHexString(p.min&0xff));
        props.setProperty("packet-"+i+"-max", Integer.toHexString(p.max&0xff));
        props.setProperty("packet-"+i+"-minCh1", Integer.toHexString(p.minCh1&0xff));
        props.setProperty("packet-"+i+"-maxCh1", Integer.toHexString(p.maxCh1&0xff));
        props.setProperty("packet-"+i+"-minCh2", Integer.toHexString(p.minCh2&0xff));
        props.setProperty("packet-"+i+"-maxCh2", Integer.toHexString(p.maxCh2&0xff));
    }
    
    private static void serializeBuffer(Properties props, Packet p, byte[] buf, int index, int len) {
        StringBuilder str = new StringBuilder(len*3);
        for (byte b: buf)
            str.append(String.format("%02x", b&0xff)).append(',');
        props.setProperty("packet-"+index+"-data", str.substring(0, str.length()-1));
    }

    DDS140.Frequency getFrequency() {
        return frequency;
    }

    String sampleToTime(int pos, int unitsId) {
        if (frequency == null)
            return "";
        if (unitsId > 0) {
            double time = pos * Math.pow(1000, 4-unitsId) / frequency.getFrequency();
            return String.format("%.2f", time);
        } else
            return String.valueOf(pos);
    }

    double timeToSamples(double value, int unitsId) {
        if (frequency == null)
            return 0.;
        if (unitsId > 0)
            return value * frequency.getFrequency() / Math.pow(1000, 4-unitsId);
        else
            return value;
    }
}
