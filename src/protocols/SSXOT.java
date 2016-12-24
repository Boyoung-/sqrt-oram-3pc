package protocols;

import java.math.BigInteger;
import java.util.Random;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreSSXOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class SSXOT extends Protocol {

	private int pid;

	// for testing
	public SSXOT(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
		this.pid = P.OP_XOT_ON;
	}

	public SSXOT(Communication con1, Communication con2, Metadata md, int pid) {
		super(con1, con2, md);
		this.pid = pid;
	}

	public Array64<Block> runE(PreData predata, Array64<Block> m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Array64<Block> a = predata.ssxot_E_r[pid];
		for (long i = 0; i < m.size(); i++)
			a.get(i).setXor(m.get(predata.ssxot_E_pi[pid].get(i)));

		timer.start(pid, M.online_write);
		con2.writeBlockArray64(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con2.readBlockArray64(pid);

		// step 2
		Array64<Long> j = con1.readLongArray64(pid);
		Array64<Block> p = con1.readBlockArray64(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Array64<Block> z = p;
		for (long i = 0; i < j.size(); i++)
			z.get(i).setXor(a.get(j.get(i)));

		timer.stop(pid, M.online_comp);
		return z;
	}
	
	public Block[] runE(PreData predata, Block[] m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Block[] a = predata.accxot_E_r;
		for (int i = 0; i < m.length; i++)
			a[i].setXor(m[predata.accxot_E_pi[i]]);

		timer.start(pid, M.online_write);
		con2.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con2.readBlockArray(pid);

		// step 2
		int[] j = con1.readIntArray(pid);
		Block[] p = con1.readBlockArray(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Block[] z = p;
		for (int i = 0; i < j.length; i++)
			z[i].setXor(a[j[i]]);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public void runD(PreData predata, Array64<Long> index, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		long k = index.size();
		Array64<Long> E_j = new Array64<Long>(k);
		Array64<Long> C_j = new Array64<Long>(k);
		Array64<Block> E_p = new Array64<Block>(k);
		Array64<Block> C_p = new Array64<Block>(k);
		for (long i = 0; i < k; i++) {
			E_j.set(i, predata.ssxot_E_pi_ivs[pid].get(index.get(i)));
			C_j.set(i, predata.ssxot_C_pi_ivs[pid].get(index.get(i)));
			E_p.set(i, predata.ssxot_E_r[pid].get(E_j.get(i)).xor(predata.ssxot_delta[pid].get(i)));
			C_p.set(i, predata.ssxot_C_r[pid].get(C_j.get(i)).xor(predata.ssxot_delta[pid].get(i)));
		}

		timer.start(pid, M.online_write);
		con2.writeLongArray64(pid, E_j);
		con2.writeBlockArray64(pid, E_p);
		con1.writeLongArray64(pid, C_j);
		con1.writeBlockArray64(pid, C_p);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}
	
	public void runD(PreData predata, int[] index, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		int k = index.length;
		int[] E_j = new int[k];
		int[] C_j = new int[k];
		Block[] E_p = new Block[k];
		Block[] C_p = new Block[k];
		for (int i = 0; i < k; i++) {
			E_j[i] = predata.accxot_E_pi_ivs[index[i]];
			C_j[i] = predata.accxot_C_pi_ivs[index[i]];
			E_p[i] = predata.accxot_E_r[E_j[i]].xor(predata.accxot_delta[i]);
			C_p[i] = predata.accxot_C_r[C_j[i]].xor(predata.accxot_delta[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, E_j);
		con2.write(pid, E_p);
		con1.write(pid, C_j);
		con1.write(pid, C_p);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public Array64<Block> runC(PreData predata, Array64<Block> m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Array64<Block> a = predata.ssxot_C_r[pid];
		for (long i = 0; i < m.size(); i++)
			a.get(i).setXor(m.get(predata.ssxot_C_pi[pid].get(i)));

		timer.start(pid, M.online_write);
		con1.writeBlockArray64(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con1.readBlockArray64(pid);

		// step 2
		Array64<Long> j = con2.readLongArray64(pid);
		Array64<Block> p = con2.readBlockArray64(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Array64<Block> z = p;
		for (long i = 0; i < j.size(); i++)
			z.get(i).setXor(a.get(j.get(i)));

		timer.stop(pid, M.online_comp);
		return z;
	}
	
	public Block[] runC(PreData predata, Block[] m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Block[] a = predata.accxot_C_r;
		for (int i = 0; i < m.length; i++)
			a[i].setXor(m[predata.accxot_C_pi[i]]);

		timer.start(pid, M.online_write);
		con1.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con1.readBlockArray(pid);

		// step 2
		int[] j = con2.readIntArray(pid);
		Block[] p = con2.readBlockArray(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Block[] z = p;
		for (int i = 0; i < j.length; i++)
			z[i].setXor(a[j[i]]);

		timer.stop(pid, M.online_comp);
		return z;
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();

		for (int j = 0; j < 100; j++) {
			long n = 200;
			long k = Crypto.sr.nextInt(50) + 50;
			Array64<Block> E_m = new Array64<Block>(n);
			Array64<Block> C_m = new Array64<Block>(n);
			int levelNum = Crypto.sr.nextInt(md.getNumLevels());
			for (long i = 0; i < n; i++) {
				C_m.set(i, new Block(levelNum, md, Crypto.sr));
				E_m.set(i, new Block(levelNum, md, (Random) null));
			}

			if (party == Party.Eddie) {
				con1.write(levelNum);
				con2.write(levelNum);

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runE(predata, timer);

				con2.writeBlockArray64(C_m);

				Array64<Block> E_out_m = runE(predata, E_m, timer);

				con2.writeBlockArray64(E_out_m);

			} else if (party == Party.Debbie) {
				levelNum = con1.readInt();

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runD(predata, n, k, timer);

				Array64<Long> index = Util.randomPermutationLong(k, Crypto.sr);
				runD(predata, index, timer);

				con2.writeLongArray64(index);

			} else if (party == Party.Charlie) {
				levelNum = con1.readInt();

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runC(predata, timer);

				C_m = con1.readBlockArray64();

				Array64<Block> C_out_m = runC(predata, C_m, timer);

				Array64<Long> index = con2.readLongArray64();
				Array64<Block> E_out_m = con1.readBlockArray64();

				boolean pass = true;
				for (long i = 0; i < index.size(); i++) {
					BigInteger input = new BigInteger(C_m.get(index.get(i)).toByteArray());
					BigInteger output = new BigInteger(E_out_m.get(i).xor(C_out_m.get(i)).toByteArray());
					if (input.compareTo(output) != 0) {
						System.err.println(j + " " + i + ": SSXOT test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": SSXOT test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
		
		pid = P.ACC_XOT;
		for (int j = 100; j < 200; j++) {
			int n = 200;
			int k = Crypto.sr.nextInt(50) + 50;
			Block[] E_m = new Block[n];
			Block[] C_m = new Block[n];
			int levelNum = Crypto.sr.nextInt(md.getNumLevels());
			for (int i = 0; i < n; i++) {
				C_m[i] = new Block(levelNum, md, Crypto.sr);
				E_m[i] = new Block(levelNum, md, (Random) null);
			}

			if (party == Party.Eddie) {
				con1.write(levelNum);
				con2.write(levelNum);

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runE(predata, timer);

				con2.write(C_m);

				Block[] E_out_m = runE(predata, E_m, timer);

				con2.write(E_out_m);

			} else if (party == Party.Debbie) {
				levelNum = con1.readInt();

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runD(predata, n, k, timer);

				int[] index = Util.randomPermutation(k, Crypto.sr);
				runD(predata, index, timer);

				con2.write(index);

			} else if (party == Party.Charlie) {
				levelNum = con1.readInt();

				PreData predata = new PreData(levelNum);
				PreSSXOT pressxot = new PreSSXOT(con1, con2, md, pid);
				pressxot.runC(predata, timer);

				C_m = con1.readBlockArray();

				Block[] C_out_m = runC(predata, C_m, timer);

				int[] index = con2.readIntArray();
				Block[] E_out_m = con1.readBlockArray();

				boolean pass = true;
				for (int i = 0; i < index.length; i++) {
					BigInteger input = new BigInteger(C_m[index[i]].toByteArray());
					BigInteger output = new BigInteger(E_out_m[i].xor(C_out_m[i]).toByteArray());
					if (input.compareTo(output) != 0) {
						System.err.println(j + " " + i + ": SSXOT test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println(j + ": SSXOT test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
