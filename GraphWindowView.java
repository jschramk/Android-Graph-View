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

  private List<PointF> points = new ArrayList<>();
  private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private ScaleGestureDetector scaleGestureDetector;

  private float initialCursorPropX, initialCursorPropY;
  private float initialViewportX, initialViewportY;
  private float initialSpan, initialSpanX, initialSpanY;
  private float initialViewportWidth, initialViewportHeight;

  private OnViewportBoundsChangedListener o;
  private GraphFunction f;

  private float viewPortX;
  private float viewPortY;
  private float viewPortWidth;
  private float viewPortHeight;

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

    setViewportBounds(-5, 5, -5, 5);

    paint.setTextSize(50);
    paint.setStrokeWidth(5);

    scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleGestureListener);

  }

  @Override
  protected void onDraw(Canvas canvas) {

    // background
    //canvas.drawColor(Color.rgb(200, 200, 200));

    paint.setColor(Color.BLACK);

    //origin
    canvas.drawLine(getScreenX(0), 0, getScreenX(0), getHeight(), paint);
    canvas.drawLine(0, getScreenY(0), getWidth(), getScreenY(0), paint);

    paint.setColor(Color.BLUE);

    // plot
    for (int i = 0; i < points.size() - 1; i++) {
      canvas.drawLine(getScreenX(points.get(i).x), getScreenY(points.get(i).y), getScreenX(points.get(i+1).x), getScreenY(points.get(i+1).y), paint);
    }

  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean retVal = scaleGestureDetector.onTouchEvent(event);
    return retVal || super.onTouchEvent(event);
  }

  private final ScaleGestureDetector.OnScaleGestureListener scaleGestureListener
      = new ScaleGestureDetector.SimpleOnScaleGestureListener() {


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

      initialCursorPropX = detector.getFocusX() / getWidth();
      initialCursorPropY = detector.getFocusY() / getHeight();

      initialSpan = detector.getCurrentSpan();
      //initialSpanX = detector.getCurrentSpanX();
      //initialSpanY = detector.getCurrentSpanY();

      initialViewportX = viewPortX;
      initialViewportY = viewPortY;

      initialViewportWidth = viewPortWidth;
      initialViewportHeight = viewPortHeight;

      return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

      float currentSpan = detector.getCurrentSpan();
      //float currentSpanX = detector.getCurrentSpanX();
      //float currentSpanY = detector.getCurrentSpanY();

      float scale = initialSpan / currentSpan;
      //float scaleX = initialSpanX / currentSpanX;
      //float scaleY = initialSpanY / currentSpanY;

      float cursorPropX = detector.getFocusX() / getWidth();
      float cursorPropY = detector.getFocusY() / getHeight();

      float deltaPropX = cursorPropX - initialCursorPropX;
      float deltaPropY = cursorPropY - initialCursorPropY;

      viewPortWidth = scale * initialViewportWidth;
      viewPortHeight = scale * initialViewportHeight;

      viewPortX = initialViewportX - cursorPropX * (viewPortWidth - initialViewportWidth) - deltaPropX * initialViewportWidth;
      viewPortY = initialViewportY - cursorPropY * (viewPortHeight - initialViewportHeight) - deltaPropY * initialViewportHeight;

      if(o != null) {
        o.onBoundsChanged(getViewportBounds());
      }

      if(f != null) {

        computePoints();

      }

      ViewCompat.postInvalidateOnAnimation(GraphWindowView.this);

      return true;
    }
  };

  private void computePoints() {

    clearPoints();

    int numPoints = 300;

    for (int i = 0; i < numPoints; i++) {
      float x = viewPortX + ((float) i/numPoints) * viewPortWidth;
      addPoint(new PointF(x, f.value(x)));

    }

  }

  private PointF getScreenPoint(PointF actualPoint) {
    return new PointF(getScreenX(actualPoint.x), getScreenY(actualPoint.y));
  }

  private float getScreenX(float actualX) {
    return (actualX - viewPortX) / viewPortWidth * getWidth();
  }

  private float getScreenY(float actualY) {
    return (actualY - viewPortY) / viewPortHeight * getHeight();
  }


  public void setOnViewportBoundsChangedListener(OnViewportBoundsChangedListener o) {
    this.o = o;
  }

  public void setViewportBounds(float minX, float maxX, float minY, float maxY) {
    viewPortX = minX;
    viewPortY = minY;
    viewPortWidth = maxX - minX;
    viewPortHeight = maxY - minY;
  }

  public RectF getViewportBounds() {
    return new RectF(viewPortX, viewPortY + viewPortHeight, viewPortX + viewPortWidth, viewPortY);
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
  }

  public interface GraphFunction {
    float value(float input);
  }

}
