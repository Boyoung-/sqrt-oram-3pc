package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.Timer;
import util.Util;

public class PreSSXOT extends Protocol {

	private int id;
	private int pid;

	public PreSSXOT(Communication con1, Communication con2, Metadata md, int id, int pid) {
		super(con1, con2, md);
		this.id = id;
		this.pid = pid;
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.ssxot_E_pi[id] = con1.readLongArray64();
		predata.ssxot_E_r[id] = con1.readBlockArray64();
		timer.stop(pid, M.offline_read);
	}

	public void runD(PreData predata, long n, long k, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.ssxot_delta[id] = new Array64<Block>(k);
		for (long i = 0; i < k; i++)
			predata.ssxot_delta[id].set(i, new Block(id, md, Crypto.sr));

		predata.ssxot_E_pi[id] = Util.randomPermutationLong(n, Crypto.sr);
		predata.ssxot_C_pi[id] = Util.randomPermutationLong(n, Crypto.sr);
		predata.ssxot_E_pi_ivs[id] = Util.inversePermutationLong(predata.ssxot_E_pi[id]);
		predata.ssxot_C_pi_ivs[id] = Util.inversePermutationLong(predata.ssxot_C_pi[id]);

		predata.ssxot_E_r[id] = new Array64<Block>(n);
		predata.ssxot_C_r[id] = new Array64<Block>(n);
		for (long i = 0; i < n; i++) {
			predata.ssxot_E_r[id].set(i, new Block(id, md, Crypto.sr));
			predata.ssxot_C_r[id].set(i, new Block(id, md, Crypto.sr));
		}

		timer.start(pid, M.offline_write);
		con1.writeLongArray64(predata.ssxot_E_pi[id]);
		con1.writeBlockArray64(predata.ssxot_E_r[id]);
		con2.writeLongArray64(predata.ssxot_C_pi[id]);
		con2.writeBlockArray64(predata.ssxot_C_r[id]);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.ssxot_C_pi[id] = con2.readLongArray64();
		predata.ssxot_C_r[id] = con2.readBlockArray64();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
