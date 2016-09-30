package de.madpage.bicyclekaraoke;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * from https://dhingrakimmi.wordpress.com/2015/09/17/android-measurment-scale-my-scale/
 */
public class MyScaleView extends View {
	int width, height, currentSpeedPoint, desiredSpeedPoint, maximumSpeed;
	int startingPointY = 0;
	private Paint gradientPaint;
	private float rulersizeY = 0;
	private Paint rulerPaint, textPaint, goldenPaint, redPaint;
	private int endPointX;
	private int scaleLineSmall;
	private int scaleLineMedium;
	private int scaleLineLarge;
	private int textStartPoint;
	private int yellowLineStrokeWidth;

	public MyScaleView(Context context, AttributeSet foo) {
		super(context, foo);
		if (!isInEditMode()) {
			init(context);
		}
	}

	private void init(Context context) {
		yellowLineStrokeWidth = (int) getResources().getDimension(R.dimen.yellow_line_stroke_width);
		gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		rulerPaint = new Paint();
		rulerPaint.setStyle(Paint.Style.STROKE);
		rulerPaint.setStrokeWidth(0);
		rulerPaint.setAntiAlias(false);
		rulerPaint.setColor(Color.WHITE);
		textPaint = new TextPaint();
		textPaint.setStyle(Paint.Style.STROKE);
		textPaint.setStrokeWidth(0);
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(getResources().getDimension(R.dimen.txt_size));
		textPaint.setColor(Color.WHITE);
		goldenPaint = new Paint();
		goldenPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		goldenPaint.setColor(context.getResources().getColor(R.color.yellow));
		goldenPaint.setStrokeWidth(yellowLineStrokeWidth);
		goldenPaint.setStrokeJoin(Paint.Join.ROUND);
		goldenPaint.setStrokeCap(Paint.Cap.ROUND);
		goldenPaint.setPathEffect(new CornerPathEffect(10));
		goldenPaint.setAntiAlias(true);
        redPaint = new Paint();
        redPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        redPaint.setColor(context.getResources().getColor(R.color.red));
        redPaint.setStrokeWidth(yellowLineStrokeWidth);
        redPaint.setStrokeJoin(Paint.Join.ROUND);
        redPaint.setStrokeCap(Paint.Cap.ROUND);
        redPaint.setPathEffect(new CornerPathEffect(10));
        redPaint.setAntiAlias(true);
		scaleLineSmall = (int) getResources().getDimension(R.dimen.scale_line_small);
		scaleLineMedium = (int) getResources().getDimension(R.dimen.scale_line_medium);
		scaleLineLarge = (int) getResources().getDimension(R.dimen.scale_line_large);
		textStartPoint = (int) getResources().getDimension(R.dimen.text_start_point);
        maximumSpeed = 10;
	}

	@Override
	public void onSizeChanged(int w, int h, int oldW, int oldH) {
		width = w;
		height = h;
        rulersizeY = height / maximumSpeed;
		endPointX = width - 10;

		int green = getResources().getColor(R.color.green);
		int white = getResources().getColor(R.color.transparent_white);
		Shader.TileMode mode = android.graphics.Shader.TileMode.MIRROR;
		LinearGradient linGrad = new LinearGradient(0, 0, width, rulersizeY, green, white, mode);
		if (linGrad != null && gradientPaint != null) {
			gradientPaint.setShader(linGrad);
		}
	}

	@Override
	public void onDraw(Canvas canvas) {
        if (canvas != null && gradientPaint != null) {

            startingPointY = height;
            for (int i = 1; ; --i) {
                if (startingPointY < 0 ) {
                    break;
                }
                startingPointY = startingPointY - (int) rulersizeY /10;
                int size = (i % 10 == 0) ? scaleLineLarge : (i % 5 == 0) ? scaleLineMedium : scaleLineSmall;
                // draw horizontal lines
                canvas.drawLine(endPointX - size, startingPointY, endPointX, startingPointY, rulerPaint);
                if (i % 10 == 0) {
                    canvas.drawText((-i) + "", endPointX - textStartPoint, startingPointY + 8, textPaint);
                }
            }
            // rect around line
            //canvas.drawRect(0f, currentSpeedPoint - (rulersizeY / 2), width, currentSpeedPoint + (rulersizeY / 2), gradientPaint);
            //golden line
            int lineHeight = height - (currentSpeedPoint+2)*((int) rulersizeY /10);
            canvas.drawLine(0f, lineHeight, width - 20, lineHeight, goldenPaint);
            //red line
            lineHeight = height - (desiredSpeedPoint+2)
                    *((int) rulersizeY /10);
            canvas.drawLine(0f, lineHeight, width - 20, lineHeight, redPaint);
        }
	}

	public void setMaximumSpeed(int speed) {
		maximumSpeed = speed;
        rulersizeY = height / maximumSpeed;
        invalidate();
	}

    public void setCurrentSpeedPoint(int speed) {
        currentSpeedPoint = speed;
        invalidate();
    }

    public void setDesiredSpeedPoint(int speed) {
        desiredSpeedPoint = speed;
        invalidate();
    }
}