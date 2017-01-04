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

public class PreOblivPermute extends Protocol {

	// TODO: add level index?
	private int pid;

	public PreOblivPermute(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
	}

	public void runE(PreData predata, long s, Timer timer) {
		timer.start(pid, M.offline_comp);

		if (pid == P.OP_ON) {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.OP_XOT_ON);
			pressxot.runE(predata, timer);

			predata.op_e = new Array64<Block>(s);
			for (long i = 0; i < s; i++)
				predata.op_e.set(i, new Block(predata.getIndex(), md, Crypto.sr));

			timer.start(pid, M.offline_write);
			con2.writeBlockArray64(predata.op_e);
			timer.stop(pid, M.offline_write);
		}

		else { // pid == P.OP_OFF
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.OP_XOT_OFF);
			pressxot.runE(predata, timer);

			int lBytes = md.getLBytes(predata.getIndex());
			predata.offop_e = new Array64<byte[]>(s);
			for (long i = 0; i < s; i++)
				predata.offop_e.set(i, Util.nextBytes(lBytes, Crypto.sr));

			timer.start(pid, M.offline_write);
			con2.writeByteArray64(predata.offop_e);
			timer.stop(pid, M.offline_write);
		}

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, long s, Timer timer) {
		timer.start(pid, M.offline_comp);

		PreSSXOT pressxot = new PreSSXOT(con1, con2, md, (pid == P.OP_ON) ? P.OP_XOT_ON : P.OP_XOT_OFF);
		pressxot.runD(predata, s, s, timer);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		if (pid == P.OP_ON) {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.OP_XOT_ON);
			pressxot.runC(predata, timer);

			timer.start(pid, M.offline_read);
			predata.op_e = con1.readBlockArray64();
			timer.stop(pid, M.offline_read);
		}

		else {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.OP_XOT_OFF);
			pressxot.runC(predata, timer);

			timer.start(pid, M.offline_read);
			predata.offop_e = con1.readByteArray64();
			timer.stop(pid, M.offline_read);
		}

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
