package perceptron.core;

import java.util.ArrayList;
import java.util.Collection;


public class PerceptronTrainingData {
	
	private ArrayList<PerceptronExample> data;
	
	public PerceptronTrainingData() {
		data = new ArrayList<PerceptronExample>();
	}
	
	public void addExample(PerceptronExample ex) {
		data.add(ex);
	}
	
	public Collection<PerceptronExample> examples() {
		return data;
	}
	
	public int size() {
		return data.size();
	}
	
	public PerceptronExample get(int i) {
		return data.get(i);
	}

}
