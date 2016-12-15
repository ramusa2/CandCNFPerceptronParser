package neuralnet;

import java.util.ArrayList;

public class FeatureNormalizer {
	
	private double stdDev;
	private double mean;
	private ArrayList<Double> accumulated;
	private boolean wasLearned = false;
	
	public FeatureNormalizer(double[] data) {
		this.setMeanAndStdDev(data);
	}
	
	public FeatureNormalizer() {
		this.mean = 0.0;
		this.stdDev = 1.0;
		this.accumulated = new ArrayList<Double>();
	}
	
	public void addObservation(double feature) {
		this.accumulated.add(feature);
	}
	
	public void setMeanAndStdDevFromObservations() {
		double[] data = new double[this.accumulated.size()];
		for(int d=0; d<data.length; d++) {
			data[d] = this.accumulated.get(d);
		}
		this.setMeanAndStdDev(data);
		this.accumulated.clear();
		this.wasLearned = true;
	}
	
	private void setMeanAndStdDev(double[] data) {
		this.mean = 0.0;
		this.stdDev = 0.0;
		for(double val : data) {
			this.mean += val/data.length; // avoid overflow, but divide a lot
		}
		for(double val : data) {
			this.stdDev += Math.pow(val - this.mean, 2.0)/data.length; // avoid overflow, but divide a lot
		}
		this.wasLearned = true;
	}
	
	public double normalize(double val) {
		if(!wasLearned) {
			return val;
		}
		return (val - this.mean)/this.stdDev;
	}
	
	public double[] normalize(double[] unnormalized) {
		if(!wasLearned) {
			return unnormalized;
		}
		double[] arr = new double[unnormalized.length];
		for(int i=0; i<arr.length; i++) {
			arr[i] = this.normalize(unnormalized[i]);
		}
		return arr;
	}

	public String toString() {
		return "mean: "+this.mean+", sd: "+stdDev;
	}
	
}
