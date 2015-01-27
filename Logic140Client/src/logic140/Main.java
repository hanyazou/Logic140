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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 *
 * @author Dragon
 */
public class Main extends Application {
    static FXMLMainController controller;
    static LogicAnalyzer la;
    static final Logger logger = Logger.getLogger("Logic140");
    static Thread acquisitionThread;
    static final Object acquisitionLock = new Object();
    static volatile boolean finish;
    static volatile boolean go;
    static int totalNumSamples;
    static LogicAnalyzer.Frequency frequency;
    private static boolean enableSPI;
    private static int mosi;
    private static int miso;
    private static int sclk;
    private static final List<Packet> data = new ArrayList<>();
    static final ObservableList<Event> events = FXCollections.observableList(new LinkedList<Event>());
    private static long startTime;
    private static long lastSampleTime;
    static double percentCaptured;
    private static int totalNumDataSamples;
    private static boolean trim;
    private static boolean lead;
    
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

    @Override
    public void start(final Stage stage) throws Exception {
        logger.addHandler(new FileHandler("error.log"));
        
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResource("FXMLMain.fxml"));
//        controller = (FXMLMainController)loader.getController(); // does not work for some reason
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        stage.setOnCloseRequest((WindowEvent event) -> finish());
        stage.setOnShown((WindowEvent event) -> controller.postInitControls(stage));
        stage.show();
        
        FXReScheduler.runAsync(() -> {
            try {
                la = new LogicAnalyzer();
                la.init();

                acquisitionThread = new Thread(() -> {
                    while (!finish) {
                        synchronized (acquisitionLock) {
                            try {
                                acquisitionLock.wait();
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                        if (go) {
                            try {
                                la.setFrequency(frequency);
                                la.go();
                                while (go) {
                                    LogicAnalyzer.Packet data1 = la.getData();
                                    if (data1 == null) {
                                        if (go)
                                            throw new NullPointerException("null data");
                                        continue;
                                    }
                                    processData(data1);
                                }
                            } catch (Exception ex) {
                                reset();
                                FXReScheduler.runOnFXThread(() -> {
                                    controller.setStoppedState();
                                    error(ex, false);
                                });
                            }
                        }
                    }
                }, "Acquisition");
                acquisitionThread.start();
            }
            catch (Exception ex) {
                FXReScheduler.runOnFXThread(() -> {
                    error(ex, true);
                });
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    private static void finish() {
        Label l = new Label("Cleaning up...");
        VBox p = new VBox();
        p.setAlignment(Pos.CENTER);
        p.getChildren().add(l);
        p.setMinWidth(200);
        p.setMinHeight(50);
        Stage t = new Stage(StageStyle.UTILITY);
        Scene s = new Scene(p);
        t.initModality(Modality.APPLICATION_MODAL);
        t.setScene(s);
        t.show();
        
        finish = true;
        FXReScheduler.runAsync(() -> {
            go = false;

            controller.savePreferences();
            
            if (la != null)
                la.finish();
            if (acquisitionThread != null) {
                synchronized (acquisitionLock) {
                    acquisitionLock.notify();
                }
                try {
                    acquisitionThread.join();
                } catch (InterruptedException ignored) {
                }
            }
            Platform.exit();
        });
    }
    
    static void error(Exception ex, boolean isCritical) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        
        Label errorMessageLabel = new Label(ex.getMessage());
        Button errorDismissButton = new Button("Dismiss");
        errorDismissButton.setDefaultButton(true);
        VBox errorPane = new VBox();
        errorDismissButton.setOnAction((ActionEvent event) -> {
            ((Stage)errorPane.getScene().getWindow()).close();
        });
        errorPane.setAlignment(Pos.CENTER);
        VBox.setMargin(errorMessageLabel, new Insets(5));
        VBox.setMargin(errorDismissButton, new Insets(5));
        errorPane.getChildren().add(errorMessageLabel);
        Label l2 = new Label("(see error.log for details)");
        VBox.setMargin(l2, new Insets(5));
        errorPane.getChildren().add(l2);
        errorPane.getChildren().add(errorDismissButton);
        errorPane.setStyle("-fx-background-color: rgba(0.9, 0.9, 0.9, 0.1);");
        errorPane.setMinHeight(200);
        errorPane.setMinWidth(600);
        popup(errorPane);
        if (isCritical)
            finish();
    }
    
    static void popup(Parent p) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(p);
        s.setScene(scene);
        s.showAndWait();
    }
    
    static void enableSPI(int mosi, int miso, int sclk) {
        enableSPI = true;
        Main.mosi = mosi;
        Main.miso = miso;
        Main.sclk = sclk;
    }
    
    static void setTrim(boolean trim) {
        Main.trim = trim;
    }

    static void startAcquisition(LogicAnalyzer.Frequency frequency) {
        if (go || la == null)
            return;
        Main.frequency = frequency;
        totalNumSamples = 0;
        totalNumDataSamples = 0;
        data.clear();
        events.clear();
        startTime = System.nanoTime();
        lead = true;
        synchronized (acquisitionLock) {
            go = true;
            acquisitionLock.notify();
        }
    }

    static void stopAcquisition() {
        if (!go || la == null)
            return;
        reset();
        synchronized (acquisitionLock) {
            la.stop();
        }
        percentCaptured = totalNumDataSamples * 1e11 / 
                (frequency.getFrequency()*(lastSampleTime-startTime));
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
                controller.updateWaves(totalNumSamples, totalNumSamples);
            }
        }
    }
    
    private static void reset() {
        go = false;
        enableSPI = false;
    }
    
    private static void processData(LogicAnalyzer.Packet packet) {
        lastSampleTime = System.nanoTime();
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
        la.releaseDataBuffer(packet);
        if (!lead || !trim || min != max) {
            synchronized (data) {
                data.add(new Packet(d, packetTrailingGap, min, max));
                totalNumSamples += buf.capacity() / 2 + packetTrailingGap;
                totalNumDataSamples += buf.capacity() / 2;
            }
            lead = false;
            Platform.runLater(() -> controller.updateWaves(totalNumSamples, totalNumSamples));
        }
    }

    static String sampleToTime(int pos, int unitsId) {
        if (frequency == null)
            return "";
        if (unitsId > 0) {
            double time = pos * Math.pow(1000, 4-unitsId) / frequency.getFrequency();
            return String.format("%.2f", time);
        } else
            return String.valueOf(pos);
    }

    static double timeToSamples(double value, int unitsId) {
        if (frequency == null)
            return 0.;
        if (unitsId > 0)
            return value * frequency.getFrequency() / Math.pow(1000, 4-unitsId);
        else
            return value;
    }
}
