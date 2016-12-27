package bfr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

public class BFR {
    private static final int MAX_ITERATIONS = 5000;
    public static final int NUMBER_OF_ATTRIBUTES = 4;
    private final ArrayList<DiscardSet> discardSet;
    private final ArrayList<CompressSet> compressSet;
    private final RetainedSet retainedSet;
    //public int numberOfAttributes;
    private int numberOfClusters = 3;
    private double confidenceInterval = 500.0;

    public BFR(int numberOfClusters) {
        this.retainedSet = new RetainedSet();
        this.discardSet = new ArrayList<>(); //DiscardSet(numberOfAttributes);
        initDiscardSet();
        this.compressSet = new ArrayList<>(); //CompressSet(numberOfAttributes);
        this.numberOfClusters = numberOfClusters;
    }

    public BFR(int numberOfVectors, int numberOfClusters) {
        this.retainedSet = new RetainedSet(numberOfVectors);
        this.discardSet = new ArrayList<>(); //new DiscardSet(numberOfAttributes);
        initDiscardSet();
        this.compressSet = new ArrayList<>(); //new CompressSet(numberOfAttributes);
        this.numberOfClusters = numberOfClusters;
    }

    public static void main(String[] args) {
        BFR bfr = new BFR(3);
        bfr.init();
        bfr.calculate();
        bfr.finish();
    }

    public double getConfidenceInterval() {
        return confidenceInterval;
    }

    public void setConfidenceInterval(double confidenceInterval) {
        this.confidenceInterval = confidenceInterval;
    }

    private void initDiscardSet() {
        for (int i = 0; i < numberOfClusters; i++) {
            discardSet.add(new DiscardSet(NUMBER_OF_ATTRIBUTES));
        }
    }

    //Initializes the process
    private void init() {
        //Create Clusters
        //Set Random Centroids
        for (int i = 0; i < numberOfClusters; i++) {
            discardSet.get(i).updateStatistic(Vector.createRandomPoint(-100, 100));
        }
        initCS();
        plotClusters();
    }

    /*private void deleteFromRS(Vector del) {
        retainedSet.getVectors().removeIf(vector -> vector.equals(del));
    }*/

    private void initCS() {
        //Create sub-clusters
        double distance;
        //Vector del;
        //ArrayList<Vector> vectors = retainedSet.getVectors();
        ListIterator<Vector> iterator = retainedSet.getVectors().listIterator();

        while (iterator.hasNext()) {
            Vector vector1 = iterator.next();
            Vector vector2 = null;
            if (iterator.hasNext()) {
                vector2 = iterator.next();
            }
            if (vector1 == null || vector2 == null) break;

            distance = MahalanobisDistance.calculate(vector1, vector2);
            if (distance < confidenceInterval) { // todo check
                compressSet.add(new CompressSet(NUMBER_OF_ATTRIBUTES));
                //iterator.previous();
                iterator.remove();
                compressSet.get(compressSet.size()-1).updateStatistic(vector2); // todo check
            }
        }

        /*for (int i = 0, length = vectors.size() - 1; i < length; i++) {
            distance = MahalanobisDistance.calculate(vectors.get(i), vectors.get(i + 1));
            if (distance < 1) { // todo check
                compressSet.add(new CompressSet(numberOfAttributes));
                del = vectors.get(i);
                compressSet.get(0).updateStatistic(vectors.get(i));
                deleteFromRS(del);
            }
        }*/
        plotClusters();
    }

    private void assignDS() {
        double distance;

        Iterator<Vector> iterator = retainedSet.getVectors().iterator();
        while (iterator.hasNext()) {
            Vector vector = iterator.next();
            for (int i = 0; i < numberOfClusters; i++) {
                DiscardSet c = discardSet.get(i);
                distance = MahalanobisDistance.calculate(c, vector);

                if (distance < confidenceInterval) {
                    //confidenceInterval = distance;
                    c.updateStatistic(vector);
                    iterator.remove();
                    break;
                }

                //todo it becomes empty

                Iterator<CompressSet> iterator1 = compressSet.iterator();
                while (iterator1.hasNext()) {
                    CompressSet aCompressSet = iterator1.next();
                    distance = MahalanobisDistance.calculate(aCompressSet, c.getCentroid());
                    if (distance < confidenceInterval) {
                        //confidenceInterval = distance;
                        c.updateStatistic(aCompressSet.getCentroid());
                        iterator1.remove();
                        continue;
                    }
                }
            }
        }
    }

