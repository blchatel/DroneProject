package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.module.AutoPilotModule;
import ch.epfl.droneproject.module.OpenCvModule;

import static org.opencv.core.CvType.CV_8UC1;


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
            canvas.drawRect((float) object.tl().x, (float) object.tl().y, (float) object.br().x, (float) object.br().y, paint);

            if(objectCenter != null){
                canvas.drawCircle((float)objectCenter.x, (float)objectCenter.y, 11, paint);
            }
        }
        super.onDraw(canvas);
    }
}