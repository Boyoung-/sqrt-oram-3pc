package protocols.precomputation;

import communication.Communication;
import oram.Level;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;

public class PreRunSqrtOram extends Protocol {

	private int pid = P.RUN;

	public PreRunSqrtOram(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.offline_comp);

		for (int i = 0; i < predata.length; i++) {
			Level level = oram.getLevel(i);
			PreAccess preacc = new PreAccess(con1, con2, md);
			preacc.runE(predata[i], i == 0 ? (int) level.getFresh().size() : 1, level.getStash().size(), timer);
		}

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.offline_comp);

		for (int i = 0; i < predata.length; i++) {
			Level level = oram.getLevel(i);
			PreAccess preacc = new PreAccess(con1, con2, md);
			preacc.runD(predata[i], i == 0 ? (int) level.getFresh().size() : 1, level.getStash().size(), timer);
		}

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData[] predata, int counter, Timer timer) {
		timer.start(pid, M.offline_comp);

		for (int i = 0; i < predata.length; i++) {
			PreAccess preacc = new PreAccess(con1, con2, md);
			preacc.runC(predata[i], i == 0 ? (int) md.getNumBlocks(0) - counter : 1, counter, timer);
		}

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
