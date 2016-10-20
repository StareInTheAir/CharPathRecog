package de.razorfish.android.charpathrecog;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public class CharacterPathTransformator {
    private static final String TAG = "CPT";
    public static final int BEST_NUMBER_OF_BINS = 12;

    public static double[] getBinEdges(double min, double max, int numberOfBins) {
        double[] binEdges = new double[numberOfBins + 1];
        double step = (max - min) / numberOfBins;
        for (int i = 0; i < numberOfBins + 1; i++) {
            binEdges[i] = min + i * step;
        }
        return binEdges;
    }

    public static double[] pathsToBestVectorAngleHistogram(List<List<PointF>> paths) {
        return pathsToVectorAngleHistogram(paths, CharacterPathTransformator.getBinEdges(
                -180, 180, BEST_NUMBER_OF_BINS), 360.0 / BEST_NUMBER_OF_BINS / 2.0, true, true,
                true);
    }

    public static double[] pathsToVectorAngleHistogram(List<List<PointF>> paths, double[] binEdges,
                                                       double offset, boolean vectorBetweenPaths,
                                                       boolean weighted, boolean normalize) {
        double[] histogram = new double[binEdges.length - 1];
        for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
            List<PointF> path = paths.get(pathIndex);
            for (int pointIndex = 0; pointIndex < path.size(); pointIndex++) {
                PointF current;
                PointF next;
                if (pointIndex == path.size() - 1) {
                    // if pointIndex is last point in current path
                    if (pathIndex == paths.size() - 1 || !vectorBetweenPaths) {
                        // if pathIndex is last path or vectorBetweenPaths == false
                        continue;
                    }
                    current = path.get(pointIndex);
                    next = paths.get(pathIndex + 1).get(0);
                } else {
                    current = path.get(pointIndex);
                    next = path.get(pointIndex + 1);
                }
                double angle = Math.toDegrees(Math.atan2(next.y - current.y, next.x - current.x));
                angle += offset;
                if (weighted) {
                    double weight = getVectorNorm(histogram);
                    angle *= weight;
                }
                sortIntoHistogram(angle, histogram, binEdges);
            }
        }

        if (normalize) {
            normalizeVector(histogram);
        }

        return histogram;
    }

    public static double[] pathsToVectorAngleTemporalDivs(List<List<PointF>> paths, int divisions,
                                                          boolean vectorBetweenPaths,
                                                          boolean weighted) {
        List<Double> angles = new ArrayList<>();
        for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
            List<PointF> path = paths.get(pathIndex);
            for (int pointIndex = 0; pointIndex < path.size(); pointIndex++) {
                PointF current;
                PointF next;
                if (pointIndex == path.size() - 1) {
                    // if pointIndex is last point in current path
                    if (pathIndex == paths.size() - 1 || !vectorBetweenPaths) {
                        // if pathIndex is last path or vectorBetweenPaths == false
                        continue;
                    }
                    current = path.get(pointIndex);
                    next = paths.get(pathIndex + 1).get(0);
                } else {
                    current = path.get(pointIndex);
                    next = path.get(pointIndex + 1);
                }
                double angle = Math.toDegrees(Math.atan2(next.y - current.y, next.x - current.x));
                if (weighted) {
                    // TODO
                } else {
                    angles.add(angle);
                }
            }
        }

        int minElementsPerDivision = angles.size() / divisions;
        List<List<Double>> temporalMeans = new ArrayList<>();
        for (int i = 0; i < divisions - 1; i++) {
            temporalMeans.add(angles.subList(i * minElementsPerDivision, (i + 1) *
                    minElementsPerDivision));
        }

        // add the last division by hand, so that all remaining values are part of this division
        temporalMeans.add(angles.subList((divisions - 1) * minElementsPerDivision, angles.size()));

        double[] means = new double[divisions + 1];
        means[0] = getMean(angles);
        for (int i = 0; i < temporalMeans.size(); i++) {
            means[i + 1] = getMean(temporalMeans.get(i));
        }

        return means;
    }

    public static double getMean(List<Double> values) {
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public static void sortIntoHistogram(double value, double[] histogram, double[] binEdges) {
        for (int i = 0; i < binEdges.length - 1; i++) {
            double lower = binEdges[i];
            double upper = binEdges[i + 1];
            if (value >= lower && value <= upper) {
                histogram[i] += 1;
            }
        }
    }

    public static void normalizeVector(double[] vector) {
        double norm = getVectorNorm(vector);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    private static double getVectorNorm(double[] vector) {
        double norm = 0;
        for (int i = 0; i < vector.length; i++) {
            norm += vector[i] * vector[i];
        }
        norm = Math.sqrt(norm);
        return norm;
    }
}
