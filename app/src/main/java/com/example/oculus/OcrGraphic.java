package com.example.oculus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.oculus.camera.GraphicOverlay;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.util.List;

public class OcrGraphic extends GraphicOverlay.Graphic {

    private int id;

    private static final int TEXT_COLOR = Color.WHITE;

    private static Paint rectPaint;
    private static Paint textPaint;
    private final TextBlock text;

    OcrGraphic(GraphicOverlay overlay, TextBlock text) {
        super(overlay);

        this.text = text;

        if (rectPaint == null) {
            rectPaint = new Paint();
            rectPaint.setColor(TEXT_COLOR);
            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setStrokeWidth(4.0f);
        }

        if (textPaint == null) {
            textPaint = new Paint();
            textPaint.setColor(TEXT_COLOR);
            textPaint.setTextSize(55.0f);
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TextBlock getTextBlock() {
        return text;
    }

    public boolean contains(float x, float y) {
        // TODO: Check if this graphic's text contains this point.
        if (text == null) {
            return false;
        }
        RectF rect = new RectF(text.getBoundingBox());
        rect = translateRect(rect);
        return rect.contains(x, y);
    }

    @Override
    public void draw(Canvas canvas) {
        // TODO: Draw the text onto the canvas.
        // TODO: Draw the text onto the canvas.
        if (text == null) {
            return;
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(text.getBoundingBox());
        rect = translateRect(rect);
        canvas.drawRect(rect, rectPaint);

        // Break the text into multiple lines and draw each one according to its own bounding box.
        List<? extends Text> textComponents = text.getComponents();
        for(Text currentText : textComponents) {
            float left = translateX(currentText.getBoundingBox().left);
            float bottom = translateY(currentText.getBoundingBox().bottom);
            canvas.drawText(currentText.getValue(), left, bottom, textPaint);
        }
    }
}
