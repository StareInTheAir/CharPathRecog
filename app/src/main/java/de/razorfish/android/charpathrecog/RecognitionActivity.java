package de.razorfish.android.charpathrecog;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class RecognitionActivity extends Activity {
    private static final String TAG = "RecognitionActivity";

    private DrawingView drawingView;
    private TextView textViewPrediction;
    private CharacterClassifier characterClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        drawingView = (DrawingView) findViewById(R.id.activity_recognition_drawing_view);
        textViewPrediction = (TextView) findViewById(R.id.activity_recognition_text_view_char_prediction);
        characterClassifier = CharacterClassifier.get();
    }

    public void onButtonRecognizeClick(View view) {
        List<List<PointF>> paths = drawingView.getPaths();
//        double[] sample = CharacterPathTransformator.pathsToBestVectorAngleHistogram(paths);
        double[] sample = CharacterPathTransformator.pathsToVectorAngleTemporalDivs(paths, 4,
                true, false);
        try {
            char klass = characterClassifier.classify(sample);
            Log.d(TAG, "Predicted class: " + klass);
            textViewPrediction.setText(String.valueOf(klass));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onButtonResetClick(View view) {
        drawingView.clearPaths();
    }
}
