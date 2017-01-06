package gc;

import java.math.BigInteger;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;

import oram.Block;
import oram.Metadata;

public class GCGetPointer<T> extends IntegerLib<T> {

	private T[] AL_p;
	private T[] AF_p;
	private T[][] AP_p;
	private T[] BF_p;

	public GCGetPointer(CompEnv<T> e, Block A_prime, byte[] BF_prime, Metadata md, int levelIndex) {
		super(e);
		int ttp = md.getTwoTauPow();
		int lBits = md.getLBits(levelIndex);
		int pBits = md.getPBits(levelIndex);

		AL_p = env.newTArray(lBits);
		BigInteger L = A_prime.getL().length == 0 ? BigInteger.ZERO : new BigInteger(A_prime.getL());
		for (int i = 0; i < lBits; i++)
			AL_p[i] = L.testBit(lBits - 1 - i) ? SIGNAL_ONE : SIGNAL_ZERO;

		AF_p = env.newTArray(ttp);
		BF_p = env.newTArray(ttp);
		AP_p = env.newTArray(ttp, pBits);
		for (int i = 0; i < ttp; i++) {
			AF_p[i] = (A_prime.getF(i) & 1) == 0 ? SIGNAL_ZERO : SIGNAL_ONE;
			BF_p[i] = (BF_prime[i] & 1) == 0 ? SIGNAL_ZERO : SIGNAL_ONE;

			BigInteger P = new BigInteger(A_prime.getP(i));
			for (int j = 0; j < pBits; j++)
				AP_p[i][j] = P.testBit(pBits - 1 - j) ? SIGNAL_ONE : SIGNAL_ZERO;
		}
	}

	public T[][] execute(T[] N_E, T[] N_C, T[] AL_E, T[] AL_C, T[] AF_E, T[] AF_C, T[] BF_E, T[] BF_C, T[][] AP_E,
			T[][] AP_C, T[][] BP_E, T[][] BP_C) {
		int ttp = AP_E.length;

		T[] N = xor(N_C, N_E);
		T[] AL = xor(AL_E, AL_C);
		T[] AF = xor(AF_C, AF_E);
		T[] BF = xor(BF_C, BF_E);
		T[][] AP = xor(AP_C, AP_E);
		T[][] BP = xor(BP_C, BP_E);

		T aft = mux(AF, N);
		T[] p = mux(mux(AP, BP, aft), N);
		T[] newAF = or(demux(not(aft), N), AF);
		T[] newBF = or(demux(aft, N), BF);

		T[][] ret = env.newTArray(4 + ttp, 0);
		ret[0] = p;
		ret[1] = xor(AL, AL_p);
		ret[2] = xor(newAF, AF_p);
		ret[3] = xor(newBF, BF_p);
		for (int i = 0; i < ttp; i++)
			ret[4 + i] = xor(AP[i], AP_p[i]);

		return ret;
	}
}
