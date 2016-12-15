package neuralnet.test.tictactoe;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

import neuralnet.input.ContextExtractor;

public class TicTacToeContextExtractor extends ContextExtractor<TicTacToe> implements Externalizable {
	
	private int dimension;
	
	private DoubleMatrix output;
	
	public TicTacToeContextExtractor(int boardDimension) {
		this.dimension = boardDimension*boardDimension;
		this.output = new DoubleMatrix(this.dimension, 1);
	}

	@Override
	public DoubleMatrix extract(TicTacToe context) {
		for(int i=0; i<this.output.length; i++) {
			this.output.put(i, context.board[i]);
		}
		return this.output;
	}

	@Override
	public void updateParameters(DoubleMatrix gradient) {
		// Don't update (no parameters, keep boards fixed).
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.dimension);
		out.writeObject(this.output);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.dimension = in.readInt();
		this.output = (DoubleMatrix) in.readObject();
	}

	@Override
	public int getOutputDimension() {
		return this.dimension;
	}
}
