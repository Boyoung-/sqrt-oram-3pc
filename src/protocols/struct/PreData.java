package protocols.struct;

import com.oblivm.backend.gc.GCSignal;

import crypto.PRF;
import gc.GCGetPointer;
import oram.Block;
import util.Array64;

public class PreData {
	private int index;

	public PreData(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	// COT
	public byte[] sscot_k;
	public byte[] sscot_kprime;
	public byte[][] sscot_r;
	public PRF sscot_F_k;
	public PRF sscot_F_kprime;

	public int[] access_sigma;

	// OP_XOT_ON
	public Array64<Block> ssxot_delta;
	public Array64<Long> ssxot_E_pi;
	public Array64<Long> ssxot_C_pi;
	public Array64<Long> ssxot_E_pi_ivs;
	public Array64<Long> ssxot_C_pi_ivs;
	public Array64<Block> ssxot_E_r;
	public Array64<Block> ssxot_C_r;

	// OP_XOT_OFF
	public Array64<byte[]> offssxot_delta;
	public Array64<Long> offssxot_E_pi;
	public Array64<Long> offssxot_C_pi;
	public Array64<Long> offssxot_E_pi_ivs;
	public Array64<Long> offssxot_C_pi_ivs;
	public Array64<byte[]> offssxot_E_r;
	public Array64<byte[]> offssxot_C_r;

	// ACC_XOT
	public Block[] accxot_delta;
	public int[] accxot_E_pi;
	public int[] accxot_C_pi;
	public int[] accxot_E_pi_ivs;
	public int[] accxot_C_pi_ivs;
	public Block[] accxot_E_r;
	public Block[] accxot_C_r;

	// GPS
	public Array64<Long> gps_p;
	public Array64<Long> gps_r;

	// GPC
	public Array64<Long> gpc_pi_D;
	public Array64<Long> gpc_sig1;
	public Array64<Long> gpc_sig2;
	public Array64<Long> gpc_r1;
	public Array64<Long> gpc_r2;
	public Array64<Long> gpc_gam1;
	public Array64<Long> gpc_gam2;
	public Array64<Long> gpc_t1;
	public Array64<Long> gpc_t2;

	// OP_ON
	public Array64<Block> op_e;

	// OP_OFF
	public Array64<byte[]> offop_e;

	// IPM
	public Array64<Long> ipm_pi_prime_D;
	public Array64<Long> ipm_pi_prime_E;

	// GP
	public GCGetPointer<GCSignal> gp_circuit;
	public Block gp_A_prime;
	public byte[] gp_BF_prime;

	public GCSignal[][] gp_E_nKeyPairs;
	public GCSignal[][] gp_C_nKeyPairs;
	public GCSignal[][] gp_E_alKeyPairs;
	public GCSignal[][] gp_C_alKeyPairs;
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
	public int[] acc_rho_ivs;
	public Block acc_r;
	public Block acc_A_b;
	public int[] acc_delta;
}
