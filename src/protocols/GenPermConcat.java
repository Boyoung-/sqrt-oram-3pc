package protocols;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreGenPermConcat;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class GenPermConcat extends Protocol {

	private int pid = P.GPC;
	private int onoff = 3;

	public GenPermConcat(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public Array64<Long> runE(PreData predata, Array64<Long> pi_b, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		Array64<Long> a1 = Util.xor(Util.permute(pi_b, predata.gpc_sig2), predata.gpc_r2);

		timer.start(pid, M.online_write + onoff);
		con2.writeLongArray64(pid, a1);
		timer.stop(pid, M.online_write + onoff);

		// step 2 & 3
		timer.start(pid, M.online_read + onoff);
		Array64<Long> z1 = con2.readLongArray64(pid);
		Array64<Long> z2 = con1.readLongArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		Array64<Long> pi_E = Util.inversePermutationLong(Util.xor(z1, z2));

		timer.stop(pid, M.online_comp + onoff);
		return pi_E;
	}

	public Array64<Long> runD(PreData predata, Array64<Long> pi_a, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 3
		Array64<Long> a2 = Util.xor(Util.permute(pi_a, predata.gpc_sig1), predata.gpc_r1);
		Array64<Long> z2 = Util.xor(Util.permute(a2, predata.gpc_gam2), predata.gpc_t2);

		timer.start(pid, M.online_write + onoff);
		con1.writeLongArray64(pid, z2);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
		return predata.gpc_pi_D;
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_read + onoff);
		Array64<Long> a1 = con1.readLongArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 2
		Array64<Long> z1 = Util.xor(Util.permute(a1, predata.gpc_gam1), predata.gpc_t1);

		timer.start(pid, M.online_write + onoff);
		con1.writeLongArray64(pid, z1);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		long v = 200;
		Array64<Long> pi = null;
		Array64<Long> pi_ivs = null;
		Array64<Long> pi_D = null;
		Array64<Long> pi_E = null;
		Array64<Long> pi_a = null;
		Array64<Long> pi_b = null;
		PreData predata = null;
		PreGenPermConcat pregpc = null;

		for (int j = 0; j < 100; j++) {
			pregpc = new PreGenPermConcat(con1, con2, md);

			if (party == Party.Eddie) {
				predata = new PreData(0);
				pregpc.runE(predata, timer);

				pi = Util.randomPermutationLong(v, Crypto.sr);
				pi_a = new Array64<Long>(v);
				for (long i = 0; i < v; i++)
					pi_a.set(i, Crypto.sr.nextLong());
				pi_b = Util.xor(pi, pi_a);
				con1.writeLongArray64(pi_a);
				pi_E = runE(predata, pi_b, timer);

				pi_D = con1.readLongArray64();
				Array64<Long> concat = Util.permute(pi_D, Util.inversePermutationLong(pi_E));
				pi_ivs = Util.inversePermutationLong(pi);

				boolean pass = true;
				for (long i = 0; i < v; i++) {
					if (concat.get(i).compareTo(pi_ivs.get(i)) != 0) {
						System.err.println(j + " " + i + ": GPC test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": GPC test passed");

			} else if (party == Party.Debbie) {
				predata = new PreData(0);
				pregpc.runD(predata, v, timer);

				pi_a = con1.readLongArray64();
				pi_D = runD(predata, pi_a, timer);

				con1.writeLongArray64(pi_D);

			} else if (party == Party.Charlie) {
				predata = new PreData(0);
				pregpc.runC(predata, timer);

				runC(predata, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
