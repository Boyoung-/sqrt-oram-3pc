package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;

public class PreSSCOT extends Protocol {

	private int pid = P.COT;
	private int onoff = 3;

	public PreSSCOT(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, int n, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		predata.sscot_k = PRF.generateKey(Crypto.sr);
		predata.sscot_kprime = PRF.generateKey(Crypto.sr);
		predata.sscot_r = new byte[n][];
		for (int i = 0; i < n; i++) {
			predata.sscot_r[i] = new byte[Crypto.secParamBytes];
			Crypto.sr.nextBytes(predata.sscot_r[i]);
		}

		timer.start(pid, M.online_write + onoff);
		con1.write(pid, predata.sscot_k);
		con1.write(pid, predata.sscot_kprime);
		con1.write(pid, predata.sscot_r);
		timer.stop(pid, M.online_write + onoff);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);
		predata.sscot_F_kprime = new PRF(Crypto.secParam);
		predata.sscot_F_kprime.init(predata.sscot_kprime);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		timer.start(pid, M.online_read + onoff);
		predata.sscot_k = con1.read(pid);
		predata.sscot_kprime = con1.read(pid);
		predata.sscot_r = con1.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read + onoff);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);
		predata.sscot_F_kprime = new PRF(Crypto.secParam);
		predata.sscot_F_kprime.init(predata.sscot_kprime);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runC() {
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
