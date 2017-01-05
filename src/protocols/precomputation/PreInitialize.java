package protocols.precomputation;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.GenPermConcat;
import protocols.InitPosMap;
import protocols.OblivPermute;
import protocols.Protocol;
import protocols.struct.OutInitPosMap;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreInitialize extends Protocol {

	private int pid = P.INIT;
	private int onoff = 3;

	public PreInitialize(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		int h = md.getNumLevels();
		long n = md.getNumBlocks(h - 1);
		predata[h - 1].init_pi_E = Util.randomPermutationLong(n, Crypto.sr);

		timer.start(pid, M.online_write + onoff);
		con2.writeLongArray64(predata[h - 1].init_pi_E);
		timer.stop(pid, M.online_write + onoff);

		// step 2
		Array64<Block> fresh = oram.getLevel(h - 1).getFresh();
		Array64<byte[]> L = new Array64<byte[]>(fresh.size());
		for (long i = 0; i < L.size(); i++)
			L.set(i, fresh.get(i).getL());

		// step 3
		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.INIT_OP_OFF);
		preop.runE(predata[h - 1], n, timer);

		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_OFF);
		predata[h - 1].init_L_prime_b = op.runInitE(predata[h - 1], predata[h - 1].init_pi_E, L, timer);
		Array64<Long> L_prime = new Array64<Long>(L.size());
		for (long i = 0; i < L.size(); i++)
			L_prime.set(i, new BigInteger(1, predata[h - 1].init_L_prime_b.get(i)).longValue());

		// step 7
		PreGenPermConcat pregpc = new PreGenPermConcat(con1, con2, md);
		pregpc.runE(predata[h - 1], timer);

		GenPermConcat gpc = new GenPermConcat(con1, con2, md);
		Array64<Long> pi_E_prime = gpc.runE(predata[h - 1], L_prime, timer);

		// step 9-11
		for (int i = h - 2; i >= 0; i--) {
			PreInitPosMap preipm = new PreInitPosMap(con1, con2, md);
			preipm.runE(predata[i], timer);

			InitPosMap ipm = new InitPosMap(con1, con2, md);
			OutInitPosMap outipm = ipm.runE(predata[i], pi_E_prime, timer);
			oram.getLevel(i).setFresh(outipm.fresh_b);
			pi_E_prime = outipm.pi_prime_E;
		}

		///////////////////////////////////////////////////////////////

		// step 2
		PreOblivPermute preop_on = new PreOblivPermute(con1, con2, md, P.INIT_OP_ON);
		preop_on.runE(predata[h - 1], n, timer);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runD(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		int h = md.getNumLevels();
		long n = md.getNumBlocks(h - 1);
		predata[h - 1].init_pi_D = Util.randomPermutationLong(n, Crypto.sr);

		// step 2
		Array64<Block> fresh = oram.getLevel(h - 1).getFresh();
		Array64<byte[]> L = new Array64<byte[]>(fresh.size());
		for (long i = 0; i < L.size(); i++)
			L.set(i, fresh.get(i).getL());

		// step 3
		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.INIT_OP_OFF);
		preop.runD(predata[h - 1], n, timer);

		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_OFF);
		predata[h - 1].init_L_prime_a = op.runInitD(predata[h - 1], predata[h - 1].init_pi_D, L, timer);
		Array64<Long> L_prime = new Array64<Long>(L.size());
		for (long i = 0; i < L.size(); i++)
			L_prime.set(i, new BigInteger(1, predata[h - 1].init_L_prime_a.get(i)).longValue());

		// step 7
		PreGenPermConcat pregpc = new PreGenPermConcat(con1, con2, md);
		pregpc.runD(predata[h - 1], n, timer);

		GenPermConcat gpc = new GenPermConcat(con1, con2, md);
		Array64<Long> pi_D_prime = gpc.runD(predata[h - 1], L_prime, timer);

		// step 9-11
		for (int i = h - 2; i >= 0; i--) {
			PreInitPosMap preipm = new PreInitPosMap(con1, con2, md);
			preipm.runD(predata[i], timer);

			InitPosMap ipm = new InitPosMap(con1, con2, md);
			OutInitPosMap outipm = ipm.runD(predata[i], pi_D_prime, timer);
			oram.getLevel(i).setFresh(outipm.fresh_a);
			pi_D_prime = outipm.pi_prime_D;
		}

		///////////////////////////////////////////////////////////////

		// step 2
		PreOblivPermute preop_on = new PreOblivPermute(con1, con2, md, P.INIT_OP_ON);
		preop_on.runD(predata[h - 1], n, timer);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runC(PreData[] predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		int h = md.getNumLevels();

		timer.start(pid, M.online_read + onoff);
		predata[h - 1].init_pi_E = con1.readLongArray64();
		timer.stop(pid, M.online_read + onoff);

		// step 3
		PreOblivPermute preop = new PreOblivPermute(con1, con2, md, P.INIT_OP_OFF);
		preop.runC(predata[h - 1], timer);

		OblivPermute op = new OblivPermute(con1, con2, md, P.INIT_OP_OFF);
		op.runInitC(predata[h - 1], predata[h - 1].init_pi_E, timer);

		// step 7
		PreGenPermConcat pregpc = new PreGenPermConcat(con1, con2, md);
		pregpc.runC(predata[h - 1], timer);

		GenPermConcat gpc = new GenPermConcat(con1, con2, md);
		gpc.runC(predata[h - 1], timer);

		// step 9-11
		for (int i = h - 2; i >= 0; i--) {
			PreInitPosMap preipm = new PreInitPosMap(con1, con2, md);
			preipm.runC(predata[i], timer);

			InitPosMap ipm = new InitPosMap(con1, con2, md);
			ipm.runC(predata[i], timer);
		}

		///////////////////////////////////////////////////////////////

		// step 2
		PreOblivPermute preop_on = new PreOblivPermute(con1, con2, md, P.INIT_OP_ON);
		preop_on.runC(predata[h - 1], timer);

		timer.stop(pid, M.online_comp + onoff);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
