package protocols.struct;

import com.oblivm.backend.gc.GCSignal;

import crypto.PRF;
import gc.GCRoute;
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

	public GCSignal[][] evict_LiKeyPairs;
	public GCSignal[][][] evict_E_feKeyPairs;
	public GCSignal[][][] evict_C_feKeyPairs;
	public GCSignal[][][][] evict_E_labelKeyPairs;
	public GCSignal[][][][] evict_C_labelKeyPairs;
	public GCSignal[][][] evict_deltaKeyPairs;
	public byte[][][] evict_tiOutKeyHashes;
	public GCSignal[][][] evict_targetOutKeyPairs;
	public GCRoute<GCSignal> evict_gcroute;
	public int[] evict_pi;
	public byte[][] evict_delta;
	public byte[][] evict_rho;
	public int[][] evict_delta_p;
	public int[][] evict_rho_p;
}
