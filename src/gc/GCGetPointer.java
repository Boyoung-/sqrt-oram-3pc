package gc;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;

public class GCGetPointer<T> extends IntegerLib<T> {

	public GCGetPointer(CompEnv<T> e) {
		super(e);
	}

	public T[][] execute(T[] N_E, T[] N_C, T[] AF_E, T[] AF_C, T[] BF_E, T[] BF_C, T[][] AP_E, T[][] AP_C, T[][] BP_E,
			T[][] BP_C) {
		T[] N = xor(N_C, N_E);
		T[] AF = xor(AF_C, AF_E);
		T[] BF = xor(BF_C, BF_E);
		T[][] AP = xor(AP_C, AP_E);
		T[][] BP = xor(BP_C, BP_E);

		T aft = mux(AF, N);
		T[] p = mux(mux(AP, BP, aft), N);
		T[] newAF = or(demux(not(aft), N), AF);
		T[] newBF = or(demux(aft, N), BF);

		T[][] ret = env.newTArray(3, 0);
		ret[0] = p;
		ret[1] = newAF;
		ret[2] = newBF;
		return ret;
	}
}
