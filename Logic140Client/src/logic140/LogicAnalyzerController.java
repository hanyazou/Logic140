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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

/**
 *
 * @author Andrey Petushkov
 */
public class LogicAnalyzerController implements MainController.IController {

    private final MainController mController;
    private Pane[] chHandles;
    private AnchorPane[] wavePanes;
    private ChoiceBox<String>[] special;
    private int mosiIndex = -1;
    private int misoIndex = -1;
    private int sclkIndex = -1;
    private int ssIndex = -1;
    private final Canvas[] waves = new Canvas[8];
    private Data dataRef;
    private Data.DataIterator dataIterator;
    
    public LogicAnalyzerController(MainController mController) {
        this.mController = mController;
        dataRef = Main.laData;
        dataIterator = dataRef.new DataIterator();
    }
    
    void startImpl() {
        mController.goButton.setText("Running, click to S_top");
        mController.loadButton.setDisable(true);
        mController.saveButton.setDisable(false);
        mController.saveButton2.setDisable(true);
        mController.goButton2Enable.set(false);
        mController.loadButton2.setDisable(true);
        mController.capturedInfoLabel.setText("last capture at "+mController.freqChoice.getSelectionModel().getSelectedItem());
        DDS140.Frequency frequency =
            DDS140.Frequency.values()[mController.freqChoice.getSelectionModel().getSelectedIndex()];
        
        Main.startAcquisition(frequency, true, mController.trimDataCheckBox.isSelected());

        mController.updateZoomFactorTimeMode();
    }
    
    @Override
    public void setStoppedState() {
        mController.goButton.setSelected(false);
        mController.goButton.setText("_Go");
        mController.goButton2Enable.set(true);
        mController.loadButton2.setDisable(false);
        mController.capturedInfoLabel.setText(mController.capturedInfoLabel.getText()+
                String.format(" (%d%% in)", (int)dataRef.percentCaptured));
        mController.loadButton.setDisable(false);
    }

