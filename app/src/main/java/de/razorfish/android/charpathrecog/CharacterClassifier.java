package de.razorfish.android.charpathrecog;

import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class CharacterClassifier {
    private final Classifier classifier;
    private Instances lastInstances;
    private static CharacterClassifier instance = null;

    public static CharacterClassifier get() {
        if (instance == null) {
            instance = new CharacterClassifier();
        }
        return instance;
    }

    private CharacterClassifier() {
        classifier = new RandomForest();
    }

    public void train(double[][] data, char[] klasses) throws Exception {
        ArrayList<Attribute> attributes = new ArrayList<>();
        List<String> classes = new ArrayList<>();

        for (char i = 'A'; i <= 'Z'; i++) {
            classes.add(String.valueOf(i));
        }

        for (int binNumber = 1; binNumber <= data[0].length; binNumber++) {
            attributes.add(new Attribute("bin" + binNumber));
        }

        attributes.add(new Attribute("theClass", classes));

        lastInstances = new Instances("CharacterPathVectorAngleHistogram", attributes, data.length);
        lastInstances.setClassIndex(attributes.size() - 1);

        for (int i = 0; i < data.length; i++) {
            double[] sample = data[i];
            char klass = (char) (klasses[i] - 'A');

            DenseInstance instance = getClassLessInstance(sample);
            instance.setValue(sample.length, klass);
            instance.setDataset(lastInstances);
            lastInstances.add(instance);
        }

        classifier.buildClassifier(lastInstances);
    }

    private DenseInstance getClassLessInstance(double[] sample) {
        DenseInstance instance = new DenseInstance(sample.length + 1);
        for (int j = 0; j < sample.length; j++) {
            instance.setValue(j, sample[j]);
        }
        return instance;
    }

    public char classify(double[] sample) throws Exception {
        DenseInstance instance = getClassLessInstance(sample);
        instance.setMissing(sample.length);
        instance.setDataset(lastInstances);
        return (char) (classifier.classifyInstance(instance) + 'A');
    }
}
