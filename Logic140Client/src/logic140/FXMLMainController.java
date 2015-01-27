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

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class FXMLMainController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Pane ch3Handle;

    @FXML
    private TextField ch0NameText;

    @FXML
    private ToggleButton goButton;

    @FXML
    private TextField ch1NameText;

    @FXML
    private ChoiceBox<String> freqChoice;

    @FXML
    private Pane ch6Handle;

    @FXML
    private TextField ch2NameText;

    @FXML
    private TextField timeValueText;

    @FXML
    private TableColumn<?, ?> spiMisoColumn;

    @FXML
    private TableColumn<?, ?> spiSsColumn;

    @FXML
    private ScrollBar timeScrollBar;

    @FXML
    private TextField ch4NameText;

    @FXML
    private Pane ch1Handle;

    @FXML
    private TextField ch3NameText;

    @FXML
    private TextField ch7NameText;

    @FXML
    private Pane ch2Handle;

    @FXML
    private ChoiceBox<String> ch7SpecialChoice;

    @FXML
    private TableColumn<?, ?> spiMosiColumn;

    @FXML
    private ChoiceBox<String> timeUnitsChoice;

    @FXML
    private TextField ch5NameText;

    @FXML
    private TableColumn<?, ?> spiTimeColumn;

    @FXML
    private Pane ch4Handle;

    @FXML
    private TextField ch6NameText;

    @FXML
    private Pane ch0Handle;

    @FXML
    private ChoiceBox<String> ch0SpecialChoice;

    @FXML
    private ChoiceBox<String> ch2SpecialChoice;

    @FXML
    private ChoiceBox<String> ch1SpecialChoice;

    @FXML
    private Pane ch7Handle;

    @FXML
    private ChoiceBox<String> ch5SpecialChoice;

    @FXML
    private ChoiceBox<String> ch6SpecialChoice;

    @FXML
    private ChoiceBox<String> ch3SpecialChoice;

    @FXML
    private Pane ch5Handle;

    @FXML
    private ChoiceBox<String> ch4SpecialChoice;

    @FXML
    private TableView spiTable;

    @FXML
    private AnchorPane ch0WavePane;
    
    @FXML
    private AnchorPane ch1WavePane;
    
    @FXML
    private AnchorPane ch2WavePane;
    
    @FXML
    private AnchorPane ch3WavePane;
    
    @FXML
    private AnchorPane ch4WavePane;
    
    @FXML
    private AnchorPane ch5WavePane;
    
    @FXML
    private AnchorPane ch6WavePane;
    
    @FXML
    private AnchorPane ch7WavePane;

    @FXML
    private AnchorPane rootPane;
    
    @FXML
    private Label capturedInfoLabel;

    @FXML
    private Label cursorInfoLabel;
    
    @FXML
    private Line crossHairLine;
    
    @FXML
    private SplitPane mainWindowSplitPane;
    
    @FXML
    private AnchorPane functionsPane;

    @FXML
    private TableView<Event> logTable;
    
    @FXML
    private TableColumn<Event, Number> logTimeColumn;
    
    @FXML
    private TableColumn<Event, String> logEventColumn;
    
    @FXML
    private Slider zoomSliderTimeMode;
    
    @FXML
    private Slider zoomSliderSampleMode;
    
    @FXML
    private CheckBox trimDataCheckBox;
    
    @FXML
    void handleTimeValueTextAction(ActionEvent event) {
        try {
            double value = Double.parseDouble(timeValueText.getText().replace(',', '.'));
            timeScrollBar.setValue(Main.timeToSamples(value, timeUnitsChoice.getSelectionModel().getSelectedIndex())/zoomFactor);
            timeValueText.setPromptText("");
        } catch (NumberFormatException ex) {
            timeValueText.setText("");
            timeValueText.setPromptText("illegal");
        }
    }

    @FXML
    void handleGoButtonAction(ActionEvent event) {
        if (goButton.isSelected()) {
            startImpl();
        } else {
            Main.stopAcquisition();
            setStoppedState();
        }
    }

    private Object lastMouseControl;
    
    @FXML
    void handleMouseExited(MouseEvent event) {
        if (lastMouseControl == event.getSource()) {
            lastMouseControl = null;
            mousePosInWindow = -1;
            cursorInfoLabel.setText("");
            crossHairLine.setVisible(false);
            waveZoomChangePivotSample = (int) timeScrollBar.getValue();
            waveZoomChangePivotX = 0;
        }
    }

    @FXML
    void handleMouseMoved(MouseEvent event) {
        lastMouseControl = event.getSource();
        updateMouseInfo((int) event.getX(), (int) event.getSceneX());
        waveZoomChangePivotX = (int)event.getX();
        waveZoomChangePivotSample = (int) ((waveZoomChangePivotX + waveWindowPosition) * zoomFactor);
//        redrawWaves();
    }

    @FXML
    void initialize() {
        assert ch3Handle != null : "fx:id=\"ch3Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch0NameText != null : "fx:id=\"ch0NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert goButton != null : "fx:id=\"goButton\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch1NameText != null : "fx:id=\"ch1NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert freqChoice != null : "fx:id=\"freqChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch6Handle != null : "fx:id=\"ch6Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch2NameText != null : "fx:id=\"ch2NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert timeValueText != null : "fx:id=\"timeValueText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert spiMisoColumn != null : "fx:id=\"spiMisoColumn\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert spiSsColumn != null : "fx:id=\"spiSsColumn\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert timeScrollBar != null : "fx:id=\"timeScrollBar\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch4NameText != null : "fx:id=\"ch4NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch1Handle != null : "fx:id=\"ch1Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch3NameText != null : "fx:id=\"ch3NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch7NameText != null : "fx:id=\"ch7NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch2Handle != null : "fx:id=\"ch2Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch7SpecialChoice != null : "fx:id=\"ch7SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert spiMosiColumn != null : "fx:id=\"spiMosiColumn\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert timeUnitsChoice != null : "fx:id=\"timeUnitsChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch5NameText != null : "fx:id=\"ch5NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert spiTimeColumn != null : "fx:id=\"spiTimeColumn\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch4Handle != null : "fx:id=\"ch4Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch6NameText != null : "fx:id=\"ch6NameText\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch0Handle != null : "fx:id=\"ch0Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch0SpecialChoice != null : "fx:id=\"ch0SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch2SpecialChoice != null : "fx:id=\"ch2SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch1SpecialChoice != null : "fx:id=\"ch1SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch7Handle != null : "fx:id=\"ch7Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch5SpecialChoice != null : "fx:id=\"ch5SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch6SpecialChoice != null : "fx:id=\"ch6SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch3SpecialChoice != null : "fx:id=\"ch3SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch5Handle != null : "fx:id=\"ch5Handle\" was not injected: check your FXML file 'FXMLMain.fxml'.";
        assert ch4SpecialChoice != null : "fx:id=\"ch4SpecialChoice\" was not injected: check your FXML file 'FXMLMain.fxml'.";

        initControls();
    }
    
    private Pane[] chHandles;
    private AnchorPane[] wavePanes;
    private ChoiceBox<String>[] special;
    private int mosiIndex = -1;
    private int misoIndex = -1;
    private int sclkIndex = -1;
    private final Canvas[] waves = new Canvas[8];
    private int waveWindowWidth;
    private int waveWindowPosition;
    private int timeUnits;
    private static final String[] timeUnitsText = new String[] { "samples", "ns", "us", "ms", "s" };
    private int mousePosInWindow;
    private Point2D waveWindowOrigin;
    private Point2D waveWindowExtent;
    private AnchorPane draggingWaveObject;
    private double draggingWaveXOrigin;
    private double draggingWaveTimeOrigin;
    /**
     * Number of samples per pixel.
     */
    private double zoomFactor;
    private double[] wavesX;
    private final double[][] wavesY = new double[8][];
    private final Main.DataIterator dataIterator = new Main.DataIterator();
    private int waveZoomChangePivotSample = -1;
    private int waveZoomChangePivotX = 0;
    
    private abstract class NamedProperty<P> {
        final String name;
        final P origin;
        
        NamedProperty(String name, P origin) {
            this.name = name;
            this.origin = origin;
        }
        
        abstract void load(Preferences prefs);
        abstract void save(Preferences prefs);
    }
    
    private class NamedTextProperty<P extends WritableValue<String>> extends NamedProperty<P> {
        NamedTextProperty(String name, P origin) {
            super(name, origin);
        }

        @Override
        void load(Preferences prefs) {
            origin.setValue(prefs.get(name, origin.getValue()));
        }

        @Override
        void save(Preferences prefs) {
            prefs.put(name, origin.getValue());
        }
    }
    
    private class NamedNumberProperty<P extends WritableValue<Number>> extends NamedProperty<P> {

        public NamedNumberProperty(String name, P origin) {
            super(name, origin);
        }

        @Override
        void load(Preferences prefs) {
            Number current = origin.getValue();
            if (current instanceof Double)
                origin.setValue(prefs.getDouble(name, current.doubleValue()));
            else if (current instanceof Float)
                origin.setValue(prefs.getFloat(name, current.floatValue()));
            else if (current instanceof Long)
                origin.setValue(prefs.getLong(name, current.longValue()));
            else if (current instanceof Integer || current instanceof Short || current instanceof Byte)
                origin.setValue(prefs.getInt(name, current.intValue()));
        }

        @Override
        void save(Preferences prefs) {
            Number current = origin.getValue();
            if (current instanceof Double)
                prefs.putDouble(name, current.doubleValue());
            else if (current instanceof Float)
                prefs.putFloat(name, current.floatValue());
            else if (current instanceof Long)
                prefs.putLong(name, current.longValue());
            else if (current instanceof Integer || current instanceof Short || current instanceof Byte)
                prefs.putInt(name, current.intValue());
        }
    }
    
    private class NamedBooleanProperty<P extends WritableValue<Boolean>> extends NamedProperty<P> {

        public NamedBooleanProperty(String name, P origin) {
            super(name, origin);
        }

        @Override
        void load(Preferences prefs) {
            origin.setValue(prefs.getBoolean(name, origin.getValue()));
        }

        @Override
        void save(Preferences prefs) {
            prefs.putBoolean(name, origin.getValue());
        }
    }
    
    private static NamedProperty[] settings; 
    
    private void initControls() {
        chHandles = new Pane[] { ch0Handle, ch1Handle, ch2Handle, ch3Handle, ch4Handle, ch5Handle, ch6Handle, ch7Handle };
        wavePanes = new AnchorPane[] { ch0WavePane, ch1WavePane, ch2WavePane, ch3WavePane, ch4WavePane, ch5WavePane, ch6WavePane, ch7WavePane };
        special = new ChoiceBox[] { ch0SpecialChoice, ch1SpecialChoice, ch2SpecialChoice, ch3SpecialChoice, ch4SpecialChoice, ch5SpecialChoice, ch6SpecialChoice, ch7SpecialChoice };
        settings = new NamedProperty[] {
            new NamedTextProperty("ch0NameText", ch0NameText.textProperty()),
            new NamedTextProperty("ch0SpecialChoice", ch0SpecialChoice.valueProperty()),
            new NamedTextProperty("ch1NameText", ch1NameText.textProperty()),
            new NamedTextProperty("ch1SpecialChoice", ch1SpecialChoice.valueProperty()),
            new NamedTextProperty("ch2NameText", ch2NameText.textProperty()),
            new NamedTextProperty("ch2SpecialChoice", ch2SpecialChoice.valueProperty()),
            new NamedTextProperty("ch3NameText", ch3NameText.textProperty()),
            new NamedTextProperty("ch3SpecialChoice", ch3SpecialChoice.valueProperty()),
            new NamedTextProperty("ch4NameText", ch4NameText.textProperty()),
            new NamedTextProperty("ch4SpecialChoice", ch4SpecialChoice.valueProperty()),
            new NamedTextProperty("ch5NameText", ch5NameText.textProperty()),
            new NamedTextProperty("ch5SpecialChoice", ch5SpecialChoice.valueProperty()),
            new NamedTextProperty("ch6NameText", ch6NameText.textProperty()),
            new NamedTextProperty("ch6SpecialChoice", ch6SpecialChoice.valueProperty()),
            new NamedTextProperty("ch7NameText", ch7NameText.textProperty()),
            new NamedTextProperty("ch7SpecialChoice", ch7SpecialChoice.valueProperty()),
            new NamedTextProperty("timeUnitsChoice", timeUnitsChoice.valueProperty()),
            new NamedNumberProperty("zoomSliderSampleMode", zoomSliderSampleMode.valueProperty()),
            new NamedNumberProperty("zoomSliderTimeMode", zoomSliderTimeMode.valueProperty()),
            new NamedBooleanProperty("trimDataCheckBox", trimDataCheckBox.selectedProperty()),
            new NamedTextProperty("freqChoice", freqChoice.valueProperty())
        };
        
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
                }
                if (indexToReset >= 0)
                    special[indexToReset].getSelectionModel().select(0);
            });
        }
        
        for (LogicAnalyzer.Frequency f: LogicAnalyzer.Frequency.values())
            freqChoice.getItems().addAll(f.getText());
        freqChoice.getSelectionModel().select(0);
        timeUnitsChoice.getItems().addAll(timeUnitsText);
        timeUnitsChoice.getSelectionModel().selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    timeUnits = newValue.intValue();
                    // work-around for FX bug not allowing to change label formatter for the slider
                    zoomSliderTimeMode.setVisible(timeUnits != 0);
                    zoomSliderSampleMode.setVisible(timeUnits == 0);
                    updateZoomFactorTimeMode((int)zoomSliderTimeMode.getValue());
                    updateZoomFactorSampleMode((int)zoomSliderSampleMode.getValue());
                });
        timeUnitsChoice.getSelectionModel().select(3);
        
        timeScrollBar.valueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> redrawWaves());
        
        SplitPane.setResizableWithParent(functionsPane, Boolean.FALSE);
        
        logTimeColumn.setCellValueFactory((CellDataFeatures<Event, Number> param) -> param.getValue().timeProperty);
        logEventColumn.setCellValueFactory((CellDataFeatures<Event, String> param) -> param.getValue().eventProperty);
        logTable.setItems(Main.events);
        
        zoomSliderTimeMode.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%d%s", (int)Math.pow(10, (object.intValue())%3), timeUnitsText[1+(object.intValue())/3]);
            }

            @Override
            public Double fromString(String string) {
                return 0.;
            }
        });
        zoomSliderTimeMode.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> 
                updateZoomFactorTimeMode(newValue.intValue()));
        zoomSliderSampleMode.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format(object.intValue() >=0 ? "%.0fx" : "%.2fx", Math.pow(10, object.intValue()));
            }

            @Override
            public Double fromString(String string) {
                return 0.;
            }
        });
        zoomSliderSampleMode.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                updateZoomFactorSampleMode(newValue.intValue()));
        
        Main.controller = this;
    }
    
    void postInitControls(Stage stage) {
        stage.setMinWidth(stage.getWidth());
        final double stageHeight = stage.getHeight();
        stage.setMinHeight(stageHeight);
        
        double waveWindowsHeightTotal = 0;
        for (int i = 0; i < 8; i++) {
            waves[i] = new Canvas(wavePanes[i].getBoundsInLocal().getWidth(), wavePanes[i].getBoundsInLocal().getHeight());
            wavePanes[i].getChildren().add(waves[i]);
            waves[i].relocate(0, 0);
            waves[i].widthProperty().bind(wavePanes[i].widthProperty().subtract(3));
            waves[i].heightProperty().bind(wavePanes[i].heightProperty().subtract(3));
            waves[i].widthProperty().addListener((Observable observable) -> updateWaveWindowBounds());
            waves[i].heightProperty().addListener((Observable observable) -> updateWaveWindowBounds());
            wavePanes[i].setMinWidth(wavePanes[i].getWidth());
            double wavePaneHeight = wavePanes[i].getHeight();
            wavePanes[i].setMinHeight(wavePaneHeight);
            waveWindowsHeightTotal += wavePaneHeight;
            wavePanes[i].setOnDragDetected((MouseEvent event) -> {
                if (draggingWaveObject != null || !(event.getSource() instanceof AnchorPane))
                    return;
                draggingWaveObject = (AnchorPane)event.getSource();
                draggingWaveXOrigin = (int)event.getSceneX();
                draggingWaveTimeOrigin = timeScrollBar.getValue();
                event.consume();
            });
            wavePanes[i].setOnMouseDragged((MouseEvent event) -> {
                if (draggingWaveObject != event.getSource()) 
                    return;
                double value = draggingWaveTimeOrigin-event.getSceneX()+draggingWaveXOrigin;
                final double max = timeScrollBar.getMax();
                if (value < 0)
                    value = 0;
                else if (value > max)
                    value = max;
                timeScrollBar.setValue(value);
                event.consume();
            });
            wavePanes[i].setOnMouseReleased((MouseEvent event) -> {
                if (draggingWaveObject == event.getSource())  {
                    double value = draggingWaveTimeOrigin-event.getSceneX()+draggingWaveXOrigin;
                    final double max = timeScrollBar.getMax();
                    if (value < 0)
                        value = 0;
                    else if (value > max)
                        value = max;
                    timeScrollBar.setValue(value);
                }
                draggingWaveObject = null;
                event.consume();
            });
            wavePanes[i].setOnScroll((ScrollEvent event) -> {
                if (event.getEventType() == ScrollEvent.SCROLL &&
                        event.getTextDeltaYUnits() == ScrollEvent.VerticalTextScrollUnits.LINES &&
                        event.getTextDeltaY() != 0) {
                    int inc = event.getTextDeltaY() > 0 ? -1 : 1;
                    Slider zoomSlider = zoomSliderSampleMode.isVisible() ? zoomSliderSampleMode : zoomSliderTimeMode;
                    double newValue = zoomSlider.getValue()+inc;
                    if (newValue >= zoomSlider.getMin() && newValue <= zoomSlider.getMax())
                        zoomSlider.setValue(newValue);
                }
            });
        }
        updateWaveWindowBounds();
        final double stageToWaveWindowOverhead = stageHeight - waveWindowsHeightTotal;
        stage.heightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            final double newWavePaneHeight = (newValue.doubleValue() - stageToWaveWindowOverhead) / 8;
            for (AnchorPane wavePane: wavePanes) {
                wavePane.setPrefHeight(newWavePaneHeight);
            }
        });
        zoomSliderSampleMode.setValue(1);
        zoomSliderTimeMode.setValue(6);
        
        loadPreferences();
    }

    void setStoppedState() {
        goButton.setSelected(false);
        goButton.setText("Go");
        capturedInfoLabel.setText(capturedInfoLabel.getText()+
                String.format(" (%d%% in)", (int)Main.percentCaptured));
    }

    void updateWaves(int totalNumSamples, int newPos) {
        final double numSamplePixels = totalNumSamples/zoomFactor;
        final double max = Math.max(0, numSamplePixels-waveWindowWidth);
        timeScrollBar.setMax(max);
        timeScrollBar.setVisibleAmount(waveWindowWidth*max/numSamplePixels);
        final double val = Math.max(0, Math.min(newPos/zoomFactor, max));
        final boolean forceRedraw = val == timeScrollBar.getValue();
        timeScrollBar.setValue(val);
        if (forceRedraw)
            redrawWaves();
    }
    
    void savePreferences() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(FXMLMainController.class);
            for (NamedProperty p: settings) {
                p.save(prefs);
            }
            prefs.flush();
        } catch (BackingStoreException ex) {
            Main.logger.log(Level.SEVERE, null, ex);
        }
    }
    
    void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(FXMLMainController.class);
        for (NamedProperty p: settings) {
            p.load(prefs);
        }
    }
    
    private void startImpl() {
        goButton.setText("Running, click to Stop");
        capturedInfoLabel.setText("last capture at "+freqChoice.getSelectionModel().getSelectedItem());
        LogicAnalyzer.Frequency frequency =
            LogicAnalyzer.Frequency.values()[freqChoice.getSelectionModel().getSelectedIndex()];
        
        if (mosiIndex >= 0 || misoIndex >= 0 || sclkIndex >= 0) {
            Main.enableSPI(mosiIndex, misoIndex, sclkIndex);
            spiTable.setDisable(false);
        } else {
            spiTable.setDisable(true);
        }

        Main.setTrim(trimDataCheckBox.isSelected());
        Main.startAcquisition(frequency);

        if (zoomSliderTimeMode.isVisible())
            updateZoomFactorTimeMode((int) zoomSliderTimeMode.getValue());
    }

    private void redrawWaves() {
        if (zoomFactor < 3./65536)
            return;
        waveWindowPosition = (int) timeScrollBar.getValue();
        final int firstSampleIndex = (int) (waveWindowPosition * zoomFactor);
        final int windowHeight = (int) ch0WavePane.getBoundsInLocal().getHeight();
        final int totalSamples = Main.totalNumSamples;
        final int y0 = windowHeight / 8;
        final int y1 = windowHeight - y0;
        if (totalSamples <= 0)
            return;
        Main.DataIterator d = dataIterator;
        if (d.init(firstSampleIndex)) {
            double x = 0;
            int xCur = 0;
            final double xInc = 1/zoomFactor;
            int xLast = -1;
            for (int i = 0; i < 8; i++) {
                GraphicsContext gc = waves[i].getGraphicsContext2D();
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, waveWindowWidth, windowHeight);
                gc.translate(0.5, 0.5);
                gc.setLineWidth(1);
                gc.setLineCap(StrokeLineCap.SQUARE);
                gc.setStroke(Color.rgb(0, 80, 40));
                gc.beginPath();
                final int gridStep = 100;
                for (int ix = 0; ix < waveWindowWidth; ix += gridStep) {
                    gc.moveTo(ix, 0);
                    gc.lineTo(ix, windowHeight);
                }
                gc.moveTo(0, 0);
                gc.lineTo(waveWindowWidth, 0);
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
                            for (int ix = xStart; ix < xLast && ix < waveWindowWidth; ix += dashPeriod) {
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
                    byte[] data = d.getData();
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
                        wavesX[0] = (int)x;
                        for (int i = 0; i < 8; i++)
                            wavesY[i][0] = (cur & (1 << i)) == 0 ? y0 : y1;
                        while (x <= waveWindowWidth && wavePoints < wavesX.length && --dataLeft > 0) {
                            xCur = (int)x;
                            byte next = data[++o];
                            if (next != cur && xCur > xLast) {
                                wavesX[wavePoints] = xCur;
                                wavesX[wavePoints+1] = xCur;
                                for (int i = 0; i < 8; i++) {
                                    int mask = 1 << i;
                                    wavesY[i][wavePoints] = (cur & mask) == 0 ? y0 : y1;
                                    wavesY[i][wavePoints+1] = (next & mask) == 0 ? y0 : y1;
                                }
                                xLast = xCur;
                                wavePoints += 2;
                                cur = next;
                            }
                            x += xInc;
                        }
                        d.skipData();
                        wavesX[wavePoints] = xCur;
                        for (int i = 0; i < 8; i++) {
                            wavesY[i][wavePoints] = wavesY[i][wavePoints-1];
                        }
                        wavePoints++;
                        for (int i = 0; i < 8; i++) {
                            GraphicsContext gc = waves[i].getGraphicsContext2D();
                            gc.strokePolyline(wavesX, wavesY[i], wavePoints);
                        }
                    }
                } // if else at lost data
            } while (x <= waveWindowWidth && d.hasMoreData());
            for (int i = 0; i < 8; i++) {
                GraphicsContext gc = waves[i].getGraphicsContext2D();
                gc.translate(-0.5, -0.5);
            }
        }        
        timeValueText.setText(Main.sampleToTime(firstSampleIndex, timeUnits));
        waveZoomChangePivotSample = (int) ((waveZoomChangePivotX + waveWindowPosition) * zoomFactor);
    }
    
    private void updateWaveWindowBounds() {
        waveWindowWidth = (int) ch0WavePane.getBoundsInLocal().getWidth();
        Transform t = ch0WavePane.getLocalToParentTransform();
        waveWindowOrigin = t.transform(0, 0);
        t = ch7WavePane.getLocalToSceneTransform();
        waveWindowExtent = t.transform(ch7WavePane.getWidth(), ch7WavePane.getHeight());
        crossHairLine.setVisible(false);
        crossHairLine.setStartY(waveWindowOrigin.getY());
        crossHairLine.setEndY(waveWindowExtent.getY());
        timeScrollBar.setBlockIncrement(waveWindowWidth);
        updateWaveArrays();
    }
    
    private void updateWaveArrays() {
        if (wavesX == null || wavesX.length < waveWindowWidth*3) {
            wavesX = new double[waveWindowWidth*3];
            for (int i = 0; i < 8; i++)
                wavesY[i] = new double[waveWindowWidth*3]; // max 3 points per sample or x coordinate
        }
        updateWaves(Main.totalNumSamples, (int) (waveZoomChangePivotSample-waveZoomChangePivotX*zoomFactor));
    }
    
    private void updateMouseInfo(int localX, int sceneX) {
        mousePosInWindow = localX;
        final int mousePos = localX + waveWindowPosition;
        final String posStr = Main.sampleToTime((int)(mousePos*zoomFactor), timeUnits);
        final String relPosStr = Main.sampleToTime((int)(localX*zoomFactor), timeUnits);
        if (posStr.length() > 0)
            cursorInfoLabel.setText(String.format("at %s %s (+%s %s)", 
                    posStr, 
                    timeUnitsText[timeUnits],
                    relPosStr,
                    timeUnitsText[timeUnits]));
        crossHairLine.setStartX(sceneX);
        crossHairLine.setEndX(sceneX);
        crossHairLine.setVisible(true);
    }
    
    private void updateZoomFactorTimeMode(int zoomSliderValue) {
        if (zoomSliderTimeMode.isVisible() && Main.frequency != null) {
            zoomFactor = Main.frequency.getFrequency()/Math.pow(10., 11-zoomSliderValue);
            updateWaveArrays();
        }
    }
    
    private void updateZoomFactorSampleMode(int zoomSliderValue) {
        if (zoomSliderSampleMode.isVisible()) {
            zoomFactor = Math.pow(10, zoomSliderValue);
            updateWaveArrays();
        }
    }
}
