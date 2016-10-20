package de.razorfish.android.charpathrecog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
    private static File SD_CARD_DIR = new File(Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "CharPaths");
    List<PathsSample> pathsSamples;
    private Gson gson;

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

        pathsSamples = new ArrayList<>();

        updateTextFields();
        checkPermissions();

        SD_CARD_DIR.mkdirs();
        gson = new Gson();
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

        pathsSamples.add(new PathsSample(paths, currentChar));

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
        writePathssToStorage();
        trainClassifierAndGotoRecognition();
    }

    private void trainClassifierAndGotoRecognition() {
        try {
            CharacterClassifier characterClassifier = CharacterClassifier.get();
            characterClassifier.train(getPrimitiveFeatures(), getPrimitiveKlasses());
            startActivity(new Intent(DrawingActivity.this, RecognitionActivity.class));
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
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS ", Locale
                    .ENGLISH);
            Date now = new Date();

            File outputFile = new File(SD_CARD_DIR, format.format(now) + getKlasses().size() + " " +
                    "samples.json");

            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(gson.toJson(pathsSamples).getBytes());

        } catch (IOException e) {
            Log.e(TAG, "Exception while writing Json to internal storage", e);
        }
    }

    public void onButtonResetClick(View view) {
        drawingView.clearPaths();
    }

    public void onButtonLoadClick(View view) {
        String[] files = SD_CARD_DIR.list();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.choose_one_file);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout
                .select_dialog_singlechoice, files);

        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String chosenFile = arrayAdapter.getItem(which);
                try {
                    PathsSample[] pathsSamples = gson.fromJson(new FileReader(new File
                                    (SD_CARD_DIR, chosenFile)),
                            PathsSample[].class);
                    DrawingActivity.this.pathsSamples = Arrays.asList(pathsSamples);
                    trainClassifierAndGotoRecognition();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        dialogBuilder.show();
    }

    public List<Character> getKlasses() {
        List<Character> characters = new ArrayList<>();
        for (PathsSample sample : pathsSamples) {
            characters.add(sample.klass);
        }
        return characters;
    }

    public List<List<List<PointF>>> getPathss() {
        List<List<List<PointF>>> pathss = new ArrayList<>();
        for (PathsSample sample : pathsSamples) {
            pathss.add(sample.paths);
        }
        return pathss;
    }

    public char[] getPrimitiveKlasses() {
        List<Character> klasses = getKlasses();
        char[] primitiveKlasses = new char[klasses.size()];
        for (int i = 0; i < klasses.size(); i++) {
            primitiveKlasses[i] = klasses.get(i);
        }
        return primitiveKlasses;
    }

    public double[][] getPrimitiveFeatures() {
        List<List<List<PointF>>> samples = getPathss();
        double[][] primitiveFeatures = new double[samples.size()][];
        for (int i = 0; i < samples.size(); i++) {

            primitiveFeatures[i] = CharacterPathTransformator.pathsToVectorAngleTemporalDivs
                    (samples.get(i), 4, true, false);
//            primitiveFeatures[i] = CharacterPathTransformator.pathsToBestVectorAngleHistogram
//                    (samples.get(i));
        }
        System.out.println(Arrays.deepToString(primitiveFeatures));
        return primitiveFeatures;
    }

}