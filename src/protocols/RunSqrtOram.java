package protocols;

import java.math.BigInteger;
import java.util.List;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Block;
import oram.Level;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreInitialize;
import protocols.precomputation.PreRunSqrtOram;
import protocols.struct.OutAccess;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Array64;
import util.Bandwidth;
import util.M;
import util.P;
import util.StopWatch;
import util.Timer;
import util.Util;

public class RunSqrtOram extends Protocol {

	private int pid = P.RUN;
	private int onoff = 0;

	public RunSqrtOram(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runInitE(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 5
		int h = md.getNumLevels();
		Level last = oram.getLevel(h - 1);
		Array64<Block> fresh = last.getFresh();
		List<Block> stash = last.getStash();
		List<Long> used = last.getUsed();
		for (int i = 0; i < used.size(); i++)
			fresh.set(used.get(i), stash.get(i));

		// step 6
		Initialize init = new Initialize(con1, con2, md);
		init.runE(predata[h - 1], last, timer);

		// step 7
		for (int i = 0; i < h; i++) {
			Level level = oram.getLevel(i);
			level.emptyStash();
			level.emptyUsed();
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runInitD(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 5
		int h = md.getNumLevels();
		Level last = oram.getLevel(h - 1);
		Array64<Block> fresh = last.getFresh();
		List<Block> stash = last.getStash();
		List<Long> used = last.getUsed();
		for (int i = 0; i < used.size(); i++)
			fresh.set(used.get(i), stash.get(i));

		// step 6
		Initialize init = new Initialize(con1, con2, md);
		init.runD(predata[h - 1], last, timer);

		// step 7
		for (int i = 0; i < h; i++) {
			Level level = oram.getLevel(i);
			level.emptyStash();
			level.emptyUsed();
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public void runInitC(PreData[] predata, SqrtOram oram, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		// step 6
		int h = md.getNumLevels();
		Initialize init = new Initialize(con1, con2, md);
		init.runC(predata[h - 1], timer);

		timer.stop(pid, M.online_comp + onoff);
	}

	public byte[] runE(PreData[] predata, SqrtOram oram, BigInteger addr, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		int h = md.getNumLevels();
		int addrBits = md.getAddrBits();
		OutAccess outacc = new OutAccess(0, null);
		for (int i = 0; i < h; i++) {
			int lBits = i < h - 1 ? md.getLBits(i + 1) : addrBits;
			int lBytes = (lBits + 7) / 8;
			byte[] N = Util.padArray(Util.getSubBits(addr, addrBits, addrBits - lBits).toByteArray(), lBytes);

			Access acc = new Access(con1, con2, md);
			outacc = acc.runE(predata[i], N, oram.getLevel(i), outacc.p, timer);
		}

		timer.stop(pid, M.online_comp + onoff);
		return outacc.rec;
	}

	public void runD(PreData[] predata, SqrtOram oram, BigInteger addr, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		int h = md.getNumLevels();
		int addrBits = md.getAddrBits();
		long p = 0;
		for (int i = 0; i < h; i++) {
			int lBits = i < h - 1 ? md.getLBits(i + 1) : addrBits;
			int lBytes = (lBits + 7) / 8;
			byte[] N = Util.padArray(Util.getSubBits(addr, addrBits, addrBits - lBits).toByteArray(), lBytes);

			Access acc = new Access(con1, con2, md);
			p = acc.runD(predata[i], N, oram.getLevel(i), p, timer);
		}

		timer.stop(pid, M.online_comp + onoff);
	}

	public byte[] runC(PreData[] predata, Timer timer) {
		timer.start(pid, M.online_comp + onoff);

		byte[] rec = null;
		for (int i = 0; i < predata.length; i++) {
			Access acc = new Access(con1, con2, md);
			rec = acc.runC(predata[i], timer);
		}

		timer.stop(pid, M.online_comp + onoff);
		return rec;
	}

	@Override
	public void run(Party party, SqrtOram oram) {
		int repeat = 4;
		int numTest = 1024 / repeat;

		Timer timer = new Timer();
		long gates = 0;
		StopWatch ete_on = new StopWatch("ETE_online");
		StopWatch ete_off = new StopWatch("ETE_offline");

		int h = md.getNumLevels();
		PreData[] predata = new PreData[h];
		PreInitialize preinit = null;
		PreRunSqrtOram prerun = null;

		int counter = 0;
		for (int n = 0; n < numTest; n++) {
			BigInteger addr = new BigInteger(md.getAddrBits(), Crypto.sr);
			BigInteger addr_a = new BigInteger(md.getAddrBits(), Crypto.sr);
			BigInteger addr_b = addr.xor(addr_a);

			for (int r = 0; r < repeat; r++) {
				for (int i = 0; i < h; i++)
					predata[i] = new PreData(i);

				if (counter == md.getPeriod()) {
					System.out.print("Reinitializing...");
					counter = 0;
					preinit = new PreInitialize(con1, con2, md);
					if (party == Party.Eddie) {
						ete_off.start();
						preinit.runE(predata, oram, timer);
						ete_off.stop();

						ete_on.start();
						this.runInitE(predata, oram, timer);
						ete_on.stop();

					} else if (party == Party.Debbie) {
						ete_off.start();
						preinit.runD(predata, oram, timer);
						ete_off.stop();

						ete_on.start();
						this.runInitD(predata, oram, timer);
						ete_on.stop();

					} else if (party == Party.Charlie) {
						ete_off.start();
						preinit.runC(predata, timer);
						ete_off.stop();

						ete_on.start();
						this.runInitC(predata, oram, timer);
						ete_on.stop();

					} else {
						throw new NoSuchPartyException(party + "");
					}
					System.out.println("done!");
				}

				//////////////////////////////////////////////////////////////////////////////////////////

				prerun = new PreRunSqrtOram(con1, con2, md);
				if (party == Party.Eddie) {
					ete_off.start();
					prerun.runE(predata, oram, timer);
					ete_off.stop();

					con1.write(addr_a);
					ete_on.start();
					byte[] rec = this.runE(predata, oram, addr_b, timer);
					ete_on.stop();

					Util.setXor(rec, con2.read());
					if (new BigInteger(1, rec).compareTo(addr) == 0)
						System.out.println((n * repeat + r) + "(" + n + "-" + r + "): SqrtOram Access passed on addr "
								+ addr.longValue());
					else
						System.err.println((n * repeat + r) + "(" + n + "-" + r + "): SqrtOram Access failed on addr "
								+ addr.longValue());

				} else if (party == Party.Debbie) {
					ete_off.start();
					gates = prerun.runD(predata, oram, timer);
					ete_off.stop();

					addr_a = con1.readBigInteger();
					ete_on.start();
					this.runD(predata, oram, addr_a, timer);
					ete_on.stop();

				} else if (party == Party.Charlie) {
					ete_off.start();
					prerun.runC(predata, counter, timer);
					ete_off.stop();

					ete_on.start();
					byte[] rec = this.runC(predata, timer);
					ete_on.stop();
					con1.write(rec);

				} else {
					throw new NoSuchPartyException(party + "");
				}

				counter++;
			}
		}

		/////////////////////////////////////////////////////////

		System.out.println();
		timer = timer.divideBy(numTest * repeat);
		timer.noPrePrint();
		System.out.println();

		StopWatch comEnc = new StopWatch("CE_online_comp");
		comEnc = comEnc.add(con1.comEnc.add(con2.comEnc));
		comEnc = comEnc.divideBy(numTest * repeat);
		System.out.println(comEnc.noPreToMS());
		System.out.println("\n");

		Bandwidth[] bandwidth = new Bandwidth[P.size];
		for (int i = 0; i < P.size; i++) {
			bandwidth[i] = new Bandwidth(P.names[i]);
			bandwidth[i] = bandwidth[i].add(con1.bandwidth[i].add(con2.bandwidth[i]));
			bandwidth[i] = bandwidth[i].divideBy(numTest * repeat);
			System.out.println(bandwidth[i].noPreToString());
		}
		System.out.println();

		System.out.println(gates);
		System.out.println();

		ete_on = ete_on.divideBy(numTest * repeat);
		ete_off = ete_off.divideBy(numTest * repeat);
		System.out.println(ete_on.noPreToMS());
		System.out.println(ete_off.noPreToMS());
		System.out.println();
	}
}
