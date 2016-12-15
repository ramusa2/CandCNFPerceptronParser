package multitagger.experimentcode;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class GridSearchResult implements Comparable<GridSearchResult> {

	private int expID, iterID;

	private double f1, tp, tn, fp, fn;

	public GridSearchResult(int EXPID, int ITERID, double F1, double TP, double TN, double FP, double FN) {
		this.expID = EXPID;
		this.iterID = ITERID;
		this.f1 = F1;
		this.tp = TP;
		this.tn = TN;
		this.fp = FP;
		this.fn = FN;
	}	

	public static ArrayList<GridSearchResult> readFromLogFile(File file) {
		ArrayList<GridSearchResult> list = new ArrayList<GridSearchResult>();
		try {
			Scanner sc = new Scanner(file);
			String line;
			int expID = Integer.parseInt(file.getName().substring(3, file.getName().indexOf('.')));
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(line.startsWith("Evaluating after iteration")) {
					int iter = Integer.parseInt(line.split(" ")[3]);
					double f1 = Double.parseDouble(sc.nextLine().trim().split("\\s+")[4]);
					double tp = Double.parseDouble(sc.nextLine().trim().split("\\s+")[3]);
					double tn = Double.parseDouble(sc.nextLine().trim().split("\\s+")[3]);
					double fp = Double.parseDouble(sc.nextLine().trim().split("\\s+")[3]);
					double fn = Double.parseDouble(sc.nextLine().trim().split("\\s+")[3]);
					list.add(new GridSearchResult(expID, iter, f1, tp, tn, fp, fn));
				}
			}
			sc.close();
		}
		catch(Exception e) {
			System.err.println("Failed to parse grid search experiment log file: "+file.getName());
			e.printStackTrace();
		}
		return list;
	}

	public String getCSVString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.iterID+",");
		sb.append(this.expID+",");
		sb.append(this.f1+",");
		sb.append(this.tp+",");
		sb.append(this.tn+",");
		sb.append(this.fp+",");
		sb.append(this.fn/*+","*/);
		/*
		sb.append(this+",");
		sb.append(this+",");
		sb.append(this+",");
		 */
		return sb.toString();
	}

	@Override
	public int compareTo(GridSearchResult o) {
		return (int) Math.signum(o.f1 - this.f1);
	}

}
