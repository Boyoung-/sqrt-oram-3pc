package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Level;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreInitialize;
import protocols.struct.OutAccess;
import protocols.struct.OutGetPointer;
import protocols.struct.OutSSCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

// TODO: think about Util.rmSignBit
public class Initialize extends Protocol {

	private int pid = P.INIT;

	public Initialize(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public OutAccess runE(PreData predata, byte[] N, Level level, long p, Timer timer) {
		timer.start(pid, M.online_comp);

		OutAccess outacc = new OutAccess(0, null);

		int levelIndex = predata.getIndex();
		boolean lastLevel = (levelIndex == md.getNumLevels() - 1);

		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = null;
		byte[] sufN = null;
		if (!lastLevel) {
			preN = Util.padArray(Util.getSubBits(bigN, tau + lBits, tau).toByteArray(), md.getLBytes(levelIndex));
			sufN = Util.padArray(Util.getSubBits(bigN, tau, 0).toByteArray(), (md.getTau() + 7) / 8);
		} else
			preN = N;

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
		if (!lastLevel) {
			GetPointer gp = new GetPointer(con1, con2, md);
			gp.runE(predata, sufN, predata.acc_A_b, B_b, timer);

			predata.acc_A_b = predata.gp_A_prime;
			B_b.setF(predata.gp_BF_prime);

			timer.start(pid, M.online_read);
			outacc.p = con1.readLong(pid);
			timer.stop(pid, M.online_read);
		}

		// step 6
		SSXOT ssxot = new SSXOT(con2, con1, md, P.ACC_XOT);
		all = ssxot.runE(predata, Util.concat(all, new Block[] { !lastLevel ? B_b : predata.acc_A_b }), timer);

		// step 7
		if (!lastLevel)
			all = Util.permute(all, predata.acc_rho_ivs);

		if (levelIndex == 0) {
			Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
			Block[] fresh = Arrays.copyOfRange(all, stash.length + 1, all.length);

			// step 8
			level.setStash(Level.toList(Util.concat(stash, new Block[] { predata.acc_A_b })));
			level.setFresh(Level.toArray64(fresh));
		} else if (!lastLevel) {
			all[all.length - 1] = predata.acc_A_b;
			level.setStash(Level.toList(all));
		} else {
			level.setStash(Level.toList(all));
			outacc.rec = predata.acc_A_b.getRec();
		}

		timer.stop(pid, M.online_comp);
		return outacc;
	}

	public OutAccess runD(PreData predata, byte[] N, Level level, long p, Timer timer) {
		timer.start(pid, M.online_comp);

		OutAccess outacc = new OutAccess(0, null);

		int levelIndex = predata.getIndex();
		boolean lastLevel = (levelIndex == md.getNumLevels() - 1);

		int tau = md.getTau();
		int lBits = md.getLBits(predata.getIndex());
		BigInteger bigN = new BigInteger(N);
		byte[] preN = null;
		byte[] sufN = null;
		if (!lastLevel) {
			preN = Util.padArray(Util.getSubBits(bigN, tau + lBits, tau).toByteArray(), md.getLBytes(levelIndex));
			sufN = Util.padArray(Util.getSubBits(bigN, tau, 0).toByteArray(), (md.getTau() + 7) / 8);
		} else
			preN = N;

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
		if (!lastLevel)
			con2.write(pid, sufN);
		con2.write(pid, all);
		con2.write(pid, B_a);
		timer.stop(pid, M.online_write);

		// step 3
		SSCOT sscot = new SSCOT(con1, con2, md);
		sscot.runD(predata, e, timer);

		// step 4, 5
		Block A_a = null;
		OutGetPointer outgp = null;
		if (!lastLevel) {
			GetPointer gp = new GetPointer(con1, con2, md);
			outgp = gp.runD(predata, timer);

			A_a = outgp.A;
			B_a.setF(outgp.BF);
			outacc.p = outgp.p;

			timer.start(pid, M.online_write);
			con1.write(pid, outgp.p);
			timer.stop(pid, M.online_write);
		} else {
			timer.start(pid, M.online_read);
			A_a = con2.readBlock(pid);
			timer.stop(pid, M.online_read);
		}

		// step 6
		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		all = ssxot.runC(predata, Util.concat(all, new Block[] { !lastLevel ? B_a : A_a }), timer);

		// step 7
		if (!lastLevel)
			all = Util.permute(all, predata.acc_rho_ivs);

		if (levelIndex == 0) {
			Block[] stash = Arrays.copyOfRange(all, 0, level.getStash().size());
			Block[] fresh = Arrays.copyOfRange(all, stash.length + 1, all.length);

			// step 8
			level.setStash(Level.toList(Util.concat(stash, new Block[] { A_a })));
			level.setFresh(Level.toArray64(fresh));
		} else if (!lastLevel) {
			all[all.length - 1] = A_a;
			level.setStash(Level.toList(all));
		} else {
			level.setStash(Level.toList(all));
		}

		timer.stop(pid, M.online_comp);
		return outacc;
	}

	public byte[] runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		byte[] sufN = null;

		timer.start(pid, M.online_read);
		if (predata.getIndex() < md.getNumLevels() - 1)
			sufN = con2.read(pid);
		Block[] all = con2.readBlockArray(pid);
		Block B_a = con2.readBlock(pid);
		timer.stop(pid, M.online_read);

		// step 3
		SSCOT sscot = new SSCOT(con1, con2, md);
		OutSSCOT outsscot = sscot.runC(timer);

		Block A_a = new Block(predata.getIndex(), md, outsscot.m_t).xor(all[outsscot.t]);

		// step 4, 5
		if (predata.getIndex() < md.getNumLevels() - 1) {
			GetPointer gp = new GetPointer(con1, con2, md);
			gp.runC(predata, sufN, A_a, B_a, timer);
		} else {
			timer.start(pid, M.online_write);
			con2.write(pid, A_a); // TODO: add a mask
			timer.stop(pid, M.online_write);
		}

		// step 6
		predata.acc_delta[outsscot.t] = all.length;

		SSXOT ssxot = new SSXOT(con1, con2, md, P.ACC_XOT);
		ssxot.runD(predata, predata.acc_delta, timer);

		timer.stop(pid, M.online_comp);
		return A_a.getRec();
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

				if (true)
					System.out.println(j + ": Init test passed");
				else
					System.err.println(j + ": Init test failed");

			} else if (party == Party.Debbie) {
				preinit.runD(predata, oram, timer);

			} else if (party == Party.Charlie) {
				preinit.runC(predata, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
