package ch.epfl.droneproject.module;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.arsdk.arcommands.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.UByteBufferIndexer;
import org.bytedeco.javacpp.opencv_objdetect;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.OpenCVView;


public class AutoPilotModule {

    private static final String TAG = "AutoPilot";

    // Is the Autopilot engaged
    private boolean isEngaged;
    private boolean isInFlightPlan;

    // Controller extension module
    private SkyControllerExtensionModule mSKEModule;
    // Flight planer extension module
    private FlightPlanerModule mFlightPlanerModule;

    // Drone current states
    private DroneStatesSettingsProceduresModule droneSettings;
    // Camera current states


    // The opencv thread
    private OpenCVThread openCVThread;


    public AutoPilotModule(SkyControllerExtensionModule skeModule) {

        //this.isEngaged = true;
        this.isEngaged = false;
        this.isInFlightPlan = false;

        this.mSKEModule = skeModule;
        this.mFlightPlanerModule = new FlightPlanerModule();

        this.droneSettings = new DroneStatesSettingsProceduresModule(skeModule);

        this.openCVThread = null;
    }

    public boolean isEngaged(){
        return this.isEngaged;
    }

    public void engage(){
        this.isEngaged = true;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_CONTROLLER);
        int b1 = 1;
        b1 = b1 | (1 << 1);
        b1 = b1 | (1 << 2);
        b1 = b1 | (1 << 3);
        mSKEModule.grabAxis(0, b1);
        if(this.isInFlightPlan){
            startAutoFlightPlan();
        }
    }

    public void disengage(){
        this.isEngaged = false;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_SKYCONTROLLER);
        mSKEModule.grabAxis(0, 0);
        if(this.isInFlightPlan){
            pauseAutoFlightPlan();
        }
    }

    public void startMission(){
        if(this.isEngaged) {
            this.droneSettings.startMission();
        }
    }

    public void becomeCloser(){
        if(this.isEngaged) {
            this.droneSettings.becomeCloser();
        }
    }
    public void stopBecomeCloser(){
        if(this.isEngaged) {
            this.droneSettings.stopBecomeCloser();
        }
    }


    public DroneStatesSettingsProceduresModule getDroneSettings() {
        return droneSettings;
    }

    public FlightPlanerModule getFlightPlanerModule() {
        return mFlightPlanerModule;
    }
    public void startAutoFlightPlan(){
        if(this.isEngaged) {
            this.isInFlightPlan = true;
            this.mSKEModule.startFlightPlan(mFlightPlanerModule);
        }
    }
    public void pauseAutoFlightPlan(){  this.mSKEModule.pauseFlightPlan();}
    public void stopAutoFlightPlan(){
        this.isInFlightPlan = false;
        this.mSKEModule.stopFlightPlan();
    }

    public void setCameraInfo(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
        droneSettings.setCameraSettings(fov, panMin, panMax, tiltMin, tiltMax);
    }

    public void setCameraSettings(float tilt, float pan){
        droneSettings.setCameraSettings(tilt, pan);
    }

    public void updateDroneSettings(float roll, float pitch, float yaw){
        droneSettings.update(roll, pitch, yaw);
        mFlightPlanerModule.updateDroneOrientation(yaw);
    }
    public void updateDroneSettings(double lat, double lon, double alt){
        droneSettings.update(lat, lon, alt);
        mFlightPlanerModule.updateDronePosition(lat, lon, alt);
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

    private class FaceDetector{

        private opencv_objdetect.CvHaarClassifierCascade classifier;
        private List<CvRect> mContours;
        private int height;
        private int width;
        IplImage grayImage;

        CvMemStorage storage;

        public FaceDetector(int width, int height) {
            this.mContours = new ArrayList<>();
            this.width = width;
            this.height = height;

            try {
                /*
                URL url = new URL("https://raw.github.com/Itseez/opencv/2.4.0/data/haarcascades/haarcascade_frontalface_alt.xml");
                File classifierFile = Loader.extractResource(url, null, "classifier", ".xml");
                classifierFile.deleteOnExit();
                */

                // Load the classifier file from Java resources.
                File classifierFile = Loader.extractResource(getClass(),
                        "/res/raw/haarcascade_frontalface_alt_old.xml",
                        DroneApplication.getApplication().getContext().getCacheDir(), "classifier", ".xml");
                classifierFile.deleteOnExit();
                if (classifierFile == null || classifierFile.length() <= 0) {
                    throw new IOException("Could not extract the classifier file from Java resource.");
                }
                Log.e(TAG, classifierFile.getAbsolutePath());
                // Preload the opencv_objdetect module to work around a known bug.
                Loader.load(opencv_objdetect.class);

                classifier = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
                //classifier = new opencv_objdetect.CvHaarClassifierCascade();
                //classifierFile.delete();
                if (classifier.isNull()) {
                    throw new IOException("Could not load the classifier file.");
                }

                grayImage = IplImage.create(width, height, IPL_DEPTH_8U, 1);
                // Objects allocated with a create*() or clone() factory method are automatically released
                // by the garbage collector, but may still be explicitly released by calling release().
                // You shall NOT call cvReleaseImage(), cvReleaseMemStorage(), etc. on objects allocated this way.
                storage = CvMemStorage.create();


            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
            }
        }

        void destroy(){
            cvReleaseImage(grayImage);
        }

        void process(IplImage rgbaImage) {
            //cvClearMemStorage(storage);
            Log.e(TAG, "LOL");
            // Let's try to detect some faces! but we need a grayscale image...
            cvCvtColor(rgbaImage, grayImage, COLOR_RGB2GRAY);
            //cvCvtColor(rgbaImage, grayImage, CV_RGBA2GRAY);
            //cvCvtColor(rgbaImage, grayImage, CV_BGR2GRAY);
            CvSeq faces = cvHaarDetectObjects(grayImage, classifier, storage,1.1, 3, CV_HAAR_FIND_BIGGEST_OBJECT | CV_HAAR_DO_ROUGH_SEARCH);
            int total = faces.total();
            mContours.clear();
            for (int i = 0; i < total; i++) {
                mContours.add(new CvRect(cvGetSeqElem(faces, i)));
            }
        }
        List<CvRect> getContours() {
            return mContours;
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

            mPyrDown2Mat = IplImage.create(width/2, height/2, IPL_DEPTH_8U, 4);
            mPyrDown4Mat = IplImage.create(width/4, height/4, IPL_DEPTH_8U, 4);
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

        private IplImage grabbedImage;

        private ColorBlobDetector mBlobDetector;
        private FaceDetector mFaceDetector;
        private int rows, cols;
        private int x1, y1, x2, y2;

        private double mFrameArea, mDistance, blobArea;
        private Point mFrameCenter, pivotCenter, blobCenter;
        private boolean mIsBlobFound, mIsFaceFound, mSearchBlob, mSearchFace;


        private OpenCVThread(BebopVideoView videoView, OpenCVView cvView) {

            ctx = cvView.getContext();

            System.loadLibrary("opencv_core");
            System.loadLibrary("opencv_imgproc");
            System.loadLibrary("jniopencv_core");
            // Preload the opencv_objdetect module to work around a known bug.
            Loader.load(opencv_objdetect.class);

            mVideoView = videoView;
            mOpenCVView = cvView;

            handler = new Handler(ctx.getMainLooper());
            interrupted = false;
            lock = new Object();

            mOpenCVView.setOnTouchListener(this);
        }

        private void init(int height, int width){

            Log.e(TAG, "Init("+height+", "+width+")");

            rows = height;
            cols = width;

            grabbedImage = IplImage.create(width, height, IPL_DEPTH_8U, 4);

            mFrameArea = cols*rows;
            mFrameCenter = new Point(width/2, height/2);
            mDistance = Math.min(width, height)/3;
            pivotCenter = new Point(0, 0);
            mIsBlobFound = false;
            mIsFaceFound = false;
            mSearchBlob = true;
            mSearchFace = false;
            mBlobDetector = new ColorBlobDetector(width, height);
            mFaceDetector = new FaceDetector(width, height);
            blobCenter = new Point(0, 0);
        }


        public void interrupt() {
            interrupted = true;
        }

        /**
         * Compute square distance between two points
         * This save a sqrt computation time
         * @param a (Point): First point
         * @param b (Point): Second point
         * @return (double) the corresponding sqr distance
         */
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

                        // Stop here if the touch is outside the view
                        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

                        calibrate(x, y);

                        // Display some information after calibration
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

            // Check if ww can calibrate or not
            if(rows == 0 || cols == 0){
                Log.e(TAG, "Nothing to calibrate on");
                return;
            }
            Log.i(TAG, "Calibrate");

            // First create a square representing the touched region. This square is 8x8 pixel
            int halfRegionSide = 4;

            // Create the square or rectangle dimension in border cases
            int recX = (x>halfRegionSide) ? x-halfRegionSide : 0;
            int recY = (y>halfRegionSide) ? y-halfRegionSide : 0;
            int recW = (x+halfRegionSide < cols) ? x + halfRegionSide - recX : cols - recX;
            int recH = (y+halfRegionSide < rows) ? y + halfRegionSide - recY : rows - recY;
            CvRect r = new CvRect(recX, recY, recW, recH);

            // Croop the original image by Copy original image (only ROI) to the cropped image
            cvSetImageROI(grabbedImage, r);
            IplImage touchedRegionRgba = cvCreateImage(cvGetSize(grabbedImage), grabbedImage.depth(), grabbedImage.nChannels());
            cvCopy(grabbedImage, touchedRegionRgba);

            IplImage touchedRegionHsv = cvCreateImage(cvGetSize(touchedRegionRgba), touchedRegionRgba.depth(), touchedRegionRgba.nChannels()-1);
            cvCvtColor(touchedRegionRgba, touchedRegionHsv, COLOR_RGB2HSV_FULL);

            cvResetImageROI(grabbedImage);

            // Create an indexer on touchedRegionHsv to easily get the pixel values
            UByteBufferIndexer idx = touchedRegionHsv.createIndexer();

            // Find the mean of each HSV channels in the touched region
            double H = 0;
            double S = 0;
            double V = 0;

            for(int i = 0; i < recW; i++){
                for(int j = 0; j < recH; j++){
                    //Log.d(TAG, i+", "+j+", "+idx.get(i, j, 0)+", "+idx.get(i, j, 1)+", "+idx.get(i, j, 2));
                    H += idx.get(i, j, 0);
                    S += idx.get(i, j, 1);
                    V += idx.get(i, j, 2);
                }
            }

            int pointCount = recW*recH;
            H /= pointCount;
            S /= pointCount;
            V /= pointCount;

            // Set the new value after calibration
            mBlobDetector.setHsvColor(H, S, V);
            blobCenter.x(x);
            blobCenter.y(y);
            mSearchBlob = true;
            mSearchFace = false;
        }

        @Override
        public void run() {

            Log.e(TAG, "RUN");

            // Sleep until first bitmap image available :
            // TODO find a better way to not have to wait
            while(mVideoView.getBitmap() == null){
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR while sleeping");
                }
            }

            // Init final parameters for the thread from the image dimension
            init(mVideoView.getBitmap().getHeight(), mVideoView.getBitmap().getWidth());


            CvMemStorage storage = CvMemStorage.create();
            List<CvRect> contours;
            List<CvRect> faceContours;

            while (!interrupted) {
                cvClearMemStorage(storage);
                final Bitmap source = mVideoView.getBitmap();

                if (source != null) {
                    // Get the input frame
                    source.copyPixelsToBuffer(grabbedImage.createBuffer());

                    // If a color is selected (i.e. the user has click on the screen once)
                    // Then:
                    // - Process the mRgba image to find blobs contours
                    // - Get the contours and find the contours which is the closest of previous found center.
                    // - Compute the approximate area and centroid of this new contour
                    // - Draw them on the frame, and return the frame
                    if (mSearchBlob || mSearchFace) {

                        if(mSearchFace){
                            mFaceDetector.process(grabbedImage);
                            contours = mFaceDetector.getContours();
                            mIsFaceFound = contours.size() > 0;
                            mIsBlobFound = false;
                        }
                        else{
                            mBlobDetector.process(grabbedImage);
                            contours = mBlobDetector.getContours();
                            mIsFaceFound = false;
                            mIsBlobFound = contours.size() > 0;
                        }

                        // If no contour ask the user to tap a new color pixel
                        if(mIsBlobFound || mIsFaceFound){
                            double d = Double.POSITIVE_INFINITY;
                            int x = 0, y = 0, w = 0, h = 0;

                            // For each found contour, compute the centroid and keep the closest from previous
                            // frame contour
                            for (int j = 0; j < contours.size(); j++) {

                                CvRect rect = contours.get(j);
                                pivotCenter.x(rect.x() + rect.width() / 2);
                                pivotCenter.y(rect.y() + rect.height() / 2);

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
                            blobArea = w * h;

                            x1 = x - w / 2;
                            x2 = x + w / 2;
                            y1 = y - h / 2;
                            y2 = y + h / 2;

                            mSearchFace = mIsFaceFound || blobArea / mFrameArea > AREA_THRESHOLD;
                        }

                        if(isEngaged) {
                            double deltaX = mFrameCenter.x() - blobCenter.x();
                            double deltaY = mFrameCenter.y() - blobCenter.y();

                            // apply correction
                            if (!mSearchFace) {
                                Log.e(TAG, "Become closer for better face detection: Press the button on screen");
                            }

                            if (Math.abs(deltaX) > mDistance) {

                                if (deltaX > 0) {
                                    Log.e(TAG, "Correct x right");
                                    //droneSettings.turnRight();
                                    droneSettings.turnLeft();
                                } else {
                                    Log.e(TAG, "Correct x left");
                                    //droneSettings.turnLeft();
                                    droneSettings.turnRight();
                                }
                            } else {
                                droneSettings.fixYaw();
                            }
                            if (Math.abs(deltaY) > mDistance) {
                                if (deltaY > 0) {
                                    Log.e(TAG, "Correct y DOWN");
                                    droneSettings.moveCameraTiltBy(5);
                                } else {
                                    Log.e(TAG, "Correct y UP");
                                    droneSettings.moveCameraTiltBy(-5);
                                }
                            }
                        }
                    }
                }
                synchronized (lock) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOpenCVView.setColor(mIsBlobFound ? OpenCVView.BLOB_RECT_COLOR : OpenCVView.FACE_RECT_COLOR);
                            mOpenCVView.setRect(x1, y1, x2, y2);
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

            mBlobDetector.destroy();
            cvReleaseImage(grabbedImage);
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }

}
