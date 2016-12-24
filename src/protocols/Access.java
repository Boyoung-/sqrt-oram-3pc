package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Level;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreAccess;
import protocols.struct.OutGetPointer;
import protocols.struct.OutSSCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class Access extends Protocol {

	private int pid = P.ACC;

	public Access(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public long runE(PreData predata, byte[] N, Level level, Timer timer) {
		timer.start(pid, M.online_comp);

		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = Util.rmSignBit(Util.getSubBits(bigN, tau + lBits, tau).toByteArray());
		byte[] sufN = Util.rmSignBit(Util.getSubBits(bigN, tau, 0).toByteArray());

		// step 1
		Block[] all = Util.permute(Util.concat(Level.toArray(level.getStash()), Level.toArray(level.getFresh())),
				predata.acc_rho);
		byte[][] e = new byte[all.length][];
		Block[] y = new Block[all.length];
		for (int i = 0; i < all.length; i++) {
			e[i] = Util.xor(all[i].getL(), preN);
			y[i] = all[i].xor(predata.acc_A_b);
		}

		// step 2
		Block B_b = level.getFreshBlock(0).xor(predata.acc_r);

		// step 3
		byte[][] y_bytes = new byte[y.length][];
		for (int i = 0; i < y.length; i++)
			y_bytes[i] = y[i].toByteArray();

		SSCOT sscot = new SSCOT(con1, con2, md);
		sscot.runE(predata, y_bytes, e, timer);

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		gp.runE(predata, sufN, predata.acc_A_b, B_b, timer);

		predata.acc_A_b.setF(predata.gp_AF_prime);
		B_b.setF(predata.gp_BF_prime);

		timer.start(pid, M.online_read);
		long p = con1.readLong(pid);
		timer.stop(pid, M.online_read);

		// step 6
		SSXOT ssxot = new SSXOT(con2, con1, md, P.ACC_XOT);
		all = ssxot.runE(predata, Util.concat(all, new Block[] { B_b }), timer);

		// step 7
		all = Util.permute(all, predata.acc_rho_ivs);
		Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
		Block[] fresh = Arrays.copyOfRange(all, stash.length, all.length);

		// step 8
		level.setStash(Level.toList(Util.concat(stash, new Block[] { predata.acc_A_b })));
		// level.setFresh(Level.toArray64(fresh));

		timer.stop(pid, M.online_comp);
		return p;
	}

	public long runD(PreData predata, byte[] N, Level level, Timer timer) {
		timer.start(pid, M.online_comp);

		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = Util.rmSignBit(Util.getSubBits(bigN, tau + lBits, tau).toByteArray());
		byte[] sufN = Util.rmSignBit(Util.getSubBits(bigN, tau, 0).toByteArray());

		// step 1
		Block[] all = Util.permute(Util.concat(Level.toArray(level.getStash()), Level.toArray(level.getFresh())),
				predata.acc_rho);
		byte[][] e = new byte[all.length][];
		for (int i = 0; i < all.length; i++) {
			e[i] = Util.xor(all[i].getL(), preN);
		}

		// step 2
		Block B_a = level.getFreshBlock(0).xor(predata.acc_r);

		timer.start(pid, M.online_write);
		con2.write(pid, sufN);
		con2.write(pid, all);
		con2.write(pid, B_a);
		timer.stop(pid, M.online_write);

		// step 3
		SSCOT sscot = new SSCOT(con1, con2, md);
		sscot.runD(predata, e, timer);
		Block A_a = con2.readBlock(); // TODO: remove this!!!

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		OutGetPointer outgp = gp.runD(predata, timer);

		A_a.setF(outgp.AF);
		B_a.setF(outgp.BF);

		timer.start(pid, M.online_write);
		con1.write(pid, outgp.p);
		timer.stop(pid, M.online_write);

		// step 6
		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		all = ssxot.runC(predata, Util.concat(all, new Block[] { B_a }), timer);

		// step 7
		all = Util.permute(all, predata.acc_rho_ivs);
		Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
		Block[] fresh = Arrays.copyOfRange(all, stash.length, all.length);

		// step 8
		level.setStash(Level.toList(Util.concat(stash, new Block[] { A_a })));
		// level.setFresh(Level.toArray64(fresh));

		timer.stop(pid, M.online_comp);
		return outgp.p;
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		timer.start(pid, M.online_read);
		byte[] sufN = con2.read(pid);
		Block[] all = con2.readBlockArray(pid);
		Block B_a = con2.readBlock(pid);
		timer.stop(pid, M.online_read);

		// step 3
		SSCOT sscot = new SSCOT(con1, con2, md);
		OutSSCOT outsscot = sscot.runC(timer);

		Block A_a = new Block(predata.getIndex(), md, outsscot.m_t).xor(all[outsscot.t]);
		con2.write(A_a); // TODO: remove this!!!

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		gp.runC(predata, sufN, A_a, B_a, timer);

		// step 6
		predata.acc_delta[outsscot.t] = all.length;

		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		ssxot.runD(predata, predata.acc_delta, timer);

		timer.stop(pid, M.online_comp);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		byte[] N = null;
		byte[] N_a = null;
		byte[] N_b = null;
		PreData predata = null;
		PreAccess preacc = null;
		int levelIndex;

		for (int j = 0; j < 100; j++) {
			preacc = new PreAccess(con1, con2, md);

			if (party == Party.Eddie) {
				levelIndex = 0;// Crypto.sr.nextInt(md.getNumLevels());
				con1.write(levelIndex);
				con2.write(levelIndex);
				Level level = oram.getLevel(levelIndex);
				con2.write(level.getStash().size());
				predata = new PreData(levelIndex);
				preacc.runE(predata, level.getStash().size(), timer);

				N = Util.nextBytes((md.getLBits(1) + 7) / 8, Crypto.sr);
				N_a = Util.nextBytes((md.getLBits(1) + 7) / 8, Crypto.sr);
				N_b = Util.xor(N, N_a);
				con1.write(N_a);
				long p = this.runE(predata, N_b, level, timer);

				int tau = md.getTau();
				int lBits = md.getLBits(levelIndex);
				BigInteger bigN = new BigInteger(N);
				int preN = Util.getSubBits(bigN, tau + lBits, tau).intValue();
				int sufN = Util.getSubBits(bigN, tau, 0).intValue();
				long realP = Util
						.getSubBits(new BigInteger(level.getFreshBlock(preN).getP(sufN)), md.getPBits(levelIndex), 0)
						.longValue();
				Block changed = con1.readBlock();
				changed.setXor(level.getStashBlock(0));
				long realP2 = Util.getSubBits(new BigInteger(changed.getP(sufN)), md.getPBits(levelIndex), 0)
						.longValue();
				level.emptyStash();

				if (p == realP && p == realP2 && (changed.getF(sufN) & 1) == 1)
					System.out.println(j + ": Acc test passed");
				else {
					System.err.println(j + ": Acc test failed");
				}

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				Level level = oram.getLevel(levelIndex);
				predata = new PreData(levelIndex);
				preacc.runD(predata, level.getStash().size(), timer);

				N_a = con1.read();
				this.runD(predata, N_a, level, timer);

				con1.write(level.getStashBlock(0));
				level.emptyStash();

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				int s = con1.readInt();
				predata = new PreData(levelIndex);
				preacc.runC(predata, s, timer);

				this.runC(predata, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
