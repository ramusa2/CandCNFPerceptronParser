package supertagger.lsbeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.ArrayList;

import multitagger.layers.EmbeddingMultitaggerInputLayer;

import org.jblas.DoubleMatrix;

import supertagger.nn.StringEmbeddings;
import supertagger.nn.Module;

public class FirstClassifierContextLookupLayer extends EmbeddingMultitaggerInputLayer {
	
	private int CXT_WINDOW_SIZE;
	
	private int CAT_EMBEDDING_DIM;
	
	private int NUM_CATS_IN_LIST;
	
	private int CXT_DEPTH;
	
	private int WORD_EMBEDDING_DIM;
	
	public FirstClassifierContextLookupLayer(int numCatsToLookAt, int contextWindowSize, 
			int wordEmbeddingDimension, int categoryEmbeddingDimension,  int surroundingContextDepth,
			StringEmbeddings learnedWordEmbeddings, StringEmbeddings initialCategoryEmbeddings) {
		super(learnedWordEmbeddings, wordEmbeddingDimension, 
				initialCategoryEmbeddings, categoryEmbeddingDimension);
		this.CXT_WINDOW_SIZE = contextWindowSize;
		this.CAT_EMBEDDING_DIM = categoryEmbeddingDimension;
		this.NUM_CATS_IN_LIST = numCatsToLookAt;
		this.WORD_EMBEDDING_DIM = wordEmbeddingDimension;
		this.CXT_DEPTH = surroundingContextDepth;
	}
	/*
	public DoubleMatrix getInput(MultitaggerTrainingSentence sentence, int i) {
		this.currentSentence = sentence;
		this.currentPosition = i;
		int index = 0;
		ArrayList<MultitaggerTrainingItem> items =  sentence.getItems();
		// Add word embedding to input
		DoubleMatrix wordEmbedding = getWordEmbedding(sentence.sentence().getTokens()[i].getWord());
		for(; index<wordEmbedding.length; index++) {
			this.cachedInputVector.put(index, wordEmbedding.get(index));
		}
		//System.out.println("Index after word embedding:"+(index-1));
		// Add cat embeddings and prob for this token to input
		MultitaggerTrainingItem thisItem = items.get(i);
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			this.cachedInputVector.put(index, thisItem.getProb(k));
			index++;
			DoubleMatrix vec = this.categoryEmbeddings.getVec(thisItem.getCat(k));
			for(int j=0; j<vec.length; j++) {
				this.cachedInputVector.put(index, vec.get(j));
				index++;
			}
		}
		//System.out.println("Index after cat/prob for three items in list:"+(index-1));
		// Add surrounding category context to list
		for(int c=-this.CXT_WINDOW_SIZE; c<=this.CXT_WINDOW_SIZE; c++) {
			if(c==0) {
				continue;
			}
			int j = i+c;
			DoubleMatrix cxtVec;
			if(j < 0){
				cxtVec = this.categoryEmbeddings.getVec(START);
			}
			else if(j >= sentence.sentence().length()) {
				cxtVec = this.categoryEmbeddings.getVec(END);
			}
			else {
				//cxtVec = this.getWordEmbedding(this.currentSentence.sentence().get(j).getWord());
				
				cxtVec = new DoubleMatrix(this.EMBEDDING_DIM, 1);
				MultitaggerTrainingItem item = items.get(j);
				for(int k=0; k<this.CXT_DEPTH; k++) {
					double itemProb = item.getProb(k);
					cxtVec.addi(this.getEmbedding(item.getCat(k)).mul(itemProb), cxtVec);
				}
				
			}
			for(int v=0; v<cxtVec.length; v++) {
				this.cachedInputVector.put(index, cxtVec.get(v));
				index++;
			}
			//System.out.println("Index after context window element:"+(index-1));
		}
		return this.cachedInputVector;
	}
	*/

