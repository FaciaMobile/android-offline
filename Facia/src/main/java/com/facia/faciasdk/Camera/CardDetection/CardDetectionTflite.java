package com.facia.faciasdk.Camera.CardDetection;

import com.facia.faciasdk.Utils.FaciaLogger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
import com.google.gson.JsonObject;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class CardDetectionTflite {
    private Interpreter tflite;
    private Bitmap bitmap;
    private int imageSizeX;
    private int imageSizeY;

    public Bitmap run(Bitmap bitmap) {
        try {
            this.bitmap = bitmap;
            try {
                tflite = new Interpreter(loadModelFile(SingletonData.getInstance().getActivity()));
            } catch (Exception e) {
                FaciaLogger.e(e);
            }
            int imageTensorIndex = 0;
            int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
            imageSizeY = imageShape[1];
            imageSizeX = imageShape[2];
            DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

            ByteBuffer inputBuffer = preprocessBitmap(bitmap, imageSizeX, imageSizeY, imageDataType);

            // Output buffers
            float[][][] output1 = new float[1][40][4];
            float[][] output2 = new float[1][40];
            float[][] output3 = new float[1][40];
            float[] output4 = new float[1];
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, output1);
            outputs.put(1, output2);
            outputs.put(2, output3);
            outputs.put(3, output4);

            tflite.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputs);
            return handleResult(output1, output3);
        }catch (Exception e){
            Webhooks.exceptionReport(e, "TfliteModelClass/run");
            return null;
        }
    }

    /**
     * Preprocess Bitmap to ByteBuffer for LiteRT 1.4.0
     */
    private ByteBuffer preprocessBitmap(Bitmap bitmap, int width, int height, DataType dataType) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
        int channels = 3; // RGB
        int bytesPerChannel = (dataType == DataType.UINT8) ? 1 : 4;
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * channels * bytesPerChannel);
        buffer.rewind();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int px = resized.getPixel(x, y);
                if (dataType == DataType.UINT8) {
                    buffer.put((byte) ((px >> 16) & 0xFF));
                    buffer.put((byte) ((px >> 8) & 0xFF));
                    buffer.put((byte) (px & 0xFF));
                } else { // FLOAT32
                    buffer.putFloat(((px >> 16) & 0xFF) / 255f);
                    buffer.putFloat(((px >> 8) & 0xFF) / 255f);
                    buffer.putFloat((px & 0xFF) / 255f);
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

    /**
     * Load model from assets
     */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        String modelName = "model.tflite";
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @SuppressLint("SetTextI18n")
    private Bitmap handleResult(float[][][] boxPoints, float[][] confidence) {
        try {
            int maxConfidenceIndex = maxConfidenceIndex(confidence[0]);
            if (confidence[0][maxConfidenceIndex] > ThresholdConstants.MIN_CARD_DETECTION_CONFIDENCE) {
                float xMin = Float.max(1, boxPoints[0][maxConfidenceIndex][1] * bitmap.getWidth());
                float yMin = Float.max(1, boxPoints[0][maxConfidenceIndex][0] * bitmap.getHeight());
                float xMax = Float.min(bitmap.getWidth(), boxPoints[0][maxConfidenceIndex][3] * bitmap.getWidth());
                float yMax = Float.min(bitmap.getHeight(), boxPoints[0][maxConfidenceIndex][2] * bitmap.getHeight());

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("xMin", xMin);
                jsonObject.addProperty("yMin", yMin);
                jsonObject.addProperty("xMax", xMax);
                jsonObject.addProperty("yMax", yMax);
                SingletonData.getInstance().setCardJson(jsonObject);

                return bitmap;
            } else {
                return null;
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "TfliteModelClass/handleResult");
            return null;
        }
    }

    private int maxConfidenceIndex(float[] confidenceArray) {
        int maxConfidenceIndex = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidenceArray.length; i++) {
            if (confidenceArray[i] > maxConfidence) {
                maxConfidence = confidenceArray[i];
                maxConfidenceIndex = i;
            }
        }
        return maxConfidenceIndex;
    }
}


// old one changed due to support of 16kb page size

