package protocols;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import oramOLD.Forest;
import oramOLD.Metadata;
import protocols.precomputation.PrePermuteTarget;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PermuteTarget extends Protocol {

	private int pid = P.PT;

	public PermuteTarget(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE() {
	}

	public int[] runD(PreData predata, boolean firstTree, GCSignal[][] targetOutKeys, Timer timer) {
		if (firstTree)
			return null;

		timer.start(pid, M.online_comp);

		// PermuteTargetI
		int d = targetOutKeys.length;
		int logD = (int) Math.ceil(Math.log(d) / Math.log(2));
		int I[] = new int[d];
		byte[][] target = new byte[d][];

		for (int i = 0; i < d; i++) {
			byte[] hashKeys = GCUtil.hashAll(targetOutKeys[i]);
			for (int j = 0; j < d; j++) {
				if (Util.equal(hashKeys, predata.pt_keyT[i][j])) {
					I[i] = j;
					target[i] = predata.pt_targetT[i][j];
					break;
				}
			}
		}

		// PermuteTargetII
		byte[][] z = Util.xor(target, predata.pt_p);

		timer.start(pid, M.online_write);
		con2.write(pid, z);
		con2.write(pid, I);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[][] g = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		target = Util.xor(predata.pt_a, g);

		int[] target_pp = new int[d];
		for (int i = 0; i < d; i++)
			target_pp[i] = Util.getSubBits(new BigInteger(target[i]), logD, 0).intValue();

		timer.stop(pid, M.online_comp);
		return target_pp;
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.online_comp);

		// PermuteTargetII
		timer.start(pid, M.online_read);
		byte[][] z = con2.readDoubleByteArray(pid);
		int[] I = con2.readIntArray(pid);
		timer.stop(pid, M.online_read);

		byte[][] mk = new byte[z.length][];
		for (int i = 0; i < mk.length; i++) {
			mk[i] = Util.xor(predata.pt_maskT[i][I[i]], z[i]);
			mk[i] = Util.xor(predata.pt_r[i], mk[i]);
		}
		byte[][] g = Util.permute(mk, predata.evict_pi);

		timer.start(pid, M.online_write);
		con2.write(pid, g);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 100; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PrePermuteTarget prepermutetarget = new PrePermuteTarget(con1, con2);

			if (party == Party.Eddie) {
				int d = Crypto.sr.nextInt(20) + 5;
				int logD = (int) Math.ceil(Math.log(d) / Math.log(2));
				int[] target = Util.randomPermutation(d, Crypto.sr);

				predata.evict_pi = Util.randomPermutation(d, Crypto.sr);
				predata.evict_targetOutKeyPairs = new GCSignal[d][][];
				GCSignal[][] targetOutKeys = new GCSignal[d][];
				for (int j = 0; j < d; j++) {
					predata.evict_targetOutKeyPairs[j] = GCUtil.genKeyPairs(logD);
					targetOutKeys[j] = GCUtil.revSelectKeys(predata.evict_targetOutKeyPairs[j],
							BigInteger.valueOf(target[j]).toByteArray());
				}

				con1.write(d);
				con1.write(predata.evict_pi);
				con1.write(predata.evict_targetOutKeyPairs);
				con1.write(targetOutKeys);

				con2.write(predata.evict_pi);

				prepermutetarget.runE(predata, d, timer);

				runE();

				int[] target_pp = con1.readIntArray();
				int[] pi_ivs = Util.inversePermutation(predata.evict_pi);
				int[] piTargetPiIvs = new int[d];

				int j = 0;
				for (; j < d; j++) {
					piTargetPiIvs[j] = predata.evict_pi[target[pi_ivs[j]]];
					if (piTargetPiIvs[j] != target_pp[j]) {
						System.err.println("PermuteTarget test failed");
						break;
					}
				}
				if (j == d)
					System.out.println("PermuteTarget test passed");

			} else if (party == Party.Debbie) {
				int d = con1.readInt();
				predata.evict_pi = con1.readIntArray();
				predata.evict_targetOutKeyPairs = con1.readTripleGCSignalArray();
				GCSignal[][] targetOutKeys = con1.readDoubleGCSignalArray();

				prepermutetarget.runD(predata, d, timer);

				int[] target_pp = runD(predata, false, targetOutKeys, timer);
				con1.write(target_pp);

			} else if (party == Party.Charlie) {
				predata.evict_pi = con1.readIntArray();

				prepermutetarget.runC(predata, timer);

				runC(predata, false, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}