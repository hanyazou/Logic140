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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class MainController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
     Pane ch3Handle;

    @FXML
    TextField ch0NameText;

    @FXML
    ToggleButton goButton;

    @FXML
    TextField ch1NameText;

    @FXML
    ChoiceBox<String> freqChoice;

    @FXML
     Pane ch6Handle;

    @FXML
    TextField ch2NameText;

    @FXML
    TextField timeValueText;

    @FXML
    TableColumn<?, ?> spiMisoColumn;

    @FXML
    TableColumn<?, ?> spiSsColumn;

    @FXML
    ScrollBar timeScrollBar;

    @FXML
    TextField ch4NameText;

    @FXML
     Pane ch1Handle;

    @FXML
    TextField ch3NameText;

    @FXML
    TextField ch7NameText;

    @FXML
     Pane ch2Handle;

    @FXML
    ChoiceBox<String> ch7SpecialChoice;

    @FXML
    TableColumn<?, ?> spiMosiColumn;

    @FXML
    ChoiceBox<String> timeUnitsChoice;

    @FXML
    TextField ch5NameText;

    @FXML
    TableColumn<?, ?> spiTimeColumn;

    @FXML
    Pane ch4Handle;

    @FXML
    TextField ch6NameText;

    @FXML
     Pane ch0Handle;

    @FXML
    ChoiceBox<String> ch0SpecialChoice;

    @FXML
    ChoiceBox<String> ch2SpecialChoice;

    @FXML
    ChoiceBox<String> ch1SpecialChoice;

    @FXML
     Pane ch7Handle;

    @FXML
    ChoiceBox<String> ch5SpecialChoice;

    @FXML
    ChoiceBox<String> ch6SpecialChoice;

    @FXML
    ChoiceBox<String> ch3SpecialChoice;

    @FXML
     Pane ch5Handle;

    @FXML
    ChoiceBox<String> ch4SpecialChoice;

    @FXML
     TableView spiTable;

    @FXML
    AnchorPane ch0WavePane;
    
    @FXML
    AnchorPane ch1WavePane;
    
    @FXML
    AnchorPane ch2WavePane;
    
    @FXML
    AnchorPane ch3WavePane;
    
    @FXML
    AnchorPane ch4WavePane;
    
    @FXML
    AnchorPane ch5WavePane;
    
    @FXML
    AnchorPane ch6WavePane;
    
    @FXML
    AnchorPane ch7WavePane;

    @FXML
    AnchorPane rootPane;
    
    @FXML
    Label capturedInfoLabel;

    @FXML
    Label cursorInfoLabel;
    
    @FXML
    Line crossHairLine;
    
    @FXML
    SplitPane mainWindowSplitPane;
    
    @FXML
    AnchorPane functionsPane;

    @FXML
    TableView<Event> logTable;
    
    @FXML
    TableColumn<Event, Number> logTimeColumn;
    
    @FXML
    TableColumn<Event, String> logEventColumn;
    
    @FXML
    Slider zoomSliderTimeMode;
    
    @FXML
    Slider zoomSliderSampleMode;
    
    @FXML
    CheckBox trimDataCheckBox;
    
    @FXML
    Button saveButton;
    
    @FXML
    Button loadButton;
    
    @FXML
    ChoiceBox<String> spiModeChoiceBox;
    
    @FXML
    Label spiModeLabel;
    
    @FXML
    TabPane modeTabPane;
    
    @FXML
    Line crossHair2VLine;
    
    @FXML
    Line crossHair2HLine;
    
    @FXML
    ChoiceBox freqChoice2;
    
    @FXML
    ToggleButton goButton2;
    
    @FXML
    Button loadButton2;
    
    @FXML
    Button saveButton2;
    
    @FXML
    Label capturedInfoLabel2;
    
    @FXML
    Label cursorInfoLabel2;
    
    @FXML
    ChoiceBox ch1VoltChoiceBox;
    
    @FXML
    ChoiceBox ch2VoltChoiceBox;
    
    @FXML
    TableView<Event> logTable2;
    
    @FXML
    TableColumn<Event, Number> log2TimeColumn;
    
    @FXML
    TableColumn<Event, String> log2EventColumn;
    
    @FXML
    AnchorPane oWavePane;
    
    @FXML
    ToggleButton ch1EnableToggleButton;
    
    @FXML
    ToggleButton ch2EnableToggleButton;
    
    @FXML
    ChoiceBox<String> ch1AcDcChoiceBox;
    
    @FXML
    ChoiceBox<String> ch2AcDcChoiceBox;
    
    @FXML
    void handleTimeValueTextAction(ActionEvent event) {
        try {
            double value = Double.parseDouble(timeValueText.getText().replace(',', '.'));
            timeScrollBar.setValue(iController.getData().timeToSamples(value, timeUnitsChoice.getSelectionModel().getSelectedIndex())/zoomFactor);
            timeValueText.setPromptText("");
        } catch (NumberFormatException ex) {
            timeValueText.setText("");
            timeValueText.setPromptText("illegal");
        }
    }

    @FXML
    void handleGoButtonAction(ActionEvent event) {
        if (goButton.isSelected()) {
            laController.startImpl();
        } else {
            Main.stopAcquisition();
            laController.setStoppedState();
        }
    }
    
    @FXML
    void handleGoButton2Action(ActionEvent event) {
        if (goButton2.isSelected()) {
            oController.startImpl();
        } else {
            Main.stopAcquisition();
            oController.setStoppedState();
        }
    }

    Object lastMouseControl;
    
    @FXML
    void handleMouseExited(MouseEvent event) {
        if (lastMouseControl == event.getSource()) {
            lastMouseControl = null;
            mousePosInWindow = -1;
            cursorInfoLabel.setText("");
            cursorInfoLabel2.setText("");
            crossHairLine.setVisible(false);
            crossHair2VLine.setVisible(false);
            crossHair2HLine.setVisible(false);
            waveZoomChangePivotSample = (int) timeScrollBar.getValue();
            waveZoomChangePivotX = 0;
        }
    }

    @FXML
    void handleMouseMoved(MouseEvent event) {
        lastMouseControl = event.getSource();
        updateMouseInfo((int) event.getX(), (int) event.getSceneX(), (int) event.getY());
        waveZoomChangePivotX = (int)event.getX();
        waveZoomChangePivotSample = (int) ((waveZoomChangePivotX + waveWindowPosition) * zoomFactor);
//        redrawWaves();
    }

    @FXML
    void handleSaveButtonAction(ActionEvent event) {
        Main.saveData(Main.laData);
    }

    @FXML
    void handleSaveButton2Action(ActionEvent event) {
        Main.saveData(Main.oData);
    }

    @FXML
    void handleLoadButtonAction(ActionEvent event) {
        Main.loadData();
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

    interface IController {
        void initControls();
        
        void postInitControls(Stage stage);
        
        void setStoppedState();
        
        void updateWaveWindowBounds();
        
        void redrawWaves(int firstSampleIndex);
        
        void updateMouseInfo(int localX, int sceneX, int sceneY, int mousePos);
        
        void dataLoaded(boolean modeActive);
        
        Data getData();
    }
    
    int timeUnits;
    static final String[] timeUnitsText = new String[] { "samples", "ns", "us", "ms", "s" };
    private int mousePosInWindow;
    int waveWindowWidth;
    int waveWindowPosition;
    Point2D waveWindowOrigin;
    Point2D waveWindowExtent;
    AnchorPane draggingWaveObject;
    double draggingWaveXOrigin;
    double draggingWaveTimeOrigin;
    /**
     * Number of samples per pixel.
     */
    double zoomFactor;
    final double[][] waves = new double[9][];
    int waveZoomChangePivotSample = -1;
    int waveZoomChangePivotX = 0;
    final LogicAnalyzerController laController = new LogicAnalyzerController(this);
    final OscilloscopeController oController = new OscilloscopeController(this);
    private IController iController = laController;

    final BooleanProperty goButton2Enable = new BooleanPropertyBase() {

        @Override
        public Object getBean() {
            return this;
        }

        @Override
        public String getName() {
            return "goButtonEnable";
        }
    };
    
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
    
    private class NamedSelectionProperty<P extends SingleSelectionModel<?>> extends NamedProperty<P> {
        NamedSelectionProperty(String name, P origin) {
            super(name, origin);
        }

        @Override
        void load(Preferences prefs) {
            origin.select(prefs.getInt(name, origin.getSelectedIndex()));
        }

        @Override
        void save(Preferences prefs) {
            prefs.putInt(name, origin.getSelectedIndex());
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
            new NamedTextProperty("freqChoice", freqChoice.valueProperty()),
            new NamedTextProperty("freqChoice2", freqChoice2.valueProperty()),
            new NamedTextProperty("cpolChoiceBox", spiModeChoiceBox.valueProperty()),
            new NamedSelectionProperty("modeTabPane", modeTabPane.getSelectionModel()),
            new NamedTextProperty("ch1Voltage", ch1VoltChoiceBox.valueProperty()),
            new NamedTextProperty("ch2Voltage", ch2VoltChoiceBox.valueProperty()),
            new NamedTextProperty("ch1AcDc", ch1AcDcChoiceBox.valueProperty()),
            new NamedTextProperty("ch2AcDc", ch2AcDcChoiceBox.valueProperty()),
            new NamedBooleanProperty("ch1Enable", ch1EnableToggleButton.selectedProperty()),
            new NamedBooleanProperty("ch2Enable", ch2EnableToggleButton.selectedProperty())
        };
        
        for (DDS140.Frequency f: DDS140.Frequency.values()) {
            freqChoice.getItems().add(f.getText());
            freqChoice2.getItems().add(f.getText());
        }
        freqChoice.getSelectionModel().select(0);
        freqChoice2.getSelectionModel().select(0);
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
                return object.intValue() >=4 ? String.format("10^%dx", object.intValue()) :
                        String.format("%dx", (int)Math.pow(10, object.intValue()));
            }

            @Override
            public Double fromString(String string) {
                return 0.;
            }
        });
        zoomSliderSampleMode.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                updateZoomFactorSampleMode(newValue.intValue()));
        modeTabPane.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            iController = newValue.intValue() == 1 ? oController : laController;
            updateWaveWindowBounds();
        });

        laController.initControls();
        oController.initControls();
        
        Main.controller = this;
    }
    
    void postInitControls(Stage stage) {
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());

        updateWaveWindowBounds();
        laController.postInitControls(stage);
        oController.postInitControls(stage);
        zoomSliderSampleMode.setValue(3);
        zoomSliderTimeMode.setValue(6);
        
        loadPreferences();
    }
    
    double initWaveControl(AnchorPane pane, Canvas wave) {
        wave.relocate(0, 0);
        wave.widthProperty().bind(pane.widthProperty().subtract(3));
        wave.heightProperty().bind(pane.heightProperty().subtract(3));
        wave.widthProperty().addListener((Observable observable) -> updateWaveWindowBounds());
        wave.heightProperty().addListener((Observable observable) -> updateWaveWindowBounds());
        pane.setMinWidth(pane.getWidth());
        double wavePaneHeight = pane.getHeight();
        pane.setMinHeight(wavePaneHeight);
        pane.setOnDragDetected((MouseEvent event) -> {
            if (draggingWaveObject != null || !(event.getSource() instanceof AnchorPane))
                return;
            draggingWaveObject = (AnchorPane)event.getSource();
            draggingWaveXOrigin = (int)event.getSceneX();
            draggingWaveTimeOrigin = timeScrollBar.getValue();
            event.consume();
        });
        pane.setOnMouseDragged((MouseEvent event) -> {
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
        pane.setOnMouseReleased((MouseEvent event) -> {
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
        pane.setOnScroll((ScrollEvent event) -> {
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
        return wavePaneHeight;
    }

    void dataLoaded(boolean isLogicAnalyzerModeDataLoaded) {
        IController c = isLogicAnalyzerModeDataLoaded ? laController : oController;
        c.dataLoaded(c == iController);

        if (isLogicAnalyzerModeDataLoaded == (modeTabPane.getSelectionModel().getSelectedIndex() == 0)) {
            if (zoomSliderTimeMode.isVisible())
                updateZoomFactorTimeMode((int) zoomSliderTimeMode.getValue());

            final int totalNumSamples = iController.getData().totalNumSamples;
            updateWaves(totalNumSamples, totalNumSamples);
        }
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
            Preferences prefs = Preferences.userNodeForPackage(MainController.class);
            for (NamedProperty p: settings) {
                p.save(prefs);
            }
            prefs.flush();
        } catch (BackingStoreException ex) {
            Main.logger.log(Level.SEVERE, null, ex);
        }
    }
    
    void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        for (NamedProperty p: settings) {
            p.load(prefs);
        }
    }
    
    void disableGo() {
        goButton.setDisable(true);
        trimDataCheckBox.setDisable(true);
        freqChoice.setDisable(true);
    }
    
    private void updateMouseInfo(int localX, int sceneX, int localY) {
        mousePosInWindow = localX;
        iController.updateMouseInfo(localX, sceneX, localY, localX + waveWindowPosition);
    }
    
    private void updateZoomFactorTimeMode(int zoomSliderValue) {
        if (zoomSliderTimeMode.isVisible() && iController.getData().getFrequency() != null) {
            zoomFactor = iController.getData().getFrequency().getFrequency()/Math.pow(10., 11-zoomSliderValue);
            updateWaveArrays();
        }
    }
    
    void updateZoomFactorTimeMode() {
        if (zoomSliderTimeMode.isVisible())
            updateZoomFactorTimeMode((int) zoomSliderTimeMode.getValue());
    }
    
    void updateZoomFactorSampleMode(int zoomSliderValue) {
        if (zoomSliderSampleMode.isVisible()) {
            zoomFactor = Math.pow(10., zoomSliderValue-2);
            updateWaveArrays();
        }
    }
    
    private void updateWaveWindowBounds() {
        iController.updateWaveWindowBounds();
    }
    
    void updateWaveArrays() {
        if (waves[0] == null || waves[0].length < waveWindowWidth*3) {
            for (int i = 0; i < waves.length; i++)
                waves[i] = new double[waveWindowWidth*3+1]; // max 3 points per sample or x coordinate plus initial point
        }
        updateWaves(iController.getData().totalNumSamples, (int) (waveZoomChangePivotSample-waveZoomChangePivotX*zoomFactor));
    }
    
    void redrawWaves() {
        waveWindowPosition = (int) timeScrollBar.getValue();
        final int firstSampleIndex = (int) (waveWindowPosition * zoomFactor);
        iController.redrawWaves(firstSampleIndex);
        waveZoomChangePivotSample = (int) ((waveZoomChangePivotX + waveWindowPosition) * zoomFactor);
    }
    
    void setStoppedState() {
        iController.setStoppedState();
    }
}
