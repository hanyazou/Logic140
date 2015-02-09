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
import java.io.IOException;
import java.util.LinkedList;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 *
 * @author Andrey Petushkov
 */
public class Main extends Application {
    static MainController controller;
    static DDS140 device;
    static final Logger logger = Logger.getLogger("Logic140");
    static Thread acquisitionThread;
    static final Object acquisitionLock = new Object();
    static volatile boolean finish;
    static volatile boolean go;
    private static boolean enableSPI;
    private static long startTime;
    private static long lastSampleTime;
    static Stage mainStage;
    private static FileChooser openFileChooser;
    private static FileChooser saveFileChooser;
    private static boolean isCaptureLogicAnalyzerMode;
    static Data oData = new Data();
    static Data laData = new Data();
    static final ObservableList<Event> laEvents = FXCollections.observableList(new LinkedList<Event>());
    static final ObservableList<Event> oEvents = FXCollections.observableList(new LinkedList<Event>());
    private static Data captureData;
    private static DDS140.Voltage ch1Voltage;
    private static DDS140.Voltage ch2Voltage;
    
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
        stage.setTitle("LA140");
        stage.show();
        mainStage = stage;

        openFileChooser = new FileChooser();
        openFileChooser.setTitle("Choose the data file");
        openFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Data file", "*.la140"));
        openFileChooser.setInitialDirectory(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile());
        
        saveFileChooser = new FileChooser();
        saveFileChooser.setTitle("Choose the data file");
        saveFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Data file", "*.la140"));
        saveFileChooser.setInitialDirectory(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile());
        
        FXReScheduler.runAsync(() -> {
            try {
                device = new DDS140();
                device.init();

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
                                device.setFrequency(captureData.getFrequency());
                                device.setVoltage(ch1Voltage, ch2Voltage);
                                device.setIsLogicAnalyzer(isCaptureLogicAnalyzerMode);
                                device.go();
                                while (go) {
                                    DDS140.Packet data1 = device.getData();
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
                    device = null;
                    controller.disableGo();
                    error(ex, false);
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
            
            if (device != null)
                device.finish();
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
        error(ex.getMessage(), isCritical);
    }
    
    static void error(String text, boolean isCritical) {
        Label errorMessageLabel = new Label(text);
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
    
    static void enableSPI(int mosi, int miso, int sclk, int ss, int mode) {
        enableSPI = true;
        SPI.setData(laData);
        SPI.init(sclk, mosi, miso, ss, (mode & 2) != 0, (mode & 1) != 0);
        parseSpi();
    }

    private static void parseSpi() {
        if (enableSPI && laData.totalNumDataSamples > 0)
            SPI.parse(new SPI.SPIEventHandler() {

            @Override
            public void spiError(int sample, String msg) {
                System.out.println("SPI parse error at "+sample+" "+msg);
            }

            @Override
            public void spiData(int sample, int mosi, int miso, int ss) {
            }
        });
    }
    
    static void disableSPI() {
        enableSPI = false;
        SPI.deinit();
    }
    
    static void setVoltage(DDS140.Voltage ch1Voltage, DDS140.Voltage ch2Voltage) {
        Main.ch1Voltage = ch1Voltage;
        Main.ch2Voltage = ch2Voltage;
    }
    
    static void startAcquisition(DDS140.Frequency frequency, boolean isLogicAnalyzerMode, boolean trim) {
        if (go || device == null)
            return;
        mainStage.setTitle("LA140 - acquired data");
        Main.isCaptureLogicAnalyzerMode = isLogicAnalyzerMode;
        captureData = isLogicAnalyzerMode ? laData : oData;
        captureData.startAcquisition(frequency, isLogicAnalyzerMode, trim);
        (isLogicAnalyzerMode ? laEvents : oEvents).clear();
        startTime = System.nanoTime();
        SPI.reset();
        synchronized (acquisitionLock) {
            go = true;
            acquisitionLock.notify();
        }
    }

    static void stopAcquisition() {
        if (!go || device == null)
            return;
        reset();
        synchronized (acquisitionLock) {
            device.stop();
        }
        captureData.percentCaptured = captureData.totalNumDataSamples * 1e11 / 
                (captureData.getFrequency().getFrequency()*(lastSampleTime-startTime));
        captureData.stopAcquisition();
        captureData = null;
    }

    static void addEvent(Event event) {
        (isCaptureLogicAnalyzerMode ? laEvents : oEvents).add(event);
    }
    
    private static void reset() {
        go = false;
        enableSPI = false;
    }
    
    private static void processData(DDS140.Packet packet) {
        if (packet == null)
            return;
        lastSampleTime = System.nanoTime();
        captureData.processData(packet);
        parseSpi();
    }

    static void loadData() {
        File dataFile = openFileChooser.showOpenDialog(mainStage);
        if (dataFile != null) {
            mainStage.setTitle("LA140 - " + dataFile.getName());
            try {
                SPI.reset();
                Data data = Data.load(dataFile);
                if (data.isLogicAnalyzerMode) {
                    laData = data;
                    if (enableSPI)
                        SPI.setData(laData);
                    parseSpi();
                } else {
                    oData = data;
                }
                controller.dataLoaded(data.isLogicAnalyzerMode);
            } catch (IOException ex) {
                error(ex, false);
            }
        }
    }
    
    static void saveData(Data data) {
        File dataFile = saveFileChooser.showSaveDialog(mainStage);
        if (dataFile != null) {
            mainStage.setTitle("LA140 - " + dataFile.getName());
            try {
                data.save(dataFile);
            }
            catch (IOException ex) {
                error(ex, false);
            }
        }
    }
}
