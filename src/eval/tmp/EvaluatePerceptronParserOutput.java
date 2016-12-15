package eval.tmp;

import illinoisParser.AutoDecoder;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Sentence;
import illinoisParser.Tree;
import illinoisParser.models.BaselineModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import eval.Dep;
import eval.DepSet;

public class EvaluatePerceptronParserOutput {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String parsedOutput = "eval.auto";
		String targetFile = "combined_training/eval.parsed.auto";
		ArrayList<String> autoViterbi = getAutoViterbi(parsedOutput);
		PrintWriter pw = new PrintWriter(new File(targetFile));
		for(String sen : autoViterbi) {
			pw.println(sen.trim());
		}
		pw.close();
		evaluate(parsedOutput);
	}

	private static void evaluate(String parsedOutput) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(parsedOutput));
		double total = 0;
		int failures = 0;
		int tooLong = 0;

		double totalDeps = 0.0;
		double predictedDeps = 0.0;
		double matchedLabeled = 0.0;
		double matchedUnlabeled = 0.0;


		String line = null;
		String auto;
		String lastLine;
		PrintWriter pw = new PrintWriter(new File("eval.parsed.auto"));
		PrintWriter pfpw = new PrintWriter(new File("eval.failures.auto"));
		//PrintWriter pw = new PrintWriter(new File("wsj0.parsed.auto"));
		//PrintWriter pfpw = new PrintWriter(new File("wsj0.failures.auto"));
		while(sc.hasNextLine()) {
			lastLine = line;
			line = sc.nextLine().trim();
			if(!line.isEmpty()) {
				if(line.startsWith("(<T")) {
					total++;
					auto = line;
					pw.println(auto);
					line = sc.nextLine().trim();
					if(line.startsWith("<s>")) {
						DepSet deps = null;
						deps = new DepSet(Integer.parseInt(line.substring(4)));
						while(!line.isEmpty()) {
							line = sc.nextLine().trim();
							if(line.startsWith("<\\s>")) {
								break;
							}
							else if(!line.isEmpty()) {
								deps.addDep(new Dep(line));
								String[] toks = line.split("\\s+");
								deps.addWord(Integer.parseInt(toks[0]), toks[4]);
								deps.addWord(Integer.parseInt(toks[1]), toks[5]);
							}
						}
						Sentence sen = new Sentence(lastLine);
						AutoDecoder decoder = new AutoDecoder(sen, auto);
						Tree<? extends FineChartItem> gold = decoder.buildTree(new BaselineModel(new Grammar()));
						DepSet goldDeps = DepSet.getDepSetFromPargEntry(gold.buildPargString(sen));
						totalDeps += goldDeps.size();
						predictedDeps += deps.size();
						matchedLabeled += deps.numMatchedLabeled(goldDeps);
						matchedUnlabeled += deps.numMatchedUnlabeled(goldDeps);
						
					}
					else if(line.equals("TOO_LONG")) {
						tooLong++;
					}
					else if(line.equals("PARSE_FAILURE")) {
						pfpw.println(auto);
						failures++;
					}					
				}	
			}
		}
		sc.close();
		pw.close();
		pfpw.close();
		System.out.println(total+ " total sentences");
		System.out.println(tooLong+ " were too long");
		System.out.println(failures+ " parse failures\n");
		System.out.println("Results on remaining "+(total-tooLong-failures)+" sentences:");
		
		System.out.println("Labeled recall:       "+(matchedLabeled/totalDeps));
		System.out.println("Labeled precision:    "+(matchedLabeled/predictedDeps)+"\n");
		
		System.out.println("Unabeled recall:      "+(matchedUnlabeled/totalDeps));
		System.out.println("Unlabeled precision:  "+(matchedUnlabeled/predictedDeps)+"\n");
	}

	private static ArrayList<String> getAutoViterbi(String parsedOutput) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(parsedOutput));
		ArrayList<String> list = new ArrayList<String>();
		String line;
		while(sc.hasNextLine()) {
			line = sc.nextLine().trim();
			if(line.startsWith("(<T")) {
				list.add(line);
			}
			else if(line.equals("TOO_LONG")) {
				list.add("");
			}
			else if(line.equals("PARSE_FAILURE")) {
				list.add("");
			}		
		}
		sc.close();
		return list;
	}

}
