package ch.epfl.droneproject.module;


import android.graphics.Color;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.Log;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.util.Map;

import ch.epfl.droneproject.DroneApplication;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_AREA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;

/**
 * AutoFaceRecognizer.java
 * @author blchatel
 *
 * Use Eigen faces algorithm to recognize subject from a color images.
 * http://www.shervinemami.info/faceRecognition.html
 * @see org.bytedeco.javacpp.opencv_face
 *
 * The class use a model saved on the phone at /Training/eigenFaces.yml (about 50Mo for 150 images or 3 faces)
 * The class will train the model if the yml file doesn't exists. It assume images are in /Training/ folder and
 * follow the rules:
 *  - Images are in jpg, pgm or png format
 *  - Images are TRAINING_WIDTH x TRAINING_HEIGHT resolution
 *  - the name is T_X_label_NN.[pgm|jpg|png] where:
 *      - T is the subject type: [A]dmin, [F]riend, [E]nemy
 *      - X is the subject number (0-9)
 *      - label is for example the subject name (without special char)
 *      - NN is the training picture number for subject X (1 to 99)
 *
 *      More of that at this step of implementation, the drone recognize:
 *       - admin if X==0
 *       - friend if 1<=X<=5 -> 1, 2, 3, 4, 5
 *       - enemy if X>5 -> 6, 7, 8, 9
 *
 *       Another polit
 *
 * If the model exists the class will load it (about 10 sec)
 */
public class AutoFaceRecognizer {

    // The possible recognized person : ADMIN, FRIEND, ENEMY, UNKNOWN
    public enum Recognized {
        ADMIN(1, 'A', "Admin", Color.GREEN),
        FRIEND(2, 'F', "Friend", Color.BLUE),
        ENEMY(3, 'E', "Enemy",  Color.RED),
        UNKNOWN(4, 'U', "Unknown",  Color.BLACK)
        ;

        private final int id;
        private final char type;
        private final int color;
        private final String text;

        private static double confidence;


        Recognized(final int id, final char type, String text, int color) {
            this.id = id;
            this.type = type;
            this.color = color;
            this.text = text;
        }

        public int id(){
            return id;
        }
        public char type() {
            return type;
        }
        public int color(){
            return color;
        }
        public String text(){
            return text +" "+confidence;
        }

        public boolean equals(Recognized other){
            return other != null && this.id == other.id;
        }

        //public static double confidence(){return confidence; }
        public static void setConfidence(double c){confidence = c; }
    }

    // Contants
    private static final String TAG = "AutoFaceRecognizer";
    private static final String MODEL_FILE_PATH = "eigenFaces.yml";
    private static final String TRAINING_FOLDER_PATH = "/Training/";
    private static final int TRAINING_WIDTH = 120;
    private static final int TRAINING_HEIGHT = 90;
    private static final double CONFIDENCE_THRESHOLD = 6000;

    private final String EXTERNAL_DIRECTORY; // computed in constructor

    private FaceRecognizer faceRecognizer;

    // Useful variable
    private Mat labelsMat;
    private ArrayMap<Integer, String> labelNamesList;
    private MatVector images;
    private IplImage grayIpl;

    private boolean isTrained;


