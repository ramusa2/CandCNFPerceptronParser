package multitagger.experimentcode;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class GridSearchResultAnalysis {
	
	private static String RESULTS_DIR = "break_grid_search_results/dev";
	
	private static String OUTPUT_FILE = "break_grid_search_results/results.csv";
	
	public static void main(String[] args) throws Exception {
		summarize(RESULTS_DIR, OUTPUT_FILE);
		summarize("break_grid_search_results/15nodes", "break_grid_search_results/15results.csv");
		summarize("break_grid_search_results/50nodes", "break_grid_search_results/50results.csv");
		summarize("break_grid_search_results/100nodes", "break_grid_search_results/100results.csv");
		summarize("break_grid_search_results/200nodes", "break_grid_search_results/200results.csv");
	}
	
	public static void summarize(String RESULTS, String OUTPUT) throws Exception {
		
		File[] logFiles = (new File(RESULTS)).listFiles();
		ArrayList<GridSearchResult> results = new ArrayList<GridSearchResult>();
		for(File file : logFiles) {
			results.addAll(GridSearchResult.readFromLogFile(file));
		}
		Collections.sort(results);
		PrintWriter pw = new PrintWriter(new File(OUTPUT));
		String header = "Iter,ExpID,Acc,TP,TN,FP,FN";
		pw.println(header);
		for(GridSearchResult result : results) {
			String csv = result.getCSVString();
			//System.out.println(csv);
			pw.println(csv);
		}
		pw.close();
	}

}
