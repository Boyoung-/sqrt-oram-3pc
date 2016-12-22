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

public class PreOblivPermute extends Protocol {

	// TODO: add level index?
	private int pid;

	public PreOblivPermute(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
	}

	public void runE(PreData predata, long s, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.op_e = new Array64<Block>(s);
		for (long i = 0; i < s; i++)
			predata.op_e.set(i, new Block(predata.getIndex(), md, Crypto.sr));

		timer.start(pid, M.offline_write);
		con2.writeBlockArray64(predata.op_e);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runD() {
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.op_e = con1.readBlockArray64();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
