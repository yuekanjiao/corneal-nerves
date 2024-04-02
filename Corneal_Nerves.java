/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Yuekan Jiao
 */
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.ParticleAnalyzer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.swing.UIManager;

public class Corneal_Nerves implements PlugIn, ActionListener {

    int delta = 30;

    ImagePlus imp;
    String title;
    int width;
    int height;
    int nChannels;
    int nSlices;
    int nFrames;

    ImageStack stack;

    Dimension screen;

    Frame layerFrame;
    TextField stromalField;
    Button stromalButton;
    TextField subbasalField;
    Button subbasalButton;
    TextField ietField;
    Button ietButton;

    int stromalStart;
    int subbasalStart;
    int ietStart;

    ImageStack projectedStack;
    ImagePlus impProjected;
    String[] layerArray = {"Stromal 2", "Stromal 3",
        "Subbasal 2", "Subbasal 3",
        "IET 2", "IET 3"};  // channels 2 and 3 each has 3 layers

    Panel threshPanel;
    int[] threshArray;
    Frame threshFrame;
    TextField stromal2Field;
    TextField subbasal2Field;
    TextField iet2Field;
    TextField stromal3Field;
    TextField subbasal3Field;
    TextField iet3Field;

    Button stromal2UpButton;
    Button stromal2DownButton;
    Button stromal3UpButton;
    Button stromal3DownButton;
    Button subbasal2UpButton;
    Button subbasal2DownButton;
    Button subbasal3UpButton;
    Button subbasal3DownButton;
    Button iet2UpButton;
    Button iet2DownButton;
    Button iet3UpButton;
    Button iet3DownButton;
    Button editButton;
    Button updateButton;
    Button resetButton;

    ImagePlus impMask;
    ImagePlus impColocal;
    ImagePlus impInterception;

    Frame gridFrame;
    int gridSizeX;
    int gridSizeY;
    int gridWidth;
    TextField gridXField;
    TextField gridYField;
    TextField gridWidthField;
    Button gridXUpButton;
    Button gridXDownButton;
    Button gridYUpButton;
    Button gridYDownButton;
    Button gridWidthUpButton;
    Button gridWidthDownButton;

    ImageProcessor ipGrid;
    int[] InterceptionArray;
    double[] volumeArray;

    MyKeyListener myKeyListener;

    public void run(String arg) {
        imp = IJ.getImage();

        title = imp.getTitle();

        screen = Toolkit.getDefaultToolkit().getScreenSize();

        width = imp.getWidth();
        height = imp.getHeight();
        nChannels = imp.getNChannels();
        nSlices = imp.getNSlices();
        nFrames = imp.getNFrames();

        zoomExact(imp, 0.5);
        Window window = imp.getWindow();
        window.setLocation(0, 0);

        stack = imp.getStack();
        projectedStack = new ImageStack();

        stromalStart = 1;
        subbasalStart = (int) (nSlices / 3.0) + 1;
        ietStart = (int) (nSlices / 3.0 * 2.0) + 1;

        myKeyListener = new MyKeyListener();
        IJ.log("==================================================");
        IJ.log(title);

        showLayerFrame();

        threshArray = new int[6]; // channels 2 and 3 each has 3 layers
        volumeArray = new double[6]; // channels 2 and 3 each has 3 layers
        gridSizeX = 20;
        gridSizeY = 20;
        gridWidth = 1;
        InterceptionArray = new int[6]; // channels 2 and 3 each has 3 layers
    }

