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
 * @author Dragon
 */
public class Data {
    private static final List<Packet> data = new ArrayList<>();
    private static boolean lead;
    private static LogicAnalyzer.Frequency frequency;

    static double percentCaptured;
    static int totalNumDataSamples;
    static int totalNumSamples;

    static class Packet {
        final byte[] buf;
        final int trailingGapLength;
        final byte min;
        final byte max;
        
        private Packet(byte[] buf, int trailingGapLength, byte min, byte max) {
            this.buf = buf;
            this.trailingGapLength = trailingGapLength;
            this.min = min;
            this.max = max;
        }
    }
    
    static class DataIterator {
        Packet packet;
        int index;
        int sample;
        
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
                throw new IndexOutOfBoundsException();
            index = 0;
            packet = null;
            while (index < data.size()) {
                packet = data.get(index);
                int packetLength = packet.buf.length + packet.trailingGapLength;
                if (startSample < packetLength) {
                    sample = startSample;
                    break;
                }
                startSample -= packetLength;
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
            return sample >= packet.buf.length;
        }
        
        int getRemainingDataLength() {
            return packet.buf.length - sample;
        }
        
        int getRemainingLostLength() {
            return packet.buf.length + packet.trailingGapLength - sample;
        }
        
        int getLostDataLength() {
            return packet.trailingGapLength;
        }
        
        byte[] getData() {
            if (atLostData())
                throw new IllegalStateException();
            return packet.buf;
        }
        
        int getDataStart() {
            if (atLostData())
                throw new IllegalStateException();
            return sample;
        }
        
        boolean hasMoreData() {
            synchronized (data) {
                return packet != null && (index < data.size() || sample < packet.buf.length + packet.trailingGapLength);
            }
        }
        
        private void loadNextPacket() {
            synchronized (data) {
                packet = index < data.size()-1 ? data.get(++index) : null;
                sample = 0;
            }
        }
        
        byte getPacketMin() {
            return packet.min;
        }
        
        byte getPacketMax() {
            return packet.max;
        }
        
        void skip(int samples) {
            sample += samples;
            if (sample >= packet.buf.length + packet.trailingGapLength) {
                if (sample > packet.buf.length + packet.trailingGapLength)
                    throw new UnsupportedOperationException();
                loadNextPacket();
            }
        }
        
        void skipData() {
            if (sample <= packet.buf.length) {
                if (packet.trailingGapLength > 0)
                    sample = packet.buf.length;
                else 
                    loadNextPacket();
            } else
                throw new IllegalStateException();
        }
        
        void skipLostData() {
            if (sample < packet.buf.length || packet.trailingGapLength == 0)
                throw new IllegalStateException();
            loadNextPacket();
        }
    }

    static void startAcquisition(LogicAnalyzer.Frequency frequency) {
        clear();
        Data.frequency = frequency;
    }
    
    static void stopAcquisition(boolean trim) {
        if (trim) {
            for (int i = data.size(); --i >= 1; ) {
                Packet p = data.get(i);
                synchronized (data) {
                    if (p.min == p.max)
                        data.remove(i);
                    else
                        break;
                    totalNumSamples -= p.buf.length + p.trailingGapLength;
                    totalNumDataSamples -= p.buf.length;
                }
                Main.controller.updateWaves(totalNumSamples, totalNumSamples);
            }
        }
        if (data.size() == 0)
            Main.error("No data captured (or all data trimmed)", false);
    }
    
    private static void clear() {
        totalNumSamples = 0;
        totalNumDataSamples = 0;
        lead = true;
        data.clear();
    }
    
    static void processData(LogicAnalyzer.Packet packet) {
        ByteBuffer buf = packet.getBuffer();
        final int packetTrailingGap = packet.getTrailingGapLength();
        byte[] d = new byte[LogicAnalyzer.SAMPLES_PER_BUFFER];
        byte min = (byte)0xff;
        byte max = 0;
        for (int i = LogicAnalyzer.SAMPLES_PER_BUFFER; --i >= 0; ) {
            byte val = buf.get((i<<1)+1);
            min &= val;
            max |= val;
            d[i] = val;
        }
        Main.la.releaseDataBuffer(packet);
        if (!lead || !Main.trim || min != max) {
            synchronized (data) {
                data.add(new Packet(d, packetTrailingGap, min, max));
                totalNumSamples += buf.capacity() / 2 + packetTrailingGap;
                totalNumDataSamples += buf.capacity() / 2;
            }
            lead = false;
            Platform.runLater(() -> Main.controller.updateWaves(totalNumSamples, totalNumSamples));
        }
    }
    
    static void load(File file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        clear();
        try {
            totalNumDataSamples = Integer.parseInt(props.getProperty("totalNumDataSamples"));
            totalNumSamples = Integer.parseInt(props.getProperty("totalNumSamples"));
            frequency = LogicAnalyzer.Frequency.valueOf(props.getProperty("frequency"));
            final int numPackets = Integer.parseInt(props.getProperty("numPackets"));
            for (int i = 0; i < numPackets; i++) {
                final int len = Integer.parseInt(props.getProperty("packet-"+i+"-length"));
                final byte[] buf = new byte[len];
                final String d = props.getProperty("packet-"+i+"-data");
                if (d.length() != len*3-1)
                    throw new IOException("Corrupt data file");
                for (int k = 0; k < len; k++) {
                    if (k < len-1 && d.charAt(k*3+2) != ',')
                        throw new IOException("Corrupt data file");
                    buf[k] = (byte) Integer.parseInt(d.substring(k*3, k*3+2), 16);
                }
                data.add(new Packet(
                        buf,
                        Integer.parseInt(props.getProperty("packet-"+i+"-gap")),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-min"), 16),
                        (byte)Integer.parseInt(props.getProperty("packet-"+i+"-max"), 16)
                ));
            }
            lead = false;
            Platform.runLater(() -> Main.controller.dataLoaded(totalNumSamples));
        }
        catch (IllegalArgumentException | NullPointerException | OutOfMemoryError ex) {
            clear();
            throw new IOException("Corrupt data file", ex);
        }
    }
    
    static void save(File file) throws IOException {
        if (data.size() == 0) {
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
        final int len = p.buf.length;
        props.setProperty("packet-"+i+"-length", Integer.toString(len));
        StringBuilder str = new StringBuilder(len*3);
        for (byte b: p.buf)
            str.append(String.format("%02x", b&0xff)).append(',');
        props.setProperty("packet-"+i+"-data", str.substring(0, str.length()-1));
        props.setProperty("packet-"+i+"-gap", Integer.toString(p.trailingGapLength));
        props.setProperty("packet-"+i+"-min", Integer.toHexString(p.min&0xff));
        props.setProperty("packet-"+i+"-max", Integer.toHexString(p.max&0xff));
    }

    static LogicAnalyzer.Frequency getFrequency() {
        return frequency;
    }
}
