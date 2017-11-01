package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.module.OpenCvModule;


public class CVClassifierView extends View {

    private final static String TAG = CVClassifierView.class.getSimpleName();

    private final static int FACE_RECT_COLOR = Color.GREEN;

    private final Context ctx;
    private final Object lock = new Object();

    private BebopVideoView bebopVideoView;
    private OpenCvModule mNativeDetector;
    private Rect face;
    private boolean faceTracked;

    private float mRelativeFaceSize;
    private int mAbsoluteFaceSize;

    private Thread openCVThread;
    private Paint paint;



    private BaseLoaderCallback mLoaderCallback;

    public CVClassifierView(Context context) {
        this(context, null);
    }

    public CVClassifierView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CVClassifierView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ctx = context;

        mLoaderCallback = new BaseLoaderCallback(ctx) {
            @Override
            public void onManagerConnected(int status) {

                switch(status){
                    case BaseLoaderCallback.SUCCESS:

                        // Load native library after(!) OpenCV initialization
                        System.loadLibrary("MyModule");

                        try {
                            // load cascade file from application resources
                            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                            //InputStream is = getResources().openRawResource(R.raw.haarcascade_fullbody);
                            File cascadeDir = ctx.getDir("cascade", Context.MODE_PRIVATE);
                            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
                            //File mCascadeFile = new File(cascadeDir, "haarcascade_fullbody.xml");
                            FileOutputStream os = new FileOutputStream(mCascadeFile);

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                            is.close();
                            os.close();

                            mNativeDetector = new OpenCvModule(mCascadeFile.getAbsolutePath(), 0);
                            mNativeDetector.start();

                            boolean isDeleted = cascadeDir.delete();
                            if(!isDeleted){
                                Log.e(TAG, "Error file il not deleted");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                        }
                        Log.e("RECT", "Start thread");
                        openCVThread = new CascadingThread(ctx);
                        openCVThread.start();

                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        bebopVideoView = null;
        face = null;
        faceTracked = false;

        mRelativeFaceSize = 0.1f;
        mAbsoluteFaceSize = 0;

        openCVThread = null;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    public void pause() {

        openCVThread.interrupt();

        try {
            openCVThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(mNativeDetector != null) {
            mNativeDetector.stop();
        }
    }

    public void onDestroy() {
        pause();
    }

    public void resume(final BebopVideoView bebopVideoView) {

        this.bebopVideoView = bebopVideoView;

        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV successfully loaded !");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else{
            Log.d(TAG, "OpenCV not loaded !");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, ctx, mLoaderCallback);
        }
    }


    private class CascadingThread extends Thread {

        private final Handler handler;
        boolean interrupted = false;

        private Mat mRgba;
        private Mat mGray;


        private CascadingThread(final Context ctx) {
            handler = new Handler(ctx.getMainLooper());
        }

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public void run() {

            mRgba = new Mat();
            mGray = new Mat();

            while (!interrupted) {
                final Bitmap source = bebopVideoView.getBitmap();

                if (source != null) {
                    Utils.bitmapToMat(source, mRgba);
                    mRgba.assignTo(mGray);
                    Imgproc.cvtColor(mGray, mGray, Imgproc.COLOR_RGBA2GRAY);
                    Imgproc.equalizeHist(mGray, mGray);

                    if (mAbsoluteFaceSize == 0) {
                        int height = mGray.rows();
                        if (Math.round(height * mRelativeFaceSize) > 0) {
                            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                        }
                        mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
                    }

                    MatOfRect faces = new MatOfRect();
                    if (mNativeDetector != null)
                        mNativeDetector.detect(mGray, faces);

                    synchronized (lock) {
                        Rect[] facesArray = faces.toArray();
                        // If a face is found : take the first one and start tracking it
                        if (facesArray.length > 0) {
                            face = facesArray[0];
                            faceTracked = true;
                        } else {
                            faceTracked = false;
                            Log.e("RECT", "LOST OF FACE ! - Draw last known position");
                        }

                        faces.release();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                invalidate();
                            }
                        });
                    }
                }
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            mRgba.release();
            mGray.release();
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        synchronized(lock) {
            Log.e("RECT", "DRAW");
            if (face != null) {
                if(faceTracked) {
                    paint.setColor(FACE_RECT_COLOR);
                }else{
                    paint.setColor(Color.RED);
                }

                canvas.drawRect((float) face.tl().x, (float) face.tl().y, (float) face.br().x, (float) face.br().y, paint);
            }
        }
        super.onDraw(canvas);
    }
}