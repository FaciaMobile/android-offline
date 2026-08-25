package com.facia.faciasdk.Camera.CameraXHelpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.facia.faciasdk.Logs.Webhooks;

public class FocusView extends View {
    private Paint mTransparentPaint;
    private Paint mSemiBlackPaint;
    private final Path mPath = new Path();
    int ovalWidth, ovalHeight, ovalX, ovalY;

    public FocusView(Context context) {
        super(context);
        initPaints();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        try {
            mTransparentPaint = new Paint();
            mTransparentPaint.setColor(Color.TRANSPARENT);
            mTransparentPaint.setStrokeWidth(10);

            mSemiBlackPaint = new Paint();
            mSemiBlackPaint.setColor(Color.WHITE);
            mSemiBlackPaint.setStrokeWidth(10);
        }catch (Exception e){
            Webhooks.exceptionReport(e, "FaceDetection/FocusView/initPaints");
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            mPath.reset();
            ovalWidth = (int) (getWidth());
            ovalHeight = (int) (getHeight());
            ovalX = (getWidth() - ovalWidth) / 2;
            ovalY = (getHeight() - ovalHeight) / 2;
            mPath.addOval(new RectF(ovalX, ovalY, ovalWidth + ovalX, ovalHeight + ovalY), Path.Direction.CCW);
            mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);
            canvas.drawCircle(getWidth(), getHeight(), 500, mTransparentPaint);
            canvas.drawPath(mPath, mSemiBlackPaint);
            canvas.clipPath(mPath);
            canvas.drawColor(Color.WHITE);
        }catch (Exception e){
            Webhooks.exceptionReport(e, "FaceDetection/FocusView/onDraw");
        }
    }
}

