package multitagger.layers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;


import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;

public class ContextSensitiveFirstClassifierLookupLayer extends EmbeddingMultitaggerInputLayer {
	
	private int CXT_WINDOW_SIZE;
	
	private int CAT_EMBEDDING_DIM;
	
	private int NUM_CATS_IN_LIST;
	
	private int CXT_DEPTH;
	
	private int WORD_EMBEDDING_DIM;
	
	protected int MAPPING_DIMENSION;
	
	protected ArrayList<LinearLayer> mappingFunctions;
	
	protected ArrayList<Integer> mappingDimensions;
	protected ArrayList<int[]> segmentIndices;
	
	protected ArrayList<DoubleMatrix> inputVectors;
	protected ArrayList<DoubleMatrix> outputSegments;
	
	public ContextSensitiveFirstClassifierLookupLayer(int numCatsToLookAt, int contextWindowSize, 
			int wordEmbeddingDimension, int categoryEmbeddingDimension,  int surroundingContextDepth,
			StringEmbeddings learnedWordEmbeddings, StringEmbeddings initialCategoryEmbeddings,
			int mappingDimension) {
		super(learnedWordEmbeddings, wordEmbeddingDimension, 
				initialCategoryEmbeddings, categoryEmbeddingDimension);
		this.CXT_WINDOW_SIZE = contextWindowSize;
		this.CAT_EMBEDDING_DIM = categoryEmbeddingDimension;
		this.NUM_CATS_IN_LIST = numCatsToLookAt;
		this.WORD_EMBEDDING_DIM = wordEmbeddingDimension;
		this.CXT_DEPTH = surroundingContextDepth;
		this.MAPPING_DIMENSION = mappingDimension;
		
		// Set up mapping functions
		this.mappingFunctions = new ArrayList<LinearLayer>();
		this.inputVectors = new ArrayList<DoubleMatrix>();
		this.mappingDimensions = new ArrayList<Integer>();
		this.segmentIndices = new ArrayList<int[]>();
		// Add fields for current word vector
		this.addInputSegment(wordEmbeddingDimension);
		// Add fields for categories in supertagger list
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			this.addInputSegment(categoryEmbeddingDimension);
			this.addInputSegment(1);
		}
		// Add fields for surrounding categories
		for(int c=-this.CXT_WINDOW_SIZE; c<=this.CXT_WINDOW_SIZE; c++) {
			if(c==0) {
				continue;
			}
			this.addInputSegment(categoryEmbeddingDimension);
			this.addInputSegment(wordEmbeddingDimension);
		}
	}
	
	protected void addInputSegment(int dimension) {
		this.mappingFunctions.add(new LinearLayer(dimension, this.MAPPING_DIMENSION));
		this.segmentIndices.add(buildIndexArrayInclusive(this.getOutputDimension(), this.MAPPING_DIMENSION));
		this.mappingDimensions.add(this.MAPPING_DIMENSION);
		this.inputVectors.add(new DoubleMatrix(dimension, 1));
		//this.outputSegments.add(new DoubleMatrix(1, this.MAPPING_DIMENSION));
	}
	
	protected void addInputSegmentWithExtraField(int dimension) {
		this.mappingFunctions.add(new LinearLayer(dimension, this.MAPPING_DIMENSION));
		this.segmentIndices.add(buildIndexArrayInclusive(this.getOutputDimension(), this.MAPPING_DIMENSION));
		this.mappingDimensions.add(this.MAPPING_DIMENSION);
		this.inputVectors.add(new DoubleMatrix(1, dimension));
		//this.outputSegments.add(new DoubleMatrix(1, this.MAPPING_DIMENSION));
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
		for(int segment=0; segment<this.mappingFunctions.size(); segment++) {
			this.updateSegment(segment, outputGradient.get(this.segmentIndices.get(segment)), learningRate);
		}
		/*
		
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
				
				//DoubleMatrix itemVec = this.getWordEmbedding(this.currentSentence.sentence().get(j).getWord());
				//for(int v=0; v<itemVec.length; v++) {
				//	itemVec.put(v, itemVec.get(v)+outputGradient.get(index+v));
				//}
				//index += this.WORD_DIM;
				
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
*/
	}

	private void updateSegment(int segment, DoubleMatrix outputGradient,
			double learningRate) {
		DoubleMatrix input = this.inputVectors.get(segment);
		DoubleMatrix otherGradient = this.mappingFunctions.get(segment).calculateGradientWithRespectToInput(input, outputGradient);
		this.mappingFunctions.get(segment).updateParameters(
				this.inputVectors.get(segment), outputGradient, learningRate);
		otherGradient.muli(learningRate);
		this.inputVectors.get(segment).addi(otherGradient);
	}

	public void saveWeightsToFile(File saveDir) {
		super.saveWeightsToFile(saveDir);
	}

	public void loadWeightsFromFile(File loadDir) {
		super.loadWeightsFromFile(loadDir);
	}

	public int getOutputDimension() {
		int dim = 0;
		for(Integer d : this.mappingDimensions) {
			dim += d;
		}
		return dim;
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int i) {
		ArrayList<MultitaggerTrainingItem> items =  sentence.getItems();
		int segment = 0;
		// Add mapped word embedding to input
		DoubleMatrix wordEmbedding = this.getWordEmbedding(sentence.sentence().getTokens()[i].getWord());
		this.setSegmentOutput(output, wordEmbedding, segment++);
		
		// Add mapped category embeddings to input
		MultitaggerTrainingItem thisItem = items.get(i);
		for(int k=0; k<this.NUM_CATS_IN_LIST; k++) {
			DoubleMatrix categoryEmbedding = this.getCategoryEmbedding(thisItem.getCat(k));
			this.setSegmentOutput(output, categoryEmbedding, segment++);
			this.setSegmentOutput(output, new DoubleMatrix(new double[][]{new double[]{thisItem.getProb(k)}}), segment++);
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
			this.setSegmentOutput(output, cxtVec, segment++);
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
			this.setSegmentOutput(output, wordVec, segment++);
		}
	}

	private void setSegmentOutput(DoubleMatrix output, DoubleMatrix embedding, int segIndex) {
		int[] indices = this.segmentIndices.get(segIndex);
		this.inputVectors.set(segIndex, embedding);
		LinearLayer map = this.mappingFunctions.get(segIndex);
		DoubleMatrix result = map.output(embedding);
		output.put(indices, result);
	}

	
	private void setSegmentOutput(DoubleMatrix output, DoubleMatrix embedding, double prob, int segIndex) {
		int[] indices = this.segmentIndices.get(segIndex);
		LinearLayer map = this.mappingFunctions.get(segIndex);
		DoubleMatrix result = map.output(embedding);
		output.put(indices, result);
		output.put(indices[indices.length-1]+1, prob);
	}
	
	protected int[] buildIndexArrayInclusive(int start, int range) {
		int[] arr = new int[range];
		for(int i=0; i<arr.length; i++) {
			arr[i] = start+i;
		}
		return arr;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(CXT_WINDOW_SIZE);
		out.writeInt(CXT_DEPTH);
		out.writeInt(NUM_CATS_IN_LIST);
		out.writeInt(WORD_EMBEDDING_DIM);
		out.writeInt(CAT_EMBEDDING_DIM);		
		out.writeObject(this.mappingFunctions);
		out.writeObject(this.mappingDimensions);
		out.writeObject(this.segmentIndices);
		out.writeObject(this.inputVectors);
		out.writeObject(this.outputSegments);
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
		this.mappingFunctions = (ArrayList<LinearLayer>) in.readObject();
		this.mappingDimensions = (ArrayList<Integer>) in.readObject();
		this.segmentIndices = (ArrayList<int[]>) in.readObject();
		this.inputVectors = (ArrayList<DoubleMatrix>) in.readObject();
		this.outputSegments = (ArrayList<DoubleMatrix>) in.readObject();
	}

}
