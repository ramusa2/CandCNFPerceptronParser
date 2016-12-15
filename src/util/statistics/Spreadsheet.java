package util.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class Spreadsheet {
	
	private String name;
	
	private String xAxis, yAxis;

	private ArrayList<SpreadsheetColumn> columns;
	private HashMap<String, Integer> columnNames;

	public Spreadsheet(String ssName, String ssXaxis, String ssYaxis) {
		this.name = ssName;
		this.xAxis = ssXaxis;
		this.yAxis = ssYaxis;
		this.columns = new ArrayList<SpreadsheetColumn>();
		this.columnNames = new HashMap<String, Integer>();
	}

	public synchronized void addColumn(String header) {
		this.columnNames.put(header, this.columns.size());
		this.columns.add(new SpreadsheetColumn(header));
	}

	public void writeToCSV() {
		writeToFile(this.name+".csv", ",", this.columns);
	}

	public void writeToCSV(String... headersToInclude) {
		ArrayList<SpreadsheetColumn> cols = new ArrayList<SpreadsheetColumn>();
		for(String header : headersToInclude) {
			Integer id = this.columnNames.get(header);
			if(id != null) {
				cols.add(this.columns.get(id));
			}
		}
		writeToFile(this.name+".csv", ",", cols);
	}

	private void writeToFile(String filename, String delimiter, ArrayList<SpreadsheetColumn> cols) {
		try {
			int numRows = getNumRows();
			PrintWriter pw = new PrintWriter(new File(filename));
			StringBuilder row = new StringBuilder();
			boolean first = true;
			for(SpreadsheetColumn col : cols) {
				if(first) {						
					first = false;
				}
				else {
					row.append(delimiter);
				}
				row.append("\"");
				row.append(col.header());
				row.append("\"");
			}
			pw.println(row.toString());
			for(int r = 0; r<numRows; r++) {
				row = new StringBuilder();
				first = true;
				for(SpreadsheetColumn col : cols) {
					if(first) {						
						first = false;
					}
					else {
						row.append(delimiter);
					}
					row.append(col.dataString(r));
				}
				pw.println(row.toString());
			}

			pw.close();
		}
		catch(FileNotFoundException e) {
			System.err.println("Failed to write spreadsheet to .csv: "+filename+" not found.");
		}
	}	

	private int getNumRows() {
		int numRows = 0;
		for(SpreadsheetColumn col : this.columns) {
			numRows = Math.max(numRows, col.length());
		}
		return numRows;
	}

	public synchronized void addColumn(SpreadsheetColumn col) {
		this.columnNames.put(col.header(), this.columns.size());
		this.columns.add(col);
		
	}

	public void appendData(String colHeader, Object data) {
		SpreadsheetColumn col = this.getColumn(colHeader);
		if(col != null) {
			col.append(data);
		}
	}

	private SpreadsheetColumn getColumn(String header) {
		Integer id = this.columnNames.get(header);
		if(id != null) {
			return this.columns.get(id);
		}
		return null;
	}
}
