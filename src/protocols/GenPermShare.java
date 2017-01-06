package protocols;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreGenPermShare;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class GenPermShare extends Protocol {

	private int pid = P.GPS;
	private int onoff = 3;

	public GenPermShare(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public Array64<Long> runE(PreData predata, Array64<Long> pi_E, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_write + onoff);
		con2.writeLongArray64(pid, pi_E);
		timer.stop(pid, M.online_write + onoff);

		// step 3
		timer.start(pid, M.online_read + onoff);
		Array64<Long> z = con1.readLongArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 4
		Array64<Long> pi_ivs = Util.inversePermutationLong(pi_E);
		Array64<Long> pi_b = Util.permute(Util.xor(z, predata.gps_r), pi_ivs);

		timer.stop(pid, M.online_comp + onoff);
		return pi_b;
	}

	public Array64<Long> runD(PreData predata, Array64<Long> pi_D, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 2
		timer.start(pid, M.online_read + onoff);
		Array64<Long> pi_a = con2.readLongArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 3
		Array64<Long> z = Util.xor(pi_D, predata.gps_p);

		timer.start(pid, M.online_write + onoff);
		con1.writeLongArray64(pid, z);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
		return pi_a;
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_read + onoff);
		Array64<Long> pi_E = con1.readLongArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 2
		Array64<Long> pi_ivs = Util.inversePermutationLong(pi_E);
		Array64<Long> pi_a = Util.permute(Util.xor(predata.gps_p, predata.gps_r), pi_ivs);

		timer.start(pid, M.online_write + onoff);
		con2.writeLongArray64(pid, pi_a);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		long v = 200;
		Array64<Long> pi_D = null;
		Array64<Long> pi_E = null;
		Array64<Long> pi_a = null;
		Array64<Long> pi_b = null;
		PreData predata = null;
		PreGenPermShare pregps = null;

		for (int j = 0; j < 100; j++) {
			pregps = new PreGenPermShare(con1, con2, md);

			if (party == Party.Eddie) {
				predata = new PreData(0);
				pregps.runE(predata, timer);

				pi_E = Util.randomPermutationLong(v, Crypto.sr);
				con2.writeLongArray64(pi_E);
				pi_b = runE(predata, pi_E, timer);

				con2.writeLongArray64(pi_b);

			} else if (party == Party.Debbie) {
				predata = new PreData(0);
				pregps.runD(predata, timer);

				pi_D = Util.randomPermutationLong(v, Crypto.sr);
				pi_a = runD(predata, pi_D, timer);

				con2.writeLongArray64(pi_D);
				con2.writeLongArray64(pi_a);

			} else if (party == Party.Charlie) {
				predata = new PreData(0);
				pregps.runC(predata, v, timer);

				pi_E = con1.readLongArray64();
				runC(predata, timer);

				pi_b = con1.readLongArray64();
				pi_D = con2.readLongArray64();
				pi_a = con2.readLongArray64();

				Array64<Long> concat = Util.permute(pi_D, Util.inversePermutationLong(pi_E));
				// Array64<Long> concat = new Array64<Long>(v);
				// for (long i=0; i<v; i++)
				// concat.set(i, pi_D.get(pi_E.get(i)));
				Array64<Long> share = Util.xor(pi_a, pi_b);

				boolean pass = true;
				for (long i = 0; i < v; i++) {
					if (concat.get(i).compareTo(share.get(i)) != 0) {
						System.err.println(j + " " + i + ": GPS test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": GPS test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
