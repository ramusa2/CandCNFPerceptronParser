package util.serialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;


public class FSTDiskSerializer<T> {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static final ThreadLocal<FSTConfiguration> conf = new ThreadLocal() {
		public FSTConfiguration initialValue() {
			return FSTConfiguration.createDefaultConfiguration();
		}};


		@SuppressWarnings({ "unchecked", "rawtypes" })
		static final ThreadLocal<LZ4Factory> lz4 = new ThreadLocal() {
			public LZ4Factory initialValue() {
				return LZ4Factory.fastestInstance();
			}};

			public synchronized void serialize(T obj, File file) {
				try {
					FileOutputStream fos = new FileOutputStream(file);
					BufferedOutputStream BOS = new BufferedOutputStream(fos);
					FSTObjectOutput oos = conf.get().getObjectOutput(BOS);
					oos.writeObject(obj);
					oos.flush();
					BOS.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}

			public synchronized T deserialize(File file) {
				try {
					BufferedInputStream BIS = new BufferedInputStream(new FileInputStream(file));
					FSTObjectInput ois = conf.get().getObjectInput(BIS);
					@SuppressWarnings("unchecked")
					T obj = (T) ois.readObject();
					BIS.close();
					return obj;
				}
				catch(Exception e) {
					return null;
				}
			}

			public synchronized void serializeCompressGZIP(T obj, File file) {
				try {
					FileOutputStream fos =new FileOutputStream(file);
					BufferedOutputStream BOS = new BufferedOutputStream(
							new GZIPOutputStream(fos));
					FSTObjectOutput oos = conf.get().getObjectOutput(BOS);
					oos.writeObject(obj);
					oos.flush();
					BOS.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}

			public synchronized T deserializeDecompressGZIP(File file) {
				try {
					BufferedInputStream BIS = new BufferedInputStream(
							new GZIPInputStream(new FileInputStream(file)));
					FSTObjectInput ois = conf.get().getObjectInput(BIS);
					@SuppressWarnings("unchecked")
					T obj = (T) ois.readObject();
					BIS.close();
					return obj;
				}
				catch(Exception e) {
					return null;
				}
			}
			public synchronized int serializeCompressLZ4(T obj, File file) {
				try {
					byte[] barray = conf.get().asByteArray(obj);
					int compressedLength = barray.length;
					int decompressedLength = -1;
					// Notes: uses an LZ4 block size of 64KB, fast compressor 
					LZ4BlockOutputStream LZOS = new LZ4BlockOutputStream(new FileOutputStream(file));
					FSTObjectOutput oos = conf.get().getObjectOutput(LZOS);
					oos.writeObject(obj);
					oos.flush();
					LZOS.close();
					return decompressedLength;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				return -1;
			}

			public synchronized T deserializeDecompressLZ4(File file) {
				try {
					LZ4BlockInputStream LZIS = new LZ4BlockInputStream(new FileInputStream(file));
					FSTObjectInput ois = conf.get().getObjectInput(LZIS);
					@SuppressWarnings("unchecked")
					T obj = (T) ois.readObject();
					LZIS.close();
					return obj;
				}
				catch(Exception e) {
					return null;
				}
			}

			public synchronized byte[] readBytesFromFile(File file) {
				try {
					Path path = Paths.get(file.getCanonicalPath());
					return Files.readAllBytes(path);
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				return null;
			}
}
