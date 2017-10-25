package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Image;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import ch.epfl.droneproject.R;


public class CVClassifierView extends View {

    private final static String TAG = CVClassifierView.class.getSimpleName();
    private final Context ctx;

    private CascadeClassifier faceClassifier;
    private CascadeClassifier palmClassifier;
    private CascadeClassifier fistClassifier;

    private Handler openCVHandler = new Handler();
    private Thread openCVThread = null;

    private BebopVideoView bebopVideoView = null;

    private Rect[] facesArray = null;

    private Paint paint;

    private final Object lock = new Object();



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
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        Log.i("VideoFragment", "OpenCV loaded successfully");
                        //mOpenCvCameraView.enableView();
                        // initialize our opencv cascade classifiers
                        cascadeFile(R.raw.haarcascade_upperbody);
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        // initialize our canvas paint object
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
    }

    private void cascadeFile(final int id) {

        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(id);
            File cascadeDir = ctx.getDir("cascade", Context.MODE_PRIVATE);
            final File cascadeFile = new File(cascadeDir, String.format(Locale.US, "%d.xml", id));

            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            Log.e(TAG, cascadeFile.getAbsolutePath());

            faceClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            //faceClassifier.load(cascadeFile.getAbsolutePath());

            if (faceClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                faceClassifier = null;
            } else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    public void resume(final BebopVideoView bebopVideoView) {

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, ctx, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, mView.getContext(), mLoaderCallback);

        if (getVisibility() == View.VISIBLE) {
            this.bebopVideoView = bebopVideoView;

            openCVThread = new CascadingThread(ctx);
            openCVThread.start();
        }
    }

    public void pause() {
        if (getVisibility() == View.VISIBLE) {
            openCVThread.interrupt();

            try {
                openCVThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private float mX = 0;
    private float mY = 0;

    private class CascadingThread extends Thread {
        private final Handler handler;
        boolean interrupted = false;

        private CascadingThread(final Context ctx) {
            handler = new Handler(ctx.getMainLooper());
        }

        public void interrupt() {
            interrupted = true;
        }

        @Override
        public void run() {
            Log.d(TAG, "cascadeRunnable");

            final Mat firstMat = new Mat();
            final Mat mat = new Mat();

            while (!interrupted) {
                final Bitmap source = bebopVideoView.getBitmap();

                if (source != null) {
                    Utils.bitmapToMat(source, firstMat);
                    firstMat.assignTo(mat);

                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);

                    final int minRows = Math.round(mat.rows() * .12f);

                    final Size minSize = new Size(minRows, minRows);
                    final Size maxSize = new Size(0, 0);

                    final MatOfRect faces = new MatOfRect();

                    faceClassifier.detectMultiScale(mat, faces);

                    synchronized (lock) {
                        facesArray = faces.toArray();

                        mX = firstMat.width() / mat.width();
                        mY = firstMat.height() / mat.height();

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

            firstMat.release();
            mat.release();
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {

        Log.e("DRAW", "DRAW");

        synchronized(lock) {
            if (facesArray != null && facesArray.length > 0) {
                for (Rect target : facesArray) {
                    Log.i(TAG, "found face size=" + target.area());
                    paint.setColor(Color.RED);
                    canvas.drawRect((float) target.tl().x * mX, (float) target.tl().y * mY, (float) target.br().x * mX, (float) target.br().y * mY, paint);
                }
            }
        }
        super.onDraw(canvas);
    }
}