package ch.epfl.droneproject.module;

import android.annotation.SuppressLint;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.OpenCVView;

/**
 * AutoPilotModule.java
 * @author blchatel
 * Autopilot for a Bebop 2 Drone of parrot. This Autopilot can be engaged or disengaged. It deal with:
 * @see FlightPlanerModule: Mavlink flight plan  and with Iterative procedural Mission
 * @see DroneStatesSettingsProceduresModule.Mission: Iterative procedural Mission and
 * @see SkyControllerExtensionModule: can also send directly order to the drone
 *
 * This file contains some private classes:
 * @see ColorBlobDetector
 * @see FaceDetector
 * @see OpenCVThread
 */
public class AutoPilotModule {

    private static final String TAG = "AutoPilot";

    // Is the Autopilot engaged
    private boolean isEngaged;
    private boolean isInFlightPlan;
    private boolean isInMission;

    // Controller extension module
    private SkyControllerExtensionModule mSKEModule;
    // Flight planer extension module
    private FlightPlanerModule mFlightPlanerModule;

    // Drone current states
    private DroneStatesSettingsProceduresModule droneSettings;
    // Camera current states


    // The opencv thread
    private OpenCVThread openCVThread;


    /**
     * Default Constructor
     * @param skeModule (SkyControllerExtensionModule)
     */
    public AutoPilotModule(SkyControllerExtensionModule skeModule) {

        this.isEngaged = false;
        this.isInFlightPlan = false;
        this.isInMission = false;

        this.mSKEModule = skeModule;
        this.mFlightPlanerModule = new FlightPlanerModule();
        this.droneSettings = new DroneStatesSettingsProceduresModule(skeModule, this);
        this.openCVThread = null;
    }

    /**
     * Getter for the current state of the autopilot
     * @return (boolean): true if the autopilot is engaged, and false otherwise
     */
    public boolean isEngaged(){
        return this.isEngaged;
    }

    /**
     * Getter for droneSettings
     * @return droneSettings (DroneStatesSettingsProceduresModule): the drone settings
     */
    public DroneStatesSettingsProceduresModule getDroneSettings() {
        return droneSettings;
    }

    /**
     * Getter for the flight planner
     * @return mFlightPlanerModule (FlightPlanerModule): the flight planner
     */
    public FlightPlanerModule getFlightPlanerModule() {
        return mFlightPlanerModule;
    }

