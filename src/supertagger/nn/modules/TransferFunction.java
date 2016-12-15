package supertagger.nn.modules;

import org.jblas.DoubleMatrix;

import supertagger.nn.Module;

/**
 * A transfer function is a layer in the network that transforms its
 * inputs according to a particular function. Because the function doesn't
 * change, there aren't any parameters updates to make.
 * 
 * @author ramusa2
 *
 */
public abstract class TransferFunction extends Module {
	
	@Override
	public final void updateParameters(DoubleMatrix outputGradient, double learningRate){}

}
