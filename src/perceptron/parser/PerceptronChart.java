package perceptron.parser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import illinoisParser.Chart;
import illinoisParser.Grammar;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;
import illinoisParser.TAGSET;

public class PerceptronChart extends Chart implements Externalizable {
	
	private int[] punctDistance, verbDistance;
	
	public PerceptronChart(){}
	
	public PerceptronChart(Sentence sentence, Grammar grammar) {
		super(sentence, grammar);
		punctDistance = new int[sentence.length()];
		verbDistance = new int[sentence.length()];
		int pc = 0;
		int vc = 0;
		LexicalToken[] lts = sentence.getTokens();
		for(int i=0; i<sentence.length(); i++) {
			if(TAGSET.Punct(lts[i].getPOS())) {
				pc++;
			}
			if(TAGSET.verb(lts[i].getPOS())) {
				vc++;
			}
			punctDistance[i] = pc;
			verbDistance[i] = vc;
		}
	}
	
	// TODO: check that we need to shift the distance by one (for neighbors)
	public int punctDist(int leftIndex, int rightIndex) {
		return punctDistance[rightIndex-1] - punctDistance[leftIndex];
	}
	
	public int verbDist(int leftIndex, int rightIndex) {
		return verbDistance[rightIndex-1] - verbDistance[leftIndex];
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		super.readExternal(in);
		punctDistance = (int[]) in.readObject();
		verbDistance = (int[]) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO: do we need to store grammar?
		super.writeExternal(out);
		out.writeObject(punctDistance);
		out.writeObject(verbDistance);
	}

}