    private void assignCS() {
        double distance;
        Iterator<Vector> iterator = retainedSet.getVectors().iterator();

        while (iterator.hasNext()) {
            Vector vector = iterator.next();
            for (CompressSet aCompressSet : compressSet) {
                distance = MahalanobisDistance.calculate(aCompressSet, vector);
                if (distance < 2) {
                    //confidenceInterval = distance;
                    aCompressSet.updateStatistic(vector);
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void assignRS() {
        retainedSet.updateRS();
    }

    // TODO
    private void finish() {
        double distance;
        ArrayList<Double> distances = new ArrayList<>();

        Iterator<CompressSet> iterator = compressSet.iterator();
        while (iterator.hasNext()) {
            CompressSet tmp = iterator.next();
            for (int i = 0; i < numberOfClusters; i++) {
                DiscardSet c = discardSet.get(i);
                distance = MahalanobisDistance.calculate(c, tmp.getCentroid());
                distances.add(distance);
            }
            int id = 0;
            Double min = distances.get(0);
            for (int i = 1; i < numberOfClusters; i++) {
                if (Double.compare(min, distances.get(i)) == 1) {
                    min = distances.get(i);
                    id = i;
                }
            }
            discardSet.get(id).updateStatistic(tmp.getCentroid());
            iterator.remove();
            distances = new ArrayList<>();
        }

        Iterator<Vector> iterator1 = retainedSet.getVectors().iterator();
        while (iterator1.hasNext()) {
            Vector vector = iterator1.next();
            for (int i = 0; i < numberOfClusters; i++) {
                DiscardSet c = discardSet.get(i);
                distance = MahalanobisDistance.calculate(c, vector);
                distances.add(distance);
                }
            int id = 0;
            Double min = distances.get(0);
            for (int i = 1; i < numberOfClusters; i++) {
                if (Double.compare(min, distances.get(i)) == 1) {
                    min = distances.get(i);
                    id = i;
                }
            }
            discardSet.get(id).updateStatistic(vector);
            iterator1.remove();
            distances = new ArrayList<>();
        }

        plotClusters();
    }

    private void calculate() {
        boolean finish = false;
        int iteration = 0;

        // Add in new data, one at a time, recalculating centroids with each new one.
        while (!finish) {
            //Clear cluster state
            //clearClusters();

            //List lastCentroids = getCentroids();

            // Assign points to the closer cluster
            assignDS();

            // Assign points to the closer sub-cluster
            if (compressSet.size() == 0) {
                initCS();
            }
            else assignCS();

            // TODO
           /* // Assign points to the closer sub-cluster
            // if (compressSet.size() == 0) {
            initCS();
            // }
            //else
            assignCS();*/

            // Updating rs (getting new vectors from buffer)
            assignRS();

            iteration++;

            // Calculates total distance between new and old Centroids
            /*double distance = 0;
            for(int i = 0; i < lastCentroids.size(); i++) {
                distance += MahalanobisDistance.calculate(lastCentroids.get(i),currentCentroids.get(i));
            }*/
            System.out.println("Iteration: " + iteration);
            //System.out.println("Centroid distances: " + distance);
            plotClusters();

            if (retainedSet.getVectors().equals(Collections.EMPTY_LIST) || iteration > MAX_ITERATIONS) {
                finish = true;
            }
        }
    }

    private void plotClusters() {
        System.out.println("rS: " + retainedSet.getVectors().size());
        System.out.println("discardSet " + discardSet.size());
        for (int i = 0; i < numberOfClusters; i++) {
            System.out.println(discardSet.get(i).toString());
        }
        System.out.println("compressSet " + compressSet.size());
        for (int i = 0; i < compressSet.size() && !compressSet.isEmpty(); i++) {
            System.out.println(compressSet.get(i).toString());
        }
        System.out.println("+++++++++++++++++++");
    }
}
