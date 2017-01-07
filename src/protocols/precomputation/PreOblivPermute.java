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

	private int pid;
	private int onoff = 3;

	public PreOblivPermute(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
	}

	public void runE(PreData predata, long s, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		if (pid == P.IPM_OP) {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.IPM_OP_XOT);
			pressxot.runE(predata, timer);

			predata.op_e = new Array64<Block>(s);
			for (long i = 0; i < s; i++)
				predata.op_e.set(i, new Block(predata.getIndex(), md, Crypto.sr));

			timer.start(pid, M.online_write + onoff);
			con2.writeBlockArray64(predata.op_e);
			timer.stop(pid, M.online_write + onoff);
		}

		else { // pid == P.INIT_OP_OFF or P.INIT_OP_ON
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md,
					pid == P.INIT_OP_OFF ? P.INIT_OP_XOT_OFF : P.INIT_OP_XOT_ON);
			pressxot.runE(predata, timer);

			int bytes = pid == P.INIT_OP_OFF ? md.getLBytes(predata.getIndex()) : md.getDBytes();

			predata.initop_e = new Array64<byte[]>(s);
			for (long i = 0; i < s; i++)
				predata.initop_e.set(i, Util.nextBytes(bytes, Crypto.sr));

			timer.start(pid, M.online_write + onoff);
			con2.writeByteArray64(predata.initop_e);
			timer.stop(pid, M.online_write + onoff);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runD(PreData predata, long s, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid + 1);
		pressxot.runD(predata, s, s, timer);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		if (pid == P.IPM_OP) {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.IPM_OP_XOT);
			pressxot.runC(predata, timer);

			timer.start(pid, M.online_read + onoff);
			predata.op_e = con1.readBlockArray64();
			timer.stop(pid, M.online_read + onoff);
		}

		else {
			PreSSXOT pressxot = new PreSSXOT(con1, con2, md,
					pid == P.INIT_OP_OFF ? P.INIT_OP_XOT_OFF : P.INIT_OP_XOT_ON);
			pressxot.runC(predata, timer);

			timer.start(pid, M.online_read + onoff);
			predata.initop_e = con1.readByteArray64();
			timer.stop(pid, M.online_read + onoff);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
