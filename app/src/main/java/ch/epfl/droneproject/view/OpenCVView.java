package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Point;

public class OpenCVView extends View {

    private final static String TAG = "OpenCVView";
    private final static int FACE_RECT_COLOR = Color.GREEN;

    private final Context ctx;
    private Paint paint;

    private Rect object;
    private Point objectCenter;

    public OpenCVView(Context context) {
        this(context, null);
    }

    public OpenCVView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpenCVView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ctx = context;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(FACE_RECT_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);


    }

    public void setObject(Rect object, Point center){
        this.objectCenter = center;
        this.object = object;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(object != null) {
            canvas.drawRect((float) object.tl().x(), (float) object.tl().y(), (float) object.br().x(), (float) object.br().y(), paint);

            if(objectCenter != null){
                canvas.drawCircle((float)objectCenter.x(), (float)objectCenter.y(), 11, paint);
            }
        }
        super.onDraw(canvas);
    }
}