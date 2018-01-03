package ch.epfl.droneproject.module;

import android.os.Environment;
import android.util.ArrayMap;
import android.util.Log;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.util.Map;

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


public class AutoFaceRecognizer {

    /** the logger */
    private static final String TAG = "AutoFaceRecognizer";
    private static final String MODEL_FILE_PATH = "eigenFaces.yml";
    private static final String TRAINING_FOLDER_PATH = "/Training/";
    private static final int TRAINING_WIDTH = 120;
    private static final int TRAINING_HEIGHT = 90;

    private final String EXTERNAL_DIRECTORY;
    private FaceRecognizer faceRecognizer;

    private Mat labelsMat;
    private ArrayMap<Integer, String> labelNamesList;
    private MatVector images;

    private IplImage grayIpl;


    private boolean isTrained;


    /** Constructs a new FaceRecognition instance. */
    AutoFaceRecognizer() {

        EXTERNAL_DIRECTORY = Environment.getExternalStorageDirectory().toString().concat(TRAINING_FOLDER_PATH);

        faceRecognizer = createEigenFaceRecognizer();
        grayIpl = cvCreateImage(cvSize(TRAINING_WIDTH, TRAINING_HEIGHT), IPL_DEPTH_8U, 1);

        File file = new File(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
        if(file.exists()){
            Log.i(TAG, "Model exists");
            faceRecognizer.load(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
            isTrained = true;
            Log.i(TAG, "Model Loaded");
        }
        else{
            Log.i(TAG, "Model does not exist");
            isTrained = false;
            loadTrainingImages();
            train();
        }
    }

    public String process(IplImage rgbaImage, int w, int h){
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
            //Log.e(TAG, "Predicted label: " + labelInfo + ", confidence: " + confidence);
            Log.e(TAG, "Predicted label: " + label+ " - "+labelInfo + ", confidence: " + confidence);
            return labelInfo;
        }
        return "";
    }


    private void loadTrainingImages() {
        Log.i(TAG, "Load Training Images");

        // if the directory doesn't exist -> no training files
        File root = new File(EXTERNAL_DIRECTORY);
        if(!(root.exists() && root.isDirectory())) {
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
                int label = Integer.parseInt(ss[0]);
                String name = ss[1];

                labelNamesList.put(label, name);
                images.put(counter, img);

                labelsBuf.put(counter, label);
                counter++;
            }
        }
    }

    private void train(){
        Log.i(TAG, "Train if no null");
        if(faceRecognizer != null && images != null && labelsMat != null) {
            Log.i(TAG, "Set the labels info");
            for(Map.Entry<Integer, String> entry : labelNamesList.entrySet()) {
                int key = entry.getKey();
                String name = entry.getValue();
                faceRecognizer.setLabelInfo(key, name);
            }
            Log.i(TAG, "Start of Train ! ");
            faceRecognizer.train(images, labelsMat);
            Log.i(TAG, "End of Train ! now Start Saving the model");
            faceRecognizer.save(EXTERNAL_DIRECTORY+MODEL_FILE_PATH);
            isTrained = true;
            Log.i(TAG, "End of Save");
        }
    }
}