    /**
     * Default AutoFaceRecognizer constructor
     */
    AutoFaceRecognizer() {

        EXTERNAL_DIRECTORY = Environment.getExternalStorageDirectory().toString().concat(TRAINING_FOLDER_PATH);

        faceRecognizer = createEigenFaceRecognizer();
        grayIpl = cvCreateImage(cvSize(TRAINING_WIDTH, TRAINING_HEIGHT), IPL_DEPTH_8U, 1);

        File file = new File(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
        if(file.exists()){
            DroneApplication.pushInfoMessage("Model exists");
            faceRecognizer.load(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
            isTrained = true;
            DroneApplication.pushInfoMessage("Model Loaded");
        }
        else{
            DroneApplication.pushInfoMessage("Model does not exist");
            isTrained = false;
            loadTrainingImages();
            train();
        }
    }

    /**
     * If the model is trained this class try to recognize the face given in the rgbaImage
     * To test an image, it must be of the same size as the trained one. So we need to
     *  - convert it in gray
     *  - equalize the level
     *  - Resize the image in a TRAINING_WIDTH x TRAINING_HEIGHT resolution
     * @param rgbaImage (IplImage): The input rgba image. It contains a single face to recognize an has w x h resolution
     * @param w (int): width of the rgbaImage
     * @param h (int): height of the rgbaImage
     * @return (Recognized) the Recognized person
     */
    public Recognized process(IplImage rgbaImage, int w, int h){
        if(isTrained) {

            IplImage inputIpl = cvCreateImage(cvSize(w, h), IPL_DEPTH_8U, 1);
            cvCvtColor(rgbaImage, inputIpl, COLOR_RGB2GRAY);
            cvEqualizeHist(inputIpl, inputIpl);

            // Scale the image to the new dimensions, even if the aspect ratio will be changed.

            if (TRAINING_WIDTH > w && TRAINING_HEIGHT > h) {
                // Make the image larger
                cvResetImageROI(inputIpl);
                cvResize(inputIpl, grayIpl, CV_INTER_LINEAR);	// CV_INTER_CUBIC or CV_INTER_LINEAR is good for enlarging
            }
            else {
                // Make the image smaller
                cvResetImageROI(inputIpl);
                cvResize(inputIpl, grayIpl, CV_INTER_AREA);	// CV_INTER_AREA is good for shrinking / decimation, but bad at enlarging.
            }

            Mat matImage = new Mat(grayIpl);
            IntPointer l = new IntPointer(1);
            DoublePointer c = new DoublePointer(1);

            faceRecognizer.predict(matImage, l, c);

            int label = l.get(0);
            double confidence = c.get(0);

            String labelInfo = faceRecognizer.getLabelInfo(label).getString();
            char type = labelInfo.charAt(0);

            DroneApplication.pushDebugMessage("Predicted label: " + label+ " - "+labelInfo + ", confidence: " + confidence);

            Recognized.setConfidence(confidence);

            if(confidence < CONFIDENCE_THRESHOLD){

                if(type == Recognized.ADMIN.type()){
                    return Recognized.ADMIN;
                }
                else if(type == Recognized.FRIEND.type()){
                    return Recognized.FRIEND;
                }
                else if(type == Recognized.ENEMY.type()){
                    return Recognized.ENEMY;
                }
            }
        }
        return Recognized.UNKNOWN;
    }


    /**
     * Load all T_X_label_NN.[jpg|pgm|png] images in the EXTERNAL_DIRECTORY
     * and store them by X value
     */
    private void loadTrainingImages() {
        DroneApplication.pushInfoMessage("Load Training Images");

        // if the directory doesn't exist -> no training files
        File root = new File(EXTERNAL_DIRECTORY);
        if(!(root.exists() && root.isDirectory())) {
            DroneApplication.pushErrorMessage("Failed to load image, no directory: " + EXTERNAL_DIRECTORY);
            Log.e(TAG, "Failed to load image, no directory: " + EXTERNAL_DIRECTORY);
            return;
        }

        FilenameFilter imgFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };

        File[] imageFiles = root.listFiles(imgFilter);

        if(imageFiles.length < 3){
            DroneApplication.pushErrorMessage("Not enough images for training !");
            Log.e(TAG, "Not enough images for training !");
        }else{

            // Release previous loaded image
            if(images != null){
                for(int i = 0; i< images.size(); i++){
                    images.get(i).release();
                }
            }
            if(labelsMat != null){
                labelsMat.release();
            }

            images = new MatVector(imageFiles.length);
            labelsMat = new Mat(imageFiles.length, 1, CV_32SC1);
            labelNamesList = new ArrayMap<>();

            IntBuffer labelsBuf = labelsMat.createBuffer();

            int counter = 0;

            for (File image : imageFiles) {
                Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

                String[] ss = image.getName().split("\\_");
                String type = ss[0];
                int label = Integer.parseInt(ss[1]);
                String name = ss[2];

                //labelNamesList.put(label, name);
                labelNamesList.put(label, type+"+"+name);
                images.put(counter, img);

                labelsBuf.put(counter, label);
                counter++;
            }
        }
    }

    /**
     * Train the model on the loaded images and save the model for next start
     */
    private void train(){
        if(faceRecognizer != null && images != null && labelsMat != null) {
            for(Map.Entry<Integer, String> entry : labelNamesList.entrySet()) {
                int key = entry.getKey();
                String name = entry.getValue();
                faceRecognizer.setLabelInfo(key, name);
            }
            DroneApplication.pushInfoMessage("Start training");
            faceRecognizer.train(images, labelsMat);
            DroneApplication.pushInfoMessage("End training, Start saving");
            faceRecognizer.save(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
            isTrained = true;
            DroneApplication.pushInfoMessage("End saving");
        }
    }
}