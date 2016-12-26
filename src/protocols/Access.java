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

// TODO: think about Util.rmSignBit
public class Access extends Protocol {

	private int pid = P.ACC;

	public Access(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public long runE(PreData predata, byte[] N, Level level, long p, Timer timer) {
		timer.start(pid, M.online_comp);

		int levelIndex = predata.getIndex();
		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = Util.padArray(Util.getSubBits(bigN, tau + lBits, tau).toByteArray(), md.getLBytes(levelIndex));
		byte[] sufN = Util.padArray(Util.getSubBits(bigN, tau, 0).toByteArray(), (md.getTau() + 7) / 8);

		// step 1
		Block B_b = level.getFreshBlock(p);

		Block[] all = null;
		if (levelIndex == 0)
			all = Util.permute(Util.concat(Level.toArray(level.getStash()), Level.toArray(level.getFresh())),
					predata.acc_rho);
		else
			all = Util.permute(Util.concat(Level.toArray(level.getStash()), new Block[] { B_b }), predata.acc_rho);
		byte[][] e = new byte[all.length][];
		Block[] y = new Block[all.length];
		for (int i = 0; i < all.length; i++) {
			e[i] = Util.xor(
					Util.padArray(Util.getSubBits(new BigInteger(all[i].getL()), lBits, 0).toByteArray(), preN.length),
					preN); // TODO: this is ugly: make GP embedded input with
							// leading zeros
			y[i] = all[i].xor(predata.acc_A_b);
		}

		// step 2
		B_b = B_b.xor(predata.acc_r);

		// step 3
		byte[][] y_bytes = new byte[y.length][];
		for (int i = 0; i < y.length; i++)
			y_bytes[i] = y[i].toByteArray();

		SSCOT sscot = new SSCOT(con1, con2, md);
		sscot.runE(predata, y_bytes, e, timer);

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		gp.runE(predata, sufN, predata.acc_A_b, B_b, timer);

		predata.acc_A_b = predata.gp_A_prime;
		B_b.setF(predata.gp_BF_prime);

		timer.start(pid, M.online_read);
		p = con1.readLong(pid);
		timer.stop(pid, M.online_read);

		// step 6
		SSXOT ssxot = new SSXOT(con2, con1, md, P.ACC_XOT);
		all = ssxot.runE(predata, Util.concat(all, new Block[] { B_b }), timer);

		// step 7
		all = Util.permute(all, predata.acc_rho_ivs);

		if (levelIndex == 0) {
			Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
			Block[] fresh = Arrays.copyOfRange(all, stash.length + 1, all.length);

			// step 8
			level.setStash(Level.toList(Util.concat(stash, new Block[] { predata.acc_A_b })));
			level.setFresh(Level.toArray64(fresh));
		} else {
			all[all.length - 1] = predata.acc_A_b;
			level.setStash(Level.toList(all));
		}

		timer.stop(pid, M.online_comp);
		return p;
	}

	public long runD(PreData predata, byte[] N, Level level, long p, Timer timer) {
		timer.start(pid, M.online_comp);

		int levelIndex = predata.getIndex();
		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = Util.padArray(Util.getSubBits(bigN, tau + lBits, tau).toByteArray(), md.getLBytes(levelIndex));
		byte[] sufN = Util.padArray(Util.getSubBits(bigN, tau, 0).toByteArray(), (md.getTau() + 7) / 8);

		// step 1
		Block B_a = level.getFreshBlock(p);

		Block[] all = null;
		if (levelIndex == 0)
			all = Util.permute(Util.concat(Level.toArray(level.getStash()), Level.toArray(level.getFresh())),
					predata.acc_rho);
		else
			all = Util.permute(Util.concat(Level.toArray(level.getStash()), new Block[] { B_a }), predata.acc_rho);
		byte[][] e = new byte[all.length][];
		for (int i = 0; i < all.length; i++) {
			e[i] = Util.xor(
					Util.padArray(Util.getSubBits(new BigInteger(all[i].getL()), lBits, 0).toByteArray(), preN.length),
					preN);
		}

		// step 2
		B_a = B_a.xor(predata.acc_r);

		timer.start(pid, M.online_write);
		con2.write(pid, sufN);
		con2.write(pid, all);
		con2.write(pid, B_a);
		timer.stop(pid, M.online_write);

		// step 3
		SSCOT sscot = new SSCOT(con1, con2, md);
		sscot.runD(predata, e, timer);

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		OutGetPointer outgp = gp.runD(predata, timer);

		Block A_a = outgp.A;
		B_a.setF(outgp.BF);

		timer.start(pid, M.online_write);
		con1.write(pid, outgp.p);
		timer.stop(pid, M.online_write);

		// step 6
		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		all = ssxot.runC(predata, Util.concat(all, new Block[] { B_a }), timer);

		// step 7
		all = Util.permute(all, predata.acc_rho_ivs);

		if (levelIndex == 0) {
			Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
			Block[] fresh = Arrays.copyOfRange(all, stash.length + 1, all.length);

			// step 8
			level.setStash(Level.toList(Util.concat(stash, new Block[] { A_a })));
			level.setFresh(Level.toArray64(fresh));
		} else {
			all[all.length - 1] = A_a;
			level.setStash(Level.toList(all));
		}

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

		// step 4, 5
		GetPointer gp = new GetPointer(con1, con2, md);
		gp.runC(predata, sufN, A_a, B_a, timer);

		// step 6
		predata.acc_delta[outsscot.t] = all.length;

		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		ssxot.runD(predata, predata.acc_delta, timer);

		timer.stop(pid, M.online_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
		testAccMid(party, oram);
	}

	public void testAccFirst(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		byte[] N = null;
		byte[] N_a = null;
		byte[] N_b = null;
		PreData predata = null;
		PreAccess preacc = null;
		int levelIndex;

		for (int j = 0; j < md.getPeriod(); j++) {
			preacc = new PreAccess(con1, con2, md);

			if (party == Party.Eddie) {
				levelIndex = 0;
				con1.write(levelIndex);
				con2.write(levelIndex);
				Level level = oram.getLevel(levelIndex);
				con2.write(level.getStash().size());
				predata = new PreData(levelIndex);
				preacc.runE(predata, (int) level.getFresh().size(), level.getStash().size(), timer);

				N_a = Util.nextBytes((md.getLBits(1) + 7) / 8, Crypto.sr);
				N = Util.padArray(BigInteger.valueOf(j).shiftLeft(md.getTau())
						.xor(new BigInteger(md.getTau(), Crypto.sr)).toByteArray(), N_a.length);
				N_b = Util.xor(N, N_a);
				con1.write(N_a);
				long p = this.runE(predata, N_b, level, 0, timer);

				int tau = md.getTau();
				int lBits = md.getLBits(levelIndex);
				BigInteger bigN = new BigInteger(N);
				int preN = Util.getSubBits(bigN, tau + lBits, tau).intValue();
				int sufN = Util.getSubBits(bigN, tau, 0).intValue();

				Block changed = con1.readBlock();
				changed = changed.xor(level.getStashBlock(level.getStash().size() - 1));
				long realP = Util.getSubBits(new BigInteger(changed.getP(sufN)), md.getPBits(levelIndex), 0)
						.longValue();

				if (p == realP && (changed.getF(sufN) & 1) == 1
						&& Util.getSubBits(new BigInteger(changed.getL()), lBits, 0).intValue() == preN)
					System.out.println(j + ": AccFirst test passed");
				else {
					System.err.println(j + ": AccFirst test failed");
				}

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				Level level = oram.getLevel(levelIndex);
				predata = new PreData(levelIndex);
				preacc.runD(predata, (int) level.getFresh().size(), level.getStash().size(), timer);

				N_a = con1.read();
				this.runD(predata, N_a, level, 0, timer);

				con1.write(level.getStashBlock(level.getStash().size() - 1));

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				int s = con1.readInt();
				predata = new PreData(levelIndex);
				preacc.runC(predata, (int) md.getNumBlocks(levelIndex) - s, s, timer);

				this.runC(predata, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}

	public void testAccMid(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		byte[] N = null;
		byte[] N_a = null;
		byte[] N_b = null;
		PreData predata = null;
		PreAccess preacc = null;
		Level level = null;

		for (int j = 0; j < md.getPeriod(); j++) {
			preacc = new PreAccess(con1, con2, md);
			long p = 0;
			int levelIndex = 0;

			if (party == Party.Eddie) {
				BigInteger addr = BigInteger.valueOf(j);

				for (levelIndex = 0; levelIndex < md.getNumLevels() - 1; levelIndex++) {
					level = oram.getLevel(levelIndex);
					predata = new PreData(levelIndex);
					preacc.runE(predata, levelIndex == 0 ? (int) level.getFresh().size() : 1, level.getStash().size(),
							timer);

					N_a = Util.nextBytes((md.getLBits(levelIndex + 1) + 7) / 8, Crypto.sr);
					N = Util.padArray(
							Util.getSubBits(addr, md.getAddrBits(), md.getAddrBits() - md.getLBits(levelIndex + 1))
									.toByteArray(),
							N_a.length);
					N_b = Util.xor(N, N_a);
					con1.write(N_a);
					p = this.runE(predata, N_b, level, p, timer);
				}

				Block target = oram.getLevel(levelIndex).getFreshBlock(p).xor(con1.readBlock());

				if (new BigInteger(1, target.getRec()).compareTo(addr) == 0)
					System.out.println(j + ": Acc test passed");
				else {
					System.err.println(j + ": Acc test failed");
				}

			} else if (party == Party.Debbie) {
				for (levelIndex = 0; levelIndex < md.getNumLevels() - 1; levelIndex++) {
					level = oram.getLevel(levelIndex);
					predata = new PreData(levelIndex);
					preacc.runD(predata, levelIndex == 0 ? (int) level.getFresh().size() : 1, level.getStash().size(),
							timer);

					N_a = con1.read();
					p = this.runD(predata, N_a, level, p, timer);
				}

				con1.write(oram.getLevel(levelIndex).getFreshBlock(p));

			} else if (party == Party.Charlie) {
				for (levelIndex = 0; levelIndex < md.getNumLevels() - 1; levelIndex++) {
					predata = new PreData(levelIndex);
					preacc.runC(predata, levelIndex == 0 ? (int) md.getNumBlocks(0) - j : 1, j, timer);

					this.runC(predata, timer);
				}

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
