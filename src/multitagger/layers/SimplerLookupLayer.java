package multitagger.layers;

import java.util.ArrayList;
import java.util.HashMap;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;

public class SimplerLookupLayer extends LookupInputLayer {

	protected int numCatsInList;

	protected int contextWidth;

	protected HashMap<String, Integer> frequentCategories;

	protected HashMap<String, Integer> categoryFrequencies;

	protected final String RARE_CAT = "**RARE_CAT**";

	protected int RARE_CAT_ID;

	protected final String START = "START_CAT";
	protected int START_ID;
	protected final int SEN_FREQ = 10000;	
	protected final String END = "END_CAT";
	protected int END_ID;

	protected String[] catsInList;
	protected double[] listProbs;

	public SimplerLookupLayer(int categoryEmbeddingDim, ArrayList<String> freqCats, 
			HashMap<String, Integer> catFreqs, 
			int numberOfCatsInList, int contextWindowWidth) {
		super(categoryEmbeddingDim);
		this.frequentCategories = new HashMap<String, Integer>();
		this.categoryFrequencies = catFreqs;
		this.numCatsInList = numberOfCatsInList;
		this.contextWidth = contextWindowWidth;
		for(int c=0; c<freqCats.size(); c++) {
			String cat = freqCats.get(c);
			this.frequentCategories.put(cat, c);
			this.addRandomVector(cat);
		}
		this.addRandomVector(RARE_CAT);
		this.RARE_CAT_ID = this.frequentCategories.size();
		this.START_ID = this.RARE_CAT_ID+1;
		this.END_ID = this.RARE_CAT_ID+2;
		//this.outputVector = new DoubleMatrix(1, this.getOutputDimension());
		this.outputVector = new DoubleMatrix(this.getOutputDimension(), 1);
		this.catsInList = new String[this.numCatsInList];
		this.listProbs = new double[this.numCatsInList];
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int position) {
		int index = 0;
		MultitaggerTrainingItem item = sentence.getItems().get(position);
		// Set features for index in list
		for(int c=0; c<this.numCatsInList; c++) {
			String cat = item.getCat(c);
			double catProb = item.getProb(c);
			double catLogProb = catProb > 0.0 ? Math.log(catProb) : Double.NEGATIVE_INFINITY;
			Integer catFreq = this.categoryFrequencies.get(cat);
			if(catFreq==null) {
				catFreq = 0;
			}
			double catLogFreq = catFreq > 0 ? Math.log(catProb) : -1;
			index = setCatFeature(cat, 0);
			

			catLogFreq /= 10;
			catFreq = 0;
			
			
			this.outputVector.put(index++, catProb);
			this.outputVector.put(index++, catLogProb);
			this.outputVector.put(index++, catFreq);
			this.outputVector.put(index++, catLogFreq);
			DoubleMatrix embed = this.getCategoryEmbedding(cat);
			for(int i=0; i<embed.length; i++) {
				this.outputVector.put(index++, embed.get(i));
			}
			this.catsInList[c] = cat;
			this.listProbs[c] = catProb;
		}
		// Set context features
		for(int j=-this.contextWidth; j<=this.contextWidth; j++) {
			if(j==0) {
				continue;
			}
			int p = position+j;
			Integer catID;
			double catProb;
			double catLogProb;
			Integer catFreq;
			double catLogFreq;
			if(p<0) {
				catID = START_ID;
				catProb = 1.0;
				catFreq = SEN_FREQ;
			}
			else if(p >= sentence.sentence().length()) {
				catID = END_ID;
				catProb = 1.0;
				catFreq = SEN_FREQ;
			}
			else {
				MultitaggerTrainingItem pItem =sentence.getItems().get(p); 
				catID = this.getCatID(pItem.getCat(0));
				catProb = pItem.getProb(0);
				catFreq = this.categoryFrequencies.get(pItem.getCat(0));
				if(catFreq == null) {
					catFreq = 0;
				}
			}
			catLogProb = catProb > 0.0 ? Math.log(catProb) : Double.NEGATIVE_INFINITY;
			catLogFreq = catFreq > 0 ? Math.log(catFreq) : -1.0;
			
			catLogFreq /= 10;
			catFreq = 0;
			
			index = this.setCatFeature(catID, index);
			this.outputVector.put(index++, catProb);
			this.outputVector.put(index++, catLogProb);
			this.outputVector.put(index++, catFreq);
			this.outputVector.put(index++, catLogFreq);
		}
	}

	@Override
	protected DoubleMatrix getCategoryEmbedding(String category) {
		DoubleMatrix vec = this.lookupTable.get(category);
		if(vec == null) {
			vec = this.lookupTable.get(RARE_CAT);
		}
		return vec;
	}

	private int setCatFeature(String cat, int index) {
		return this.setCatFeature(this.getCatID(cat), index);
	}

	private int getCatID(String cat) {
		Integer f = this.frequentCategories.get(cat);
		if(f==null) {
			f = this.RARE_CAT_ID;
		}	
		return f;
	}

	private int setCatFeature(int f, int index) {
		for(int i=0; i<this.END_ID; i++) {
			this.outputVector.put(index+i, (i==f) ? 1.0 : 0.0);
		}
		return index + this.END_ID;
	}

	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {
		int index = 0;
		// Set features for index in list
		for(int c=0; c<this.numCatsInList; c++) {
			index += 5+this.END_ID;
			String cat = this.catsInList[c];
			double prob = this.listProbs[c];
			DoubleMatrix embed = this.getCategoryEmbedding(cat);
			for(int i=0; i<embed.length; i++) {
				embed.put(i, embed.get(i) + learningRate*prob*outputGradient.get(index++));
			}
		}

	}

	@Override
	public int getOutputDimension() {
		int dim = this.numCatsInList*(this.lookupDimension+4+this.END_ID+1) // Features for categories in list 
				+ (2*this.contextWidth*(this.END_ID+1+4)); // Cat id, prob, and log prob for context window
		return dim;
	}

}
