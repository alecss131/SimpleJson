package com.csharp.SimpleJson;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class JsonBinaryWriter {

	private final static byte TAG_ARRAY = 1;
	private final static byte TAG_OBJECT = 2;
	private final static byte TAG_STRING_VALUE = 3;
	private final static byte TAG_INT_VALUE = 4;
	private final static byte TAG_DOUBLE_VALUE = 5;
	private final static byte TAG_BOOLEAN_VALUE = 6;
	private final static byte TAG_FLOAT_VALUE = 7;
	private final static Charset charset = StandardCharsets.UTF_8;
	private final static ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

	public void saveToCompressedStream(JsonElement in, OutputStream os) throws IOException {
		try (BZip2CompressorOutputStream bic = new BZip2CompressorOutputStream(os)) {
			saveToStream(in, bic);
		}
	}

	public void saveToFile(JsonElement in, String fname) throws IOException {
		File file = new File(fname);
		if (!file.exists()) {
			file.createNewFile();
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			try(BufferedOutputStream bos = new BufferedOutputStream(fos)) {
				saveToStream(in, bos);
			}
		}
	}

	public void saveToCompressedFile(JsonElement in, String fname) throws IOException {
		File file = new File(fname);
		if (!file.exists()) {
			file.createNewFile();
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
				saveToCompressedStream(in, bos);
			}
		}
	}

	public void saveToStream(JsonElement in, OutputStream os) throws IOException {
		serialize(os, in);
	}

	private void serialize(OutputStream out, JsonElement in) throws IOException {
		if (in.isJsonObject()) {
			out.write(TAG_OBJECT);
			int size = in.getAsJsonObject().size();
			writeLen(out, size);
			Set<Entry<String, JsonElement>> set = in.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> ent : set) {
				String name = ent.getKey().toString();
				writeName(out, name);
				serialize(out, ent.getValue());
			}
		} else if (in.isJsonPrimitive()) {
			JsonPrimitive prim = in.getAsJsonPrimitive();
			if (prim.isBoolean()) {
				writeBoolean(out, prim.getAsBoolean());
			} else if (prim.isNumber()) {
				String num = prim.getAsString();
				if (num.contains(".") || num.contains(",")) {
					try {
						Float.parseFloat(num);
						writeFloat(out, prim.getAsFloat());
					} catch (NumberFormatException e) {
						writeDouble(out, prim.getAsDouble());
					}
				} else {
					writeInt(out, prim.getAsInt());
				}
			} else if (prim.isString()) {
				writeString(out, prim.getAsString());
			}
		} else if (in.isJsonArray()) {
			out.write(TAG_ARRAY);
			int size = in.getAsJsonArray().size();
			writeLen(out, size);
			for (int i = 0; i < size; i++) {
				serialize(out, in.getAsJsonArray().get(i));
			}
		}
	}

	private void writeInt(OutputStream out, int num) throws IOException {
		out.write(TAG_INT_VALUE);
		writeLen(out, num);
	}

	private void writeLen(OutputStream out, int num) throws IOException {
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.order(byteOrder).putInt(num);
		out.write(dbuf.array());
	}

	private void writeDouble(OutputStream out, double num) throws IOException {
		ByteBuffer dbuf = ByteBuffer.allocate(8);
		dbuf.order(byteOrder).putDouble(num);
		out.write(TAG_DOUBLE_VALUE);
		out.write(dbuf.array());
	}

	private void writeFloat(OutputStream out, float num) throws IOException {
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.order(byteOrder).putFloat(num);
		out.write(TAG_FLOAT_VALUE);
		out.write(dbuf.array());
	}

	private void writeString(OutputStream out, String str) throws IOException {
		out.write(TAG_STRING_VALUE);
		writeName(out, str);
	}

	private void writeName(OutputStream out, String str) throws IOException {
		byte b[] = str.getBytes(charset);
		int len = b.length;
		while (true) {
			String bin = "";
			for (int i = 0; i < 7; i++) {
				bin = len % 2 + bin;
				len = len / 2;
			}
			if (len == 0) {
				bin = "0" + bin;
				out.write((byte) Integer.parseInt(bin, 2));
				break;
			} else {
				bin = "1" + bin;
				out.write((byte) Integer.parseInt(bin, 2));
			}
		}
		out.write(b);
	}

	private void writeBoolean(OutputStream out, boolean bool) throws IOException {
		out.write(TAG_BOOLEAN_VALUE);
		if (bool) {
			out.write((byte) 1);
		} else {
			out.write((byte) 0);
		}
	}
}