package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class OpenCVView extends View {

    public final static int FACE_RECT_COLOR = Color.GREEN;
    public final static int BLOB_RECT_COLOR = Color.RED;
    private Paint paint;
    private MyCvRect rect;

    public OpenCVView(Context context) {
        this(context, null);
    }

    public OpenCVView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpenCVView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(FACE_RECT_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);

        rect = new MyCvRect();
    }

    public void setRect(int x1, int y1, int x2, int y2){
        this.rect.setRect(x1, y1, x2, y2);
    }
    public void setColor(int color){
        paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(rect.x1, rect.y1, rect.x2, rect.y2, paint);
        canvas.drawCircle(rect.xc, rect.yc, 11, paint);
        super.onDraw(canvas);
    }


    private class MyCvRect{

        float x1, y1, x2, y2, xc, yc;

        private void setRect(int x1, int y1, int x2, int y2){
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.xc = (x1+x2)/2;
            this.yc = (y1+y2)/2;
        }
    }
}