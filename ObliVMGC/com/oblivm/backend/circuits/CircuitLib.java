// Copyright (C) 2014 by Xiao Shaun Wang <wangxiao@cs.umd.edu>
package com.oblivm.backend.circuits;

import java.util.Arrays;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.flexsc.Mode;
import com.oblivm.backend.flexsc.Party;
import com.oblivm.backend.gc.GCSignal;

public class CircuitLib<T> {
	public CompEnv<T> env;
	public final T SIGNAL_ZERO;
	public final T SIGNAL_ONE;

	public CircuitLib(CompEnv<T> e) {
		env = e;
		SIGNAL_ZERO = e.ZERO();
		SIGNAL_ONE = e.ONE();
	}

	public T[] toSignals(long a, int width) {
		T[] result = env.newTArray(width);
		for (int i = 0; i < width; ++i) {
			if ((a & 1) == 1)
				result[i] = SIGNAL_ONE;
			else
				result[i] = SIGNAL_ZERO;
			a >>= 1;
		}
		return result;

	}

	public T[] enforceBits(T[] a, int length) {
		if (length > a.length)
			return padSignal(a, length);
		else
			return Arrays.copyOfRange(a, 0, length);
	}

	public T enforceBits(T a) {
		if (a == null)
			return SIGNAL_ZERO;
		else
			return a;
	}

	public T[] enforceBits(T a, int length) {
		T[] ret = env.newTArray(length);
		if (a == null)
			ret[0] = SIGNAL_ZERO;
		else
			ret[0] = a;
		for (int i = 1; i < ret.length; ++i) {
			ret[i] = SIGNAL_ZERO;
		}
		return ret;
	}

	public T[] randBools(int length) {
		if (env.getMode() == Mode.COUNT) {
			return zeros(length);
		}
		boolean[] res = new boolean[length];
		for (int i = 0; i < length; ++i)
			res[i] = CompEnv.rnd.nextBoolean();
		T[] alice = env.inputOfAlice(res);
		T[] bob = env.inputOfBob(res);
		T[] resSC = xor(alice, bob);

		return resSC;
	}

	public boolean[] declassifyToAlice(T[] x) {
		return env.outputToAlice(x);
	}

	public boolean[] declassifyToBob(T[] x) {
		return env.outputToBob(x);
	}

	public boolean[] declassifyToBoth2(T[] x) {

		if (env.getMode() == Mode.COUNT) {
			return new boolean[x.length];
		}

		boolean[] pos = env.outputToBob(x);

		if (env.getParty() == Party.Bob) {
			byte[] tmp = new byte[pos.length];
			for (int i = 0; i < pos.length; ++i)
				tmp[i] = (byte) (pos[i] ? 1 : 0);
			env.channel.writeByte(tmp, tmp.length);
			env.flush();
		} else {
			byte tmp[] = env.channel.readBytes(x.length);
			pos = new boolean[x.length];
			for (int k = 0; k < tmp.length; ++k) {
				pos[k] = ((tmp[k] - 1) == 0);
			}
		}
		return pos;
	}

	public boolean[] declassifyToBoth(T[] x) {
		if (env.getMode() == Mode.COUNT) {
			return new boolean[x.length];
		} else if (env.getMode() == Mode.VERIFY) {
			return com.oblivm.backend.util.Utils.tobooleanArray((Boolean[]) x);
		} else {

			GCSignal[] in = (GCSignal[]) x;

			boolean[] pos = new boolean[x.length];
			GCSignal tmp;
			for (int i = 0; i < x.length; ++i) {
				if (in[i].isPublic()) {
					pos[i] = in[i].v;
				} else {
					in[i].send(env.channel);
				}
			}
			env.channel.flush();
			for (int i = 0; i < x.length; ++i) {
				if (!in[i].isPublic()) {
					tmp = GCSignal.receive(env.channel);
					if (tmp.equals(in[i]))
						pos[i] = false;
					else
						pos[i] = true;
				}
			}
			return pos;
		}
	}

	// Defaults to 32 bit constants.
	public T[] toSignals(int value) {
		return toSignals(value, 32);
	}

	public GCSignal[] toSignals(GCSignal[] value) {
		return value;
	}

	public T[] zeros(int length) {
		T[] result = env.newTArray(length);
		for (int i = 0; i < length; ++i) {
			result[i] = SIGNAL_ZERO;
		}
		return result;
	}

	public T[] ones(int length) {
		T[] result = env.newTArray(length);
		for (int i = 0; i < length; ++i) {
			result[i] = SIGNAL_ONE;
		}
		return result;
	}

	/*
	 * Basic logical operations on Signal and Signal[]
	 */
	public T and(T x, T y) {
		assert (x != null && y != null) : "CircuitLib.and: bad inputs";

		return env.and(x, y);
	}

	public T[] and(T[] x, T[] y) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.and[]: bad inputs";

