package de.razorfish.android.charpathrecog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    private Path path;
    private Paint strokePaint;
    private Paint pointPaint;
    private Paint backgroundPaint;
    private List<List<PointF>> paths;
    private List<PointF> currentPath;
    private List<Float> points;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        path = new Path();

        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setDither(true);
        strokePaint.setColor(Color.GRAY);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(24);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(240, 240, 240));
        backgroundPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setDither(true);
        pointPaint.setColor(Color.BLACK);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeCap(Paint.Cap.ROUND);
        pointPaint.setStrokeWidth(12);

        clearPaths();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        canvas.drawPath(path, strokePaint);
        float[] primitivePoints = new float[points.size()];
        int i = 0;
        for (Float point : points) {
            primitivePoints[i++] = point;
        }
        canvas.drawPoints(primitivePoints, pointPaint);
    }

    private void touchStart(float x, float y) {
        currentPath = new ArrayList<>();
        paths.add(currentPath);
        currentPath.add(new PointF(x, y));
        path.moveTo(x, y);
        points.add(x);
        points.add(y);
    }

    private void touchMove(float x, float y) {
        path.lineTo(x, y);
        currentPath.add(new PointF(x, y));
        points.add(x);
        points.add(y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
        }
        return true;
    }

    public List<List<PointF>> getPaths() {
        return paths;
    }

    public void clearPaths() {
        path.reset();
        paths = new ArrayList<>();
        points = new ArrayList<>();
        invalidate();
    }

}