    @Override
    public void initControls() {
        chHandles = new Pane[] { mController.ch0Handle, mController.ch1Handle, mController.ch2Handle, mController.ch3Handle, mController.ch4Handle, mController.ch5Handle, mController.ch6Handle, mController.ch7Handle };
        wavePanes = new AnchorPane[] { mController.ch0WavePane, mController.ch1WavePane, mController.ch2WavePane, mController.ch3WavePane, mController.ch4WavePane, mController.ch5WavePane, mController.ch6WavePane, mController.ch7WavePane };
        special = new ChoiceBox[] { mController.ch0SpecialChoice, mController.ch1SpecialChoice, mController.ch2SpecialChoice, mController.ch3SpecialChoice, mController.ch4SpecialChoice, mController.ch5SpecialChoice, mController.ch6SpecialChoice, mController.ch7SpecialChoice };
        
        for (int i = 0; i < 8; i++) {
            ChoiceBox<String> item = special[i];
            final int index = i;
            item.getItems().addAll("----", "MOSI", "MISO", "SCLK", "!SS", "SDA", "SCL");
            item.getSelectionModel().select(0);
            item.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                if (index == mosiIndex)
                    mosiIndex = -1;
                else if (index == misoIndex)
                    misoIndex = -1;
                else if (index == sclkIndex)
                    sclkIndex = -1;
                else if (index == ssIndex)
                    ssIndex = -1;
                int indexToReset = -1;
                switch (newValue.intValue()) {
                    case 1:
                        indexToReset = mosiIndex;
                        mosiIndex = index;
                        break;
                    case 2:
                        indexToReset = misoIndex;
                        misoIndex = index;
                        break;
                    case 3:
                        indexToReset = sclkIndex;
                        sclkIndex = index;
                        break;
                    case 4:
                        indexToReset = ssIndex;
                        ssIndex = index;
                }
                if (indexToReset >= 0)
                    special[indexToReset].getSelectionModel().select(0);
                updateSpiState();
            });
        }
        
        mController.logTimeColumn.setCellValueFactory((TableColumn.CellDataFeatures<Event, Number> param) -> param.getValue().timeProperty);
        mController.logEventColumn.setCellValueFactory((TableColumn.CellDataFeatures<Event, String> param) -> param.getValue().eventProperty);
        mController.logTable.setItems(Main.laEvents);

        mController.spiModeChoiceBox.getItems().addAll("mode 0", "mode 1", "mode 2", "mode 3");
        mController.spiModeChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                String modeText = "";
                switch (newValue.intValue()) {
                    case 0:
                        modeText = "(CPOL=0 CPHA=0)";
                        break;
                    case 1:
                        modeText = "(CPOL=0 CPHA=1)";
                        break;
                    case 2:
                        modeText = "(CPOL=1 CPHA=0)";
                        break;
                    case 3:
                        modeText = "(CPOL=1 CPHA=1)";
                }
                mController.spiModeLabel.setText(modeText);
                updateSpiState();
            }
        });
        mController.spiModeChoiceBox.getSelectionModel().select(0);
    }

    private void updateSpiState() {
        if ((mosiIndex >= 0 || misoIndex >= 0) && sclkIndex >= 0) {
            Main.enableSPI(mosiIndex, misoIndex, sclkIndex, ssIndex, mController.spiModeChoiceBox.getSelectionModel().getSelectedIndex());
            mController.spiTable.setDisable(false);
        } else {
            Main.disableSPI();
            mController.spiTable.setDisable(true);
        }
    }

    @Override
    public void postInitControls(Stage stage) {
        double waveWindowsHeightTotal = 0;
        for (int i = 0; i < 8; i++) {
            waves[i] = new Canvas(wavePanes[i].getBoundsInLocal().getWidth(), wavePanes[i].getBoundsInLocal().getHeight());
            wavePanes[i].getChildren().add(waves[i]);
            waveWindowsHeightTotal += mController.initWaveControl(wavePanes[i], waves[i]);
        }
        final double stageToWaveWindowOverhead = stage.getHeight() - waveWindowsHeightTotal;
        stage.heightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            final double newWavePaneHeight = (newValue.doubleValue() - stageToWaveWindowOverhead) / 8;
            for (AnchorPane wavePane: wavePanes) {
                wavePane.setPrefHeight(newWavePaneHeight);
            }
        });
    }

    @Override
    public void updateWaveWindowBounds() {
        mController.waveWindowWidth = (int) mController.ch0WavePane.getBoundsInLocal().getWidth();
        Transform t = mController.ch0WavePane.getLocalToParentTransform();
        mController.waveWindowOrigin = t.transform(0, 0);
        t = mController.ch7WavePane.getLocalToSceneTransform();
        mController.waveWindowExtent = t.transform(mController.ch7WavePane.getWidth(), mController.ch7WavePane.getHeight());
        mController.crossHairLine.setVisible(false);
        mController.crossHairLine.setStartY(mController.waveWindowOrigin.getY());
        mController.crossHairLine.setEndY(mController.waveWindowExtent.getY());
        mController.timeScrollBar.setBlockIncrement(mController.waveWindowWidth);
        mController.updateWaveArrays();
    }
    
    @Override
    public void redrawWaves(int firstSampleIndex) {
        if (mController.zoomFactor < 3./65536)
            return;
        final int windowHeight = (int) mController.ch0WavePane.getBoundsInLocal().getHeight();
        final int totalSamples = dataRef.totalNumSamples;
        final int y1 = windowHeight / 8;
        final int y0 = windowHeight - y1;
        if (totalSamples <= 0)
            return;
        Data.DataIterator d = dataIterator;
        if (d.init(firstSampleIndex)) {
            double x = 0;
            int xCur = 0;
            final double xInc = 1/mController.zoomFactor;
            int xLast = -1;
            for (int i = 0; i < 8; i++) {
                GraphicsContext gc = waves[i].getGraphicsContext2D();
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, mController.waveWindowWidth, windowHeight);
                gc.translate(0.5, 0.5);
                gc.setLineWidth(1);
                gc.setLineCap(StrokeLineCap.SQUARE);
                gc.setStroke(Color.rgb(0, 80, 40));
                gc.beginPath();
                final int gridStep = 100;
                for (int ix = 0; ix < mController.waveWindowWidth; ix += gridStep) {
                    gc.moveTo(ix, 0);
                    gc.lineTo(ix, windowHeight);
                }
                gc.moveTo(0, 0);
                gc.lineTo(mController.waveWindowWidth, 0);
                gc.stroke();
                gc.setLineWidth(1);
                gc.setStroke(Color.YELLOW);
            }
            do {
                if (d.atLostData()) {
                    int lostLength = d.getLostDataLength();
                    int xStart = (int)(x + xInc * (d.getRemainingLostLength() - lostLength));
                    int lostWidth = (int)(xInc * lostLength);
                    xLast = xStart+lostWidth-1;
                    for (int i = 0; i < 8 ; i++) {
                        GraphicsContext gc = waves[i].getGraphicsContext2D();
                        gc.save();
                        gc.setStroke(Color.RED);
                        if (xLast - xStart <= 5) {
                            gc.strokeLine(xStart, (y0+y1)/2, xLast, (y0+y1)/2);
                        } else {
                            gc.beginPath();
                            gc.rect(xStart, y0, lostWidth, y1-y0);
                            gc.clip();
                            final int dashPeriod = 20;
                            for (int ix = xStart; ix < xLast && ix < mController.waveWindowWidth; ix += dashPeriod) {
                                if (ix + dashPeriod > 0) {
                                    gc.moveTo(ix, y1);
                                    gc.lineTo(ix + dashPeriod, y0);
                                }
                            }
                            gc.stroke();
                        }
                        gc.restore();
                    }
                    x = xLast; // fraction part lost, might be a problem
                    d.skipLostData();
                } else {
                    byte[] data = d.getData2();
                    int o = d.getDataStart();
                    int dataLeft = d.getRemainingDataLength();
                    if (dataLeft * xInc <= 5) {
                        int xStart = xLast;
                        x += dataLeft * xInc;
                        xLast = (int) x;
                        byte min = d.getPacketMin();
                        byte max = d.getPacketMax();
                        for (int i = 0; i < 8; i++) {
                            GraphicsContext gc = waves[i].getGraphicsContext2D();
                            int mask = 1 << i;
                            if ((min & mask) == 0) {
                                gc.strokeLine(xStart, y0, xLast, y0);
                            }
                            if ((max & mask) != 0) {
                                gc.strokeLine(xStart, y1, xLast, y1);
                            }
                        }
                    } else {
                        byte cur = data[o];
                        int wavePoints = 1;
                        final double[][] wavesY = mController.waves;
                        final double[] wavesX = mController.waves[0];
                        wavesX[0] = (int)x;
                        for (int i = 0; i < 8; i++)
                            wavesY[i+1][0] = (cur & (1 << i)) == 0 ? y0 : y1;
                        while (x < (mController.waveWindowWidth+xInc) && wavePoints < wavesX.length-2 && --dataLeft > 0) {
                            xCur = (int)x;
                            byte next = data[++o];
                            if (next != cur && xCur > xLast) {
                                wavesX[wavePoints] = xCur;
                                wavesX[wavePoints+1] = xCur;
                                for (int i = 0; i < 8; i++) {
                                    int mask = 1 << i;
                                    wavesY[i+1][wavePoints] = (cur & mask) == 0 ? y0 : y1;
                                    wavesY[i+1][wavePoints+1] = (next & mask) == 0 ? y0 : y1;
                                }
                                xLast = xCur;
                                wavePoints += 2;
                                cur = next;
                            }
                            x += xInc;
                        }
                        wavesX[wavePoints] = xCur;
                        for (int i = 0; i < 8; i++) {
                            wavesY[i+1][wavePoints] = wavesY[i+1][wavePoints-1];
                        }
                        wavePoints++;
                        for (int i = 0; i < 8; i++) {
                            GraphicsContext gc = waves[i].getGraphicsContext2D();
                            gc.strokePolyline(wavesX, wavesY[i+1], wavePoints);
                        }
                    }
                    d.skipData();
                } // if else at lost data
            } while (x <= mController.waveWindowWidth && d.hasMoreData());
            for (int i = 0; i < 8; i++) {
                GraphicsContext gc = waves[i].getGraphicsContext2D();
                gc.translate(-0.5, -0.5);
            }
        }        
        mController.timeValueText.setText(dataRef.sampleToTime(firstSampleIndex, mController.timeUnits));
    }

    @Override
    public void updateMouseInfo(int localX, int sceneX, int localY, int mousePos) {
        final String posStr = dataRef.sampleToTime((int)(mousePos*mController.zoomFactor), mController.timeUnits);
        final String relPosStr = dataRef.sampleToTime((int)(localX*mController.zoomFactor), mController.timeUnits);
        if (posStr.length() > 0)
            mController.cursorInfoLabel.setText(String.format("at %s %s (+%s %s)", 
                    posStr, 
                    MainController.timeUnitsText[mController.timeUnits],
                    relPosStr,
                    MainController.timeUnitsText[mController.timeUnits]));
        mController.crossHairLine.setStartX(sceneX);
        mController.crossHairLine.setEndX(sceneX);
        mController.crossHairLine.setVisible(true);
    }
    
    @Override
    public void dataLoaded(boolean modeActive) {
        dataRef = Main.laData;
        dataIterator = dataRef.new DataIterator();
        mController.capturedInfoLabel.setText("last capture at "+dataRef.getFrequency().getText());
    }
    
    @Override
    public Data getData() {
        return dataRef;
    }
}
