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
import static logic140.Data.percentCaptured;

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
    static LogicAnalyzer.Frequency frequency;
    private static boolean enableSPI;
    private static int mosi;
    private static int miso;
    private static int sclk;
    static final ObservableList<Event> events = FXCollections.observableList(new LinkedList<Event>());
    private static long startTime;
    private static long lastSampleTime;
    static boolean trim;
    static Stage mainStage;
    private static FileChooser openFileChooser;
    private static FileChooser saveFileChooser;
    
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
        mainStage.setTitle("LA140 - acquired data");
        Main.frequency = frequency;
        Data.startAcquisition(frequency);
        events.clear();
        startTime = System.nanoTime();
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
        percentCaptured = Data.totalNumDataSamples * 1e11 / 
                (frequency.getFrequency()*(lastSampleTime-startTime));
        Data.stopAcquisition(trim);
    }
    
    private static void reset() {
        go = false;
        enableSPI = false;
    }
    
    private static void processData(LogicAnalyzer.Packet packet) {
        lastSampleTime = System.nanoTime();
        Data.processData(packet);
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
    
    static void loadData() {
        File dataFile = openFileChooser.showOpenDialog(mainStage);
        if (dataFile != null) {
            mainStage.setTitle("LA140 - " + dataFile.getName());
            try {
                Data.load(dataFile);
                frequency = Data.getFrequency();
            } catch (IOException ex) {
                error(ex, false);
            }
        }
    }
    
    static void saveData() {
        File dataFile = saveFileChooser.showSaveDialog(mainStage);
        if (dataFile != null) {
            mainStage.setTitle("LA140 - " + dataFile.getName());
            try {
                Data.save(dataFile);
            }
            catch (IOException ex) {
                error(ex, false);
            }
        }
    }
}
