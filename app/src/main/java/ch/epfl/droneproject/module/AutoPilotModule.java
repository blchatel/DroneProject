package ch.epfl.droneproject.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.arsdk.arcommands.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.OpenCVView;

import static org.opencv.core.CvType.CV_8UC1;



public class AutoPilotModule {

    private static final String TAG = "AutoPilot";


    private boolean isEngaged;


    private SkyControllerExtensionModule mSKEModule;
    private FlightPlanerModule mFlightPlanerModule;

    private DroneStatesAndSettings droneSettings;
    private CameraStatesAndSettings cameraSettings;

    private OpenCVThread openCVThread;


    public AutoPilotModule(SkyControllerExtensionModule skeModule) {

        //this.isEngaged = false;
        this.isEngaged = true;

        this.mSKEModule = skeModule;
        this.mFlightPlanerModule = new FlightPlanerModule();

        this.droneSettings = new DroneStatesAndSettings();
        this.cameraSettings = new CameraStatesAndSettings();
        this.openCVThread = null;
    }

    public boolean isEngaged(){
        return this.isEngaged;
    }
    public void engage(){
        this.isEngaged = true;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_CONTROLLER);
    }

    public void disengage(){
        this.isEngaged = false;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_SKYCONTROLLER);
    }

    public FlightPlanerModule getFlightPlanerModule() {
        return mFlightPlanerModule;
    }
    public void startAutoFlightPlan(){
        if(this.isEngaged)
            this.mSKEModule.startFlightPlan(mFlightPlanerModule);
    }
    public void pauseAutoFlightPlan(){  this.mSKEModule.pauseFlightPlan();}
    public void stopAutoFlightPlan(){  this.mSKEModule.stopFlightPlan();}


    public void setCameraInfo(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
        cameraSettings.set(fov, panMin, panMax, tiltMin, tiltMax);
    }

    public void setCameraSettings(float tilt, float pan){
        cameraSettings.set(tilt, pan);
    }

    public void updateDroneSettings(float roll, float pitch, float yaw){
        droneSettings.update(roll, pitch, yaw);
    }

    /**
     * Sleep during time, a pausing function
     * @param time (long): time to sleep in milisecond, i.e 1000 = 1 sec
     */
    private void sleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean autoTakeOffProcedure(){

        return false;
    }





    // Threads
    public void resumeThreads(BebopVideoView videoView, OpenCVView cvView) {
        Log.e("MAMAN", "ON RESUME THREAD");

        openCVThread = new OpenCVThread(videoView, cvView);
        openCVThread.initOpenCV();
    }

    public void pauseThreads() {

        if(openCVThread != null) {
            openCVThread.interrupt();
            try {
                openCVThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private class DroneStatesAndSettings{

        private float roll, pitch, yaw, gaz;

        DroneStatesAndSettings(){
            update(0, 0, 0);
            this.gaz = 0;
        }

        void update(float roll, float pitch, float yaw){
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
        }

        void turnRight(){
            mSKEModule.setYaw((byte) 50);
        }
        void turnLeft(){
            mSKEModule.setYaw((byte) -50);
        }
        void fixYaw(){
            if(this.yaw != 0) {
                mSKEModule.setYaw((byte) 0);
            }
        }

        void climb(){
            mSKEModule.setGaz((byte) 50);
            this.gaz = 50;
        }
        void descend(){
            mSKEModule.setGaz((byte) -50);
            this.gaz = -50;
        }
        void stabilize(){
            if(this.gaz != 0) {
                mSKEModule.setGaz((byte) 0);
                this.gaz = 0;
            }
        }


    }


    private class CameraStatesAndSettings {

        private float fov, panMax, panMin, tiltMax, tiltMin;
        private float tilt, pan;

        CameraStatesAndSettings(){
            this.fov = 0;
            this.panMin = 0;
            this.panMax = 0;
            this.tiltMin = 0;
            this.tiltMax = 0;
            this.tilt = 0;
            this.pan = 0;
        }

        void set(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
            this.fov = fov;
            this.panMin = panMin;
            this.panMax = panMax;
            this.tiltMin = tiltMin;
            this.tiltMax = tiltMax;
            this.tilt = 0;
            this.pan = 0;
        }

        void set(float tilt, float pan){
            this.tilt = tilt;
            this.pan = pan;
        }

        void moveCamera(float deltaTilt, float deltaPan){
            float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
            float newPan = Math.min(Math.max(pan + deltaPan, panMin), panMax);
            mSKEModule.cameraOrientation(newTilt, newPan);
        }

        void moveCameraTiltBy(float deltaTilt){
            float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
            mSKEModule.cameraOrientation(newTilt, pan);
        }
    }

    private class ColorBlobDetector {
        // Color radius for range checking in HSV color space
        private final Scalar mColorRadius = new Scalar(25,50,50,0);
        // Minimum contour area in percent for contours filtering
        private final double mMinContourArea = 0.1;

        // Lower and Upper bounds for range checking in HSV color space
        private Scalar mLowerBound = new Scalar(0);
        private Scalar mUpperBound = new Scalar(0);
        private List<MatOfPoint> mContours = new ArrayList<>();

        // Cache
        Mat mPyrDownMat = new Mat();
        Mat mHsvMat = new Mat();
        Mat mMask = new Mat();
        Mat mDilatedMask = new Mat();
        Mat mHierarchy = new Mat();

        void setHsvColor(Scalar hsvColor) {
            double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
            double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

            mLowerBound.val[0] = minH;
            mUpperBound.val[0] = maxH;

            mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
            mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

            mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
            mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

            mLowerBound.val[3] = 0;
            mUpperBound.val[3] = 255;
        }

        void process(Mat rgbaImage) {
            Imgproc.pyrDown(rgbaImage, mPyrDownMat);
            Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

            Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

            Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
            Imgproc.dilate(mMask, mDilatedMask, new Mat());

            List<MatOfPoint> contours = new ArrayList<>();

            Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Find max contour area
            double maxArea = 0;
            Iterator<MatOfPoint> each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint wrapper = each.next();
                double area = Imgproc.contourArea(wrapper);
                if (area > maxArea)
                    maxArea = area;
            }
            // Filter contours by area and resize to fit the original image size
            mContours.clear();
            each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint contour = each.next();
                if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                    Core.multiply(contour, new Scalar(4,4), contour);
                    mContours.add(contour);
                }
            }
        }
        List<MatOfPoint> getContours() {
            return mContours;
        }
    }

    private class OpenCVThread extends Thread implements View.OnTouchListener{

        private static final double AREA_THRESHOLD = 0.01;
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime;

        private Context ctx;
        private BaseLoaderCallback mLoaderCallback;
        private BebopVideoView mVideoView;
        private OpenCVView mOpenCVView;

        private final Handler handler;
        boolean interrupted;
        private final Object lock;

        private int rows, cols;
        private Mat mRgba;
        private Mat mGray;

        private Rect object;

        private ColorBlobDetector mDetector;
        private double mFrameArea;
        private Point mFrameCenter;
        private double mDistance;
        private Point pivotCenter;
        private boolean mIsColorSelected;

        private double blobArea;
        private Point blobCenter;

        private OpenCVThread(BebopVideoView videoView, OpenCVView cvView) {
            Log.e("MAMAN", "OPENCV");
            ctx = cvView.getContext();

            mLoaderCallback = new BaseLoaderCallback(ctx) {
                @Override
                public void onManagerConnected(int status) {

                    switch(status){
                        case BaseLoaderCallback.SUCCESS:
                            // Load native library after(!) OpenCV initialization
                            System.loadLibrary("MyModule");
                            Log.e("MAMAN", "OPENCVSTART TREAD");
                            openCVThread.start();
                            break;

                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };
            mVideoView = videoView;
            mOpenCVView = cvView;

            handler = new Handler(ctx.getMainLooper());
            interrupted = false;
            lock = new Object();

            mOpenCVView.setOnTouchListener(this);

        }

        private void initOpenCV(){

            if(OpenCVLoader.initDebug()){
                Log.d(TAG, "OpenCV successfully loaded !");
                Log.e("MAMAN", "OPENCVSTART SUCCCESS LOADED");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }else{
                Log.d(TAG, "OpenCV not loaded !");
                Log.e("MAMAN", "OPENCVSTART NOTLOADED");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, ctx, mLoaderCallback);
            }
        }

        private void init(int height, int width){

            Log.e("MAMAN", "row:"+height);
            Log.e("MAMAN", "cols:"+width);

            rows = height;
            cols = width;
            mRgba = new Mat(height, width, CvType.CV_8UC4);
            mGray = new Mat(height, width, CV_8UC1);

            mFrameArea = cols*rows;
            mFrameCenter = new Point(width/2, height/2);
            mDistance = Math.min(width, height)/5;
            pivotCenter = new Point(0, 0);
            mIsColorSelected = false;

            object = null;
            mDetector = new ColorBlobDetector();

            blobArea = 0;
            blobCenter = new Point(0, 0);

        }

        public void interrupt() {
            interrupted = true;
        }

        private double sqrDistance(Point a, Point b){
            return (a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y);
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            Log.e("TAG", ""+event.getAction());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;

                    // Click event has occurred
                    if(clickDuration < MAX_CLICK_DURATION) {

                        Log.e(TAG, "CLICK");

                        int xOffset = (view.getWidth() - cols) / 2;
                        int yOffset = (view.getHeight() - rows) / 2;

                        int x = (int)event.getX() - xOffset;
                        int y = (int)event.getY() - yOffset;

                        Log.i(TAG, "View: (" + view.getWidth() + ", " + view.getHeight() + ")");
                        Log.i(TAG, "CR: (" + cols + ", " + rows + ")");
                        Log.i(TAG, "OFFSET: (" + xOffset + ", " + yOffset + ")");
                        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

                        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

                        Rect touchedRect = new Rect();
                        touchedRect.x = (x>4) ? x-4 : 0;
                        touchedRect.y = (y>4) ? y-4 : 0;
                        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
                        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

                        Mat touchedRegionRgba = mRgba.submat(touchedRect);
                        Mat touchedRegionHsv = new Mat();
                        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

                        // Calculate average color of touched region
                        Scalar blobColorHsv = Core.sumElems(touchedRegionHsv);
                        int pointCount = touchedRect.width*touchedRect.height;
                        for (int i = 0; i < blobColorHsv.val.length; i++) {
                            blobColorHsv.val[i] /= pointCount;
                        }
                        mDetector.setHsvColor(blobColorHsv);

                        touchedRegionRgba.release();
                        touchedRegionHsv.release();

                        blobCenter.x = x;
                        blobCenter.y = y;
                        mIsColorSelected = true;
                    }
                }
            }
            return false; // don't need subsequent touch events
        }

        @Override
        public void run() {
            Log.e(TAG, "RUN");

            while(mVideoView.getBitmap() == null){
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR while sleeping");
                }
            }

            // Init final parameters for the thread
            init(mVideoView.getBitmap().getHeight(), mVideoView.getBitmap().getWidth());

            while (!interrupted) {
                final Bitmap source = mVideoView.getBitmap();

                if (source != null) {
                    // Get the input frame
                    Utils.bitmapToMat(source, mRgba);

                    // If a color is selected (i.e. the user has click on the screen once)
                    // Then:
                    // - Process the mRgba image to find blobs contours
                    // - Get the contours and find the contours which is the closest of previous found center.
                    // - Compute the approximate area and centroid of this new contour
                    // - Draw them on the frame, and return the frame
                    if (mIsColorSelected) {
                        mDetector.process(mRgba);
                        List<MatOfPoint> contours = mDetector.getContours();

                        // If no contour ask the user to tap a new color pixel
                        if (contours.size() < 1) {
                            mIsColorSelected = false;
                        } else {
                            double d = Double.POSITIVE_INFINITY;
                            int i = 0;
                            double x = 0, y = 0;
                            // For each found contour, compute the centroid and keep the closest from previous
                            // frame contour
                            for (int j = 0; j < contours.size(); j++) {

                                Moments M = Imgproc.moments(contours.get(j));
                                pivotCenter.x = (M.get_m10() / M.get_m00());
                                pivotCenter.y = (M.get_m01() / M.get_m00());

                                double dd = sqrDistance(pivotCenter, blobCenter);
                                if (dd < d) {
                                    d = dd;
                                    i = j;
                                    x = pivotCenter.x;
                                    y = pivotCenter.y;
                                }
                            }
                            blobCenter.x = x;
                            blobCenter.y = y;

                            //blobArea = Imgproc.contourArea(contours.get(i));
                            object = Imgproc.boundingRect(contours.get(i));
                            blobArea = object.area();

                            //Imgproc.drawContours(mRgba, contours, i, CONTOUR_COLOR);
                            //Imgproc.circle(mRgba, blobCenter, 11, CONTOUR_COLOR, -1);
                        }

                        double deltaX = mFrameCenter.x - blobCenter.x;
                        double deltaY = mFrameCenter.y - blobCenter.y;

                        // apply correction
                        if (blobArea / mFrameArea < AREA_THRESHOLD) {
                            Log.e(TAG, "Become Closer");
                        }
                        if (Math.abs(deltaX) > mDistance) {
                            if(deltaX > 0) {
                                Log.e(TAG, "Correct x right");
                                droneSettings.turnRight();
                                //droneSettings.turnLeft();
                            }else{
                                Log.e(TAG, "Correct x left");
                                droneSettings.turnLeft();
                                //droneSettings.turnRight();
                            }
                        }
                        else{
                            droneSettings.fixYaw();
                        }
                        if (Math.abs(deltaY) > mDistance) {
                            if(deltaY > 0) {
                                Log.e(TAG, "Correct y DOWN");
                                cameraSettings.moveCameraTiltBy(5);
                            }
                            else{
                                Log.e(TAG, "Correct y UP");
                                cameraSettings.moveCameraTiltBy(-5);
                            }
                        }
                    }


                    // TODO be sure of this synchronized call see in the draw of opencvview if correct
                    synchronized (lock) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOpenCVView.setObject(object, blobCenter);
                                mOpenCVView.invalidate();
                            }
                        });
                    }
                }

                try {
                    sleep(10);
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

}
