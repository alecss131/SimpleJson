package com.csharp.SimpleJson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonBinaryReader {

	private final static byte TAG_ARRAY = 1;
	private final static byte TAG_OBJECT = 2;
	private final static byte TAG_STRING_VALUE = 3;
	private final static byte TAG_INT_VALUE = 4;
	private final static byte TAG_DOUBLE_VALUE = 5;
	private final static byte TAG_BOOLEAN_VALUE = 6;
	private final static byte TAG_FLOAT_VALUE = 7;
	private final static Charset charset = StandardCharsets.UTF_8;
	private final static ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

	public JsonElement loadFromCompressedFile(String fname) throws Exception {
		File file = new File(fname);
		try (FileInputStream fis = new FileInputStream(file)) {
			return loadFromCompressedStream(fis);
		}
	}

	public JsonElement loadFromCompressedStream(InputStream is) throws Exception {
		try (BZip2CompressorInputStream cis = new BZip2CompressorInputStream(is)) {
			return loadFromStream(cis);
		}
	}

	public JsonElement loadFromFile(String fname) throws Exception {
		File file = new File(fname);
		try (FileInputStream fis = new FileInputStream(file)) {
			return loadFromStream(fis);
		}
	}

	public JsonElement loadFromStream(InputStream is) throws Exception {
		try (BufferedInputStream bis = new BufferedInputStream(is)) {
			return deserialize(is);
		}
	}

	private JsonElement deserialize(InputStream in) throws Exception {
		byte z = readByte(in);
		switch (z) {
		case TAG_ARRAY: {
			int num = readInt(in);
			JsonArray jarr = new JsonArray();
			for (int i = 0; i < num; i++) {
				jarr.add(deserialize(in));
			}
			return jarr;
		}
		case TAG_OBJECT: {
			int num = readInt(in);
			JsonObject jobj = new JsonObject();
			for (int i = 0; i < num; i++) {
				String name = readString(in);
				jobj.add(name, deserialize(in));
			}
			return jobj;
		}
		case TAG_STRING_VALUE: {
			return new JsonPrimitive(readString(in));
		}
		case TAG_INT_VALUE: {
			return new JsonPrimitive(readInt(in));
		}
		case TAG_DOUBLE_VALUE: {
			return new JsonPrimitive(readDouble(in));
		}
		case TAG_BOOLEAN_VALUE: {
			return new JsonPrimitive(readBoolean(in));
		}
		case TAG_FLOAT_VALUE: {
			return new JsonPrimitive(readFloat(in));
		}
		default: {
			throw new Exception("Error deserializing JSON. Unknown tag: " + z);
		}
		}
	}

	private int readInt(InputStream in) throws IOException {
		byte out[] = new byte[4];
		int test = in.read(out);
		if (test != 4) {
			throw new IOException("Error reading file");
		}
		return ByteBuffer.wrap(out).order(byteOrder).getInt();
	}

	private double readDouble(InputStream in) throws IOException {
		byte out[] = new byte[8];
		int test = in.read(out);
		if (test != out.length) {
			throw new IOException("Error reading file");
		}
		return ByteBuffer.wrap(out).order(byteOrder).getDouble();
	}

	private float readFloat(InputStream in) throws IOException {
		byte out[] = new byte[4];
		int test = in.read(out);
		if (test != out.length) {
			throw new IOException("Error reading file");
		}
		return ByteBuffer.wrap(out).order(byteOrder).getFloat();
	}

	private boolean readBoolean(InputStream in) throws IOException {
		byte out[] = new byte[1];
		int test = in.read(out);
		if (test == -1) {
			throw new IOException("Error reading file");
		}
		return out[0] == 0 ? false : true;
	}

	private byte readByte(InputStream in) throws IOException {
		int out = in.read();
		if (out == -1) {
			throw new IOException("Error reading file");
		}
		return (byte)out;
	}

	private String readString(InputStream in) throws IOException {
		char c;
		String str = "";
		do {
			int tmp = Byte.toUnsignedInt(readByte(in));
			String s = String.format("%8s", Integer.toBinaryString(tmp)).replace(' ', '0');
			c = s.charAt(0);
			str = s.substring(1) + str;
		} while (c == '1');
		return new String(read(in, Integer.parseInt(str, 2)), charset);
	}

	private byte[] read(InputStream in, int len) throws IOException {
		byte out[] = new byte[len];
		int test = in.read(out);
		if (test != len) {
			throw new IOException("Error reading file");
		}
		return out;
	}
}