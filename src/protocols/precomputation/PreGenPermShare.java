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

public class PreGenPermShare extends Protocol {

	private int pid = P.GPS;
	private int onoff = 3;

	public PreGenPermShare(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		timer.start(pid, M.online_read + onoff);
		predata.gps_r = con2.readLongArray64();
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		timer.start(pid, M.online_read + onoff);
		predata.gps_p = con2.readLongArray64();
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runC(PreData predata, long v, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		predata.gps_p = new Array64<Long>(v);
		predata.gps_r = new Array64<Long>(v);
		for (long i = 0; i < v; i++) {
			predata.gps_p.set(i, Crypto.sr.nextLong());
			predata.gps_r.set(i, Crypto.sr.nextLong());
		}

		timer.start(pid, M.online_write + onoff);
		con2.writeLongArray64(predata.gps_p);
		con1.writeLongArray64(predata.gps_r);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
