package com.example.mathsolver.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;

public class GraphWindowView extends View {

  private static final float MIN_GRID_SCALE = 0.000001f;

  private List<PointF> points = new ArrayList<>();
  private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private ScaleGestureDetector scaleGestureDetector;

  private float initialCursorPropX, initialCursorPropY;
  private float initialViewportX, initialViewportY;
  private float initialSpan;
  private float initialViewportWidth, initialViewportHeight;

  private OnViewportBoundsChangedListener o;
  private GraphFunction f;

  private float viewPortX;
  private float viewPortY;
  private float viewPortWidth;
  private float viewPortHeight;

  private float unitsPerDivX = 1, unitsPerDivY = 1;

  private int minDivisions = 8;

  private int numFunctionPoints = 400;

  private ZoomMode currentZoomMode = ZoomMode.XY;

  public enum ZoomMode {
    XY,
    X,
    Y
  }

  public GraphWindowView(Context context) {
    super(context);
    init();
  }

  public GraphWindowView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public GraphWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public GraphWindowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private void init() {

    recenter();

    paint.setTextSize(50);
    paint.setStrokeWidth(5);

    scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleGestureListener);

  }

  int gridLineColor = Color.rgb(240, 240, 240);
  int gridLineDarkerColor = Color.rgb(210, 210, 210);

  @Override
  protected void onDraw(Canvas canvas) {

    // background
    //canvas.drawColor(Color.rgb(240, 240, 240));

    // minor grid lines
    paint.setColor(gridLineColor);
    for (float x = getFirstLine(viewPortX, unitsPerDivX); x <= viewPortX + viewPortWidth; x += unitsPerDivX) {
      canvas.drawLine(getScreenX(x), 0, getScreenX(x), getHeight(), paint);
    }

    for (float y = getFirstLine(viewPortY, unitsPerDivY); y <= viewPortY + viewPortHeight; y += unitsPerDivY) {
      canvas.drawLine(0, getScreenY(y), getWidth(), getScreenY(y), paint);
    }

    // major grid lines
    paint.setColor(gridLineDarkerColor);
    for (float x = getFirstLine(viewPortX, unitsPerDivX * 10); x <= viewPortX + viewPortWidth; x += unitsPerDivX * 10) {
      canvas.drawLine(getScreenX(x), 0, getScreenX(x), getHeight(), paint);
    }

    for (float y = getFirstLine(viewPortY, unitsPerDivY * 10); y <= viewPortY + viewPortHeight; y += unitsPerDivY * 10) {
      canvas.drawLine(0, getScreenY(y), getWidth(), getScreenY(y), paint);
    }

    // origin
    paint.setColor(Color.BLACK);
    canvas.drawLine(getScreenX(0), 0, getScreenX(0), getHeight(), paint);
    canvas.drawLine(0, getScreenY(0), getWidth(), getScreenY(0), paint);

    // plot
    paint.setColor(Color.RED);
    for (int i = 0; i < points.size() - 1; i++) {
      canvas.drawLine(getScreenX(points.get(i).x), getScreenY(points.get(i).y), getScreenX(points.get(i + 1).x), getScreenY(points.get(i + 1).y), paint);
    }

  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return scaleGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
  }

  private final ScaleGestureDetector.OnScaleGestureListener scaleGestureListener
      = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

      initialCursorPropX = detector.getFocusX() / getWidth();
      initialCursorPropY = 1 - detector.getFocusY() / getHeight();

      initialSpan = detector.getCurrentSpan();

      initialViewportX = viewPortX;
      initialViewportY = viewPortY;

      initialViewportWidth = viewPortWidth;
      initialViewportHeight = viewPortHeight;

      return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

      float currentSpan = detector.getCurrentSpan();

      float scale = initialSpan / currentSpan;

      float cursorPropX = detector.getFocusX() / getWidth();
      float cursorPropY = 1 - detector.getFocusY() / getHeight();

      //Log.i("VIEWPORT", String.format("screen prop: (%f, %f)", cursorPropX, cursorPropY));

      float deltaPropX = cursorPropX - initialCursorPropX;
      float deltaPropY = cursorPropY - initialCursorPropY;

      //Log.i("VIEWPORT", String.format("delta screen prop: (%f, %f)", deltaPropX, deltaPropY));

      // change scale x
      if (currentZoomMode != ZoomMode.Y) {
        viewPortWidth = scale * initialViewportWidth;
      }

      // pan x
      viewPortX = initialViewportX - cursorPropX * (viewPortWidth - initialViewportWidth) - deltaPropX * initialViewportWidth;

      // change scale y
      if (currentZoomMode != ZoomMode.X) {
        viewPortHeight = scale * initialViewportHeight;
      }

      // pan y
      viewPortY = initialViewportY - cursorPropY * (viewPortHeight - initialViewportHeight) - deltaPropY * initialViewportHeight;

      // update grid scale for x and y
      float optimalScale = Math.max(
          getOptimalGridScale(viewPortWidth, minDivisions),
          getOptimalGridScale(viewPortHeight, minDivisions)
      );

      unitsPerDivX = unitsPerDivY = optimalScale;

      //unitsPerDivX = getOptimalGridScale(viewPortWidth, minDivisions);
      //unitsPerDivX = getOptimalGridScale(viewPortWidth, minDivisions);

      // report bounds changed
      reportBoundsChanged();

      // compute function points in new window
      if (f != null) {
        computePoints();
      }

      // force redraw
      invalidate();

      return true;
    }

  };

  public void setZoomMode(ZoomMode zoomMode) {
    currentZoomMode = zoomMode;
  }

  public ZoomMode getZoomMode() {
    return currentZoomMode;
  }

  private void computePoints() {

    clearPoints();

    for (int i = 0; i < numFunctionPoints; i++) {
      float x = viewPortX + ((float) i / numFunctionPoints) * viewPortWidth;
      addPoint(new PointF(x, f.value(x)));
    }

  }

  public void redraw() {
    computePoints();
    invalidate();
  }

  public void recenter(){
    setViewportBounds(-10, 10, -10, 10);
  }

  public void squareScale() {
    viewPortHeight = viewPortWidth/getWidth()*getHeight();
    reportBoundsChanged();
    invalidate();
  }

  private static float nearestPower(float number, float powerOf) {
    return (float) Math.pow(powerOf, Math.floor(Math.log(number) / Math.log(powerOf)));
  }

  private static float getOptimalGridScale(float viewPortDimension, int minGridLines) {
    return Math.max(MIN_GRID_SCALE, nearestPower(viewPortDimension / minGridLines, 10));
  }

  private static float getFirstLine(float viewportMin, float divisionSize) {
    float sum = viewportMin + divisionSize;

    return sum - (sum % divisionSize) - divisionSize;
  }

  private PointF getScreenPoint(PointF actualPoint) {
    return new PointF(getScreenX(actualPoint.x), getScreenY(actualPoint.y));
  }

  private float getScreenX(float actualX) {
    return (actualX - viewPortX) / viewPortWidth * getWidth();
  }

  private float getScreenY(float actualY) {
    return (1 - (actualY - viewPortY) / viewPortHeight) * getHeight();
  }


  public void setOnViewportBoundsChangedListener(OnViewportBoundsChangedListener o) {
    this.o = o;
  }

  public void setViewportBounds(float minX, float maxX, float minY, float maxY) {

    viewPortX = minX;
    viewPortY = minY;
    viewPortWidth = maxX - minX;
    viewPortHeight = maxY - minY;

    unitsPerDivX = getOptimalGridScale(viewPortWidth, minDivisions);
    unitsPerDivY = getOptimalGridScale(viewPortHeight, minDivisions);

    reportBoundsChanged();

    invalidate();

  }

  public RectF getViewportBounds() {
    return new RectF(viewPortX, viewPortY + viewPortHeight, viewPortX + viewPortWidth, viewPortY);
  }

  private void reportBoundsChanged() {
    if(o != null) {
      o.onBoundsChanged(getViewportBounds());
    }
  }

  public float getGridScaleX() {
    return unitsPerDivX;
  }

  public float getGridScaleY() {
    return unitsPerDivY;
  }

  public interface OnViewportBoundsChangedListener {
    void onBoundsChanged(RectF bounds);
  }

  public void addPoint(PointF point) {
    points.add(point);
  }

  public void clearPoints() {
    points.clear();
  }

  public void setFunction(GraphFunction f) {
    this.f = f;
    computePoints();
    ViewCompat.postInvalidateOnAnimation(GraphWindowView.this);
  }

  public interface GraphFunction {
    float value(float input);
  }

}