    public void showLayerFrame() {

        layerFrame = new Frame("Corneal nerves - layer projection");
        layerFrame.setLayout(new BorderLayout());
        layerFrame.setSize(screen.width / 4, screen.height / 4);

        Panel reversePanel = new Panel(new GridLayout(1, 1));
        reversePanel.setBackground(UIManager.getColor("Panel.background"));

        //reversePanel.add(new Label());
        Button reverseButton = new Button("Reverse Stack Z");
        reverseButton.addActionListener(this);
        reversePanel.add(reverseButton);
        //reversePanel.add(new Label());
        layerFrame.add(reversePanel, "North");

        Panel layerPanel = new Panel(new GridLayout(4, 3));
        layerPanel.setBackground(UIManager.getColor("Panel.background"));
        
        layerPanel.add(new Label("Start slice:"));
        layerPanel.add(new Label());
        layerPanel.add(new Label());
        
        layerPanel.add(new Label("Stromal "));
        stromalField = new TextField(Integer.toString(stromalStart));
        stromalButton = new Button("Sync Stromal");
        stromalButton.addActionListener(this);
        layerPanel.add(stromalField);
        layerPanel.add(stromalButton);

        layerPanel.add(new Label("Subbasal"));
        subbasalField = new TextField(Integer.toString(subbasalStart));
        subbasalButton = new Button("Sync Subbasal");
        subbasalButton.addActionListener(this);
        layerPanel.add(subbasalField);
        layerPanel.add(subbasalButton);

        layerPanel.add(new Label("IET"));
        ietField = new TextField(Integer.toString(ietStart));
        ietButton = new Button("Sync IET");
        ietButton.addActionListener(this);
        layerPanel.add(ietField);
        layerPanel.add(ietButton);

        Panel panel = new Panel(new BorderLayout());
        Button projectButton = new Button("Project");
        projectButton.addActionListener(this);
        Panel okCancelPanel = new Panel(new GridLayout(3, 3));
        okCancelPanel.setBackground(UIManager.getColor("Panel.background"));
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        Button okButton = new Button("OK");
        okButton.addActionListener(this);
        okCancelPanel.add(okButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        panel.add(projectButton, "North");
        panel.add(okCancelPanel, "Center");

        layerFrame.add(layerPanel, "Center");
        layerFrame.add(panel, "South");
        layerFrame.setVisible(true);
        Rectangle rect = layerFrame.getBounds();
        layerFrame.setLocation((screen.width - 1) - rect.width, screen.height / 6 - 1);

        layerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                layerFrame.dispose();
            }
        });
    }

    public void showThreshFrame() {

        threshFrame = new Frame("Corneal nerves - threshholding");
        threshFrame.setSize(screen.width / 4, screen.height / 4);
        threshFrame.setLayout(new BorderLayout());

        Panel panel = new Panel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));

        threshPanel = new Panel(new GridLayout(3, 2));
        threshPanel.setBackground(UIManager.getColor("Panel.background"));

        Panel stromal2Panel = new Panel(new BorderLayout());
        Panel stromal2Panel1 = new Panel(new GridLayout(1, 2));
        stromal2Panel1.add(new Label(layerArray[0]));
        stromal2Field = new TextField(Integer.toString(threshArray[0]));
        stromal2Field.addKeyListener(myKeyListener);
        stromal2Panel1.add(stromal2Field);
        stromal2Panel.add(stromal2Panel1, "Center");
        Panel stromal2Panel2 = new Panel(new GridLayout(2, 1));
        stromal2UpButton = new Button("/\\");
        stromal2DownButton = new Button("\\/");
        stromal2UpButton.addActionListener(this);
        stromal2DownButton.addActionListener(this);
        stromal2Panel2.add(stromal2UpButton);
        stromal2Panel2.add(stromal2DownButton);
        stromal2Panel.add(stromal2Panel2, "East");
        threshPanel.add(stromal2Panel);

        Panel stromal3Panel = new Panel(new BorderLayout());
        Panel stromal3Panel1 = new Panel(new GridLayout(1, 2));
        stromal3Panel1.add(new Label(layerArray[1]));
        stromal3Field = new TextField(Integer.toString(threshArray[1]));
        stromal3Field.addKeyListener(myKeyListener);
        stromal3Panel1.add(stromal3Field);
        stromal3Panel.add(stromal3Panel1, "Center");
        Panel stromal3Panel2 = new Panel(new GridLayout(2, 1));
        stromal3UpButton = new Button("/\\");
        stromal3DownButton = new Button("\\/");
        stromal3UpButton.addActionListener(this);
        stromal3DownButton.addActionListener(this);
        stromal3Panel2.add(stromal3UpButton);
        stromal3Panel2.add(stromal3DownButton);
        stromal3Panel.add(stromal3Panel2, "East");
        threshPanel.add(stromal3Panel);
        if (nChannels < 3) {
            stromal3Panel.setVisible(false);
            stromal3Panel.setEnabled(false);
        }

        Panel subbasal2Panel = new Panel(new BorderLayout());
        Panel subbasal2Panel1 = new Panel(new GridLayout(1, 2));
        subbasal2Panel1.add(new Label(layerArray[2]));
        subbasal2Field = new TextField(Integer.toString(threshArray[2]));
        subbasal2Field.addKeyListener(myKeyListener);
        subbasal2Panel1.add(subbasal2Field);
        subbasal2Panel.add(subbasal2Panel1, "Center");
        Panel subbasal2Panel2 = new Panel(new GridLayout(2, 1));
        subbasal2UpButton = new Button("/\\");
        subbasal2DownButton = new Button("\\/");
        subbasal2UpButton.addActionListener(this);
        subbasal2DownButton.addActionListener(this);
        subbasal2Panel2.add(subbasal2UpButton);
        subbasal2Panel2.add(subbasal2DownButton);
        subbasal2Panel.add(subbasal2Panel2, "East");
        threshPanel.add(subbasal2Panel);

        Panel subbasal3Panel = new Panel(new BorderLayout());
        Panel subbasal3Panel1 = new Panel(new GridLayout(1, 2));
        subbasal3Panel1.add(new Label(layerArray[3]));
        subbasal3Field = new TextField(Integer.toString(threshArray[3]));
        subbasal3Field.addKeyListener(myKeyListener);
        subbasal3Panel1.add(subbasal3Field);
        subbasal3Panel.add(subbasal3Panel1, "Center");
        Panel subbasal3Panel2 = new Panel(new GridLayout(2, 1));
        subbasal3UpButton = new Button("/\\");
        subbasal3DownButton = new Button("\\/");
        subbasal3UpButton.addActionListener(this);
        subbasal3DownButton.addActionListener(this);
        subbasal3Panel2.add(subbasal3UpButton);
        subbasal3Panel2.add(subbasal3DownButton);
        subbasal3Panel.add(subbasal3Panel2, "East");
        threshPanel.add(subbasal3Panel);
        if (nChannels < 3) {
            subbasal3Panel.setVisible(false);
            subbasal3Panel.setEnabled(false);
        }

        Panel iet2Panel = new Panel(new BorderLayout());
        Panel iet2Panel1 = new Panel(new GridLayout(1, 2));
        iet2Panel1.add(new Label(layerArray[4]));
        iet2Field = new TextField(Integer.toString(threshArray[4]));
        iet2Field.addKeyListener(myKeyListener);
        iet2Panel1.add(iet2Field);
        iet2Panel.add(iet2Panel1, "Center");
        Panel iet2Panel2 = new Panel(new GridLayout(2, 1));
        iet2UpButton = new Button("/\\");
        iet2DownButton = new Button("\\/");
        iet2UpButton.addActionListener(this);
        iet2DownButton.addActionListener(this);
        iet2Panel2.add(iet2UpButton);
        iet2Panel2.add(iet2DownButton);
        iet2Panel.add(iet2Panel2, "East");
        threshPanel.add(iet2Panel);

        Panel iet3Panel = new Panel(new BorderLayout());
        Panel iet3Panel1 = new Panel(new GridLayout(1, 2));
        iet3Panel1.add(new Label(layerArray[5]));
        iet3Field = new TextField(Integer.toString(threshArray[5]));
        iet3Field.addKeyListener(myKeyListener);
        iet3Panel1.add(iet3Field);
        iet3Panel.add(iet3Panel1, "Center");
        Panel iet3Panel2 = new Panel(new GridLayout(2, 1));
        iet3UpButton = new Button("/\\");
        iet3DownButton = new Button("\\/");
        iet3UpButton.addActionListener(this);
        iet3DownButton.addActionListener(this);
        iet3Panel2.add(iet3UpButton);
        iet3Panel2.add(iet3DownButton);
        iet3Panel.add(iet3Panel2, "East");
        threshPanel.add(iet3Panel);
        if (nChannels < 3) {
            iet3Panel.setVisible(false);
            iet3Panel.setEnabled(false);
        }
        panel.add(threshPanel, "Center");

        Panel editPanel = new Panel(new GridLayout(1, 3));
        editPanel.setBackground(UIManager.getColor("Panel.background"));
        editButton = new Button("Edit");
        editButton.addActionListener(this);
        updateButton = new Button("Update");
        updateButton.addActionListener(this);
        resetButton = new Button("Reset");
        resetButton.addActionListener(this);
        updateButton.setEnabled(false);
        updateButton.setVisible(false);
        resetButton.setEnabled(false);
        resetButton.setVisible(false);
        editPanel.add(editButton);
        editPanel.add(updateButton);
        editPanel.add(resetButton);
        panel.add(editPanel, "South");

        threshFrame.add(panel, "Center");

        Panel okCancelPanel = new Panel(new GridLayout(3, 3));
        okCancelPanel.setBackground(UIManager.getColor("Panel.background"));
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        Button okButton = new Button("OK");
        okButton.addActionListener(this);
        okCancelPanel.add(okButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        threshFrame.add(okCancelPanel, "South");

        threshFrame.setVisible(true);
        Rectangle rect = threshFrame.getBounds();
        threshFrame.setLocation((screen.width - 1) - rect.width, screen.height / 6 - 1);

        threshFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                threshFrame.dispose();
            }
        });
    }

    public void showGridFrame() {

        gridFrame = new Frame("Corneal nerves - griding");
        gridFrame.setSize(screen.width / 4, screen.height / 3);
        gridFrame.setLayout(new BorderLayout());

        Panel gridPanel = new Panel(new GridLayout(3, 2));
        gridPanel.setBackground(UIManager.getColor("Panel.background"));

        gridPanel.add(new Label("Grid size X"));
        Panel gridXPanel = new Panel(new BorderLayout());
        gridXField = new TextField(Integer.toString(gridSizeX));
        gridXField.addKeyListener(myKeyListener);
        gridXPanel.add(gridXField, "Center");
        Panel gridXPanel1 = new Panel(new GridLayout(2, 1));
        gridXUpButton = new Button("/\\");
        gridXUpButton.addActionListener(this);
        gridXDownButton = new Button("\\/");
        gridXDownButton.addActionListener(this);
        gridXPanel1.add(gridXUpButton);
        gridXPanel1.add(gridXDownButton);
        gridXPanel.add(gridXPanel1, "East");
        gridPanel.add(gridXPanel);

        gridPanel.add(new Label("Grid size Y"));
        Panel gridYPanel = new Panel(new BorderLayout());
        gridYField = new TextField(Integer.toString(gridSizeY));
        gridYField.addKeyListener(myKeyListener);
        gridYPanel.add(gridYField, "Center");
        Panel gridYPanel1 = new Panel(new GridLayout(2, 1));
        gridYUpButton = new Button("/\\");
        gridYUpButton.addActionListener(this);
        gridYDownButton = new Button("\\/");
        gridYDownButton.addActionListener(this);
        gridYPanel1.add(gridYUpButton);
        gridYPanel1.add(gridYDownButton);
        gridYPanel.add(gridYPanel1, "East");
        gridPanel.add(gridYPanel);

        gridPanel.add(new Label("Grid width"));
        Panel gridWidthPanel = new Panel(new BorderLayout());
        gridWidthField = new TextField(Integer.toString(gridWidth));
        gridWidthField.addKeyListener(myKeyListener);
        gridWidthPanel.add(gridWidthField, "Center");
        Panel gridWidthPanel1 = new Panel(new GridLayout(2, 1));
        gridWidthUpButton = new Button("/\\");
        gridWidthUpButton.addActionListener(this);
        gridWidthDownButton = new Button("\\/");
        gridWidthDownButton.addActionListener(this);
        gridWidthPanel1.add(gridWidthUpButton);
        gridWidthPanel1.add(gridWidthDownButton);
        gridWidthPanel.add(gridWidthPanel1, "East");
        gridPanel.add(gridWidthPanel);

        gridFrame.add(gridPanel, "Center");

        Panel okCancelPanel = new Panel(new GridLayout(3, 3));
        okCancelPanel.setBackground(UIManager.getColor("Panel.background"));
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        Button okButton = new Button("OK");
        okButton.addActionListener(this);
        okCancelPanel.add(okButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        okCancelPanel.add(new Label());
        gridFrame.add(okCancelPanel, "South");

        gridFrame.setVisible(true);
        Rectangle rect = gridFrame.getBounds();
        gridFrame.setLocation((screen.width - 1) - rect.width, screen.height / 6 - 1);

        gridFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                gridFrame.dispose();
            }
        });
    }

    public void reverseStackZ() {

        ImageProcessor ipSlice;
        ImageStack reversedStack = new ImageStack();
        for (int k = nSlices; k > 0; k--) {
            for (int ch = 1; ch < (nChannels + 1); ch++) {
                ipSlice = stack.getProcessor((k - 1) * nChannels + ch).duplicate();
                reversedStack.addSlice(ipSlice);
            }
        }

        double mag = imp.getCanvas().getMagnification();
        imp.setStack(reversedStack, nChannels, nSlices, nFrames);
        zoomExact(imp, mag);
        stack = imp.getStack();
    }

    public void getProjectionStack() {

        projectedStack = new ImageStack();
        ImageProcessor ipSlice;
        ImageProcessor ipSliceProjected;
        ImageProcessor ipSliceProjected8;

        int pixelValue;
        int nSlicesProjected = 0;

        for (int ch = 2; ch < (nChannels + 1); ch++) {
            // use a duplicate otherwise the slice will be modified
            ipSliceProjected = stack.getProcessor(nChannels * (stromalStart - 1) + ch).duplicate();
            for (int k = stromalStart + 1; k < subbasalStart; k++) {
                ipSlice = stack.getProcessor((k - 1) * nChannels + ch);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        pixelValue = ipSlice.getPixel(i, j);
                        if (pixelValue > ipSliceProjected.getPixel(i, j)) {
                            ipSliceProjected.putPixel(i, j, pixelValue);
                        }
                    }
                }
            }
            ipSliceProjected8 = new ByteProcessor(width, height);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    ipSliceProjected8.set(i, j, (int) (ipSliceProjected.getPixel(i, j) / (4095.0 / 255.0)));
                }
            }
            projectedStack.addSlice(ipSliceProjected8);
            nSlicesProjected++;
            projectedStack.setSliceLabel("Stromal " + ch, nSlicesProjected);
        }
        for (int ch = 2; ch < (nChannels + 1); ch++) {
            ipSliceProjected = stack.getProcessor(nChannels * (subbasalStart - 1) + ch).duplicate();
            for (int k = subbasalStart + 1; k < ietStart; k++) {
                ipSlice = stack.getProcessor((k - 1) * nChannels + ch);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        pixelValue = ipSlice.getPixel(i, j);
                        if (pixelValue > ipSliceProjected.getPixel(i, j)) {
                            ipSliceProjected.putPixel(i, j, pixelValue);
                        }
                    }
                }
            }
            ipSliceProjected8 = new ByteProcessor(width, height);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    ipSliceProjected8.set(i, j, (int) (ipSliceProjected.getPixel(i, j) / (4095.0 / 255.0)));
                }
            }
            projectedStack.addSlice(ipSliceProjected8);
            nSlicesProjected++;
            projectedStack.setSliceLabel("Subbasal " + ch, nSlicesProjected);
        }
        for (int ch = 2; ch < (nChannels + 1); ch++) {
            ipSliceProjected = stack.getProcessor(nChannels * (ietStart - 1) + ch).duplicate();
            for (int k = ietStart + 1; k < (nSlices + 1); k++) {
                ipSlice = stack.getProcessor((k - 1) * nChannels + ch);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        pixelValue = ipSlice.getPixel(i, j);
                        if (pixelValue > ipSliceProjected.getPixel(i, j)) {
                            ipSliceProjected.putPixel(i, j, pixelValue);
                        }
                    }
                }
            }
            ipSliceProjected8 = new ByteProcessor(width, height);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    ipSliceProjected8.set(i, j, (int) (ipSliceProjected.getPixel(i, j) / (4095.0 / 255.0)));
                }
            }
            projectedStack.addSlice(ipSliceProjected8);
            nSlicesProjected++;
            projectedStack.setSliceLabel("IET " + ch, nSlicesProjected);
        }
    }

    public void getProjection() {
        getProjectionStack();
        impProjected = IJ.createImage("Projection", "8-bit", width, height, nChannels - 1, 3, nFrames);
        impProjected.setStack(projectedStack);
        impProjected.show();
        for (int k = projectedStack.size() - 1; k > -1; k--) {
            impProjected.setPosition(k + 1);
            IJ.run(impProjected, "Red", "");
        }
        zoomExact(impProjected, 0.5);
        Window window = impProjected.getWindow();
        window.setLocation(delta * 1, delta * 1);
    }

    public void updateProjection() {
        getProjectionStack();
        if (!impProjected.isVisible()) {
            impProjected = IJ.createImage("Projection", "8-bit", width, height, nChannels - 1, 3, nFrames);
            impProjected.setStack(projectedStack);
            impProjected.show();
            for (int k = projectedStack.size() - 1; k > -1; k--) {
                impProjected.setPosition(k + 1);
                IJ.run(impProjected, "Red", "");
            }
            zoomExact(impProjected, 0.5);
            Window window = impProjected.getWindow();
            window.setLocation(delta * 1, delta * 1);
        } else {
            double mag = impProjected.getCanvas().getMagnification();
            impProjected.setStack(projectedStack, nChannels - 1, 3, nFrames);
            for (int k = projectedStack.size() - 1; k > -1; k--) {
                impProjected.setPosition(k + 1);
                IJ.run(impProjected, "Red", "");
            }
            zoomExact(impProjected, mag);
        }
    }

    public void getThresholding() {
        ImageProcessor ipSlice;
        ImageStack maskStack = new ImageStack();
        ByteProcessor ipMask;

        int stackSize = projectedStack.getSize();
        for (int k = 0; k < stackSize; k++) {
            ipSlice = projectedStack.getProcessor(k + 1);
            ipMask = new ByteProcessor(width, height);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    if (ipSlice.get(i, j) > threshArray[6 / stackSize * k]) {
                        ipMask.set(i, j, 255);
                    }
                }
            }
            maskStack.addSlice(ipMask);
            maskStack.setSliceLabel(layerArray[k], k + 1);
        }
        impMask = IJ.createImage("Mask", "8-bit", width, height,
                impProjected.getNChannels(), impProjected.getNSlices(), impProjected.getNFrames());
        impMask.setStack(maskStack);
        for (int k = maskStack.size() - 1; k > -1; k--) {
            impMask.setPosition(k + 1);
            IJ.run(impMask, "Grays", "");
        }
        impMask.show();
        zoomExact(impProjected, 0.5);
        Window window = impProjected.getWindow();
        window.setLocation(delta * 1, delta * 1);
        Rectangle rect = window.getBounds();
        zoomExact(impMask, 0.5);
        window = impMask.getWindow();
        window.setLocation(rect.x + rect.width, rect.y);
    }

    public Boolean ResetThresholding() {

        if (!impProjected.isVisible()) {
            IJ.showMessage("No Projection", "There is no projection.");
            threshFrame.dispose();
            return false;
        }
        threshArray[0] = Integer.parseInt(stromal2Field.getText());
        threshArray[1] = Integer.parseInt(stromal3Field.getText());
        threshArray[2] = Integer.parseInt(subbasal2Field.getText());
        threshArray[3] = Integer.parseInt(subbasal3Field.getText());
        threshArray[4] = Integer.parseInt(iet2Field.getText());
        threshArray[5] = Integer.parseInt(iet3Field.getText());

        ImageProcessor ipSlice;
        ImageStack maskStack;
        ImageProcessor ipMask;
        int stackSize;
        int slice;
        if (impMask.isVisible()) {
            maskStack = impMask.getStack();
            stackSize = maskStack.getSize();
            for (int k = 0; k < projectedStack.size(); k++) {
                ipSlice = projectedStack.getProcessor(k + 1);
                ipMask = maskStack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        if (ipSlice.get(i, j) > threshArray[6 / stackSize * k]) {
                            ipMask.set(i, j, 255);
                        } else {
                            ipMask.set(i, j, 0);
                        }
                    }
                }
            }
            impMask.updateAndDraw();

        } else { // In case the mask image is closed
            stackSize = impProjected.getNChannels() * 3;
            maskStack = new ImageStack();
            for (int k = 0; k < projectedStack.size(); k++) {
                ipSlice = projectedStack.getProcessor(k + 1);
                ipMask = new ByteProcessor(width, height);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        if (ipSlice.get(i, j) > threshArray[6 / stackSize * k]) {
                            ipMask.set(i, j, 255);
                        }
                    }
                }
                maskStack.addSlice(ipMask);
                maskStack.setSliceLabel(layerArray[k], k + 1);
            }

            slice = impProjected.getCurrentSlice();
            impMask = IJ.createImage("Mask", "8-bit", width, height,
                    impProjected.getNChannels(), impProjected.getNSlices(), impProjected.getNFrames());
            impMask.setStack(maskStack);
            for (int k = maskStack.size() - 1; k > -1; k--) {
                impMask.setPosition(k + 1);
                IJ.run(impMask, "Grays", "");
            }
            impMask.setPosition(slice);
            impMask.show();

            Window window = impProjected.getWindow();
            Rectangle rect = window.getBounds();
            window = impMask.getWindow();
            zoomExact(impMask, 0.5);
            window.setLocation(rect.x + rect.width, rect.y);
        }
        return true;
    }

    public Boolean updateThresholding() {

        if (!impProjected.isVisible()) {
            IJ.showMessage("No Projection", "There is no projection.");
            threshFrame.dispose();
            return false;
        }

        int[] threshArray0 = new int[6];
        for (int k = 0; k < 6; k++) {
            threshArray0[k] = threshArray[k];
        }
        threshArray[0] = Integer.parseInt(stromal2Field.getText());
        threshArray[1] = Integer.parseInt(stromal3Field.getText());
        threshArray[2] = Integer.parseInt(subbasal2Field.getText());
        threshArray[3] = Integer.parseInt(subbasal3Field.getText());
        threshArray[4] = Integer.parseInt(iet2Field.getText());
        threshArray[5] = Integer.parseInt(iet3Field.getText());

        ImageProcessor ipSlice;
        ImageStack maskStack;
        ImageProcessor ipMask;
        int stackSize;
        int slice;
        if (impMask.isVisible()) {
            maskStack = impMask.getStack();
            stackSize = maskStack.getSize();

            // Handling threshold adjust - up and down buttons and Enter key   
            for (int k = 0; k < projectedStack.size(); k++) {
                if (threshArray[6 / stackSize * k] != threshArray0[6 / stackSize * k]) {
                    ipSlice = projectedStack.getProcessor(k + 1);
                    ipMask = maskStack.getProcessor(k + 1);
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            if (ipSlice.get(i, j) > threshArray[6 / stackSize * k]) {
                                ipMask.set(i, j, 255);
                            } else {
                                ipMask.set(i, j, 0);
                            }
                        }
                    }
                }
            }
            impMask.updateAndDraw();

        } else { // In case the mask image is closed
            stackSize = impProjected.getNChannels() * 3;
            maskStack = new ImageStack();
            for (int k = 0; k < projectedStack.size(); k++) {
                ipSlice = projectedStack.getProcessor(k + 1);
                ipMask = new ByteProcessor(width, height);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        if (ipSlice.get(i, j) > threshArray[6 / stackSize * k]) {
                            ipMask.set(i, j, 255);
                        }
                    }
                }
                maskStack.addSlice(ipMask);
                maskStack.setSliceLabel(layerArray[k], k + 1);
            }

            slice = impProjected.getCurrentSlice();
            impMask = IJ.createImage("Mask", "8-bit", width, height,
                    impProjected.getNChannels(), impProjected.getNSlices(), impProjected.getNFrames());
            impMask.setStack(maskStack);
            for (int k = maskStack.size() - 1; k > -1; k--) {
                impMask.setPosition(k + 1);
                IJ.run(impMask, "Grays", "");
            }
            impMask.setPosition(slice);
            impMask.show();

            Window window = impProjected.getWindow();
            Rectangle rect = window.getBounds();
            window = impMask.getWindow();
            zoomExact(impMask, 0.5);
            window.setLocation(rect.x + rect.width, rect.y);
        }
        return true;
    }

    public void getColocal() {
        ImageStack maskStack = impMask.getImageStack();
        ImageStack colocalStack = new ImageStack();
        ImageProcessor ipSlice2, ipSlice3;
        ImageProcessor ipColocal;
        int numChannels = impMask.getNChannels();
        for (int z = 0; z < impMask.getNSlices(); z++) {
            ipColocal = new ByteProcessor(width, height);
            ipSlice2 = maskStack.getProcessor(z * numChannels + 1);
            ipSlice3 = maskStack.getProcessor(z * numChannels + 2);
            for (int j = 1; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    if ((ipSlice2.get(i, j) > 0) && (ipSlice3.get(i, j) > 0)) {
                        ipColocal.set(i, j, 255);
                    } else {
                        ipColocal.set(i, j, 0);
                    }
                }
            }
            colocalStack.addSlice(ipColocal);
        }
        colocalStack.setSliceLabel("Stromal", 1);
        colocalStack.setSliceLabel("Subbasal", 2);
        colocalStack.setSliceLabel("IET", 3);
        impColocal = new ImagePlus("Colocalization", colocalStack);
        impColocal.show();
    }

    public void getInterception() {

        ImageStack maskStack = impMask.getStack();
        ImageStack interceptionStack = new ImageStack();
        ImageProcessor ipMask;
        ImageProcessor ipInterception0;
        ImagePlus impInterception0;
        ImageProcessor ipInterception;
        ParticleAnalyzer pa;
        ResultsTable rt = new ResultsTable();
        Overlay overlay;
        Roi[] rois;
        Roi roi;
        Rectangle rect;

        int stackSize = maskStack.size();
        for (int k = 0; k < stackSize; k++) {
            ipInterception0 = new ByteProcessor(width, height);
            ipMask = maskStack.getProcessor(k + 1);
            for (int j = 1; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    if ((ipGrid.get(i, j) > 0) && (ipMask.get(i, j) > 0)) {
                        ipInterception0.set(i, j, 255);
                    } else {
                        ipInterception0.set(i, j, 0);
                    }
                }
            }

            impInterception0 = new ImagePlus(layerArray[k], ipInterception0);
            ipInterception0.setThreshold(255, 255);
            pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                    | ParticleAnalyzer.INCLUDE_HOLES
                    | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                    Measurements.AREA,
                    rt, 1, Long.MAX_VALUE, 0, 1);
            pa.analyze(impInterception0);
            overlay = impInterception0.getOverlay();

            rois = null;
            if (overlay != null) {
                rois = overlay.toArray();
            }
            ipInterception = new ByteProcessor(width, height);
            ipInterception.setColor(Color.white);
            if (rois != null) {
                for (int index = 0; index < rois.length; index++) {
                    roi = rois[index];
                    rect = roi.getBounds();
                    if ((!(rect.width < gridWidth)) && (!(rect.height < gridWidth))) {
                        ipInterception.setRoi(roi);
                        ipInterception.fill(roi);
                    }
                }
            }
            interceptionStack.addSlice(ipInterception);
            interceptionStack.setSliceLabel(layerArray[6 / stackSize * k], k + 1);
        }

        impInterception = IJ.createImage("Interception", "8-bit", width, height,
                impMask.getNChannels(), impMask.getNSlices(), impMask.getNFrames());
        impInterception.setStack(interceptionStack);

        for (int k = interceptionStack.size() - 1; k > -1; k--) {
            impInterception.setPosition(k + 1);
            IJ.run(impInterception, "Grays", "");
        }
        int slice = impMask.getCurrentSlice();
        impInterception.setPosition(slice);
        impInterception.show();

    }

    public void updateInterception() {

        ImageStack maskStack = impMask.getStack();
        ImageStack interceptionStack;
        ImageProcessor ipMask;
        ImageProcessor ipInterception0;
        ImagePlus impInterception0;
        ImageProcessor ipInterception;
        ParticleAnalyzer pa;
        ResultsTable rt = new ResultsTable();
        Overlay overlay;
        Roi[] rois;
        Roi roi;
        Rectangle rect;

        int stackSize = maskStack.size();
        if (impInterception.isVisible()) {
            for (int k = 0; k < stackSize; k++) {
                ipInterception0 = new ByteProcessor(width, height);
                ipMask = maskStack.getProcessor(k + 1);
                for (int j = 1; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        if ((ipGrid.get(i, j) > 0) && (ipMask.get(i, j) > 0)) {
                            ipInterception0.set(i, j, 255);
                        } else {
                            ipInterception0.set(i, j, 0);
                        }
                    }
                }
                impInterception0 = new ImagePlus(layerArray[k], ipInterception0);
                ipInterception0.setThreshold(255, 255);
                pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                        | ParticleAnalyzer.INCLUDE_HOLES
                        | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                        Measurements.AREA,
                        rt, 1, Long.MAX_VALUE, 0, 1);
                pa.analyze(impInterception0);
                overlay = impInterception0.getOverlay();
                rois = null;
                if (overlay != null) {
                    rois = overlay.toArray();
                }

                interceptionStack = impInterception.getStack();
                ipInterception = interceptionStack.getProcessor(k + 1);
                ipInterception.setColor(Color.black);
                ipInterception.fill();

                ipInterception.setColor(Color.white);
                if (rois != null) {
                    for (int index = 0; index < rois.length; index++) {
                        roi = rois[index];
                        rect = roi.getBounds();
                        if ((!(rect.width < gridWidth)) && (!(rect.height < gridWidth))) {
                            ipInterception.setRoi(roi);
                            ipInterception.fill(roi);
                        }
                    }
                }
            }
            impInterception.updateAndDraw();

        } else {
            interceptionStack = new ImageStack();
            for (int k = 0; k < stackSize; k++) {
                ipInterception0 = new ByteProcessor(width, height);
                ipMask = maskStack.getProcessor(k + 1);
                for (int j = 1; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        if ((ipGrid.get(i, j) > 0) && (ipMask.get(i, j) > 0)) {
                            ipInterception0.set(i, j, 255);
                        } else {
                            ipInterception0.set(i, j, 0);
                        }
                    }
                }

                impInterception0 = new ImagePlus(layerArray[k], ipInterception0);
                ipInterception0.setThreshold(255, 255);
                pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                        | ParticleAnalyzer.INCLUDE_HOLES
                        | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                        Measurements.AREA,
                        rt, 1, Long.MAX_VALUE, 0, 1);
                pa.analyze(impInterception0);
                overlay = impInterception0.getOverlay();
                rois = null;
                if (overlay != null) {
                    rois = overlay.toArray();
                }
                ipInterception = new ByteProcessor(width, height);
                ipInterception.setColor(Color.white);
                if (rois != null) {
                    for (int index = 0; index < rois.length; index++) {
                        roi = rois[index];
                        rect = roi.getBounds();
                        if ((!(rect.width < gridWidth)) && (!(rect.height < gridWidth))) {
                            ipInterception.setRoi(roi);
                            ipInterception.fill(roi);
                        }
                    }
                }
                interceptionStack.addSlice(ipInterception);
                interceptionStack.setSliceLabel(layerArray[6 / stackSize * k], k + 1);
            }

            int slice = impMask.getCurrentSlice();
            impInterception = IJ.createImage("Interception", "8-bit", width, height,
                    impMask.getNChannels(), impMask.getNSlices(), impMask.getNFrames());
            impInterception.setStack(interceptionStack);

            impInterception.setStack(interceptionStack);
            for (int k = interceptionStack.size() - 1; k > -1; k--) {
                impInterception.setPosition(k + 1);
                IJ.run(impInterception, "Grays", "");
            }
            impInterception.setPosition(slice);
            impInterception.show();

            zoomExact(impMask, 0.5);
            Window window = impMask.getWindow();
            window.setLocation(delta * 3, delta * 3);
            window.toFront();
            rect = window.getBounds();
            zoomExact(impInterception, 0.5);
            window = impInterception.getWindow();
            window.setLocation(rect.x + rect.width, rect.y);
            window.toFront();
        }

    }

    public void showStart() {
        IJ.log("==================================================");
        IJ.log(String.format("%-15s\t%15s\t%15s\t%15s\t",
                "Layer",
                "Stromal",
                "Subbasal",
                "IET"));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "Start",
                stromalStart,
                subbasalStart,
                ietStart));
    }

    public void showThresholding() {
        ImageStack maskStack = impMask.getImageStack();
        int stackSize = impMask.getStackSize();
        ImageProcessor ipSlice;
        for (int z = 1; z < (stackSize + 1); z++) {
            ipSlice = maskStack.getProcessor(z);
            volumeArray[6 / stackSize * (z - 1)] = 0;
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    volumeArray[6 / stackSize * (z - 1)] = volumeArray[6 / stackSize * (z - 1)] + ipSlice.get(i, j);
                }
            }
            volumeArray[6 / stackSize * (z - 1)] = volumeArray[6 / stackSize * (z - 1)] / 255 / (width * height) * 100;
        }
        IJ.log("==================================================");
        IJ.log(String.format("%-15s\t%15s\t%15s\t%15s\t",
                "Layer",
                "Stromal",
                "Subbasal",
                "IET"));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "Threshold 2",
                threshArray[0],
                threshArray[2],
                threshArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                    "Threshold 3",
                    threshArray[1],
                    threshArray[3],
                    threshArray[5]));
        }
        IJ.log(String.format("%-15s\t%15f\t%15f\t%15f\t",
                "Volume (%) 2",
                volumeArray[0],
                volumeArray[2],
                volumeArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15f\t%15f\t%15f\t",
                    "Volume (%) 3",
                    volumeArray[1],
                    volumeArray[3],
                    volumeArray[5]));
        }
    }

    public void showGriding() {

        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa;
        int slice = impInterception.getCurrentSlice();
        ImageStack interceptionStack = impInterception.getStack();

        ImageProcessor ipSlice;
        ImagePlus impSlice;
        String sliceLabel;
        int stackSize = impInterception.getStackSize();
        for (int k = 0; k < stackSize; k++) {
            impInterception.setPosition(k + 1);
            rt.reset();
            ipSlice = interceptionStack.getProcessor(k + 1);
            sliceLabel = interceptionStack.getSliceLabel(k + 1);
            impSlice = new ImagePlus(sliceLabel, ipSlice);
            ipSlice.setThreshold(255, 255);
            pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                    | ParticleAnalyzer.INCLUDE_HOLES,
                    Measurements.CENTROID,
                    rt, 0, Long.MAX_VALUE, 0, 1);
            pa.analyze(impSlice);
            InterceptionArray[6 / stackSize * k] = rt.size(); // stackSize could be 3
        }
        impInterception.setPosition(slice);

        IJ.log("==================================================");
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid size X", gridSizeX));
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid size Y", gridSizeY));
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid width", gridWidth));
        IJ.log(String.format("%-15s\t%15s\t%15s\t%15s\t",
                "Layer",
                "Stromal",
                "Subbasal",
                "IET"));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "interception (n) 2",
                InterceptionArray[0],
                InterceptionArray[2],
                InterceptionArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                    "interception (n) 3",
                    InterceptionArray[1],
                    InterceptionArray[3],
                    InterceptionArray[5]));
        }
    }

    public Boolean updateGriding() {
        if (!impMask.isVisible()) {
            IJ.showMessage("No Mask", "There is no mask.");
            gridFrame.dispose();
            return false;
        }
        gridSizeX = Integer.parseInt(gridXField.getText());
        gridSizeY = Integer.parseInt(gridYField.getText());
        gridWidth = Integer.parseInt(gridWidthField.getText());

        drawGrid();
        updateInterception();
        return true;
    }

    public void drawGrid() {

        Overlay overlay = new Overlay();
        double gridPitchY = (double) height / gridSizeY;
        double gridPitchX = (double) width / gridSizeX;
        ipGrid = new ByteProcessor(width, height);
        ipGrid.setColor(Color.white);
        Roi roi;
        for (int j = 0; j < gridSizeY; j++) {
            for (int t = 0; t < gridWidth; t++) {
                roi = new Line(0, (int) (j * gridPitchY) + t, width - 1, (int) (j * gridPitchY) + t);
                roi.setStrokeWidth(1);
                roi.setStrokeColor(Color.red);
                overlay.add(roi);
                ipGrid.setRoi(roi);
                ipGrid.fill(roi);
            }
        }
        for (int i = 0; i < gridSizeX; i++) {
            for (int t = 0; t < gridWidth; t++) {
                roi = new Line((int) (i * gridPitchX) + t, 0, (int) (i * gridPitchX) + t, height - 1);
                roi.setStrokeWidth(1);
                roi.setStrokeColor(Color.red);
                overlay.add(roi);
                ipGrid.setRoi(roi);
                ipGrid.fill(roi);
            }
        }
        impMask.setOverlay(overlay);
        impMask.updateAndDraw();
    }

    public void showResult() {
        IJ.log("==================================================");
        IJ.log(title);
        IJ.log(String.format("%-15s\t%15s\t%15s\t%15s\t",
                "Layer",
                "Stromal",
                "Subbasal",
                "IET"));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "Start",
                stromalStart,
                subbasalStart,
                ietStart));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "Threshold 2",
                threshArray[0],
                threshArray[2],
                threshArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                    "Threshold 3",
                    threshArray[1],
                    threshArray[3],
                    threshArray[5]));
        }
        IJ.log(String.format("%-15s\t%15f\t%15f\t%15f\t",
                "Volume (%) 2",
                volumeArray[0],
                volumeArray[2],
                volumeArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15f\t%15f\t%15f\t",
                    "Volume (%) 3",
                    volumeArray[1],
                    volumeArray[3],
                    volumeArray[5]));
        }
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid size X", gridSizeX));
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid size Y", gridSizeY));
        IJ.log(String.format("%-15s\t%15d\t",
                "Grid width", gridWidth));
        IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                "Interception (n) 2",
                InterceptionArray[0],
                InterceptionArray[2],
                InterceptionArray[4]));
        if (nChannels > 2) {
            IJ.log(String.format("%-15s\t%15d\t%15d\t%15d\t",
                    "Interception (n) 3",
                    InterceptionArray[1],
                    InterceptionArray[3],
                    InterceptionArray[5]));
        }
    }

    public void zoomExact(ImagePlus img, double mag) {
        ImageWindow win = img.getWindow();
        if (win == null) {
            return;
        }
        ImageCanvas c = win.getCanvas();
        if (c == null) {
            return;
        }
        c.setMagnification(mag);
        // see if it fits
        double w = img.getWidth() * mag;
        double h = img.getHeight() * mag;
        if (w > screen.width - 10) {
            w = screen.width - 10;
        }
        if (h > screen.height - 30) {
            h = screen.height - 30;
        }
        try {
            Field f_srcRect = c.getClass().getDeclaredField("srcRect");
            f_srcRect.setAccessible(true);
            f_srcRect.set(c, new Rectangle(0, 0, (int) (w / mag), (int) (h / mag)));
            c.setDrawingSize((int) w, (int) h);
            win.pack();
            c.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getThreshArray() {
        // get the threshold for each slice in the projected stack
        int[] hist = new int[256];
        ImageProcessor ipSlice;
        int stackSize = projectedStack.size();
        for (int k = 1; k < (stackSize + 1); k++) {
            Arrays.fill(hist, 0);
            ipSlice = projectedStack.getProcessor(k);
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {
                    hist[ipSlice.get(i, j)]++;
                }
            }
            hist = smoothHist(hist);
            // threshArray is for 2 channels and 3 layers
            // in case of 1 channel, the inflection values are assignened to
            // threshArray[0], threshArray[2], threshArray[4],
            threshArray[6 / stackSize * (k - 1)] = getInflection(hist);
        }
    }

    public int getInflection(int[] histArray) {
        int length = histArray.length;
        int[] histArray1 = new int[length];
        int[] histArray2 = new int[length];

        for (int index = 1; index < (length - 1); index++) {
            histArray1[index] = histArray[index + 1] - histArray[index - 1];
        }
        for (int index = 2; index < (length - 2); index++) {
            histArray2[index] = histArray1[index + 1] - histArray1[index - 1];
        }
        int indexMax = 0;
        int max = 0;
        for (int index = 0; index < length; index++) {
            if (histArray[index] > max) {
                indexMax = index;
                max = histArray[index];
            }
        }

        int indexInfl1 = indexMax;
        if (indexInfl1 < 1) {
            indexInfl1 = 1;
        }
        while (histArray1[indexInfl1] > 0) {
            indexInfl1++;
        }

        if (indexInfl1 < 2) {
            indexInfl1 = 2;
        }
        int indexInfl2 = indexInfl1;
        while (histArray2[indexInfl2] < 0) {
            indexInfl2++;
        }
        return indexInfl2;
    }

    public int[] smoothHist(int[] hist) {
        int iter = 0;
        double[] dHist = new double[256];

        for (int i = 0; i < 256; i++) {
            dHist[i] = (double) hist[i];
        }

        double[] tHist = dHist;

        while (!monoModalTest(dHist)) {
            //smooth with a 3 point running mean filter
            for (int i = 1; i < 255; i++) {
                tHist[i] = (dHist[i - 1] + dHist[i] + dHist[i + 1]) / 3;
            }
            tHist[0] = (dHist[0] + dHist[1]) / 3; //0 outside
            tHist[255] = (dHist[254] + dHist[255]) / 3; //0 outside
            dHist = tHist;
            iter++;
        }
        int[] iHist = new int[256];

        for (int i = 0; i < 256; i++) {
            iHist[i] = (int) dHist[i];
        }
        return iHist;
    }

    public static boolean monoModalTest(double[] dHist) {
        int len = dHist.length;
        boolean bool = false;
        int modes = 0;

        for (int k = 1; k < len - 1; k++) {
            if (dHist[k - 1] < dHist[k] && dHist[k + 1] < dHist[k]) {
                modes++;
                if (modes > 2) {
                    return false;
                }
            }
        }
        if (modes == 1) {
            bool = true;
        }
        return bool;
    }

    public Frame getFrame(Component theComponent) {
        Component currParent = theComponent;
        Frame theFrame = null;
        while (currParent != null) {
            if (currParent instanceof Frame) {
                theFrame = (Frame) currParent;
                break;
            }
            currParent = currParent.getParent();
        }
        return theFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        Object object = e.getSource();
        Frame frame = getFrame((Component) object);
        if (frame.equals(layerFrame)) {
            if (label.equals("Reverse Stack Z")) {
                if (!imp.isVisible()) {
                    IJ.showMessage("No Stack", "There is no stack.");
                    layerFrame.dispose();
                    return;
                }
                reverseStackZ();
            } else if (label.equals("Sync Stromal")) {
                if (!imp.isVisible()) {
                    IJ.showMessage("No Stack", "There is no stack.");
                    layerFrame.dispose();
                    return;
                }
                stromalStart = (imp.getCurrentSlice() - 1) / nChannels + 1;
                stromalField.setText(Integer.toString(stromalStart));
            } else if (label.equals("Sync Subbasal")) {
                if (!imp.isVisible()) {
                    IJ.showMessage("No Stack", "There is no stack.");
                    layerFrame.dispose();
                    return;
                }
                subbasalStart = (imp.getCurrentSlice() - 1) / nChannels + 1;
                subbasalField.setText(Integer.toString(subbasalStart));
            } else if (label.equals("Sync IET")) {
                if (!imp.isVisible()) {
                    IJ.showMessage("No Stack", "There is no stack.");
                    layerFrame.dispose();
                    return;
                }
                ietStart = (imp.getCurrentSlice() - 1) / nChannels + 1;
                ietField.setText(Integer.toString(ietStart));
            } else if (label.equals("Project")) {
                if (!imp.isVisible()) {
                    IJ.showMessage("No Stack", "There is no stack.");
                    layerFrame.dispose();
                    return;
                }
                stromalStart = Integer.parseInt(stromalField.getText());
                subbasalStart = Integer.parseInt(subbasalField.getText());
                ietStart = Integer.parseInt(ietField.getText());
                if (impProjected != null) {
                    updateProjection();
                } else {
                    getProjection();
                }
            } else if (label.equals("OK")) {
                if ((impProjected != null) && (impProjected.isVisible())) {
                    getThreshArray();
                    getThresholding();

                    showStart();
                    layerFrame.dispose();

                    showThreshFrame();
                    showThresholding();
                } else {
                    IJ.showMessage("No Projection", "There is no projection.");
                }
            } else if (label.equals("Cancel")) {
                layerFrame.dispose();
            }
        } else if (frame.equals(threshFrame)) {
            if (label.equals("/\\")) {
                if (object.equals(stromal2UpButton)) {
                    stromal2Field.setText(Integer.toString(Integer.parseInt(stromal2Field.getText()) + 1));
                } else if (object.equals(stromal3UpButton)) {
                    stromal3Field.setText(Integer.toString(Integer.parseInt(stromal3Field.getText()) + 1));
                } else if (object.equals(subbasal2UpButton)) {
                    subbasal2Field.setText(Integer.toString(Integer.parseInt(subbasal2Field.getText()) + 1));
                } else if (object.equals(subbasal3UpButton)) {
                    subbasal3Field.setText(Integer.toString(Integer.parseInt(subbasal3Field.getText()) + 1));
                } else if (object.equals(iet2UpButton)) {
                    iet2Field.setText(Integer.toString(Integer.parseInt(iet2Field.getText()) + 1));
                } else if (object.equals(iet3UpButton)) {
                    iet3Field.setText(Integer.toString(Integer.parseInt(iet3Field.getText()) + 1));
                }
                if (updateThresholding()) {
                    showThresholding();
                }
            } else if (label.equals("\\/")) {
                if (object.equals(stromal2DownButton)) {
                    stromal2Field.setText(Integer.toString(Integer.parseInt(stromal2Field.getText()) - 1));
                } else if (object.equals(stromal3DownButton)) {
                    stromal3Field.setText(Integer.toString(Integer.parseInt(stromal3Field.getText()) - 1));
                } else if (object.equals(subbasal2DownButton)) {
                    subbasal2Field.setText(Integer.toString(Integer.parseInt(subbasal2Field.getText()) - 1));
                } else if (object.equals(subbasal3DownButton)) {
                    subbasal3Field.setText(Integer.toString(Integer.parseInt(subbasal3Field.getText()) - 1));
                } else if (object.equals(iet2DownButton)) {
                    iet2Field.setText(Integer.toString(Integer.parseInt(iet2Field.getText()) - 1));
                } else if (object.equals(iet3DownButton)) {
                    iet3Field.setText(Integer.toString(Integer.parseInt(iet3Field.getText()) - 1));
                }
                if (updateThresholding()) {
                    showThresholding();
                }
            } else if (label.equals("Edit")) {
                if (!ResetThresholding()) {
                    return;
                }
                frame.setTitle("Corneal nerves - editing");
                threshPanel.setEnabled(false);
                threshPanel.setVisible(false);
                editButton.setEnabled(false);
                editButton.setVisible(false);
                updateButton.setEnabled(true);
                updateButton.setVisible(true);
                resetButton.setEnabled(true);
                resetButton.setVisible(true);
            } else if (label.equals("Update")) {
                if (!impMask.isVisible()) {
                    if (!ResetThresholding()) {
                        return;
                    }
                }
                showThresholding();
            } else if (label.equals("Reset")) {
                frame.setTitle("Corneal nerves - thresholding");
                threshPanel.setEnabled(true);
                threshPanel.setVisible(true);
                editButton.setEnabled(true);
                editButton.setVisible(true);
                updateButton.setEnabled(false);
                updateButton.setVisible(false);
                resetButton.setEnabled(false);
                resetButton.setVisible(false);
                if (ResetThresholding()) {
                    showThresholding();
                }
            } else if (label.equals("OK")) {
                if (!impMask.isVisible()) {
                    if (!ResetThresholding()) {
                        return;
                    }
                }
                threshFrame.dispose();
                IJ.run(impMask, "Select None", "");

                Window window;
                if (nChannels > 2) {
                    getColocal();
                    zoomExact(impColocal, 0.5);
                    window = impColocal.getWindow();
                    window.setLocation(delta * 2, delta * 2);
                }
                drawGrid();

                getInterception();
                zoomExact(impMask, 0.5);
                window = impMask.getWindow();
                window.setLocation(delta * 3, delta * 3);
                window.toFront();
                Rectangle rect = window.getBounds();
                zoomExact(impInterception, 0.5);
                window = impInterception.getWindow();
                window.setLocation(rect.x + rect.width, rect.y);
                window.toFront();

                showThresholding();
                threshFrame.dispose();

                showGridFrame();
                showGriding();

            } else if (label.equals("Cancel")) {
                threshFrame.dispose();
            }
        } else if (frame.equals(gridFrame)) {
            if (label.equals("/\\")) {
                if (object.equals(gridXUpButton)) {
                    gridXField.setText(Integer.toString(Integer.parseInt(gridXField.getText()) + 1));
                } else if (object.equals(gridYUpButton)) {
                    gridYField.setText(Integer.toString(Integer.parseInt(gridYField.getText()) + 1));
                } else if (object.equals(gridWidthUpButton)) {
                    gridWidthField.setText(Integer.toString(Integer.parseInt(gridWidthField.getText()) + 1));
                }
                if (updateGriding()) {
                    showGriding();
                }

            } else if (label.equals("\\/")) {
                if (object.equals(gridXDownButton)) {
                    gridXField.setText(Integer.toString(Integer.parseInt(gridXField.getText()) - 1));
                } else if (object.equals(gridYDownButton)) {
                    gridYField.setText(Integer.toString(Integer.parseInt(gridYField.getText()) - 1));
                } else if (object.equals(gridWidthDownButton)) {
                    gridWidthField.setText(Integer.toString(Integer.parseInt(gridWidthField.getText()) - 1));
                }
                if (updateGriding()) {
                    showGriding();
                }
            } else if (label.equals("OK")) {
                showResult();
                gridFrame.dispose();
            } else if (label.equals("Cancel")) {
                gridFrame.dispose();
            }
        }
    }

    class MyKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }

            Object object = e.getSource();
            if ((object.equals(stromal2Field)
                    || object.equals(subbasal2Field)
                    || object.equals(iet2Field)
                    || object.equals(stromal3Field)
                    || object.equals(subbasal3Field)
                    || object.equals(iet3Field))) {
                if (updateThresholding()) {
                    showThresholding();
                }
            } else if (object.equals(gridXField)
                    || object.equals(gridYField)
                    || object.equals(gridWidthField)) {
                if (updateGriding()) {
                    showGriding();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
