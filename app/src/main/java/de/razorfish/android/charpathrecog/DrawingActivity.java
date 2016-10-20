package de.razorfish.android.charpathrecog;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DrawingActivity extends Activity {
    private static final String TAG = "DrawingActivity";
    private static final int WRITE_REQUEST_CODE = 43;

    private DrawingView drawingView;
    private char currentChar;
    private int sampleCount;
    private TextView textViewCharIndicator;
    private TextView textViewSampleCount;
    private List<List<List<PointF>>> pathss;
    private List<double[]> samples;
    private List<Character> klasses;
    private String jsonToWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);
        drawingView = (DrawingView) findViewById(R.id.activity_drawing_drawing_view);
        textViewCharIndicator = (TextView) findViewById(R.id
                .activity_drawing_text_view_char_indicator);
        textViewSampleCount = (TextView) findViewById(R.id.activity_drawing_text_view_sample_count);
        currentChar = 'A';
        sampleCount = 0;

        pathss = new ArrayList<>();
        samples = new ArrayList<>();
        klasses = new ArrayList<>();

        updateTextFields();
        checkPermissions();
    }

    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == WRITE_REQUEST_CODE && grantResults.length > 0 && grantResults[0] ==
                PackageManager.PERMISSION_DENIED) {
            showPermissionDeniedToast();
        }
    }

    private void showPermissionDeniedToast() {
        Toast.makeText(this, "Not writing raw dataset as Json to internal storage",
                Toast.LENGTH_LONG).show();
    }

    public void onButtonNextClick(View view) {
        List<List<PointF>> paths = drawingView.getPaths();

//        double[] sample = CharacterPathTransformator.pathsToBestVectorAngleHistogram(paths);
        double[] sample = CharacterPathTransformator.pathsToVectorAngleTemporalDivs(paths, 4,
                true, false);
        Log.d(TAG, Arrays.toString(sample));

        pathss.add(paths);
        samples.add(sample);
        klasses.add(currentChar);

        sampleCount += 1;
        currentChar += 1;
        if (currentChar > 'Z') {
            currentChar = 'A';
        }

        drawingView.clearPaths();
        updateTextFields();
    }

    void updateTextFields() {
        textViewCharIndicator.setText(String.valueOf(currentChar));
        textViewSampleCount.setText(getString(R.string.sample_counter, sampleCount));
    }

    public void onButtonTrainClick(View view) {
        CharacterClassifier characterClassifier = CharacterClassifier.get();
        double[][] primitiveSamples = new double[samples.size()][];
        for (int i = 0; i < samples.size(); i++) {
            primitiveSamples[i] = samples.get(i);
        }

        char[] primitiveKlasses = new char[klasses.size()];
        for (int i = 0; i < klasses.size(); i++) {
            primitiveKlasses[i] = klasses.get(i);
        }

        writePathssToStorage();

        try {
            characterClassifier.train(primitiveSamples, primitiveKlasses);
            startActivity(new Intent(this, RecognitionActivity.class));
        } catch (Exception e) {
            Log.e(TAG, "Exception while training classifier", e);
        }
    }

    private void writePathssToStorage() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED) {
            showPermissionDeniedToast();
            return;
        }
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + File.separator + "CharPaths");
            dir.mkdirs();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS ", Locale
                    .ENGLISH);
            Date now = new Date();
            File outputFile = new File(dir, format.format(now) + pathss.size() + " samples.json");

            Gson gson = new Gson();
            JsonArray rawDataSet = new JsonArray();
            for (int i = 0; i < pathss.size(); i++) {
                JsonObject sample = new JsonObject();

                sample.add("class", new JsonPrimitive(klasses.get(i)));
                List<List<PointF>> paths = pathss.get(i);
                sample.add("paths", gson.toJsonTree(paths));

                rawDataSet.add(sample);
            }

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(gson.toJson(rawDataSet).getBytes());

        } catch (IOException e) {
            Log.e(TAG, "Exception while writing Json to internal storage", e);
        }
    }

    public void onButtonResetClick(View view) {
        drawingView.clearPaths();
    }
}