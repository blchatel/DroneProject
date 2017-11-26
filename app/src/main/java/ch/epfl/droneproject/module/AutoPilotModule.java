package ch.epfl.droneproject.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.arsdk.arcommands.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.OpenCVView;



public class AutoPilotModule {

    private static final String TAG = "AutoPilot";

    // Is the Autopilot engaged
    private boolean isEngaged;

    // Controller extension module
    private SkyControllerExtensionModule mSKEModule;
    // Flight planer extension module
    private FlightPlanerModule mFlightPlanerModule;

    // Drone current states
    private DroneStatesAndSettings droneSettings;
    // Camera current states
    private CameraStatesAndSettings cameraSettings;

    // The opencv thread
    private OpenCVThread openCVThread;


    public AutoPilotModule(SkyControllerExtensionModule skeModule) {

        //this.isEngaged = true;
        this.isEngaged = false;

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
        private final double mMinContourArea = 60;

        // Lower and Upper bounds for range checking in HSV color space

        private List<CvRect> mContours;
        private CvScalar mLB;
        private CvScalar mUB;

        // Cache
        IplImage mPyrDown2Mat;
        IplImage mPyrDown4Mat;
        IplImage mHsvMat;
        IplImage mMask;
        IplImage mDilatedMask;

        CvMemStorage storage;

        ColorBlobDetector(int width, int height){
            mContours = new ArrayList<>();
            mLB = new CvScalar(0);
            mUB = new CvScalar(0);

            mPyrDown2Mat = IplImage.create(width/2, height/2, IPL_DEPTH_8U, 3);
            mPyrDown4Mat = IplImage.create(width/4, height/4, IPL_DEPTH_8U, 3);
            mHsvMat = IplImage.create(width/4, height/4, IPL_DEPTH_8U, 3);
            mMask = IplImage.create(width/4, height/4, IPL_DEPTH_8U, 1);
            mDilatedMask = IplImage.create(width/4, height/4, IPL_DEPTH_8U, 1);

            storage = CvMemStorage.create();
        }

        void destroy(){
            cvReleaseImage(mPyrDown2Mat);
            cvReleaseImage(mPyrDown4Mat);
            cvReleaseImage(mHsvMat);
            cvReleaseImage(mMask);
            cvReleaseImage(mDilatedMask);
        }


        void setHsvColor(double H, double S, double V) {

            //System.out.println("H="+H+", S="+S+", V="+V);

            double mLB0 = (H >= mColorRadius.blue()) ? H-mColorRadius.blue() : 0;
            double mUB0 = (H+mColorRadius.blue() <= 255) ? H+mColorRadius.blue() : 255;

            double mLB1 = S - mColorRadius.val(1);
            double mUB1 =  S + mColorRadius.val(1);

            double mLB2 = V - mColorRadius.val(2);
            double mUB2 = V + mColorRadius.val(2);

            double mLB3 = 0;
            double mUB3 = 255;

            mLB = new CvScalar(mLB0, mLB1, mLB2, mLB3);
            mUB = new CvScalar(mUB0, mUB1, mUB2, mUB3);
        }


        void process(IplImage rgbaImage) {

            //Smoothes the input image with gaussian kernel and then down-samples it.
            cvPyrDown(rgbaImage, mPyrDown2Mat);
            cvPyrDown(mPyrDown2Mat, mPyrDown4Mat);

            //Converts input Mat pixels from one color space to another
            cvCvtColor(mPyrDown4Mat, mHsvMat, COLOR_RGB2HSV_FULL);

            cvInRangeS(mHsvMat, mLB, mUB, mMask);
            cvDilate(mMask, mDilatedMask);

            CvSeq contour = new CvSeq(null);
            //cvFindContours(mDilatedMask, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
            cvFindContours(mDilatedMask, storage, contour, Loader.sizeof(CvContour.class), RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);

            mContours.clear();

            // To check if an output argument is null we may call either isNull() or equals(null).
            while (contour != null && !contour.isNull()) {
                if (contour.elem_size() > 0) {
                    CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                    if (cvContourArea(points, CV_WHOLE_SEQ, 0) > mMinContourArea){
                        CvRect rect = cvBoundingRect(points);
                        mContours.add(new CvRect(rect.x()*4, rect.y()*4, rect.width()*4, rect.height()*4));
                    }
                }
                contour = contour.h_next();
            }

        }

        List<CvRect> getContours() {
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

        IplImage grabbedImage;

        private int rows, cols;
        private ColorBlobDetector mDetector;
        int x1, y1, x2, y2;

        private double mFrameArea, mDistance, blobArea;
        private Point mFrameCenter, pivotCenter, blobCenter;
        private boolean mIsColorSelected;


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

            grabbedImage = IplImage.create(width, height, IPL_DEPTH_8U, 4);

            mFrameArea = cols*rows;
            mFrameCenter = new Point(width/2, height/2);
            mDistance = Math.min(width, height)/5;
            pivotCenter = new Point(0, 0);
            mIsColorSelected = false;
            mDetector = new ColorBlobDetector(width, height);
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

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;

                    // Click event has occurred
                    if(clickDuration < MAX_CLICK_DURATION) {

                        int xOffset = (view.getWidth() - cols) / 2;
                        int yOffset = (view.getHeight() - rows) / 2;

                        int x = (int)event.getX() - xOffset;
                        int y = (int)event.getY() - yOffset;

                        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

                        calibrate(x, y);

                        Log.i(TAG, "View: (" + view.getWidth() + ", " + view.getHeight() + ")");
                        Log.i(TAG, "CR: (" + cols + ", " + rows + ")");
                        Log.i(TAG, "OFFSET: (" + xOffset + ", " + yOffset + ")");
                        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
                    }
                }
            }
            return false; // don't need subsequent touch events
        }

        private void calibrate(int x, int y){

            System.out.println("Calibrate");

            int halfRegionSide = 4;

            int recX = (x>halfRegionSide) ? x-halfRegionSide : 0;
            int recY = (y>halfRegionSide) ? y-halfRegionSide : 0;
            int recW = (x+halfRegionSide < cols) ? x + halfRegionSide - recX : cols - recX;
            int recH = (y+halfRegionSide < rows) ? y + halfRegionSide - recY : rows - recY;

            CvRect r = new CvRect(recX, recY, recW, recH);

            cvSetImageROI(grabbedImage, r);
            IplImage touchedRegionRgba = cvCreateImage(cvGetSize(grabbedImage), grabbedImage.depth(), grabbedImage.nChannels());
            // Copy original image (only ROI) to the cropped image
            cvCopy(grabbedImage, touchedRegionRgba);

            IplImage touchedRegionHsv = cvCreateImage(cvGetSize(grabbedImage), grabbedImage.depth(), grabbedImage.nChannels());
            cvCvtColor(touchedRegionRgba, touchedRegionHsv, COLOR_RGB2HSV_FULL);

            UByteRawIndexer idx = touchedRegionHsv.createIndexer();

            double H = 0;
            double S = 0;
            double V = 0;

            for(int i = 0; i < halfRegionSide; i++){
                for(int j = 0; j < halfRegionSide; j++){
                    System.out.println(i+", "+j+", "+idx.get(i, j, 0)+", "+idx.get(i, j, 1)+", "+idx.get(i, j, 2));
                    H += idx.get(i, j, 0);
                    S += idx.get(i, j, 1);
                    V += idx.get(i, j, 2);
                }
            }

            int pointCount = recW*recH;
            H /= pointCount;
            S /= pointCount;
            V /= pointCount;

            mDetector.setHsvColor(H, S, V);

            blobCenter.x(x);
            blobCenter.y(y);
            mIsColorSelected = true;

            cvResetImageROI(grabbedImage);
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

            CvMemStorage storage = CvMemStorage.create();
            List<CvRect> contours;

            while (!interrupted) {
                cvClearMemStorage(storage);

                final Bitmap source = mVideoView.getBitmap();

                // TODO convert Bitmap -> IplImage


                if (mIsColorSelected) {

                    mDetector.process(grabbedImage);
                    contours = mDetector.getContours();

                    // If no contour ask the user to tap a new color pixel
                    if (contours.size() < 1) {
                        mIsColorSelected = false;
                    } else {

                        double d = Double.POSITIVE_INFINITY;
                        int x=0, y=0, w=0, h=0;

                        // For each found contour, compute the centroid and keep the closest from previous
                        // frame contour
                        for (int j = 0; j < contours.size(); j++) {

                            CvRect rect = contours.get(j);
                            pivotCenter.x(rect.x()+rect.width()/2);
                            pivotCenter.y(rect.y()+rect.height()/2);

                            double dd = sqrDistance(pivotCenter, blobCenter);
                            if (dd < d) {
                                d = dd;
                                x = pivotCenter.x();
                                y = pivotCenter.y();
                                w = rect.width();
                                h = rect.height();
                            }
                        }

                        blobCenter.x(x);
                        blobCenter.y(y);
                        blobArea = w*h;

                        x1 = x-w/2;
                        x2 = x+w/2;
                        y1 = y-h/2;
                        y2 = y+h/2;
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
                            mOpenCVView.setObject(x1, y1, x2, y2);
                            mOpenCVView.invalidate();
                        }
                    });
                }
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    interrupted = true;
                }

            }

            mDetector.destroy();
            cvReleaseImage(grabbedImage);
        }

        
        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }

}
