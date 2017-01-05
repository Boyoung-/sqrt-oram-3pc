package protocols;

import java.math.BigInteger;
import java.util.Random;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreInitPosMap;
import protocols.struct.OutInitPosMap;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class InitPosMap extends Protocol {

	private int pid = P.IPM;
	private int onoff = 3;

	public InitPosMap(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public OutInitPosMap runE(PreData predata, Array64<Long> pi_E, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		GenPermShare gps = new GenPermShare(con1, con2, md);
		Array64<Long> pi_b = gps.runE(predata, pi_E, timer);

		// step 2-7
		int levelIndex = predata.getIndex();
		long n = md.getNumBlocks(levelIndex);
		Array64<Block> fresh = new Array64<Block>(n);
		for (long j = 0; j < n; j++) {
			fresh.set(j, new Block(levelIndex, md, (Random) null));
			fresh.get(j).setL(Util.rmSignBit(BigInteger.valueOf(j).toByteArray()));
			for (int k = 0; k < md.getTwoTauPow(); k++) {
				fresh.get(j).setP(k, Util.rmSignBit(Util
						.getSubBits(BigInteger.valueOf(pi_b.get(j * md.getTwoTauPow() + k)), md.getPBits(levelIndex), 0)
						.toByteArray()));
			}
		}

		// step 8-10
		if (levelIndex > 0) {
			OblivPermute op = new OblivPermute(con1, con2, md, P.IPM_OP);
			fresh = op.runE(predata, predata.ipm_pi_prime_E, fresh, timer);
		}

		OutInitPosMap outipm = new OutInitPosMap(null, fresh, null, predata.ipm_pi_prime_E);

		timer.stop(pid, M.online_comp + onoff);
		return outipm;
	}

	public OutInitPosMap runD(PreData predata, Array64<Long> pi_D, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		GenPermShare gps = new GenPermShare(con1, con2, md);
		Array64<Long> pi_a = gps.runD(predata, pi_D, timer);

		// step 2-7
		int levelIndex = predata.getIndex();
		long n = md.getNumBlocks(levelIndex);
		Array64<Block> fresh = new Array64<Block>(n);
		for (long j = 0; j < n; j++) {
			fresh.set(j, new Block(levelIndex, md, (Random) null));
			for (int k = 0; k < md.getTwoTauPow(); k++) {
				fresh.get(j).setP(k, Util.rmSignBit(Util
						.getSubBits(BigInteger.valueOf(pi_a.get(j * md.getTwoTauPow() + k)), md.getPBits(levelIndex), 0)
						.toByteArray()));
			}
		}

		// step 8-10
		if (levelIndex > 0) {
			OblivPermute op = new OblivPermute(con1, con2, md, P.IPM_OP);
			fresh = op.runD(predata, predata.ipm_pi_prime_D, fresh, timer);
		}

		OutInitPosMap outipm = new OutInitPosMap(fresh, null, predata.ipm_pi_prime_D, null);

		timer.stop(pid, M.online_comp + onoff);
		return outipm;
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		GenPermShare gps = new GenPermShare(con1, con2, md);
		gps.runC(predata, timer);

		// step 8-10
		if (predata.getIndex() > 0) {
			OblivPermute op = new OblivPermute(con1, con2, md, P.IPM_OP);
			op.runC(predata, predata.ipm_pi_prime_E, timer);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		long n;
		Array64<Long> pi_D = null;
		Array64<Long> pi_E = null;
		Array64<Block> fresh = null;
		PreData predata = null;
		PreInitPosMap preipm = null;
		int levelIndex;
		OutInitPosMap outipm = null;

		for (int j = 0; j < 100; j++) {
			preipm = new PreInitPosMap(con1, con2, md);

			if (party == Party.Eddie) {
				levelIndex = Crypto.sr.nextInt(md.getNumLevels() - 1);
				con1.write(levelIndex);
				con2.write(levelIndex);
				predata = new PreData(levelIndex);
				preipm.runE(predata, timer);

				n = md.getNumBlocks(levelIndex);
				pi_E = Util.randomPermutationLong(n * md.getTwoTauPow(), Crypto.sr);
				outipm = this.runE(predata, pi_E, timer);

				pi_D = con1.readLongArray64();
				outipm.fresh_a = con1.readBlockArray64();
				fresh = Util.xorBlockArray64(outipm.fresh_a, outipm.fresh_b);

				boolean pass = true;
				for (long i = 0; i < n; i++) {
					long index = Util.getSubBits(new BigInteger(fresh.get(i).getL()), md.getLBits(levelIndex), 0)
							.longValue();
					for (int k = 0; k < md.getTwoTauPow(); k++) {
						long p = Util.getSubBits(new BigInteger(fresh.get(i).getP(k)), md.getPBits(levelIndex), 0)
								.longValue();
						long perm = pi_D.get(pi_E.get(index * md.getTwoTauPow() + k));
						if (p != perm) {
							System.err.println(j + " " + i + ": IPM test failed");
							pass = false;
							break;
						}
					}
					if (!pass)
						break;
				}
				if (pass)
					System.out.println(j + ": IPM test passed");

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preipm.runD(predata, timer);

				n = md.getNumBlocks(levelIndex);
				pi_D = Util.randomPermutationLong(n * md.getTwoTauPow(), Crypto.sr);
				outipm = this.runD(predata, pi_D, timer);

				con1.writeLongArray64(pi_D);
				con1.writeBlockArray64(outipm.fresh_a);

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preipm.runC(predata, timer);

				this.runC(predata, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
