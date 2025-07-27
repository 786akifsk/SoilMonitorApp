package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImageClassifier {
    private Interpreter interpreter;
    private List<String> labels;

    public ImageClassifier(AssetManager assetManager, String modelPath, String labelPath) throws Exception {
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath));
        labels = loadLabels(assetManager, labelPath);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(AssetManager assetManager, String labelPath) throws Exception {
        List<String> labelList = new ArrayList<>();
        Scanner scanner = new Scanner(assetManager.open(labelPath));
        while (scanner.hasNextLine()) {
            labelList.add(scanner.nextLine());
        }
        scanner.close();
        return labelList;
    }

    public String classify(Bitmap bitmap) {
        int imageSize = 224;

        bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true);

        // For UINT8 model: 1 byte per value
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * 3);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[imageSize * imageSize];
        bitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize);

        int pixelIndex = 0;
        for (int i = 0; i < imageSize * imageSize; i++) {
            int pixel = pixels[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            inputBuffer.put((byte) r);
            inputBuffer.put((byte) g);
            inputBuffer.put((byte) b);
        }

        // Output is also UINT8, shape [1, 5] = 5 classes
        byte[][] output = new byte[1][labels.size()];

        // Run inference
        interpreter.run(inputBuffer, output);

        // Apply quantization scale (0.00390625)
        float maxScore = -1f;
        int maxIndex = -1;

        for (int i = 0; i < labels.size(); i++) {
            int intVal = output[0][i] & 0xFF; // Convert byte to unsigned int
            float score = intVal * 0.00390625f; // Dequantize

            if (score > maxScore) {
                maxScore = score;
                maxIndex = i;
            }
        }
        Log.e(TAG, "maxScore is : " + maxScore);
//        if (maxScore < 0.7f) {
//            return "Not soil";
//        }

        return labels.get(maxIndex);
    }

}