    /**
     * Engage the autopilot by giving full control to the application (i.e SkyController2 loose control)
     * This method put a "listener" on SkyController axis (using grabAxis) to allow automatic disengage
     * of the autopilot on new axis input (i.e. the operator can get back the controls at any time)
     * If the drone is in Mission or in FlightPlan, the engage start/continue it/them.
     */
    public void engage(){
        this.isEngaged = true;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_CONTROLLER);
        int b1 = 1;
        b1 = b1 | (1 << 1);
        b1 = b1 | (1 << 2);
        b1 = b1 | (1 << 3);
        mSKEModule.grabAxis(b1);
        if(this.isInFlightPlan){
            startAutoFlightPlan(); // continue the flight plan
        }
        if(this.isInMission){
            startMission(); // continue the mission
        }
    }

    /**
     * Disengage the autopilot by giving back full control to the operator (i.e. SkyController2 has full control)
     * This method remove the listener on the axis @see engage
     * If the drone is in FlightPlan or Mission, this disengage pause it/them
     */
    public void disengage(){
        this.isEngaged = false;
        mSKEModule.setController(ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_ENUM.ARCOMMANDS_SKYCONTROLLER_COPILOTING_SETPILOTINGSOURCE_SOURCE_SKYCONTROLLER);
        mSKEModule.grabAxis(0);
        if(this.isInFlightPlan){
            pauseAutoFlightPlan();
        }
        if(this.isInMission){
            pauseMission();
        }
    }

    /**
     * Starts or continues the current Drone mission.
     * @see DroneStatesSettingsProceduresModule to know which mission to start (current one)
     * We can also use startHappy(), startAngry(), startUnknown() or startPart1() to start specific
     * mission
     */
    public void startMission(){
        if(this.isEngaged) {
            this.isInMission = true;
            this.droneSettings.startMission();
        }
    }

    /**
     * Pauses the drone mission
     * Warning: Please use this method only on autopilot disengage call
     */
    private void pauseMission(){
        this.droneSettings.pauseMission();
    }

    /**
     * Ends the drone mission
     */
    void endMission() {
        isInMission = false;
    }

    /**
     * Starts or continues the current Drone flight plan (i.e. Plan open in the flight planner).
     */
    public void startAutoFlightPlan(){
        if(this.isEngaged) {
            this.isInFlightPlan = true;
            this.mSKEModule.startFlightPlan(mFlightPlanerModule);
        }
    }

    /**
     * Pause the current drone flight plan if drone was in flight plan
     */
    public void pauseAutoFlightPlan(){
        if(this.isInFlightPlan) {
            this.mSKEModule.pauseFlightPlan();
        }
    }

    /**
     * Definitely stop the current drone flight plan.
     */
    public void stopAutoFlightPlan(){

        if(this.isInFlightPlan) {
            endFlightPlan();
            this.mSKEModule.stopFlightPlan();
        }
    }

    /**
     * Indicate the drone is not in flight plan.
     * Note it Has no effect if the drone was not in flight plan
     */
    void endFlightPlan() {
        isInFlightPlan = false;
    }


    /**
     * If the autopilot is engage, ask the drone to get closer to the subject
     */
    public void getCloserTo(){
        if(this.isEngaged) {
            this.droneSettings.getCloserTo();
        }
    }

    /**
     * Ask the drone stop getting closer to the subject by canceling the get closer order
     * TODO define if the isEngaged test is needed or not. Assume not for now
     */
    public void stopGetCloserTo(){
        //if(this.isEngaged) {
        this.droneSettings.stopGetCloserTo();
        //}
    }


    /**
     * Camera Settings initialization: triggered once on init.
     * @see DroneStatesSettingsProceduresModule
     * @param fov (float): Value of the camera horizontal fov (in degree)
     * @param panMax (float): Value of max pan (right pan) (in degree)
     * @param panMin (float): Value of min pan (left pan) (in degree)
     * @param tiltMax (float): Value of max tilt (top tilt) (in degree)
     * @param tiltMin (float): Value of min tilt (bottom tilt) (in degree)
     */
    public void setCameraInfo(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
        droneSettings.setCameraSettings(fov, panMin, panMax, tiltMin, tiltMax);
    }

    /**
     * Camera orientation with float arguments.
     * @see DroneStatesSettingsProceduresModule
     * @param tilt (float): Tilt camera consign for the drone [deg]
     * @param pan (float): Pan camera consign for the drone [deg]
     */
    public void setCameraSettings(float tilt, float pan){
        droneSettings.setCameraSettings(tilt, pan);
    }

    /**
     * Update the angle states of the drone. Triggered when the drone's attitude change
     * and also update the drone on the map TODO understand why the drone is not updated correctly
     * @see DroneStatesSettingsProceduresModule
     * @param roll (float): in radian
     * @param pitch (float): in radian
     * @param yaw (float): : in radian
     */
    public void updateDroneSettings(float roll, float pitch, float yaw){
        droneSettings.update(roll, pitch, yaw);
        mFlightPlanerModule.updateDroneOrientation(yaw);
    }

    /**
     * Update the 3D state of the drones
     * and also update the drone on the map TODO understand why the drone is not updated correctly
     * @see DroneStatesSettingsProceduresModule
     * @param lat (double): Latitude value
     * @param lon (double): Longitude value
     * @param alt (double): Altitude value
     */
    public void updateDroneSettings(double lat, double lon, double alt){
        droneSettings.update(lat, lon, alt);
        mFlightPlanerModule.updateDronePosition(lat, lon, alt);
    }


    // Threads and detectors


    /**
     * Create a OpenCv Thread and start it
     * @see OpenCVThread
     * @param videoView (BebopVideoView): the bebop stream layer
     * @param cvView (OpenCVView): the opencv layer
     */
    public void resumeThreads(BebopVideoView videoView, OpenCVView cvView) {
        openCVThread = new OpenCVThread(videoView, cvView);
        openCVThread.start();
    }

    /**
     * Pause the thread started in resumeThreads()
     * If openCVThread is not null, interrupt it
     */
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

    /**
     * Color Blob Detector Class
     * Each time process is called with an input frame, this class compute a new list of CvRect
     * containing the detected blobs.
     * Use constructor to create a color blob detector, process to compute the list of blobs, getContour
     * to access the computed blobs and destroy once you does not need the class any more.
     * The Color is in HSV format and can be set using setHsvColor
     */
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

    /**
     * Face Detector Class
     * Each time process is called with an input frame, this class compute a new list of CvRect
     * containing the detected face.
     * Use constructor to create a face detector, process to compute the list of faces, getContour
     * to access the computed faces and destroy once you does not need the class any more
     * assume the file /res/raw/haarcascade_frontalface_alt_old.xml exists in the application apk
     * Note FaceDetector of java cv is not compatible with the new format of cascade files.
     */
    private class FaceDetector{

        private opencv_objdetect.CvHaarClassifierCascade classifier;
        private List<CvRect> mContours;
        IplImage grayImage;

        CvMemStorage storage;

        FaceDetector(int width, int height) {
            this.mContours = new ArrayList<>();

            try {
                // Load the classifier file from Java resources.
                File classifierFile = Loader.extractResource(getClass(),
                        "/res/raw/haarcascade_frontalface_alt_old.xml",
                        DroneApplication.getApplication().getContext().getCacheDir(), "classifier", ".xml");
                classifierFile.deleteOnExit();
                if (classifierFile.length() <= 0) {
                    throw new IOException("Could not extract the classifier file from Java resource.");
                }
                Log.i(TAG, classifierFile.getAbsolutePath());

                classifier = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
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
            // Let's try to detect some faces! but we need a grayscale image...
            cvCvtColor(rgbaImage, grayImage, COLOR_RGB2GRAY);
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

    /**
     * OpenCv Thread class
     * Run a Computer vision thread while not interrupted.
     * I.e grab each frame streamed by the drone and compute CV algorithms on it. The thread is here
     * to influence the drone behavior by updating periodically its states or by giving it order directly
     *
     * @see ColorBlobDetector: The blob detection detect the closest blob of a touched point on screen
     * @see FaceDetector: Once the blob is big enough, try to detect face
     * @see AutoFaceRecognizer which from detected faces tries to recognize face using eigen faces
     */
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
        private AutoFaceRecognizer mFaceRecognizer;
        private int rows, cols;
        private int x1, y1, x2, y2;

        private double mFrameArea, mDistance, blobArea;
        private Point mFrameCenter, pivotCenter, blobCenter;
        private boolean mIsBlobFound, mIsFaceFound, mSearchBlob, mSearchFace;
        private boolean mIsReady;
        private String mText;


        private OpenCVThread(BebopVideoView videoView, OpenCVView cvView) {

            ctx = cvView.getContext();

            // Load libraries needed
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
            blobCenter = new Point(0, 0);
            mIsBlobFound = false;
            mIsFaceFound = false;
            mSearchBlob = true;
            mSearchFace = false;

            mBlobDetector = new ColorBlobDetector(width, height);
            mFaceDetector = new FaceDetector(width, height);
            mFaceRecognizer = new AutoFaceRecognizer();
            mIsReady = true;
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


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {


            if(!mIsReady){
                return false;
            }

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

                            if(mSearchFace){
                                // Crop the image to keep the face only
                                cvSetImageROI(grabbedImage, new CvRect(x1, y1, w, h));
                                IplImage subIpl = cvCreateImage(cvGetSize(grabbedImage), grabbedImage.depth(), grabbedImage.nChannels());
                                cvCopy(grabbedImage, subIpl);
                                mText = mFaceRecognizer.process(subIpl, w, h);
                                cvResetImageROI(grabbedImage);
                            }else{
                                mText = "";
                            }

                            // TODO !


                        }

                        // If the drone is in flight plan or inMission -> do nothing for not altering the plan or the mission !
                        // Once the mission ended, start tracking.
                        if(isEngaged && !isInFlightPlan && !isInMission) {
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
                            mOpenCVView.setText(mIsBlobFound ? "" : mText);
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
            mFaceDetector.destroy();
            cvReleaseImage(grabbedImage);
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }

}
