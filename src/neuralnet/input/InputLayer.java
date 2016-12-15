package neuralnet.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import neuralnet.FeatureNormalizer;

import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Solve;

public abstract class InputLayer<T> implements Externalizable {

	/**
	 * True until we first ask for input, then false (output can't change dimension after we've used it)
	 */
	//private boolean canStillAddSegments;
	// TODO: create ConcatenatedInputLayer that can concatenate separate lookups
	
	public InputLayer(){}

	private FeatureNormalizer[] normalizers;
	
	private DoubleMatrix whitener;
	
	private boolean wasWhitened;
	
	private DoubleMatrix means;

	protected boolean standardized;
	
	protected int dimension;

	protected InputLayer(int dim, boolean standardizeFeatures) {
		this.standardized = standardizeFeatures;
		this.dimension = dim;
		this.normalizers = new FeatureNormalizer[this.dimension];
		for(int i=0; i<this.normalizers.length; i++) {
			this.normalizers[i] = new FeatureNormalizer();
		}
		this.whitener = new DoubleMatrix(this.dimension, this.dimension);
		this.means = new DoubleMatrix(this.dimension, 1);
		this.wasWhitened = false;
	}

	public void learnFeatureNormalizers(ArrayList<T> data) {
		if(!this.standardized) {
			System.out.println("Skipping feature standardization (set flag in input layer constructor if desired).");
			return;
		}
		// Add observations
		for(T t : data) {
			DoubleMatrix vec = this.getOutput(t);
			for(int i=0; i<vec.length; i++) {
				this.normalizers[i].addObservation(vec.get(i));
			}
		}
		// Normalize
		for(FeatureNormalizer f : this.normalizers) {
			f.setMeanAndStdDevFromObservations();
		}
	}

	public void learnWhiteningMatrix(ArrayList<T> data) {
		this.means = getMeans(data);
		DoubleMatrix X = new DoubleMatrix(data.size(), this.dimension);
		// Center observations w.r.t. their means
		int row = 0;
		for(T t : data) {
			DoubleMatrix x_i = this.getOutput(t);
			X.putRow(row++, x_i.subi(means, x_i).transpose());
		}
		// Sphericize data
		DoubleMatrix M = X.transpose().mmul(X);
		M.divi(data.size(), M);
		DoubleMatrix[] ED = Eigen.symmetricEigenvectors(M);
		DoubleMatrix E = ED[0];
		DoubleMatrix D = ED[1];

		//DoubleMatrix D_invrt = Solve.pinv(sqrt(D));
		DoubleMatrix D_invrt = diagPow(D, -0.5);
		
		this.whitener = E.mmul(D_invrt);
		this.wasWhitened = true;
		
		checkSampleCovariance(X.mmul(this.whitener));
		
		//DoubleMatrix X_std = X.mmul(E).mmul(D_invrt);
	}

	private void checkSampleCovariance(DoubleMatrix X) {
		System.out.println("Verifying whitening...");
		// Get whitened data
		int n = X.rows;
		int m = X.columns;
		// Check means
		DoubleMatrix means = getMeans(X);
		boolean meansZero = true;
		for(double x : means.data) {
			meansZero = meansZero && Math.abs(x) < Math.pow(10, -10);
		}
		if(meansZero) {
			System.out.println("  PASS: Means are zero.");
		}
		else {
			System.out.println("  FAIL: Means are non-zero.");
		}
		// Fill covariance matrix
		DoubleMatrix Q = new DoubleMatrix(m, m);
		for(int j=0; j<m; j++) {
			for(int k=0; k<m; k++) {
				double q_jk = 0.0;
				for(int i=0; i<n; i++) {
					q_jk += (X.get(i,j) /*- means.get(j)*/)*(X.get(i,k) /*- means.get(k)*/)/(n);
				}
				Q.put(j, k, q_jk);				
			}
		}
		boolean covarianceIdentity = true;
		for(int j=0; j<m; j++) {
			for(int k=0; k<m; k++) {
				double target;
				if(j==k) {
					target = 1.0;
				}
				else {
					target = 0.0;
				}
				covarianceIdentity = covarianceIdentity && Math.abs(Q.get(j,k) - target) < Math.pow(10, -10);
			}
		}
		if(covarianceIdentity) {
			System.out.println("  PASS: Co-variance matrix is identity matrix.");
		}
		else {
			System.out.println("  FAIL: Co-variance matrix is not identity matrix.");
		}
	}

	/**
	 * Precondition: D is a diagonal matrix
	 */
	private DoubleMatrix diagPow(DoubleMatrix D, double pow) {
		// TODO: check for diagonal
		int dim = Math.min(D.rows, D.columns);
		DoubleMatrix D_sqrt = new DoubleMatrix(dim, dim);
		for(int i=0; i<dim; i++) {
			D_sqrt.put(i, i, Math.pow(D.get(i, i), pow));
		}
		return D_sqrt;
	}

	private DoubleMatrix getMeans(ArrayList<T> data) {
		DoubleMatrix means = new DoubleMatrix(this.dimension, 1);
		int N = data.size();
		for(T t : data) {
			DoubleMatrix vec = this.getOutput(t);
			means.addi(vec.divi(N), means);
		}
		return means;
	}


	private DoubleMatrix getMeans(DoubleMatrix X) {
		DoubleMatrix means = new DoubleMatrix(1, X.columns);
		for(int r=0; r<X.rows; r++) {
			means.addi(X.getRow(r).div(X.rows), means);
		}
		return means;
	}


	private DoubleMatrix outputVector;

	/**
	 * Interface with InputLayer; subclasses should overload this method
	 * with appropriate arguments in order to save state before returning 
	 * super(getOutput());
	 */
	public DoubleMatrix getOutput(T context) {
		if(this.outputVector == null) {
			this.outputVector = new DoubleMatrix(this.getOutputVectorDimension(), 1);
		}
		this.fillOutputVector(this.outputVector, context);
		if(this.wasWhitened) {
			this.outputVector = this.outputVector.subi(this.means, this.outputVector).transpose().mmuli(this.whitener).transpose();
		}
		else if(this.standardized) {
			for(int i=0; i<this.normalizers.length; i++) {
				this.outputVector.put(i, this.normalizers[i].normalize(this.outputVector.get(i)));
			}
		}
		return this.outputVector;
	}

	/**
	 * Subclasses need to use state to fill the vector
	 * @param output
	 */
	protected abstract void fillOutputVector(DoubleMatrix output, T context);

	public abstract int getOutputVectorDimension();

	/**
	 * Subclasses should use previously saved state to update parameters, if necessary
	 */
	public abstract void updateParameters(DoubleMatrix outputGradient);


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.outputVector);
		out.writeObject(this.normalizers);
		out.writeBoolean(this.standardized);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.outputVector = (DoubleMatrix) in.readObject();
		this.normalizers = (FeatureNormalizer[]) in.readObject();
		this.standardized = in.readBoolean();
	}
}
