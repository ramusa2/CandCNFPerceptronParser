package util.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FrequencyList<T extends Comparable> {

	private HashMap<T, FrequencyListEntry<T>> map;

	public FrequencyList() {
		this.map = new HashMap<T, FrequencyListEntry<T>>();
	}

	public void addCount(T value) {
		this.addCount(value, 1.0);
	}

	public void addCount(T value, double count) {
		FrequencyListEntry<T> entry = map.get(value);
		if(entry == null) {
			entry = new FrequencyListEntry<T>(value);
			map.put(value, entry);
		}
		entry.addCount(count);

	}

	public List<FrequencyListEntry<T>> sortedList() {
		ArrayList<FrequencyListEntry<T>> list = new ArrayList<FrequencyListEntry<T>>();
		list.addAll(map.values());
		Collections.sort(list);
		return list;
	}

	public void addEntry(FrequencyListEntry<T> newEntry) {
		T val = newEntry.value();
		FrequencyListEntry<T> entry = map.get(val);
		if(entry == null) {
			map.put(val, newEntry);
			return;
		}
		entry.addCount(newEntry.frequency());
	}

	public String toString() {
		List<FrequencyListEntry<T>> list =  this.sortedList();
		if(list.isEmpty()) {
			return "";
		}
		double max = list.get(0).frequency();
		int tabs = (int) ((Math.log10(max)/3)+1);
		String ret = "";
		for(FrequencyListEntry<T> entry : list) {
			ret += entry.toString(tabs)+"\n";
		}
		return ret;
	}

	public void writeToFile(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			List<FrequencyListEntry<T>> list =  this.sortedList();
			double max = list.get(0).frequency();
			int tabs = (int) ((Math.log10(max)/3)+1);
			for(FrequencyListEntry<T> entry : list) {
				pw.println(entry.toString(tabs));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(pw != null) {
			pw.close();
		}
	}

	public int size() {
		return this.map.size();
	}

	public double getFrequency(T value) {
		FrequencyListEntry<T> f = this.map.get(value);
		if(f != null) {
			return f.frequency();
		}
		return 0;
	}

	public void exportToCSV(File file) {
		ArrayList<T> keys = new ArrayList<T>();
		keys.addAll(map.keySet());
		Collections.sort(keys);
		PrintWriter pw;
		try {
			pw = new PrintWriter(file);
			for(T k : keys) {
				pw.println(k+","+map.get(k).frequency());
			}
			pw.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}
}
