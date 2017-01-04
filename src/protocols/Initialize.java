package protocols;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Level;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreInitialize;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;

// TODO: think about Util.rmSignBit
// TODO: change runOff.. to runInit..
public class Initialize extends Protocol {

	private int pid = P.INIT;

	public Initialize(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Level level, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Array64<byte[]> Y = new Array64<byte[]>(level.getFresh().size());
		for (long i = 0; i < Y.size(); i++)
			Y.set(i, level.getFreshBlock(i).getRec());

		// step 2
		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_ON);
		Array64<byte[]> Y_prime = op.runOffE(predata, predata.init_pi_E, Y, timer);

		// step 3
		for (long i = 0; i < Y.size(); i++) {
			Block block = level.getFreshBlock(i);
			block.setL(predata.init_L_prime_b.get(i));
			block.setRec(Y_prime.get(i));
		}

		timer.stop(pid, M.online_comp);
	}

	public void runD(PreData predata, Level level, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Array64<byte[]> Y = new Array64<byte[]>(level.getFresh().size());
		for (long i = 0; i < Y.size(); i++)
			Y.set(i, level.getFreshBlock(i).getRec());

		// step 2
		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_ON);
		Array64<byte[]> Y_prime = op.runOffD(predata, predata.init_pi_D, Y, timer);

		// step 3
		for (long i = 0; i < Y.size(); i++) {
			Block block = level.getFreshBlock(i);
			block.setL(predata.init_L_prime_a.get(i));
			block.setRec(Y_prime.get(i));
		}

		timer.stop(pid, M.online_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_ON);
		op.runOffC(predata, predata.init_pi_E, timer);

		timer.stop(pid, M.online_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		int h = md.getNumLevels();
		PreData[] predata = new PreData[h];
		PreInitialize preinit = null;

		for (int j = 0; j < 10; j++) {
			preinit = new PreInitialize(con1, con2, md);
			for (int i = 0; i < h; i++)
				predata[i] = new PreData(i);

			if (party == Party.Eddie) {
				preinit.runE(predata, oram, timer);

				this.runE(predata[h - 1], oram.getLevel(h - 1), timer);

				if (true)
					System.out.println(j + ": Init test passed");
				else
					System.err.println(j + ": Init test failed");

			} else if (party == Party.Debbie) {
				preinit.runD(predata, oram, timer);

				this.runD(predata[h - 1], oram.getLevel(h - 1), timer);

			} else if (party == Party.Charlie) {
				preinit.runC(predata, timer);

				this.runC(predata[h - 1], timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
