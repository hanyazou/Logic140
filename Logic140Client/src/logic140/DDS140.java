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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import libUsb.UsbDevice;
import libUsb.UsbDeviceFactory;

/**
 *
 * @author Andrey Petushkov
 */
class DDS140 implements Runnable {
    private static final int BUFFER_SIZE = 0x20000;
    private static final int MAX_MEMORY = 10*1024*1024; // TODO base on configured heap maximum
    static int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    
    private final UsbDevice usb;
    private final Thread dataThread;
    private final Object lock = new Object();
    private volatile State state;
    private IOException dataLoopException;
    private final List<Packet> freePool = new LinkedList<>();
    private final List<Packet> busyPool = new LinkedList<>();
    private Frequency curFrequency = Frequency.F_10M;
    private boolean isLogicAnalyzerMode;
    private Voltage ch1Voltage;
    private Voltage ch2Voltage;

    private enum State {
        INIT,
        GO,
        RUNNING,
        STOPPED,
        INTERRUPTED,
        FINISHED;
    }

    class Packet {
        private final ByteBuffer buf;
        private int trailingGapLength;
        
        Packet(ByteBuffer buf) {
            this.buf = buf;
        }
        
        ByteBuffer getBuffer() {
            return buf;
        }
        
        int getTrailingGapLength() {
            return trailingGapLength;
        }
    }
    
    enum Frequency {
        F_39k(0x1b, 39_000, "39k"),
        F_625k(0x18, 625_000, "625k"),
        F_10M(0x1c, 10_000_000, "10M"),
        F_80M(0x11, 80_000_000, "80M"),
        F_100M(0x10, 100_000_000, "100M");
        
        private final byte val;
        private final int freq;
        private final String text;
        
        Frequency(int val, int freq, String text) {
            this.val = (byte)val;
            this.freq = freq;
            this.text = text;
        }

        public int getFrequency() {
            return freq;
        }
        
        private byte getCode() {
            return val;
        }
        
        public String getText() {
            return text;
        }
    }

    enum Voltage {
//22,08 50mV
//22,04 100mV
//22,00 200mV
//22,06 500mV
//22,02 1V
//22,02 2V
//22,02 5V
//
//CH2 Voltage setting:
//23,20 50mV
//23,10 100mV
//23,00 200mV
//23,12 500mV
//23,02 1V
//23,02 2V
//23,02 5V        
        
        V_50m(0x08, 0x20, 50, "50mV"),
        V_100m(0x04, 0x10, 100, "100mV"),
        V_200m(0x00, 0x00, 200, "200mV"),
        V_500m(0x02, 0x12, 500, "500mV"),
        V_1(0x02, 0x02, 1000, "1V"),
        V_2(0x02, 0x02, 2000, "2V"),
        V_5(0x02, 0x02, 5000, "5V");
        
        private final int ch1val;
        private final int ch2val;
        private final String name;
        private final int ref;
        
