package util.serialization;

import illinoisParser.LexicalToken;
import illinoisParser.POS;
import illinoisParser.Sentence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;


import perceptron.parser.PerceptronChart;

public class FastSerializationUtil {

	static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
	static FSTObjectInput in;
	static FSTObjectOutput out;
	
	static {
		  conf.registerSerializer(Sentence.class, new SentenceFSTSerializer(), false);

			conf.registerClass(LexicalToken.class, POS.class);
		//conf.registerClass(Sentence.class, LexicalToken.class, POS.class);
		//conf.registerClass(PerceptronChart.class, CoarseChartItem.class, BackPointer.class, 
		//		Sentence.class, Cell.class, Chart.class);
	}
	
	public static void setInputStream(InputStream stream) {
		in = conf.getObjectInput(stream);
	}
	
	public static void closeInputStream() {
		if(in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void setOutputStream(OutputStream stream) {
		out = conf.getObjectOutput(stream);
	}
	
	public static void closeOutputStream() {
		if(in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public static PerceptronChart readPerceptronChart(InputStream stream) throws IOException, ClassNotFoundException
	{
		Sentence sen = null;
		try {
			sen = (Sentence) in.readObject(Sentence.class);
			//sen = (Sentence) in.readObject(Sentence.class);

			/*
			LexicalToken[] sentenceLT = (LexicalToken[]) in.readObject();
			LexicalToken[]sentence_wP = (LexicalToken[]) in.readObject(); 
			int id = in.readInt();
			String autoString = (String) in.readObject();
			String pargString = (String) in.readObject();
			sen = new Sentence();
			*/
			//stream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new PerceptronChart(sen, null);
		/*
		FSTObjectInput in = conf.getObjectInput(stream);
		PerceptronChart result = null;
		try {
			result = (PerceptronChart) in.readObject(PerceptronChart.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
		*/
	}

	public static void writePerceptronChart( OutputStream stream, PerceptronChart toWrite) throws IOException 
	{
		out.writeObject(toWrite.getSentence(), Sentence.class);
		out.flush();
		/*
		FSTObjectOutput out = conf.getObjectOutput(stream);
		out.writeObject(toWrite, PerceptronChart.class);
		out.flush();
		*/
	}

	public static int readInt(InputStream stream) throws IOException
	{
		FSTObjectInput in = conf.getObjectInput(stream);
		int i = in.readInt();
		return i;
	}

	public static void writeInt(OutputStream stream, int i) throws IOException {
		FSTObjectOutput out = conf.getObjectOutput(stream);
		out.writeInt(i);
		out.flush();
	}

}
