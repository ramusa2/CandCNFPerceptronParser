package multitagger.layers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;

public class SimpleInputLayer extends EmbeddingMultitaggerInputLayer {

	private int CXT_WINDOW_SIZE;

	private int CXT_DEPTH;

	private int NUM_CATS_IN_LIST;

	private int WORD_EMBEDDING_DIM;

	private int CAT_EMBEDDING_DIM;

	public SimpleInputLayer(int numCatsToLookAt, int contextWindowSize, 
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
		try {
			PrintWriter pw = new PrintWriter(new File(saveDir.getPath()+File.separator+"params"));
			pw.println("contextWindowsSize="+this.CXT_WINDOW_SIZE);
			pw.println("contextDepth="+this.CXT_DEPTH);
			pw.println("numCatsInList="+this.NUM_CATS_IN_LIST);
			pw.println("wordEmbeddingDim="+this.WORD_EMBEDDING_DIM);
			pw.println("catEmbeddingDim="+this.CAT_EMBEDDING_DIM);
			pw.close();
		} catch(Exception e) {
			System.out.println("Failed to save input layer parameters to disk.");
		}
	}

	public void loadWeightsFromFile(File loadDir) {
		super.loadWeightsFromFile(loadDir);
		try {
			Scanner sc = new Scanner(new File("mapping_dimensions"));
			this.CXT_WINDOW_SIZE = Integer.parseInt(sc.nextLine().split("=")[1]);
			this.CXT_DEPTH = Integer.parseInt(sc.nextLine().split("=")[1]);
			this.NUM_CATS_IN_LIST = Integer.parseInt(sc.nextLine().split("=")[1]);
			this.WORD_EMBEDDING_DIM = Integer.parseInt(sc.nextLine().split("=")[1]);
			this.CAT_EMBEDDING_DIM = Integer.parseInt(sc.nextLine().split("=")[1]);
			sc.close();
		} catch(Exception e) {
			System.out.println("Failed to load input layer parameters from disk.");
		}
	}

	public int getOutputDimension() {
		return this.WORD_EMBEDDING_DIM 								// Embedding of current word
				+ this.NUM_CATS_IN_LIST*(this.CAT_EMBEDDING_DIM+1)  // Embeddings and prob of categories in list
				+ (2*this.CXT_WINDOW_SIZE*this.CAT_EMBEDDING_DIM);	// Embeddings of surrounding categories
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int i) {
		ArrayList<MultitaggerTrainingItem> items =  sentence.getItems();
		int index = 0;
		// Add mapped word embedding to input
		DoubleMatrix wordEmbedding = this.getWordEmbedding(sentence.sentence().getTokens()[i].getWord());
		index = this.setSegment(output, wordEmbedding, index);

		// Add mapped category embeddings to input
		MultitaggerTrainingItem thisItem = items.get(i);
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			DoubleMatrix categoryEmbedding = this.getCategoryEmbedding(thisItem.getCat(k));
			index = this.setSegment(output, categoryEmbedding, index);
			output.put(index++, thisItem.getProb(k));
		}

		// Add surrounding category context to list
		for(int c=-this.CXT_WINDOW_SIZE; c<=this.CXT_WINDOW_SIZE; c++) {
			if(c==0) {
				continue;
			}
			int j = i+c;
			// Add cat vector
			DoubleMatrix cxtVec;
			if(j < 0){
				cxtVec = this.getCategoryEmbedding(CAT_START);
			}
			else if(j >= sentence.sentence().length()) {
				cxtVec = this.getCategoryEmbedding(CAT_END);
			}
			else {
				cxtVec = new DoubleMatrix(this.CAT_EMBEDDING_DIM, 1);
				MultitaggerTrainingItem item = items.get(j);
				for(int k=0; k<this.CXT_DEPTH; k++) {
					double itemProb = item.getProb(k);
					cxtVec.addi(this.getCategoryEmbedding(item.getCat(k)).mul(itemProb), cxtVec);
				}

			}
			index = this.setSegment(output, cxtVec, index);
			/*
			// Add word vector
			DoubleMatrix wordVec;
			if(j < 0){
				wordVec = this.getWordEmbedding(WORD_START);
			}
			else if(j >= sentence.sentence().length()) {
				wordVec = this.getWordEmbedding(WORD_END);
			}
			else {
				wordVec = this.getWordEmbedding(sentence.sentence().get(i).getWord());

			}
			index = this.setSegment(output, wordVec, index);
			 */
		}
	}

	private int setSegment(DoubleMatrix output,
			DoubleMatrix embedding, int index) {
		for(int i=0; i<embedding.length; i++) {
			output.put(index+i, embedding.get(i));
		}
		return index + embedding.length;
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