        Voltage(int ch1val, int ch2val, int ref, String name) {
            this.ch1val = ch1val;
            this.ch2val = ch2val;
            this.name = name;
            this.ref = ref;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public DDS140() {
        usb = UsbDeviceFactory.createInstance(0xDC5D4E19_D8E4_4BE9L, 0x9D_CD_71_A0_5D_36_FA_CDL);
        dataThread = new Thread(this, "LogicAnalyzer");
        dataThread.setPriority(7);
    }
    
    public void init() throws IOException {
        usb.open();
        usb.controlRequest((byte) 0x76, (byte) 0xe8); // timers
        usb.controlRequest((byte) 0x77, (byte) 0x9b); // timers
        usb.controlRequest((byte) 0x78, (byte) 0xe8); // timers
        usb.controlRequest((byte) 0x79, (byte) 0x9b); // timers
        usb.controlRequest((byte) 0x63, (byte) 0x04); // 
        usb.controlRequest((byte) 0x75, (byte) 0x00); // timers
        if (usb.controlRequest((byte) 0x34, (byte) 0x00) != (byte) 0x34) // 
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x34, (byte) 0x00) != (byte) 0x34) // 
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x7a, (byte) 0xfb) != (byte) 0x7a) // timers
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x7b, (byte) 0x8c) != (byte) 0x7b) // timers
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x7c, (byte) 0xff) != (byte) 0x7c) // timers
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x7d, (byte) 0xc4) != (byte) 0x7d) // timers
            throw new IOException("DD140 init failed");

        if (usb.controlRequest((byte) 0x24, (byte) 0x10) != (byte) 0x24) // 
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x94, (byte) 0x1c) != (byte) 0x94) // 10mhz
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x22, (byte) 0x00) != (byte) 0x00) // voltage ch1
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x24, (byte) 0x18) != (byte) 0x24) // 
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x23, (byte) 0x00) != (byte) 0x00) // voltage ch2
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x24, (byte) 0x18) != (byte) 0x24) // 
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x94, (byte) 0x1c) != (byte) 0x94) // 10mhz
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0x24, (byte) 0x18) != (byte) 0x24) // enable ch1 & ch2
            throw new IOException("DD140 init failed");
        if (usb.controlRequest((byte) 0xe7, (byte) 0x00) != (byte) 0xe7) // 
            throw new IOException("DD140 init failed");
        state = State.INIT;
        dataThread.start();
    }

    void setIsLogicAnalyzer(boolean isLogicAnalyzerMode) {
        this.isLogicAnalyzerMode = isLogicAnalyzerMode;
    }
    
    public void go() throws IOException {
        if (!(state == State.INIT || state == State.STOPPED))
            return;
        if (state == State.INTERRUPTED)
            throw new IOException("Internal error: data thread was interrupted");
        synchronized (lock) {
            state = State.GO;
            dataLoopException = null;
            lock.notifyAll();
            while (state == State.GO) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }
    
    public void stop() {
        if (state != State.RUNNING)
            return;
        synchronized (lock) {
            state = State.STOPPED;
            lock.notifyAll();
            freePool.addAll(busyPool);
            busyPool.clear();
        }
    }
    
    public Packet getData() throws IOException {
        if (dataLoopException != null)
            throw dataLoopException;
        
        synchronized (lock) {
            while (state == State.RUNNING && busyPool.isEmpty())
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    return null;
                }
        }
        if (dataLoopException != null)
            throw dataLoopException;
        return busyPool.isEmpty() ? null : busyPool.remove(0);
    }
    
    public void releaseDataBuffer(Packet packet) {
        freePool.add(packet);
    }

    void finish() {
        try {
            stop();
            state = State.FINISHED;
            synchronized (lock) {
                lock.notifyAll();
            }
            Thread.sleep(100);
            usb.close();
            dataThread.join();
        } catch (InterruptedException ignore) {
        }
    }

    void setFrequency(Frequency frequency) throws IOException {
        if (usb.controlRequest((byte) 0x94, (byte) frequency.getCode()) != (byte) 0x94)
            throw new IOException("DD140 set frequency failed");
        curFrequency = frequency;
    }
    
    void setVoltage(Voltage ch1, Voltage ch2) throws IOException {
        ch1Voltage = ch1;
        ch2Voltage = ch2;
        if (usb.controlRequest((byte) 0x22, (byte) ch1Voltage.ch1val) != (byte) ch1Voltage.ch1val)
            throw new IOException("DDS140 set ch1 voltage failed");
        if (usb.controlRequest((byte) 0x23, (byte) ch2Voltage.ch2val) != (byte) ch2Voltage.ch2val)
            throw new IOException("DDS140 set ch2 voltage failed");
    }
    
    @Override
    public void run() {
        try {
            while (state != State.FINISHED) {
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                    if (state == State.GO) {
                        
                        state = State.RUNNING;
                        synchronized (lock) {
                            lock.notifyAll();
                        }

                        if (usb.controlRequest((byte) 0x34, isLogicAnalyzerMode ? (byte) 0x01 : (byte) 0x00) != (byte) 0x34) // 
                            throw new IOException("DD140 go failed");
                        if (usb.controlRequest((byte) 0x35, (byte) 0x00) != (byte) 0x35) // 
                            throw new IOException("DD140 go failed");

                        final long startTime = System.nanoTime();
                        long dataStartTime = 0;
                        long dataReadyTime;
                        long dataInTime;
                        
                        if (usb.controlRequest((byte) 0x33, (byte) 0x00) != (byte) 0x33) // 
                            throw new IOException("DD140 go failed");
                        boolean firstPacket = true;
                        while (true) {
                            waitFifoFull();
                            if (state != State.RUNNING)
                                break;
                            dataReadyTime = System.nanoTime();
                            Packet packet = getFreePacket();
                            usb.fillBuffer(packet.getBuffer());
                            dataInTime = System.nanoTime();
                            boolean isRunning = state == State.RUNNING;
                            if (isRunning) {
                                if (usb.controlRequest((byte) 0x33, (byte) 0x00) != (byte) 0x33) // 
                                    throw new IOException("DD140 go failed");
                            }
                            if (!isRunning) {
                                releaseDataBuffer(packet);
                                break;
                            }
                            if (firstPacket) {
                                firstPacket = false;
                                releaseDataBuffer(packet);
                            } else {
                                Main.addEvent(new Event(dataReadyTime-startTime, "data ready"));
                                Main.addEvent(new Event(dataInTime-startTime, 
                                        String.format("%d%% in/%d%% lost", 
                                                (int)(65536e11/(dataInTime-dataStartTime))/curFrequency.getFrequency(),
                                                (int)((dataInTime-dataReadyTime)*100./(dataInTime-dataStartTime)))));
                                packet.trailingGapLength = (int)((dataInTime-dataReadyTime)/1e9*curFrequency.getFrequency());
                                synchronized (lock) {
                                    busyPool.add(packet);
                                    lock.notifyAll();
                                }
                            }
                            dataStartTime = dataInTime;
                        }
                    }
                }
                catch (IOException ex) {
                    dataLoopException = ex;
                }
                synchronized (lock) {
                    if (state == State.RUNNING)
                        state = State.STOPPED;
                }
                if (dataLoopException == null) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                } else {
                    stop();
                }
            } // while
        } // try
        catch (InterruptedException ex) {
            state = State.INTERRUPTED;
        }
    }

    private void waitFifoFull() throws IOException {
        while (state == State.RUNNING && usb.controlRequest((byte)0x50, (byte)0x0) != 0x21) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                throw new IOException("interrupted", ex);
            }
        }
    }

    private Packet getFreePacket() throws IOException {
        if (freePool.size() > 0)
            return freePool.remove(0);
        if (busyPool.size() * SAMPLES_PER_BUFFER > MAX_MEMORY)
            throw new IOException("Not enough memory: possible UI threading issue or machine is too slow");
        return new Packet(ByteBuffer.allocateDirect(BUFFER_SIZE));
    }

}
