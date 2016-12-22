package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
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

public class PreGenPermConcat extends Protocol {

	private int pid = P.GPC;

	public PreGenPermConcat(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		predata.gpc_sig2 = con1.readLongArray64();
		predata.gpc_r2 = con1.readLongArray64();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, long v, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.gpc_pi_D = Util.randomPermutationLong(v, Crypto.sr);
		predata.gpc_sig1 = Util.randomPermutationLong(v, Crypto.sr);
		predata.gpc_sig2 = Util.randomPermutationLong(v, Crypto.sr);
		Array64<Long> s = new Array64<Long>(v);
		predata.gpc_r1 = new Array64<Long>(v);
		predata.gpc_r2 = new Array64<Long>(v);
		for (long i = 0; i < v; i++) {
			s.set(i, Crypto.sr.nextLong());
			predata.gpc_r1.set(i, Crypto.sr.nextLong());
			predata.gpc_r2.set(i, Crypto.sr.nextLong());
		}
		Array64<Long> pi_D_ivs = Util.inversePermutationLong(predata.gpc_pi_D);
		predata.gpc_gam1 = Util.permute(pi_D_ivs, predata.gpc_sig2);
		predata.gpc_gam2 = Util.permute(pi_D_ivs, predata.gpc_sig1);
		predata.gpc_t1 = Util.xor(Util.permute(predata.gpc_r2, predata.gpc_gam1), s);
		predata.gpc_t2 = Util.xor(Util.permute(predata.gpc_r1, predata.gpc_gam2), s);

		timer.start(pid, M.offline_write);
		con2.writeLongArray64(predata.gpc_gam1);
		con2.writeLongArray64(predata.gpc_t1);
		con1.writeLongArray64(predata.gpc_sig2);
		con1.writeLongArray64(predata.gpc_r2);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		predata.gpc_gam1 = con2.readLongArray64();
		predata.gpc_t1 = con2.readLongArray64();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
