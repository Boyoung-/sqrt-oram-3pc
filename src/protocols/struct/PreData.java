package protocols.struct;

import com.oblivm.backend.gc.GCSignal;

import crypto.PRF;
import gc.GCGetPointer;
import oram.Block;
import util.Array64;
import util.P;
import util.Util;

public class PreData {
	private int index;

	public PreData(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	// SSCOT
	public byte[] sscot_k;
	public byte[] sscot_kprime;
	public byte[][] sscot_r;
	public PRF sscot_F_k;
	public PRF sscot_F_kprime;

	public int[] access_sigma;

	// SSXOT
	@SuppressWarnings("unchecked")
	public Array64<Block>[] ssxot_delta = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Long>[] ssxot_E_pi = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Long>[] ssxot_C_pi = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Long>[] ssxot_E_pi_ivs = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Long>[] ssxot_C_pi_ivs = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Block>[] ssxot_E_r = Util.genericArray(Array64.class, P.size);
	@SuppressWarnings("unchecked")
	public Array64<Block>[] ssxot_C_r = Util.genericArray(Array64.class, P.size);

	// GPS
	public Array64<Long> gps_p;
	public Array64<Long> gps_r;

	// GPC
	public Array64<Long> gpc_pi_D;
	public Array64<Long> gpc_sig1;
	public Array64<Long> gpc_sig2;
	// public Array64<Long> gpc_s;
	public Array64<Long> gpc_r1;
	public Array64<Long> gpc_r2;
	public Array64<Long> gpc_gam1;
	public Array64<Long> gpc_gam2;
	public Array64<Long> gpc_t1;
	public Array64<Long> gpc_t2;

	// OP
	public Array64<Block> op_e;

	// IPM
	public Array64<Long> ipm_pi_prime_D;
	public Array64<Long> ipm_pi_prime_E;

	// GP
	public byte[] gp_AF_prime;
	public byte[] gp_BF_prime;
	public GCGetPointer<GCSignal> gp_circuit;
	public GCSignal[][] gp_E_nKeyPairs;
	public GCSignal[][] gp_C_nKeyPairs;
	public GCSignal[][] gp_E_afKeyPairs;
	public GCSignal[][] gp_C_afKeyPairs;
	public GCSignal[][] gp_E_bfKeyPairs;
	public GCSignal[][] gp_C_bfKeyPairs;
	public GCSignal[][][] gp_E_apKeyPairs;
	public GCSignal[][][] gp_C_apKeyPairs;
	public GCSignal[][][] gp_E_bpKeyPairs;
	public GCSignal[][][] gp_C_bpKeyPairs;
	public byte[][][] gp_outKeyHashes;

	// ACC
	public int[] acc_rho;
	public Block acc_r;
	public Block acc_A_b;
	public int[] acc_delta;
}
