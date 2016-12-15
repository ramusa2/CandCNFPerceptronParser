package util.statistics;

import java.util.ArrayList;

public class SpreadsheetColumn<T> {
	
	private String header;

	private ArrayList<T> cells;
	
	public SpreadsheetColumn(String headerName) {
		this.header = headerName;
		this.cells = new ArrayList<T>();
	}
	
	public void append(T cellData) {
		this.cells.add(cellData);
	}
	
	public void set(int row, T cellData) {
		if(row < 0) {
			return;
		}
		while(row >= this.cells.size()) {
			this.cells.add(null);
		}
		this.cells.set(row, cellData);
	}
	
	public T get(int row) {
		if(row >= 0 && row <cells.size()) {
			return cells.get(row);
		}
		return null;
	}
	
	public int length() {
		return cells.size();
	}
	
	public ArrayList<T> data() {
		return this.cells;
	}
	
	public String header() {
		return this.header;
	}
	
	public String dataString(int row) {
		T data = this.get(row);
		if(data == null) {
			return "";
		}
		if(data instanceof String) {
			return "\""+data+"\"";
		}
		return data.toString();
	}
}
