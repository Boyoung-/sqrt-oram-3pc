package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreAccess extends Protocol {

	private int pid = P.ACC;

	public PreAccess(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, int n, int s, Timer timer) {
		timer.start(pid, M.offline_comp);

		int levelIndex = predata.getIndex();

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2, md);
		presscot.runE(predata, n + s, timer);

		// GP
		PreGetPointer pregp = new PreGetPointer(con1, con2, md);
		pregp.runE(predata, timer);

		// SSXOT
		PreSSXOT pressxot = new PreSSXOT(con2, con1, md, P.ACC_XOT);
		pressxot.runE(predata, timer);

		// ACC
		predata.acc_A_b = new Block(levelIndex, md, Crypto.sr);

		timer.start(pid, M.offline_read);
		predata.acc_rho = con1.readIntArray();
		predata.acc_rho_ivs = con1.readIntArray();
		predata.acc_r = con1.readBlock();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, int n, int s, Timer timer) {
		timer.start(pid, M.offline_comp);

		int levelIndex = predata.getIndex();

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2, md);
		presscot.runD(predata, timer);

		// GP
		PreGetPointer pregp = new PreGetPointer(con1, con2, md);
		pregp.runD(predata, timer);

		// SSXOT
		PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.ACC_XOT);
		pressxot.runC(predata, timer);

		// ACC
		predata.acc_rho = Util.randomPermutation(n + s, Crypto.sr);
		predata.acc_rho_ivs = Util.inversePermutation(predata.acc_rho);
		predata.acc_r = new Block(levelIndex, md, Crypto.sr);

		timer.start(pid, M.offline_write);
		con1.write(predata.acc_rho);
		con1.write(predata.acc_rho_ivs);
		con1.write(predata.acc_r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, int n, int s, Timer timer) {
		timer.start(pid, M.offline_comp);

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2, md);
		presscot.runC();

		// GP
		PreGetPointer pregp = new PreGetPointer(con1, con2, md);
		pregp.runC(predata, timer);

		// SSXOT
		PreSSXOT pressxot = new PreSSXOT(con1, con2, md, P.ACC_XOT);
		pressxot.runD(predata, n + s + 1, n + s, timer);

		// ACC
		predata.acc_delta = Util.identityPermutation(n + s);

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
