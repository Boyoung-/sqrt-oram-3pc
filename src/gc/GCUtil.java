package gc;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCGenComp;
import com.oblivm.backend.gc.GCSignal;

import crypto.Crypto;
import exceptions.LengthNotMatchException;
import util.Util;

public class GCUtil {

	public static GCSignal[] genEmptyKeys(int n) {
		GCSignal[] keys = new GCSignal[n];
		for (int i = 0; i < n; i++)
			keys[i] = new GCSignal(new byte[GCSignal.len]);
		return keys;
	}

	public static GCSignal[][] genKeyPairs(int n) {
		GCSignal[][] pairs = new GCSignal[n][];
		for (int i = 0; i < n; i++)
			pairs[i] = GCGenComp.genPair();
		return pairs;
	}

	public static GCSignal[] getZeroKeys(GCSignal[][] pairs) {
		GCSignal[] keys = new GCSignal[pairs.length];
		for (int i = 0; i < keys.length; i++)
			keys[i] = pairs[i][0];
		return keys;
	}

	public static GCSignal[] revSelectKeys(GCSignal[][] pairs, byte[] input) {
		BigInteger in = new BigInteger(1, input);
		GCSignal[] out = new GCSignal[pairs.length];
		for (int i = 0; i < pairs.length; i++)
			out[i] = pairs[i][in.testBit(i) ? 1 : 0];
		return out;
	}

	public static GCSignal[] selectKeys(GCSignal[][] pairs, byte[] input) {
		BigInteger in = new BigInteger(1, input);
		GCSignal[] out = new GCSignal[pairs.length];
		for (int i = 0; i < pairs.length; i++)
			out[i] = pairs[i][in.testBit(pairs.length - 1 - i) ? 1 : 0];
		return out;
	}

	public static synchronized byte[][] genOutKeyHashes(GCSignal[] outZeroKeys) {
		byte[][] hashes = new byte[outZeroKeys.length][];
		for (int i = 0; i < outZeroKeys.length; i++) {
			hashes[i] = Crypto.sha1.digest(outZeroKeys[i].bytes);
		}
		return hashes;
	}

	public static BigInteger revEvaOutKeys(GCSignal[] outKeys, byte[][] genHashes) {
		if (outKeys.length != genHashes.length)
			throw new LengthNotMatchException(outKeys.length + " != " + genHashes.length);
		byte[][] evaHashes = genOutKeyHashes(outKeys);
		BigInteger output = BigInteger.ZERO;
		for (int i = 0; i < outKeys.length; i++) {
			if (outKeys[i].isPublic()) {
				if (outKeys[i].v)
					output = output.setBit(i);
			} else if (!Util.equal(genHashes[i], evaHashes[i])) {
				output = output.setBit(i);
			}
		}
		return output;
	}

	public static BigInteger evaOutKeys(GCSignal[] outKeys, byte[][] genHashes) {
		if (outKeys.length != genHashes.length)
			throw new LengthNotMatchException(outKeys.length + " != " + genHashes.length);
		byte[][] evaHashes = genOutKeyHashes(outKeys);
		BigInteger output = BigInteger.ZERO;
		for (int i = 0; i < outKeys.length; i++) {
			if (outKeys[i].isPublic()) {
				if (outKeys[i].v)
					output = output.setBit(outKeys.length - 1 - i);
			} else if (!Util.equal(genHashes[i], evaHashes[i])) {
				output = output.setBit(outKeys.length - 1 - i);
			}
		}
		return output;
	}

	public static GCSignal[][] recoverOutKeyPairs(GCSignal[] outZeroKeys) {
		GCSignal[][] pairs = new GCSignal[outZeroKeys.length][2];
		for (int i = 0; i < outZeroKeys.length; i++) {
			pairs[i][0] = outZeroKeys[i];
			pairs[i][1] = GCGenComp.R.xor(pairs[i][0]);
		}
		return pairs;
	}

	public static synchronized byte[] hashAll(GCSignal[] keys) {
		for (int i = 0; i < keys.length; i++)
			Crypto.sha1.update(keys[i].bytes);
		return Crypto.sha1.digest();
	}
}
