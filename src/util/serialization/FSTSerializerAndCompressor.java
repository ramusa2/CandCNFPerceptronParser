package util.serialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public class FSTSerializerAndCompressor<T> {
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

			public byte[] serialize(T obj) {
				return conf.get().asByteArray(obj);
			}

			@SuppressWarnings("unchecked")
			public T deserialize(byte[] arr) {
				return (T) conf.get().asObject(arr);
			}

			public byte[] compressLZ4(byte[] arr) {
				LZ4Compressor compressor = lz4.get().fastCompressor();
				int maxCompressedLength = compressor.maxCompressedLength(arr.length);
				byte[] compressed = new byte[maxCompressedLength];
				int compressedLength = compressor.compress(arr, 0, arr.length, compressed, 0, maxCompressedLength);
				return Arrays.copyOf(compressed, compressedLength);
			}

			public byte[] decompressLZ4(byte[] compressed, int decompressedLength) {
				LZ4FastDecompressor decompressor = lz4.get().fastDecompressor();
				byte[] restored = new byte[decompressedLength];
				decompressor.decompress(compressed, 0, restored, 0, decompressedLength);
				return restored;
			}

			public byte[] compressGZIP(byte[] uncompressed) {
				try {
					ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
					GZIPOutputStream gzip = new GZIPOutputStream(byteStream);
					gzip.write(uncompressed);
					gzip.close();
					return byteStream.toByteArray();
				}
				catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			public byte[] decompressGZIP(byte[] compressed) {
				try {
					ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
					GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
					int res = 0;
					byte[] buf = new byte[1024];
					while(res >= 0) {
						res = gzip.read(buf, 0, buf.length);
						if(res > 0) {
							byteStream.write(buf, 0, res);
						}
					}
					return byteStream.toByteArray();
				}
				catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			public synchronized T decompressDeserializeLZ4(byte[] compressed, int decompressedLength) {
				return deserialize(decompressLZ4(compressed, decompressedLength));
			}

			public byte[] serializeCompressGZIP(T obj){
				try {
					ByteArrayOutputStream byteStream=new ByteArrayOutputStream();
					BufferedOutputStream BOS = new BufferedOutputStream(
							new GZIPOutputStream(byteStream));
					FSTObjectOutput oos = conf.get().getObjectOutput(BOS);
					oos.writeObject(obj);
					oos.flush();
					BOS.close();
					return byteStream.toByteArray();
				}
				catch(Exception e) {
					return null;
				}
			}

			public T decompressDeserializeGZIP(byte[] compressed) {
				return deserialize(decompressGZIP(compressed));
			}

			public synchronized void serializeAndCompressAndWriteTargetToFile(File file, T target) {
				this.serializeAndCompressAndWriteTargetToFile(file, target, CompressionType.NONE);
			}

			public void serializeAndCompressAndWriteTargetToFile(File file,
					T target, CompressionType compType) {
				try {
					// Open output stream to file
					FileOutputStream fos = new FileOutputStream(file);
					BufferedOutputStream BOS = new BufferedOutputStream(fos);
					FSTObjectOutput oos = conf.get().getObjectOutput(BOS);			
					// Switch on compression type to get byte array to write
					byte[] serialized = this.serialize(target);
					switch(compType) {
					case NONE:
						oos.write(serialized);
						//oos.writeObject(serialized);
						break;
					case LZ4:
						//int decompressedLength = serialized.length;
						//oos.writeInt(decompressedLength);
						//serialized = this.compressLZ4(serialized);
						oos.writeObject(serialized);
						break;
					case GZIP:
						serialized = this.compressGZIP(serialized);
						oos.writeObject(serialized);
					}
					// Close stream
					oos.flush();
					BOS.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}

			public synchronized T deserializeAndDecompress(File file) {
				return this.deserializeAndDecompress(file, CompressionType.NONE);
			}

			public synchronized T deserializeAndDecompress(File file, CompressionType compType) {
				try {
					T object = null;
					BufferedInputStream BIS = new BufferedInputStream(new FileInputStream(file));
					FSTObjectInput ois = conf.get().getObjectInput(BIS);
					byte[] serialized;
					switch(compType) {
					case NONE:
						serialized = (byte[]) ois.readObject();
						object = this.deserialize(serialized);
						break;
					case LZ4:
						/*
						int decompressedLength = ois.readInt();
						System.out.println(decompressedLength);
						serialized = (byte[]) ois.readObject();
						object = this.decompressDeserializeLZ4(serialized, decompressedLength);
						*/
						object = (T) ois.readObject();
						break;
					case GZIP:
						serialized = (byte[]) ois.readObject();
						object = this.decompressDeserializeGZIP(serialized);
					}
					BIS.close();
					return object;
				}
				catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			public byte[] readByteArrayFromFile(File file, CompressionType compType) {
				try {
					BufferedInputStream BIS = new BufferedInputStream(new FileInputStream(file));
					FSTObjectInput ois = conf.get().getObjectInput(BIS);
					byte[] serialized;
					switch(compType) {
					case NONE:
					case GZIP:
						serialized = (byte[]) ois.readObject();
						break;
					case LZ4:
						ois.readInt();
						serialized = (byte[]) ois.readObject();
					default:
						serialized = null;
					}
					BIS.close();
					return serialized;
				}
				catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			public int readLZ4DecompressedLengthFromFile(File file) {
				try {
					BufferedInputStream BIS = new BufferedInputStream(new FileInputStream(file));
					FSTObjectInput ois = conf.get().getObjectInput(BIS);
					int decompressedLength = ois.readInt();
					BIS.close();
					return decompressedLength;
				}
				catch(Exception e) {
					e.printStackTrace();
					return -1;
				}
			}
}
