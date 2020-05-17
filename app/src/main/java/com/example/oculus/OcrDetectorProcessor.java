package com.example.oculus;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import android.util.Log;
import android.util.SparseArray;
import com.example.oculus.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock>{

    private GraphicOverlay<OcrGraphic> graphicOverlay;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
        graphicOverlay = ocrGraphicOverlay;
    }

    @Override
    public void release() {
        graphicOverlay.clear();
    }     //clears the graphic overlay

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {    //receives the text and draws it on our overlay
        graphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                Log.d("Processor", "Text detected! " + item.getValue());
                OcrGraphic graphic = new OcrGraphic(graphicOverlay, item);
                graphicOverlay.add(graphic);
            }
        }
    }
}

