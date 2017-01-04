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
		} else if (pid == P.IPM_OP_XOT) {
			predata.ssxot_E_pi = con1.readLongArray64();
			predata.ssxot_E_r = con1.readBlockArray64();
		} else {
			predata.offssxot_E_pi = con1.readLongArray64();
			predata.offssxot_E_r = con1.readByteArray64();
		}
		timer.stop(pid, M.offline_read);
	}

	// pid == P.ACC_XOT
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

	public void runD(PreData predata, long n, long k, Timer timer) {
		timer.start(pid, M.offline_comp);

		if (pid == P.IPM_OP_XOT) {
			predata.ssxot_delta = new Array64<Block>(k);
			for (long i = 0; i < k; i++)
				predata.ssxot_delta.set(i, new Block(predata.getIndex(), md, Crypto.sr));

			predata.ssxot_E_pi = Util.randomPermutationLong(n, Crypto.sr);
			predata.ssxot_C_pi = Util.randomPermutationLong(n, Crypto.sr);
			predata.ssxot_E_pi_ivs = Util.inversePermutationLong(predata.ssxot_E_pi);
			predata.ssxot_C_pi_ivs = Util.inversePermutationLong(predata.ssxot_C_pi);

			predata.ssxot_E_r = new Array64<Block>(n);
			predata.ssxot_C_r = new Array64<Block>(n);
			for (long i = 0; i < n; i++) {
				predata.ssxot_E_r.set(i, new Block(predata.getIndex(), md, Crypto.sr));
				predata.ssxot_C_r.set(i, new Block(predata.getIndex(), md, Crypto.sr));
			}

			timer.start(pid, M.offline_write);
			con1.writeLongArray64(predata.ssxot_E_pi);
			con1.writeBlockArray64(predata.ssxot_E_r);
			con2.writeLongArray64(predata.ssxot_C_pi);
			con2.writeBlockArray64(predata.ssxot_C_r);
			timer.stop(pid, M.offline_write);
		}

		else { // pid == P.INIT_OP_XOT_OFF or P.INIT_OP_XOT_ON
			int bytes = pid == P.INIT_OP_XOT_OFF ? md.getLBytes(predata.getIndex()) : md.getDBytes();

			predata.offssxot_delta = new Array64<byte[]>(k);
			for (long i = 0; i < k; i++)
				predata.offssxot_delta.set(i, Util.nextBytes(bytes, Crypto.sr));

			predata.offssxot_E_pi = Util.randomPermutationLong(n, Crypto.sr);
			predata.offssxot_C_pi = Util.randomPermutationLong(n, Crypto.sr);
			predata.offssxot_E_pi_ivs = Util.inversePermutationLong(predata.offssxot_E_pi);
			predata.offssxot_C_pi_ivs = Util.inversePermutationLong(predata.offssxot_C_pi);

			predata.offssxot_E_r = new Array64<byte[]>(n);
			predata.offssxot_C_r = new Array64<byte[]>(n);
			for (long i = 0; i < n; i++) {
				predata.offssxot_E_r.set(i, Util.nextBytes(bytes, Crypto.sr));
				predata.offssxot_C_r.set(i, Util.nextBytes(bytes, Crypto.sr));
			}

			timer.start(pid, M.offline_write);
			con1.writeLongArray64(predata.offssxot_E_pi);
			con1.writeByteArray64(predata.offssxot_E_r);
			con2.writeLongArray64(predata.offssxot_C_pi);
			con2.writeByteArray64(predata.offssxot_C_r);
			timer.stop(pid, M.offline_write);
		}

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		if (pid == P.ACC_XOT) {
			predata.accxot_C_pi = con2.readIntArray();
			predata.accxot_C_r = con2.readBlockArray();
		} else if (pid == P.IPM_OP_XOT) {
			predata.ssxot_C_pi = con2.readLongArray64();
			predata.ssxot_C_r = con2.readBlockArray64();
		} else {
			predata.offssxot_C_pi = con2.readLongArray64();
			predata.offssxot_C_r = con2.readByteArray64();
		}
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
