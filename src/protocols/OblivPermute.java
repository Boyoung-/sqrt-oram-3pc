package protocols;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreOblivPermute;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class OblivPermute extends Protocol {

	private int pid;
	private int onoff;

	// only for testing
	public OblivPermute(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
		pid = P.INIT_OP_ON;
		onoff = 0;
	}

	public OblivPermute(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
		this.onoff = (pid == P.INIT_OP_ON) ? 0 : 3;
	}

	public Array64<Block> runE(PreData predata, Array64<Long> pi_E, Array64<Block> x_b, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, P.IPM_OP_XOT);
		Array64<Block> b = ssxot.runE(predata, Util.permute(x_b, pi_E), timer);

		// step 4
		Array64<Block> x_prime_b = Util.xorBlockArray64(b, predata.op_e);

		timer.stop(pid, M.online_comp + onoff);
		return x_prime_b;
	}

	public Array64<byte[]> runInitE(PreData predata, Array64<Long> pi_E, Array64<byte[]> x_b, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, pid == P.INIT_OP_OFF ? P.INIT_OP_XOT_OFF : P.INIT_OP_XOT_ON);
		Array64<byte[]> b = ssxot.runInitE(predata, Util.permute(x_b, pi_E), timer);

		// step 4
		Array64<byte[]> x_prime_b = Util.xorByteArray64(b, predata.initop_e);

		timer.stop(pid, M.online_comp + onoff);
		return x_prime_b;
	}

	public Array64<Block> runD(PreData predata, Array64<Long> pi_D, Array64<Block> x_a, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_write + onoff);
		con2.writeBlockArray64(pid, x_a);
		timer.stop(pid, M.online_write + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, P.IPM_OP_XOT);
		ssxot.runD(predata, Util.inversePermutationLong(pi_D), timer);

		// step 3
		timer.start(pid, M.online_read + onoff);
		Array64<Block> x_prime_a = con2.readBlockArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
		return x_prime_a;
	}

	public Array64<byte[]> runInitD(PreData predata, Array64<Long> pi_D, Array64<byte[]> x_a, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_write + onoff);
		con2.writeByteArray64(pid, x_a);
		timer.stop(pid, M.online_write + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, pid == P.INIT_OP_OFF ? P.INIT_OP_XOT_OFF : P.INIT_OP_XOT_ON);
		ssxot.runInitD(predata, Util.inversePermutationLong(pi_D), timer);

		// step 3
		timer.start(pid, M.online_read + onoff);
		Array64<byte[]> x_prime_a = con2.readByteArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
		return x_prime_a;
	}

	public void runC(PreData predata, Array64<Long> pi_E, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_read + onoff);
		Array64<Block> x_a = con2.readBlockArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, P.IPM_OP_XOT);
		Array64<Block> a = ssxot.runC(predata, Util.permute(x_a, pi_E), timer);

		// step 3
		Array64<Block> x_prime_a = Util.xorBlockArray64(a, predata.op_e);

		timer.start(pid, M.online_write + onoff);
		con2.writeBlockArray64(pid, x_prime_a);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runInitC(PreData predata, Array64<Long> pi_E, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 1
		timer.start(pid, M.online_read + onoff);
		Array64<byte[]> x_a = con2.readByteArray64(pid);
		timer.stop(pid, M.online_read + onoff);

		// step 2
		SSXOT ssxot = new SSXOT(con1, con2, md, pid == P.INIT_OP_OFF ? P.INIT_OP_XOT_OFF : P.INIT_OP_XOT_ON);
		Array64<byte[]> a = ssxot.runInitC(predata, Util.permute(x_a, pi_E), timer);

		// step 3
		Array64<byte[]> x_prime_a = Util.xorByteArray64(a, predata.initop_e);

		timer.start(pid, M.online_write + onoff);
		con2.writeByteArray64(pid, x_prime_a);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		long s = 200;
		Array64<Long> pi_D = null;
		Array64<Long> pi_E = null;
		PreData predata = null;
		PreOblivPermute preop = null;
		int levelIndex;

		pid = P.IPM_OP;
		for (int j = 0; j < 100; j++) {
			preop = new PreOblivPermute(con1, con2, md, pid);

			if (party == Party.Eddie) {
				levelIndex = Crypto.sr.nextInt(md.getNumLevels());
				con1.write(levelIndex);
				con2.write(levelIndex);
				predata = new PreData(levelIndex);
				preop.runE(predata, s, timer);

				pi_E = Util.randomPermutationLong(s, Crypto.sr);
				Array64<Block> x = new Array64<Block>(s);
				Array64<Block> x_a = new Array64<Block>(s);
				for (long i = 0; i < s; i++) {
					x.set(i, new Block(levelIndex, md, Crypto.sr));
					x_a.set(i, new Block(levelIndex, md, Crypto.sr));
				}
				Array64<Block> x_b = Util.xorBlockArray64(x, x_a);
				con2.writeLongArray64(pi_E);
				con1.writeBlockArray64(x_a);
				Array64<Block> x_prime_b = runE(predata, pi_E, x_b, timer);

				pi_D = con1.readLongArray64();
				Array64<Block> x_prime_a = con1.readBlockArray64();
				Array64<Block> x_prime = Util.xorBlockArray64(x_prime_a, x_prime_b);
				Array64<Block> perm = Util.permute(Util.permute(x, pi_E), pi_D);

				boolean pass = true;
				for (long i = 0; i < s; i++) {
					if (!perm.get(i).equals(x_prime.get(i))) {
						System.err.println(j + " " + i + ": IPM_OP test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": IPM_OP test passed");

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preop.runD(predata, s, timer);

				pi_D = Util.randomPermutationLong(s, Crypto.sr);
				Array64<Block> x_a = con1.readBlockArray64();
				Array64<Block> x_prime_a = runD(predata, pi_D, x_a, timer);

				con1.writeLongArray64(pi_D);
				con1.writeBlockArray64(x_prime_a);

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preop.runC(predata, timer);

				pi_E = con1.readLongArray64();
				runC(predata, pi_E, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		pid = P.INIT_OP_ON;
		for (int j = 100; j < 200; j++) {
			preop = new PreOblivPermute(con1, con2, md, pid);

			if (party == Party.Eddie) {
				levelIndex = Crypto.sr.nextInt(md.getNumLevels());
				int bytes = pid == P.INIT_OP_OFF ? md.getLBytes(levelIndex) : md.getDBytes();
				con1.write(levelIndex);
				con2.write(levelIndex);
				predata = new PreData(levelIndex);
				preop.runE(predata, s, timer);

				pi_E = Util.randomPermutationLong(s, Crypto.sr);
				Array64<byte[]> x = new Array64<byte[]>(s);
				Array64<byte[]> x_a = new Array64<byte[]>(s);
				for (long i = 0; i < s; i++) {
					x.set(i, Util.nextBytes(bytes, Crypto.sr));
					x_a.set(i, Util.nextBytes(bytes, Crypto.sr));
				}
				Array64<byte[]> x_b = Util.xorByteArray64(x, x_a);
				con2.writeLongArray64(pi_E);
				con1.writeByteArray64(x_a);
				Array64<byte[]> x_prime_b = runInitE(predata, pi_E, x_b, timer);

				pi_D = con1.readLongArray64();
				Array64<byte[]> x_prime_a = con1.readByteArray64();
				Array64<byte[]> x_prime = Util.xorByteArray64(x_prime_a, x_prime_b);
				Array64<byte[]> perm = Util.permute(Util.permute(x, pi_E), pi_D);

				boolean pass = true;
				for (long i = 0; i < s; i++) {
					if (!Util.equal(perm.get(i), x_prime.get(i))) {
						System.err.println(j + " " + i + ": " + P.names[pid] + " test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": " + P.names[pid] + " test passed");

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preop.runD(predata, s, timer);

				pi_D = Util.randomPermutationLong(s, Crypto.sr);
				Array64<byte[]> x_a = con1.readByteArray64();
				Array64<byte[]> x_prime_a = runInitD(predata, pi_D, x_a, timer);

				con1.writeLongArray64(pi_D);
				con1.writeByteArray64(x_prime_a);

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				preop.runC(predata, timer);

				pi_E = con1.readLongArray64();
				runInitC(predata, pi_E, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
