package util.serialization;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class SerializedData<T> {

	protected AtomicInteger current;	
	protected AtomicInteger total;
	
	protected SerializedData() {
		this.total = new AtomicInteger(0);
		this.current = new AtomicInteger(0);
	}
	
	public abstract void addObject(T obj);
	
	public abstract T next();
	
	public abstract void open();
	
	public abstract void reset();
	
	public abstract void close();
	
	public int size() {
		return total.get();
	}
}
