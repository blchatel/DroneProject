package ch.epfl.droneproject.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.arsdk.arcommands.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.OpenCVView;



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
        openCVThread = new OpenCVThread(videoView, cvView);
        openCVThread.start();
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
            if(isEngaged)
              mSKEModule.setYaw((byte) 50);
        }
        void turnLeft(){
            if(isEngaged)
                mSKEModule.setYaw((byte) -50);
        }
        void fixYaw(){

            if(isEngaged && this.yaw != 0) {
                mSKEModule.setYaw((byte) 0);
            }
        }

        void climb(){
            if(isEngaged) {
                mSKEModule.setGaz((byte) 50);
                this.gaz = 50;
            }
        }
        void descend(){
            if(isEngaged) {
                mSKEModule.setGaz((byte) -50);
                this.gaz = -50;
            }
        }
        void stabilize(){
            if(isEngaged && this.gaz != 0) {
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

        void moveCamera(float deltaTilt, float deltaPan) {
            if (isEngaged){
                float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
                float newPan = Math.min(Math.max(pan + deltaPan, panMin), panMax);
                mSKEModule.cameraOrientation(newTilt, newPan);
            }
        }

        void moveCameraTiltBy(float deltaTilt){
            if(isEngaged){
                float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
                mSKEModule.cameraOrientation(newTilt, pan);
            }
        }
    }

    private class ColorBlobDetector {
        // Color radius for range checking in HSV color space
        private final CvScalar mColorRadius = new CvScalar(25,50,50,0);
        // Minimum contour area in percent for contours filtering
        private final double mMinContourArea = 0.1;

        // Lower and Upper bounds for range checking in HSV color space

        private List<Mat> mContours = new ArrayList<>();
        private DoublePointer mLB = new DoublePointer(0);
        private DoublePointer mUB = new DoublePointer(0);

        // Cache
        Mat mPyrDownMat = new Mat();
        Mat mHsvMat = new Mat();
        Mat mMask = new Mat();
        Mat mDilatedMask = new Mat();
        Mat mHierarchy = new Mat();

        void setHsvColor(Scalar hsvColor) {

            double minH = (hsvColor.blue() >= mColorRadius.blue()) ? hsvColor.blue()-mColorRadius.blue() : 0;
            double maxH = (hsvColor.blue()+mColorRadius.blue() <= 255) ? hsvColor.blue()+mColorRadius.blue() : 255;

            double mLB0 = minH;
            double mUB0 = maxH;

            double mLB1 = hsvColor.green() - mColorRadius.val(1);
            double mUB1 =  hsvColor.green() + mColorRadius.val(1);

            double mLB2 = hsvColor.red() - mColorRadius.val(2);
            double mUB2 = hsvColor.red() + mColorRadius.val(2);

            double mLB3 = 0;
            double mUB3 = 255;

            mLB = new DoublePointer(mLB0, mLB1, mLB2, mLB3);
            mUB = new DoublePointer(mUB0, mUB1, mUB2, mUB3);
        }

        void process(Mat rgbaImage) {
            //Smoothes the input image with gaussian kernel and then down-samples it.
            pyrDown(rgbaImage, mPyrDownMat);
            pyrDown(mPyrDownMat, mPyrDownMat);

            //Converts input Mat pixels from one color space to another
            cvtColor(mPyrDownMat, mHsvMat, COLOR_RGB2HSV_FULL);

            inRange(mHsvMat, new Mat(1, 1, CV_32SC4, mLB, 0), new Mat(1, 1, CV_32SC4, mUB, 0), mMask);
            dilate(mMask, mDilatedMask, new Mat());

            MatVector contours = new MatVector();
            findContours(mDilatedMask, contours, mHierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

            // Find max contour area
            double maxArea = 0;

            Log.e("Process", "ContourSize: "+contours.size());

            // Filter contours by area and resize to fit the original image size
            mContours.clear();

            for(int i = 0; i < contours.size(); i++){
                Mat wrapper = contours.get(i);
                double area = contourArea(wrapper);
                Log.e("Process", "ContourSize: "+area);

                if (area > maxArea)
                    maxArea = area;
            }

            for(int i = 0; i < contours.size(); i++){
                Mat contour = contours.get(i);
                if (contourArea(contour) > mMinContourArea*maxArea) {
                    Log.e("Process", "Multiply and add");
                    multiply(contour, new Mat(4, 4, 0, 0), contour);
                    mContours.add(contour);
                }
            }
        }

        List<Mat> getContours() {
            return mContours;
        }
    }

    private class OpenCVThread extends Thread implements View.OnTouchListener{

        private static final double AREA_THRESHOLD = 0.01;
        private static final int MAX_CLICK_DURATION = 200;
        private long startClickTime;

        private Context ctx;
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
            ctx = cvView.getContext();

            System.loadLibrary("opencv_core");
            System.loadLibrary("opencv_imgproc");
            System.loadLibrary("jniopencv_core");

            mVideoView = videoView;
            mOpenCVView = cvView;

            handler = new Handler(ctx.getMainLooper());
            interrupted = false;
            lock = new Object();

            mOpenCVView.setOnTouchListener(this);
        }

        private void init(int height, int width){

            rows = height;
            cols = width;

            mRgba = new Mat(height, width, CV_8UC4);
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
            return (a.x()-b.x())*(a.x()-b.x()) + (a.y()-b.y())*(a.y()-b.y());
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            Log.e(TAG, ""+event.getAction());

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

                        int recX = (x>4) ? x-4 : 0;
                        int recY = (y>4) ? y-4 : 0;
                        int recW = (x+4 < cols) ? x + 4 - recX : cols - recX;
                        int recH = (y+4 < rows) ? y + 4 - recY : rows - recY;

                        Mat touchedRegionRgba = mRgba.rowRange(recY, recY+recH);
                        touchedRegionRgba = touchedRegionRgba.colRange(recX, recX+recW);

                        Mat touchedRegionHsv = new Mat();
                        cvtColor(touchedRegionRgba, touchedRegionHsv, COLOR_RGB2HSV_FULL);

                        // Calculate average color of touched region
                        Scalar blobColorHsv = sumElems(touchedRegionHsv);

                        int pointCount = recW*recH;
                        blobColorHsv.red(blobColorHsv.red()/pointCount);
                        blobColorHsv.green(blobColorHsv.green()/pointCount);
                        blobColorHsv.blue(blobColorHsv.blue()/pointCount);

                        mDetector.setHsvColor(blobColorHsv);

                        touchedRegionRgba.release();
                        touchedRegionHsv.release();

                        blobCenter.x(x);
                        blobCenter.y(y);
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
                    sleep(50);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR while sleeping");
                }
            }

            // Init final parameters for the thread
            init(mVideoView.getBitmap().getHeight(), mVideoView.getBitmap().getWidth());

            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();


            while (!interrupted) {
                final Bitmap source = mVideoView.getBitmap();

                if (source != null) {
                    // Get the input frame
                    mRgba = converterToMat.convert(converterToBitmap.convert(source));

                    // If a color is selected (i.e. the user has click on the screen once)
                    // Then:
                    // - Process the mRgba image to find blobs contours
                    // - Get the contours and find the contours which is the closest of previous found center.
                    // - Compute the approximate area and centroid of this new contour
                    // - Draw them on the frame, and return the frame
                    if (mIsColorSelected) {
                        mDetector.process(mRgba);

                        List<Mat> contours = mDetector.getContours();

                        Log.e("Object", ""+contours.size());

                        // If no contour ask the user to tap a new color pixel
                        if (contours.size() < 1) {
                            mIsColorSelected = false;
                        } else {
                            double d = Double.POSITIVE_INFINITY;
                            int i = 0;
                            int x = 0, y = 0;
                            // For each found contour, compute the centroid and keep the closest from previous
                            // frame contour
                            for (int j = 0; j < contours.size(); j++) {

                                Moments M = moments(contours.get(j));
                                pivotCenter.x((int)(M.m10() / M.m00()));
                                pivotCenter.y((int)(M.m01() / M.m00()));

                                double dd = sqrDistance(pivotCenter, blobCenter);
                                if (dd < d) {
                                    d = dd;
                                    i = j;
                                    x = pivotCenter.x();
                                    y = pivotCenter.y();
                                }
                            }
                            blobCenter.x(x);
                            blobCenter.y(y);

                            //blobArea = Imgproc.contourArea(contours.get(i));
                            Log.e("Object", "Cont: "+contours.get(i));
                            object = boundingRect(contours.get(i));
                            blobArea = object.area();
                        }

                        double deltaX = mFrameCenter.x() - blobCenter.x();
                        double deltaY = mFrameCenter.y() - blobCenter.y();

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
                                if(object != null)
                                    Log.e("Object", "Object: "+object.x() +", "+ object.y() +", "+object.height()+", "+object.width());
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
