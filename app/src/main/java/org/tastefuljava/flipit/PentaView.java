package org.tastefuljava.flipit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class PentaView extends View {
    private static final String TAG = PentaView.class.getSimpleName();

    private static final double DEG36 = deg2rad(36);
    private static final double COS36 = Math.cos(DEG36);
    private static final double SIN36 = Math.sin(DEG36);
    private static final double DEG72 = deg2rad(72);
    private static final double SIN72 = Math.sin(DEG72);
    private static final double COS72 = Math.cos(DEG72);

    private int margin = 4;
    private int strokeWidth = 3;
    private final Paint draw;


    public PentaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        draw = new Paint();
        draw.setStyle(Paint.Style.STROKE);
        draw.setAntiAlias(true);
        draw.setColor(Color.BLACK);
        draw.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();
        int hi = height-2*margin;
        int wi = width-2*margin;
        double rh = hi/(COS36+1);
        double rw = wi/(2*SIN72);
        double r = Math.min(rh, rw);
        int xm = margin+wi/2;
        int ym = margin+(hi-(int)(r*(1+COS36)))/2;
        Path path = new Path();
        path.moveTo(xm+(int)(r*SIN36), ym);
        path.lineTo(xm+(int)(r*SIN72), ym+(int)(r*(COS36+COS72)));
        path.lineTo(xm, ym+(int)(r*(COS36+1)));
        path.lineTo(xm-(int)(r*SIN72), ym+(int)(r*(COS36+COS72)));
        path.lineTo(xm-(int)(r*SIN36), ym);
        path.close();
        canvas.drawPath(path, draw);
    }

    private static double deg2rad(double angle) {
        return angle*2*Math.PI/360;
    }
}