//package com.facia.faciasdk.Camera.CardDetection;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.res.AssetFileDescriptor;
//import android.graphics.Bitmap;
//
//import com.facia.faciasdk.Logs.Webhooks;
//import com.facia.faciasdk.Singleton.SingletonData;
//import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
//import com.google.gson.JsonObject;
//
//import org.tensorflow.lite.DataType;
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.support.common.ops.CastOp;
//import org.tensorflow.lite.support.image.ImageProcessor;
//import org.tensorflow.lite.support.image.TensorImage;
//import org.tensorflow.lite.support.image.ops.ResizeOp;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.HashMap;
//import java.util.Map;
//
//public class CardDetectionTflite {
//    private Interpreter tflite;
//    private Bitmap bitmap;
//    private TensorImage inputImageBuffer;
//    private int imageSizeX;
//    private int imageSizeY;
//
//    public Bitmap run(Bitmap bitmap) {
//        try {
//            this.bitmap = bitmap;
//            try {
//                tflite = new Interpreter(loadModelFile(SingletonData.getInstance().getActivity()));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            int imageTensorIndex = 0;
//            int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
//            imageSizeY = imageShape[1];
//            imageSizeX = imageShape[2];
//            DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
//            inputImageBuffer = new TensorImage(imageDataType);
//            inputImageBuffer = (loadImage(bitmap));
//
//            float[][][] output1 = new float[1][40][4];
//            float[][] output2 = new float[1][40];
//            float[][] output3 = new float[1][40];
//            float[] output4 = new float[1];
//            Map<Integer, Object> outputs = new HashMap<>();
//            outputs.put(0, output1);
//            outputs.put(1, output2);
//            outputs.put(2, output3);
//            outputs.put(3, output4);
//
//            tflite.runForMultipleInputsOutputs(new ByteBuffer[]{inputImageBuffer.getBuffer()}, outputs);
//            return handleResult(output1, output3);
//        }catch (Exception e){
//            Webhooks.exceptionReport(e, "TfliteModelClass/run");
//            return null;
//        }
//    }
//
//    private TensorImage loadImage(final Bitmap bitmap) {
//        inputImageBuffer.load(bitmap);
//        ImageProcessor imageProcessor =
//                new ImageProcessor.Builder()
//                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.BILINEAR))
//                        .add(new CastOp(DataType.UINT8))
//                        .build();
//        return imageProcessor.process(inputImageBuffer);
//    }
//
//    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
//        String modelName = "model.tflite";
//        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelName);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
//
//    @SuppressLint("SetTextI18n")
//    private Bitmap handleResult(float[][][] boxPoints, float[][] confidence) {
//        try {
//            int maxConfidenceIndex = maxConfidenceIndex(confidence[0]);
////            if (confidence[0][maxConfidenceIndex] > 0.94) {
//            if (confidence[0][maxConfidenceIndex] > ThresholdConstants.MIN_CARD_DETECTION_CONFIDENCE) {
//                float xMin = Float.max(1, boxPoints[0][maxConfidenceIndex][1] * bitmap.getWidth());
//                float yMin = Float.max(1, boxPoints[0][maxConfidenceIndex][0] * bitmap.getHeight());
//                float xMax = Float.min(bitmap.getWidth(), boxPoints[0][maxConfidenceIndex][3] * bitmap.getWidth());
//                float yMax = Float.min(bitmap.getHeight(), boxPoints[0][maxConfidenceIndex][2] * bitmap.getHeight());
//
//                JsonObject jsonObject = new JsonObject();
//                jsonObject.addProperty("xMin", xMin);
//                jsonObject.addProperty("yMin", yMin);
//                jsonObject.addProperty("xMax", xMax);
//                jsonObject.addProperty("yMax", yMax);
//                SingletonData.getInstance().setCardJson(jsonObject);
//
//
////                Bitmap canvasBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
////                Canvas canvas = new Canvas(canvasBmp);
////                Paint paint = new Paint();
////                paint.setColor(Color.GREEN);
////                paint.setStrokeWidth(8);
////                paint.setStyle(Paint.Style.STROKE);
////                canvas.drawRect(xMin, yMin, xMax, yMax, paint);
//                return bitmap;
//            } else {
////                SingletonData.getInstance().getFrameProcessing().cardDetected(null, 0.0f, 0.0f, 0.0f, 0.0f);
//                return null;
//            }
//        }catch (Exception e){
//            Webhooks.exceptionReport(e, "TfliteModelClass/handleResult");
//            return null;
//        }
//    }
//
//    private int maxConfidenceIndex(float[] confidenceArray) {
//        int maxConfidenceIndex = 0;
//        float maxConfidence = 0;
//        for (int i = 0; i < confidenceArray.length; i++) {
//            if (confidenceArray[i] > maxConfidence) {
//                maxConfidence = confidenceArray[i];
//                maxConfidenceIndex = i;
//            }
//        }
//        return maxConfidenceIndex;
//    }
//
//}