	@Override
	public DoubleMatrix calculateGradientWithRespectToInput(DoubleMatrix input,
			DoubleMatrix nextGradient) {
		return null;
	}

	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {
		
		outputGradient.muli(learningRate, outputGradient);
		int index = 0;
		ArrayList<MultitaggerTrainingItem> items =  this.inputSentence.getItems();
		
		// update word embedding
		DoubleMatrix wordEmbedding = this.getWordEmbedding(this.inputSentence.sentence().getTokens()[this.inputPosition].getWord());
		for(int i=0; i<wordEmbedding.length; i++) {
			wordEmbedding.put(i, wordEmbedding.get(i)+outputGradient.get(index++));
		}
		
		// Add cat embeddings and prob for this token to input
		MultitaggerTrainingItem thisItem = items.get(this.inputPosition);
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			index++; // don't update prob
			DoubleMatrix vec = this.getCategoryEmbedding(thisItem.getCat(k));
			for(int j=0; j<vec.length; j++) {
				vec.put(j, vec.get(j)+outputGradient.get(index++));
			}
		}
		// Add surrounding category context to list
		for(int c=-this.CXT_WINDOW_SIZE; c<=this.CXT_WINDOW_SIZE; c++) {
			if(c==0) {
				continue;
			}
			int j = this.inputPosition+c;
			DoubleMatrix cxtVec;
			if(j < 0){
				cxtVec = this.getCategoryEmbedding(CAT_START);
				for(int v=0; v<cxtVec.length; v++) {
					cxtVec.put(v, cxtVec.get(v)+outputGradient.get(index++));
				}
			}
			else if(j >= this.inputSentence.sentence().length()) {
				cxtVec = this.getCategoryEmbedding(CAT_END);
				for(int v=0; v<cxtVec.length; v++) {
					cxtVec.put(v, cxtVec.get(v)+outputGradient.get(index++));
				}
			}
			else {
				/*
				DoubleMatrix itemVec = this.getWordEmbedding(this.currentSentence.sentence().get(j).getWord());
				for(int v=0; v<itemVec.length; v++) {
					itemVec.put(v, itemVec.get(v)+outputGradient.get(index+v));
				}
				index += this.WORD_DIM;
				*/
				MultitaggerTrainingItem item = items.get(j);
				for(int k=0; k<this.CXT_DEPTH; k++) {
					double itemProb = item.getProb(k);
					DoubleMatrix itemVec = this.getCategoryEmbedding(item.getCat(k));
					for(int v=0; v<itemVec.length; v++) {
						itemVec.put(v, itemVec.get(v)+outputGradient.get(index+v)*itemProb);
					}
				}
				index += this.CAT_EMBEDDING_DIM;
				
			}
		}		
	}

	public void saveWeightsToFile(File saveDir) {
		super.saveWeightsToFile(saveDir);
	}

	public void loadWeightsFromFile(File loadDir) {
		super.loadWeightsFromFile(loadDir);
	}

	public int getOutputDimension() {
		return this.CAT_EMBEDDING_DIM*(this.NUM_CATS_IN_LIST+2*this.CXT_WINDOW_SIZE)
				+ this.NUM_CATS_IN_LIST + this.WORD_EMBEDDING_DIM;
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int i) {
		int index = 0;
		ArrayList<MultitaggerTrainingItem> items =  sentence.getItems();
		// Add word embedding to input
		DoubleMatrix wordEmbedding = this.getWordEmbedding(sentence.sentence().getTokens()[i].getWord());
		for(; index<wordEmbedding.length; index++) {
			output.put(index, wordEmbedding.get(index));
		}
		//System.out.println("Index after word embedding:"+(index-1));
		// Add cat embeddings and prob for this token to input
		MultitaggerTrainingItem thisItem = items.get(i);
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			output.put(index, thisItem.getProb(k));
			index++;
			DoubleMatrix vec = this.getCategoryEmbedding(thisItem.getCat(k));
			for(int j=0; j<vec.length; j++) {
				output.put(index, vec.get(j));
				index++;
			}
		}
		//System.out.println("Index after cat/prob for three items in list:"+(index-1));
		// Add surrounding category context to list
		for(int c=-this.CXT_WINDOW_SIZE; c<=this.CXT_WINDOW_SIZE; c++) {
			if(c==0) {
				continue;
			}
			int j = i+c;
			DoubleMatrix cxtVec;
			if(j < 0){
				cxtVec = this.getCategoryEmbedding(CAT_START);
			}
			else if(j >= sentence.sentence().length()) {
				cxtVec = this.getCategoryEmbedding(CAT_END);
			}
			else {
				//cxtVec = this.getWordEmbedding(this.currentSentence.sentence().get(j).getWord());
				
				cxtVec = new DoubleMatrix(this.CAT_EMBEDDING_DIM, 1);
				MultitaggerTrainingItem item = items.get(j);
				for(int k=0; k<this.CXT_DEPTH; k++) {
					double itemProb = item.getProb(k);
					cxtVec.addi(this.getCategoryEmbedding(item.getCat(k)).mul(itemProb), cxtVec);
				}
				
			}
			for(int v=0; v<cxtVec.length; v++) {
				output.put(index, cxtVec.get(v));
				index++;
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(CXT_WINDOW_SIZE);
		out.writeInt(CXT_DEPTH);
		out.writeInt(NUM_CATS_IN_LIST);
		out.writeInt(WORD_EMBEDDING_DIM);
		out.writeInt(CAT_EMBEDDING_DIM);		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.CXT_WINDOW_SIZE = in.readInt();
		this.CXT_DEPTH = in.readInt();
		this.NUM_CATS_IN_LIST = in.readInt();
		this.WORD_EMBEDDING_DIM = in.readInt();
		this.CAT_EMBEDDING_DIM = in.readInt();
	}

}
