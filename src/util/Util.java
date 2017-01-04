package util;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import exceptions.LengthNotMatchException;
import oram.Block;

public class Util {
	public static boolean equal(byte[] a, byte[] b) {
		if (a.length == 0 && b.length == 0)
			return true;
		if (a.length != b.length)
			return false;
		return new BigInteger(a).compareTo(new BigInteger(b)) == 0;
	}

	public static byte[] nextBytes(int len, Random r) {
		byte[] data = new byte[len];
		r.nextBytes(data);
		return data;
	}

	public static long nextLong(long range, Random r) {
		long bits, val;
		do {
			bits = (r.nextLong() << 1) >>> 1;
			val = bits % range;
		} while (bits - val + (range - 1) < 0L);
		return val;
	}

	public static long getSubBits(long l, int end, int start) {
		if (start < 0)
			throw new IllegalArgumentException(start + " < 0");
		if (start > end)
			throw new IllegalArgumentException(start + " > " + end);
		long mask = (1L << (end - start)) - 1L;
		return (l >>> start) & mask;
	}

	public static BigInteger getSubBits(BigInteger bi, int end, int start) {
		if (start < 0)
			throw new IllegalArgumentException(start + " < 0");
		if (start > end)
			throw new IllegalArgumentException(start + " > " + end);
		BigInteger mask = BigInteger.ONE.shiftLeft(end - start).subtract(BigInteger.ONE);
		return bi.shiftRight(start).and(mask);
	}

	public static long setSubBits(long target, long input, int end, int start) {
		input = getSubBits(input, end - start, 0);
		long trash = getSubBits(target, end, start);
		return ((trash ^ input) << start) ^ target;
	}

	public static BigInteger setSubBits(BigInteger target, BigInteger input, int end, int start) {
		if (input.bitLength() > end - start)
			input = getSubBits(input, end - start, 0);
		BigInteger trash = getSubBits(target, end, start);
		return trash.xor(input).shiftLeft(start).xor(target);
	}

	public static byte[] rmSignBit(byte[] arr) {
		if (arr.length < 2)
			return arr;
		if (arr[0] == 0)
			return Arrays.copyOfRange(arr, 1, arr.length);
		return arr;
	}

	public static byte[][] xor(byte[][] a, byte[][] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		byte[][] c = new byte[a.length][];
		for (int i = 0; i < a.length; i++)
			c[i] = xor(a[i], b[i]);
		return c;
	}

