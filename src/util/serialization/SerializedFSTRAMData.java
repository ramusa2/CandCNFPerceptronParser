package util.serialization;

import java.util.concurrent.ConcurrentHashMap;

public class SerializedFSTRAMData<T> extends SerializedData<T> {

	private CompressionType compType;
	
	private final FSTSerializerAndCompressor<T> serializer;

	private final ConcurrentHashMap<Integer, byte[]> data;
	private final ConcurrentHashMap<Integer, Integer> decompressedLengths;

	public SerializedFSTRAMData(CompressionType compressionType) {
		super();
		this.compType = compressionType;
		this.serializer = new FSTSerializerAndCompressor<T>();
		this.decompressedLengths = new ConcurrentHashMap<Integer, Integer>();
		this.data = new ConcurrentHashMap<Integer, byte[]>();
	}

	@Override
	public synchronized void addObject(T obj) {
		int id = total.get();
		byte[] objData = serializer.serialize(obj);
		this.decompressedLengths.put(id, objData.length);
		switch(compType) {
			case NONE:
				this.data.put(id, objData);
				break;
			case LZ4:
				this.data.put(id, serializer.compressLZ4(objData));
				break;
			case GZIP:
				this.data.put(id, serializer.serializeCompressGZIP(obj));
		}
		this.total.incrementAndGet();
	}

	public synchronized void addByteArray(byte[] objData) {
		this.addByteArray(objData, -1);
	}

	public synchronized void addByteArray(byte[] objData, int decompressedLength) {
		int id = total.get();
		this.data.put(id, objData);
		this.decompressedLengths.put(id, decompressedLength);
		this.total.incrementAndGet();
	}

	// TODO: write a second-threaded version of next() that uses Future<T>
	//		 to cache/precompute the next de-serialization
	
	@Override
	public synchronized T next() {
		int id = this.current.getAndIncrement();
		if(id >= total.get()) {
			return null;
		}
		byte[] arr = this.data.get(id);
		switch(compType) {
			case NONE:
				return this.serializer.deserialize(arr);
			case LZ4:
				return this.serializer.decompressDeserializeLZ4(arr, this.decompressedLengths.get(id));
			case GZIP:
				return this.serializer.decompressDeserializeGZIP(arr);
		}
		return null;
	}

	@Override
	public void reset() {
		this.current.set(0);
	}

	@Override
	public void close() {
		this.current.set(0);
	}

	@Override
	public void open() {}
	
	public int totalBytes() {
		int bytes = 0;
		for(byte[] arr : data.values()) {
			bytes += arr.length;
		}
		return bytes;
	}
	
	/**
	 * Returns the {@link CompressionType} used to compress objects in this memory array
	 */
	public CompressionType getCompressionType() {
		return this.compType;
	}

	/**
	 * Given a set of files on disk, load the contents of those files into a memory array.
	 * Resets the diskData and iterates over the collection to read contents (disk data
	 * collection is reset at end of reading).
	 * 
	 * @param diskData	the {@link SerializedFSTDiskData} to load objects from files
	 * @return	a memory array containing the loaded objects
	 */
	public static SerializedFSTRAMData<?> loadFromSerializedFSTDiskData(
			SerializedFSTDiskData<?> diskData) {
		SerializedFSTRAMData<Object> ramData = new SerializedFSTRAMData<Object>(diskData.getCompressionType());
		diskData.reset();
		byte[] next;
		switch(diskData.getCompressionType()) {
		case NONE:
		case GZIP:
			while((next=diskData.nextAsByteArray())!= null) {
				ramData.addByteArray(next);
			}
			break;
		case LZ4:
			while((next=diskData.nextAsByteArray())!= null) {
				ramData.addByteArray(next);
			}
			int decompressedLength = diskData.getNextDecompressedLength();
			byte[] nextArr;
			while((nextArr=diskData.nextAsByteArray())!= null) {
				ramData.addByteArray(nextArr);
				/*
				ramData.addCompressedLZ4ByteArray(nextArr, decompressedLength);
				decompressedLength = diskData.getNextDecompressedLength();
				*/
			}
		}
		diskData.reset();
		return ramData;
	}

	private void addCompressedLZ4ByteArray(byte[] objData, int decompressedLength) {
		if(this.compType == CompressionType.LZ4) {
			int id = total.get();
			this.decompressedLengths.put(id, decompressedLength);
			this.data.put(id, objData);
			this.total.incrementAndGet();
		}
	}
}
