package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreInitPosMap extends Protocol {

	private int pid = P.IPM;
	private int onoff = 3;

	public PreInitPosMap(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		PreGenPermShare pregps = new PreGenPermShare(con1, con2, md);
		pregps.runE(predata, timer);

		long n = md.getNumBlocks(predata.getIndex());
		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.IPM_OP);
		preop.runE(predata, n, timer);

		if (predata.getIndex() > 0) {
			predata.ipm_pi_prime_E = Util.randomPermutationLong(n, Crypto.sr);

			timer.start(pid, M.online_write + onoff);
			con2.writeLongArray64(pid, predata.ipm_pi_prime_E);
			timer.stop(pid, M.online_write + onoff);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		PreGenPermShare pregps = new PreGenPermShare(con1, con2, md);
		pregps.runD(predata, timer);

		long n = md.getNumBlocks(predata.getIndex());
		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.IPM_OP);
		preop.runD(predata, n, timer);

		if (predata.getIndex() > 0) {
			predata.ipm_pi_prime_D = Util.randomPermutationLong(n, Crypto.sr);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		long n = md.getNumBlocks(predata.getIndex());
		PreGenPermShare pregps = new PreGenPermShare(con1, con2, md);
		pregps.runC(predata, n * md.getTwoTauPow(), timer);

		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.IPM_OP);
		preop.runC(predata, timer);

		if (predata.getIndex() > 0) {
			timer.start(pid, M.online_read + onoff);
			predata.ipm_pi_prime_E = con1.readLongArray64(pid);
			timer.stop(pid, M.online_read + onoff);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