	// c = a ^ b
	public static byte[] xor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		byte[] c = new byte[a.length];
		for (int i = 0; i < a.length; i++)
			c[i] = (byte) (a[i] ^ b[i]);
		return c;
	}

	// a = a ^ b to save memory
	public static void setXor(byte[][] a, byte[][] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		for (int i = 0; i < a.length; i++)
			setXor(a[i], b[i]);
	}

	public static void setXor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		for (int i = 0; i < a.length; i++)
			a[i] = (byte) (a[i] ^ b[i]);
	}

	public static BigInteger[] xor(BigInteger[] a, BigInteger[] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		BigInteger[] c = new BigInteger[a.length];
		for (int i = 0; i < a.length; i++)
			c[i] = a[i].xor(b[i]);
		return c;
	}

	public static Array64<Long> xor(Array64<Long> a, Array64<Long> b) {
		if (a.size() != b.size())
			throw new LengthNotMatchException(a.size() + " != " + b.size());
		Array64<Long> c = new Array64<Long>(a.size());
		for (long i = 0; i < a.size(); i++)
			c.set(i, a.get(i) ^ b.get(i));
		return c;
	}

	public static Array64<Block> xorBlockArray64(Array64<Block> a, Array64<Block> b) {
		if (a.size() != b.size())
			throw new LengthNotMatchException(a.size() + " != " + b.size());
		Array64<Block> c = new Array64<Block>(a.size());
		for (long i = 0; i < a.size(); i++)
			c.set(i, a.get(i).xor(b.get(i)));
		return c;
	}

	public static Array64<byte[]> xorByteArray64(Array64<byte[]> a, Array64<byte[]> b) {
		if (a.size() != b.size())
			throw new LengthNotMatchException(a.size() + " != " + b.size());
		Array64<byte[]> c = new Array64<byte[]>(a.size());
		for (long i = 0; i < a.size(); i++)
			c.set(i, Util.xor(a.get(i), b.get(i)));
		return c;
	}

	public static byte[] intToBytes(int i) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(i);
		return bb.array();
	}

	public static int bytesToInt(byte[] b) {
		return new BigInteger(b).intValue();
	}

	public static int[] identityPermutation(int len) {
		int[] out = new int[len];
		for (int i = 0; i < len; i++)
			out[i] = i;
		return out;
	}

	public static int[] randomPermutation(int len, Random rand) {
		List<Integer> list = new ArrayList<Integer>(len);
		for (int i = 0; i < len; i++)
			list.add(i);
		Collections.shuffle(list, rand);
		int[] array = new int[len];
		for (int i = 0; i < len; i++)
			array[i] = list.get(i);
		return array;
	}

	public static Array64<Long> identityPermutationLong(long len) {
		Array64<Long> array = new Array64<Long>(len);
		for (long i = 0; i < len; i++)
			array.set(i, i);
		return array;
	}

	public static Array64<Long> randomPermutationLong(long len, Random rand) {
		LinkedList<Long> list = new LinkedList<Long>();
		for (long i = 0; i < len; i++)
			list.add(i);
		Collections.shuffle(list, rand);
		Array64<Long> array = new Array64<Long>(len);
		for (long i = 0; i < len; i++)
			array.set(i, list.removeFirst());
		return array;
	}

	public static int[] inversePermutation(int[] p) {
		int[] ip = new int[p.length];
		for (int i = 0; i < p.length; i++)
			ip[p[i]] = i;
		return ip;
	}

	public static Array64<Long> inversePermutationLong(Array64<Long> p) {
		Array64<Long> ip = new Array64<Long>(p.size());
		for (long i = 0; i < p.size(); i++) {
			ip.set(p.get(i), i);
		}
		return ip;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] permute(T[] original, int[] p) {
		T[] permuted = (T[]) new Object[original.length];
		for (int i = 0; i < original.length; i++)
			permuted[p[i]] = original[i];
		return (T[]) Arrays.copyOf(permuted, permuted.length, original.getClass());
	}

	public static int[] permute(int[] original, int[] p) {
		int[] permuted = new int[original.length];
		for (int i = 0; i < original.length; i++)
			permuted[p[i]] = original[i];
		return permuted;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] a, T[] b) {
		T[] all = (T[]) new Object[a.length + b.length];
		for (int i = 0; i < a.length; i++)
			all[i] = a[i];
		for (int i = 0; i < b.length; i++)
			all[a.length + i] = b[i];
		return (T[]) Arrays.copyOf(all, all.length, a.getClass());
	}

	public static <T> Array64<T> permute(Array64<T> original, Array64<Long> p) {
		Array64<T> permuted = new Array64<T>(original.size());
		for (long i = 0; i < original.size(); i++)
			permuted.set(p.get(i), original.get(i));
		return permuted;
	}

	public static <T> Array64<T> concat(Array64<T> a, Array64<T> b) {
		Array64<T> all = new Array64<T>(a.size() + b.size());
		for (long i = 0; i < a.size(); i++)
			all.set(i, a.get(i));
		for (long i = 0; i < b.size(); i++)
			all.set(a.size() + i, b.get(i));
		return all;
	}

	public static byte[] longToBytes(long l, int numBytes) {
		byte[] bytes = BigInteger.valueOf(l).toByteArray();
		if (bytes.length == numBytes)
			return bytes;
		else if (bytes.length > numBytes)
			return Arrays.copyOfRange(bytes, bytes.length - numBytes, bytes.length);
		else {
			byte[] out = new byte[numBytes];
			System.arraycopy(bytes, 0, out, numBytes - bytes.length, bytes.length);
			return out;
		}
	}

	public static int[] getXorPermutation(byte[] b, int bits) {
		BigInteger bi = Util.getSubBits(new BigInteger(b), bits, 0);
		int len = (int) Math.pow(2, bits);
		int[] p = new int[len];
		for (int i = 0; i < len; i++) {
			p[i] = BigInteger.valueOf(i).xor(bi).intValue();
		}
		return p;
	}

	public static String addZeros(String a, int n) {
		String out = a;
		for (int i = 0; i < n - a.length(); i++)
			out = "0" + out;
		return out;
	}

	public static byte[] padArray(byte[] in, int len) {
		if (in.length == len)
			return in;
		else if (in.length < len) {
			byte[] out = new byte[len];
			System.arraycopy(in, 0, out, len - in.length, in.length);
			return out;
		} else {
			return Arrays.copyOfRange(in, in.length - len, in.length);
		}
	}

	public static String byteArrayToString(byte[] array, int radix) {
		return new BigInteger(1, array).toString(radix);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] genericArray(Class<T> type, int length) {
		return (T[]) Array.newInstance(type, length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[][] genericArray(Class<T> type, int len1, int len2) {
		return (T[][]) Array.newInstance(type, len1, len2);
	}

	public static void debug(String s) {
		// only to make Communication.java compile
	}

	public static void disp(String s) {
		// only to make Communication.java compile
	}

	public static void error(String s) {
		// only to make Communication.java compile
	}

	public static void error(String s, Exception e) {
		// only to make Communication.java compile
	}
}
