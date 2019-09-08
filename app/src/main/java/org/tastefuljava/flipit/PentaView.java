package org.tastefuljava.flipit;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
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

    private int margin;
    private int strokeWidth;
    private int textSize;
    private String text = "\uf4ce";
    private final Paint paint;
    private Path path;
    private double r;
    private int xm;
    private int ym;
    private final Typeface typeface;
    private final Paint.FontMetrics fm = new Paint.FontMetrics();

    public PentaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PentaView,
                0, 0);
        try {
            String s = a.getString(R.styleable.PentaView_text);
            text = s == null ? "" : s;
            strokeWidth = a.getInteger(R.styleable.PentaView_pentaWidth, 3);
            margin = a.getInteger(R.styleable.PentaView_margin, 4);
            textSize = a.getInteger(R.styleable.PentaView_textSize, 120);

        } finally {
            a.recycle();
        }
        typeface = Typeface.createFromAsset(context.getAssets(), "font/fa_solid_900.ttf");
        paint = preparePaint();
        prepare();
    }

    public String getText() {
        return text;
    }

    public void setText(String newValue) {
        text = newValue == null ? "" : newValue;
        invalidate();
    }

    private Paint preparePaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(strokeWidth);
        paint.setTypeface(typeface);
        paint.setTextSize(textSize);
        return paint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        prepare();
    }

    private void prepare() {
        prepareMetrics();
        preparePentagon();
        prepareText();
    }

    private void preparePentagon() {
        path = new Path();
        path.moveTo(xm+(int)(r*SIN36), ym);
        path.lineTo(xm+(int)(r*SIN72), ym+(int)(r*(COS36+COS72)));
        path.lineTo(xm, ym+(int)(r*(COS36+1)));
        path.lineTo(xm-(int)(r*SIN72), ym+(int)(r*(COS36+COS72)));
        path.lineTo(xm-(int)(r*SIN36), ym);
        path.close();
    }

    private void prepareMetrics() {
        int width = this.getWidth();
        int height = this.getHeight();
        int hi = height-2*margin;
        int wi = width-2*margin;
        double rh = hi/(COS36+1);
        double rw = wi/(2*SIN72);
        r = Math.min(rh, rw);
        xm = margin+wi/2;
        ym = margin+(hi-(int)(r*(1+COS36)))/2;
    }

    private void prepareText() {
        paint.getFontMetrics(fm);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
        int x = xm-(int)(paint.measureText(text)/2);
        int y = ym+(int)(r*COS36)
                -(int)((-fm.ascent+fm.descent)/2)-(int)fm.ascent;
        canvas.drawText(text, x, y, paint);
    }

    private static double deg2rad(double angle) {
        return angle*2*Math.PI/360;
    }
}
