package util.serialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import org.nustaq.serialization.FSTConfiguration;

import perceptron.parser.PerceptronChart;
import profiling.ProfilingRunner;

import illinoisParser.Chart;
import illinoisParser.Grammar;

public class ChartDeserializationCallable implements Callable<Chart> {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static final ThreadLocal<FSTConfiguration> conf = new ThreadLocal() {
	    public FSTConfiguration initialValue() {
	      return FSTConfiguration.createDefaultConfiguration();
	    }};



	private PerceptronChart readPerceptronChart(InputStream stream) throws IOException, ClassNotFoundException
	{
		return FastSerializationUtil.readPerceptronChart(stream);
	}

	private int readInt(InputStream stream) throws IOException
	{
		return FastSerializationUtil.readInt(stream);
		/*
		FSTObjectInput in = conf.getObjectInput(stream);
		int i = in.readInt();
		return i;
		*/
	}

	private String[] filenames;
	int curSection;
	int curFile;
	int filesInSection;
	ObjectInputStream input;

	private Grammar myGrammar;

	@SuppressWarnings("unused")
	public ChartDeserializationCallable(String coarseParseDir, Grammar grammar) {
		/*
		conf = FSTConfiguration.createDefaultConfiguration();
		conf.registerClass(PerceptronChart.class, CoarseChartItem.class, BackPointer.class, 
				Sentence.class, Cell.class, Chart.class);
				*/
		myGrammar = grammar;
		curSection = 0;
		File dir = new File(coarseParseDir);
		String[] files = dir.list();
		ArrayList<String> filesS = new ArrayList<String>();
		if(!coarseParseDir.endsWith("/")) {
			coarseParseDir+="/";
		}
		for(int f=0; f<files.length; f++) {
			String fn = files[f];
			if(fn.endsWith(".coarseparses.externalized.gz") && ProfilingRunner.USE_GZIP) {
				filesS.add(fn);
			}
			else if(fn.endsWith(".coarseparses.externalized")) {
				filesS.add(fn);
			}
		}
		filenames = new String[filesS.size()];
		for(int f=0; f<filenames.length; f++) {
			filenames[f] = coarseParseDir+filesS.get(f);
		}
		openSection();
	}

	@Override
	public Chart call() throws Exception {
		if(curFile == filesInSection) {
			if(curSection == filenames.length) {
				return null;
			}
			openSection();
		}
		PerceptronChart chart = null;
		try {
			if(ProfilingRunner.USE_FST) {
				chart = this.readPerceptronChart(input);
				//chart = FastSerializationUtil.readPerceptronChart(input);	 	
			}
			else {
				chart = (PerceptronChart) input.readObject();	
			}    
			chart.grammar = myGrammar;	
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		curFile++;
		return chart;
	}

	private void openSection() {
		try {
			FileInputStream fi = new FileInputStream(filenames[curSection]);
			if(ProfilingRunner.USE_GZIP) {
				GZIPInputStream gi = new GZIPInputStream(fi);
				input = new ObjectInputStream(gi);
			}
			else {
				input = new ObjectInputStream(fi);
			}
			FastSerializationUtil.setInputStream(input);
			curFile = 0;
			if(ProfilingRunner.USE_FST) {
				filesInSection = this.readInt(input);
				//filesInSection = FastSerializationUtil.readInt(input);
			}
			else {
				filesInSection = input.readInt();
			}
			curSection++;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void reset() {
		try {
			curSection = 0;
			input.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		openSection();
	}

	public void close() {
		try {
			curSection = 0;
			input.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public int size() {
		return filesInSection;
	}
}
