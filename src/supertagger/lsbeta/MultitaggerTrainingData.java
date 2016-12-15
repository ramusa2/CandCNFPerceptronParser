package supertagger.lsbeta;

import illinoisParser.Sentence;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;

public class MultitaggerTrainingData implements Externalizable {
	
	private ArrayList<MultitaggerTrainingSentence> data;
	
	private int topK;
	
	public MultitaggerTrainingData() {}
	
	public MultitaggerTrainingData(int k) {
		this.topK = k;
		this.data = new ArrayList<MultitaggerTrainingSentence>();
	}

	public void addSentence(MultitaggerTrainingSentence sen) {
		this.data.add(sen);
	}	
	
	public void addItem(MultitaggerTrainingItem item) {
		MultitaggerTrainingSentence temp = new MultitaggerTrainingSentence();
		temp.addItem(item);
		this.addSentence(temp);
	}

	public MultitaggerTrainingData(LSSupertagger tagger,
			Collection<Sentence> sentences, int topK) {
		this();
		this.generateAndOverwriteData(tagger, sentences, topK);
	}
	
	public ArrayList<MultitaggerTrainingSentence> getData() {
		return this.data;
	}

	public ArrayList<MultitaggerTrainingSentence> generateAndOverwriteData(LSSupertagger tagger,
			Collection<Sentence> sentences, int topK) {
		this.topK = topK;
		this.data = new ArrayList<MultitaggerTrainingSentence>();
		int s = 0;
		for(Sentence sen : sentences) {
			if(s%(sentences.size()/10) == 0) {
				System.out.println("Supertagging data; "+(s/(sentences.size()/10))+"0% complete.");
			}
			SupertagAssignment tags = tagger.tagSentence(sen);
			this.data.add(new MultitaggerTrainingSentence(tags, topK));
			s++;
		}
		return this.data;
	}


	public void saveToFile(File file) {
	      try {
	         FileOutputStream fileOut =
	         new FileOutputStream(file);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(this);
	         out.close();
	         fileOut.close();
	      } catch(IOException i) {
	          i.printStackTrace();
	      }
	}
	
	public static MultitaggerTrainingData loadMultitaggerTrainingData(File file) {
	      MultitaggerTrainingData data = null;
	      try
	      {
	         FileInputStream fileIn = new FileInputStream(file);
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         data = (MultitaggerTrainingData) in.readObject();
	         in.close();
	         fileIn.close();
	      }catch(IOException i)
	      {
	         i.printStackTrace();
	         return null;
	      }catch(ClassNotFoundException c)
	      {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return null;
	      }
	      return data;
	}


	public void generateAndAddToData(LSSupertagger tagger,
			Collection<Sentence> newData) {
		int s = 0;
		for(Sentence sen : newData) {
			if(s%(newData.size()/10) == 0) {
				System.out.println("Supertagging data; "+(s/(newData.size()/10))+"0% complete.");
			}
			SupertagAssignment tags = tagger.tagSentence(sen);
			this.data.add(new MultitaggerTrainingSentence(tags, topK));
			s++;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.topK);
		out.writeObject(this.data);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.topK = in.readInt();
		this.data = (ArrayList<MultitaggerTrainingSentence>) in.readObject();
	}

}
