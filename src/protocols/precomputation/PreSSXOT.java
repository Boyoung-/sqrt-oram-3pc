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
import util.P;
import util.Timer;
import util.Util;

public class PreSSXOT extends Protocol {

	// TODO: add level index?
	private int pid;

	public PreSSXOT(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		if (pid == P.ACC_XOT) {
			predata.accxot_E_pi = con1.readIntArray();
			predata.accxot_E_r = con1.readBlockArray();
		} else {
			predata.ssxot_E_pi[pid] = con1.readLongArray64();
			predata.ssxot_E_r[pid] = con1.readBlockArray64();
		}
		timer.stop(pid, M.offline_read);
	}

	public void runD(PreData predata, long n, long k, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.ssxot_delta[pid] = new Array64<Block>(k);
		for (long i = 0; i < k; i++)
			predata.ssxot_delta[pid].set(i, new Block(predata.getIndex(), md, Crypto.sr));

		predata.ssxot_E_pi[pid] = Util.randomPermutationLong(n, Crypto.sr);
		predata.ssxot_C_pi[pid] = Util.randomPermutationLong(n, Crypto.sr);
		predata.ssxot_E_pi_ivs[pid] = Util.inversePermutationLong(predata.ssxot_E_pi[pid]);
		predata.ssxot_C_pi_ivs[pid] = Util.inversePermutationLong(predata.ssxot_C_pi[pid]);

		predata.ssxot_E_r[pid] = new Array64<Block>(n);
		predata.ssxot_C_r[pid] = new Array64<Block>(n);
		for (long i = 0; i < n; i++) {
			predata.ssxot_E_r[pid].set(i, new Block(predata.getIndex(), md, Crypto.sr));
			predata.ssxot_C_r[pid].set(i, new Block(predata.getIndex(), md, Crypto.sr));
		}

		timer.start(pid, M.offline_write);
		con1.writeLongArray64(predata.ssxot_E_pi[pid]);
		con1.writeBlockArray64(predata.ssxot_E_r[pid]);
		con2.writeLongArray64(predata.ssxot_C_pi[pid]);
		con2.writeBlockArray64(predata.ssxot_C_r[pid]);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, int n, int k, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.accxot_delta = new Block[k];
		for (int i = 0; i < k; i++)
			predata.accxot_delta[i] = new Block(predata.getIndex(), md, Crypto.sr);

		predata.accxot_E_pi = Util.randomPermutation(n, Crypto.sr);
		predata.accxot_C_pi = Util.randomPermutation(n, Crypto.sr);
		predata.accxot_E_pi_ivs = Util.inversePermutation(predata.accxot_E_pi);
		predata.accxot_C_pi_ivs = Util.inversePermutation(predata.accxot_C_pi);

		predata.accxot_E_r = new Block[n];
		predata.accxot_C_r = new Block[n];
		for (int i = 0; i < n; i++) {
			predata.accxot_E_r[i] = new Block(predata.getIndex(), md, Crypto.sr);
			predata.accxot_C_r[i] = new Block(predata.getIndex(), md, Crypto.sr);
		}

		timer.start(pid, M.offline_write);
		con1.write(predata.accxot_E_pi);
		con1.write(predata.accxot_E_r);
		con2.write(predata.accxot_C_pi);
		con2.write(predata.accxot_C_r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		if (pid == P.ACC_XOT) {
			predata.accxot_C_pi = con2.readIntArray();
			predata.accxot_C_r = con2.readBlockArray();
		} else {
			predata.ssxot_C_pi[pid] = con2.readLongArray64();
			predata.ssxot_C_r[pid] = con2.readBlockArray64();
		}
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