		T[] result = env.newTArray(x.length);
		for (int i = 0; i < x.length; ++i) {
			result[i] = and(x[i], y[i]);
		}
		return result;
	}

	public T xor(T x, T y) {
		assert (x != null && y != null) : "CircuitLib.xor: bad inputs";

		return env.xor(x, y);
	}

	public T[] xor(T[] x, T[] y) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.xor[]: bad inputs";

		T[] result = env.newTArray(x.length);
		for (int i = 0; i < x.length; ++i) {
			result[i] = xor(x[i], y[i]);
		}
		return result;
	}

	public T[][] xor(T[][] x, T[][] y) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.xor[]: bad inputs";

		T[][] result = env.newTArray(x.length, x[0].length);
		for (int i = 0; i < x.length; ++i) {
			result[i] = xor(x[i], y[i]);
		}
		return result;
	}

	public T not(T x) {
		assert (x != null) : "CircuitLib.not: bad input";

		return env.xor(x, SIGNAL_ONE);
	}

	public T[] not(T[] x) {
		assert (x != null) : "CircuitLib.not[]: bad input";

		T[] result = env.newTArray(x.length);
		for (int i = 0; i < x.length; ++i) {
			result[i] = not(x[i]);
		}
		return result;
	}

	public T or(T x, T y) {
		assert (x != null && y != null) : "CircuitLib.or: bad inputs";

		return xor(xor(x, y), and(x, y)); // http://stackoverflow.com/a/2443029
	}

	public T[] or(T[] x, T[] y) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.or[]: bad inputs";

		T[] result = env.newTArray(x.length);
		for (int i = 0; i < x.length; ++i) {
			result[i] = or(x[i], y[i]);
		}
		return result;
	}

	/*
	 * Output x when c == 0; Otherwise output y.
	 */
	public T mux(T x, T y, T c) {
		assert (x != null && y != null && c != null) : "CircuitLib.mux: bad inputs";
		T t = xor(x, y);
		t = and(t, c);
		T ret = xor(t, x);
		return ret;
	}

	public T[] mux(T[] x, T[] y, T c) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.mux[]: bad inputs";

		T[] ret = env.newTArray(x.length);
		for (int i = 0; i < x.length; i++)
			ret[i] = mux(x[i], y[i], c);

		return ret;
	}

	public T[][] mux(T[][] x, T[][] y, T c) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.mux[][]: bad inputs";

		T[][] ret = env.newTArray(x.length, 1);
		for (int i = 0; i < x.length; i++)
			ret[i] = mux(x[i], y[i], c);

		return ret;
	}

	public T[][][] mux(T[][][] x, T[][][] y, T c) {
		assert (x != null && y != null && x.length == y.length) : "CircuitLib.mux[]: bad inputs";

		T[][][] ret = env.newTArray(x.length, 1, 1);
		for (int i = 0; i < x.length; i++)
			ret[i] = mux(x[i], y[i], c);

		return ret;
	}

	public T mux(T[] in, T[] c) {
		assert (in != null && c != null && in.length == (int) Math.pow(2, c.length)) : "CircuitLib.mux: bad inputs";

		T[] ret = in;
		for (int i = 0; i < c.length; i++)
			ret = mux(Arrays.copyOfRange(ret, 0, ret.length / 2), Arrays.copyOfRange(ret, ret.length / 2, ret.length),
					c[i]);

		return ret[0];
	}

	public T[] mux(T[][] in, T[] c) {
		assert (in != null && c != null && in.length == (int) Math.pow(2, c.length)) : "CircuitLib.mux: bad inputs";

		T[][] ret = in;
		for (int i = 0; i < c.length; i++)
			ret = mux(Arrays.copyOfRange(ret, 0, ret.length / 2), Arrays.copyOfRange(ret, ret.length / 2, ret.length),
					c[i]);

		return ret[0];
	}

	public T[] demux(T in, T c) {
		assert (in != null && c != null) : "CircuitLib.demux: bad inputs";

		T[] ret = env.newTArray(2);
		ret[0] = and(in, not(c));
		ret[1] = and(in, c);

		return ret;
	}

	public T[] demux(T in, T[] c) {
		assert (in != null && c != null && c.length > 0) : "CircuitLib.demux: bad inputs";

		if (c.length == 1)
			return demux(in, c[0]);

		T[] parent = demux(in, c[0]);
		T[] restC = Arrays.copyOfRange(c, 1, c.length);
		T[] left = demux(parent[0], restC);
		T[] right = demux(parent[1], restC);

		T[] ret = env.newTArray((int) Math.pow(2, c.length));
		System.arraycopy(left, 0, ret, 0, left.length);
		System.arraycopy(right, 0, ret, left.length, right.length);

		return ret;
	}

	public T[] decoder(T c) {
		return demux(SIGNAL_ONE, c);
	}

	public T[] decoder(T[] c) {
		assert (c != null && c.length > 0) : "CircuitLib.decoder: bad inputs";

		if (c.length == 1)
			return demux(SIGNAL_ONE, c[0]);

		return demux(SIGNAL_ONE, c);
	}

	public T[] padSignal(T[] a, int length) {
		T[] res = zeros(length);
		for (int i = 0; i < a.length && i < length; ++i)
			res[i] = a[i];
		return res;
	}

	public T[] padSignedSignal(T[] a, int length) {
		T[] res = env.newTArray(length);
		for (int i = 0; i < a.length && i < length; ++i)
			res[i] = a[i];
		for (int i = a.length; i < length; ++i)
			res[i] = a[a.length - 1];
		return res;
	}

	public T[] copy(T[] x) {
		return Arrays.copyOf(x, x.length);
	}
}