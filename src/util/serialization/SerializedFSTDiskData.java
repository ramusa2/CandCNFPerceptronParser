package util.serialization;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class SerializedFSTDiskData<T> extends SerializedData<T> {

	private File dir;

	private ArrayList<File> dataFiles;
	private CompressionType compType;

	private final FSTSerializerAndCompressor<T> serializer;
	
	private String prefix;
	
	//private final ConcurrentHashMap<Integer, Integer> decompressedLengths;

	protected SerializedFSTDiskData(File dataDirectory, CompressionType compressionType,
			String filePrefix) {
		super();
		this.dir = dataDirectory;
		this.dir.mkdirs();
		this.dataFiles = new ArrayList<File>();
		this.compType = compressionType;
		this.serializer = new FSTSerializerAndCompressor<T>();
		this.prefix = filePrefix;
		//this.decompressedLengths = new ConcurrentHashMap<Integer, Integer>();
	}

	public static SerializedFSTDiskData<?> createNew(File dataDirectory, CompressionType compressionType) {
		return new SerializedFSTDiskData<Object>(dataDirectory, compressionType, "");
	}

	public static SerializedFSTDiskData<?> createNew(File dataDirectory, CompressionType compressionType, String filePrefix) {
		return new SerializedFSTDiskData<Object>(dataDirectory, compressionType, filePrefix);
	}

	public static SerializedFSTDiskData<?> useExisting(File dataDirectory, CompressionType compressionType) {
		SerializedFSTDiskData<?> existing = new SerializedFSTDiskData<Object>(dataDirectory, compressionType, "");
		File[] files = dataDirectory.listFiles();
		Arrays.sort(files);
		for(File file : files) {
			if(file.getName().endsWith(existing.extension())) {
				existing.dataFiles.add(file);
				existing.total.incrementAndGet();
			}
		}
		return existing;
	}

	public static SerializedFSTDiskData<?> useExisting(File dataDirectory, CompressionType compressionType, 
			String filePrefix) {
		SerializedFSTDiskData<?> existing = new SerializedFSTDiskData<Object>(dataDirectory, compressionType, filePrefix);
		File[] files = dataDirectory.listFiles();
		Arrays.sort(files);
		for(File file : files) {
			if(file.getName().endsWith(existing.extension())) {
				existing.dataFiles.add(file);
				existing.total.incrementAndGet();
			}
		}
		return existing;
	}

	@Override
	public synchronized void addObject(T obj) {
		int id = total.get();
		File file = getDataFile(id);
		this.serializer.serializeAndCompressAndWriteTargetToFile(file, obj, this.compType);
		this.dataFiles.add(file);
		this.total.incrementAndGet();
	}



	// TODO: write a second-threaded version of next() that uses Future<T>
	//		 to cache/precompute the next de-serialization

	@Override
	public T next() {
		int id = this.current.getAndIncrement();
		if(id >= total.get()) {
			return null;
		}
		return this.serializer.deserializeAndDecompress(this.dataFiles.get(id), this.compType);
	}
	
	/**
	 * When loading the entire memory array from disk to RAM, we want to preserve compression
	 */
	public byte[] nextAsByteArray() {
		int id = this.current.getAndIncrement();
		if(id >= total.get()) {
			return null;
		}
		return this.serializer.readByteArrayFromFile(this.dataFiles.get(id), this.compType);
	}

	private File getDataFile(int id) {
		String filename = this.prefix+dir.getName()+"_"+idToAlphabetical(id)+this.extension();
		return new File(dir.getPath()+File.separator+filename);
	}
	
	private static final int alphaLength = 7;

	/**
	 * Returns a String of length 7 storing the provided index in the lowercase alphabetical order
	 * (up to MAX_INT range)
	 * E.g.: 0=aaaaa, 1=aaaab, ..., 26=aaaba,  etc.
	 * @param id	index to convert
	 * @return
	 */
	private static String idToAlphabetical(int id) {
		String str = "";
		for(int b=alphaLength-1; b>=0; b--) {
			int cutoff = (int) Math.pow(26, b);
			if(id >= cutoff) {
				int val = id/cutoff;
				str += (char) (val+97);
				id -= val*cutoff;
			}
			else {
				str += "a";
			}
		}
		return str;
	}
	
	public static void main(String[] args) {
		int twosix = 2147483647;
		System.out.println(idToAlphabetical(twosix));
		String str = "gytisyx";
		System.out.println(alphabeticalToID(str));
		str = "gytisyy";
		System.out.println(alphabeticalToID(str));
		str = "zzzzzzz";
		System.out.println(alphabeticalToID(str));
	}

	/**
	 * Decodes a lowercase alphabetical String into the corresponding index
	 * E.g.: aaaaaaaaaa=0, aaaaaaaaab=1, ..., aaaaaaaaba=26,  etc.
	 * @param alpha
	 */
	@SuppressWarnings("unused")
	private static int alphabeticalToID(String alpha) {
		int i = 0;
		int index = 0; 
		for(int curBase = alpha.length()-1; curBase>=0; curBase--) {
			char l = alpha.charAt(index);
			i += Math.pow(26, curBase)*(l-97);
			index++;
		}
		return i;
	}

	private String extension() {
		switch(compType) {
		case NONE:
			return ".ser";
		case LZ4:
			return".ser.lz4";
		case GZIP:
			return ".ser.gz";
		default:
			return ".ser";
		}
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
	

	/**
	 * Returns the {@link CompressionType} used to compress file on this disk data
	 */
	public CompressionType getCompressionType() {
		return this.compType;
	}

	public int getNextDecompressedLength() {
		switch(this.compType) {
			
		}
		// TODO Auto-generated method stub
		return 0;
	}
}
