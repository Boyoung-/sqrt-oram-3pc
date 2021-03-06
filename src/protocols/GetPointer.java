package protocols;

import java.math.BigInteger;
import java.util.Random;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.precomputation.PreGetPointer;
import protocols.struct.OutGetPointer;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class GetPointer extends Protocol {

	private int pid = P.GP;
	private int onoff = 0;

	public GetPointer(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, byte[] N, Block A, Block B, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return;

		timer.start(pid, M.online_comp + onoff);

		GCSignal[] nInputKeys = GCUtil.selectKeys(predata.gp_E_nKeyPairs, N);
		GCSignal[] alInputKeys = GCUtil.selectKeys(predata.gp_E_alKeyPairs, A.getL());
		GCSignal[] afInputKeys = GCUtil.selectKeys(predata.gp_E_afKeyPairs, A.getShortF());
		GCSignal[] bfInputKeys = GCUtil.selectKeys(predata.gp_E_bfKeyPairs, B.getShortF());
		GCSignal[][] apInputKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] bpInputKeys = new GCSignal[md.getTwoTauPow()][];
		for (int i = 0; i < md.getTwoTauPow(); i++) {
			apInputKeys[i] = GCUtil.selectKeys(predata.gp_E_apKeyPairs[i], A.getP(i));
			bpInputKeys[i] = GCUtil.selectKeys(predata.gp_E_bpKeyPairs[i], B.getP(i));
		}

		timer.start(pid, M.online_write + onoff);
		con1.write(pid, nInputKeys);
		con1.write(pid, alInputKeys);
		con1.write(pid, afInputKeys);
		con1.write(pid, bfInputKeys);
		con1.write(pid, apInputKeys);
		con1.write(pid, bpInputKeys);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	public OutGetPointer runD(PreData predata, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return null;

		timer.start(pid, M.online_comp + onoff);

		timer.start(pid, M.online_read + onoff);
		GCSignal[] E_nInputKeys = con1.readGCSignalArray(pid);
		GCSignal[] E_alInputKeys = con1.readGCSignalArray(pid);
		GCSignal[] E_afInputKeys = con1.readGCSignalArray(pid);
		GCSignal[] E_bfInputKeys = con1.readGCSignalArray(pid);
		GCSignal[][] E_apInputKeys = con1.readDoubleGCSignalArray(pid);
		GCSignal[][] E_bpInputKeys = con1.readDoubleGCSignalArray(pid);

		GCSignal[] C_nInputKeys = con2.readGCSignalArray(pid);
		GCSignal[] C_alInputKeys = con2.readGCSignalArray(pid);
		GCSignal[] C_afInputKeys = con2.readGCSignalArray(pid);
		GCSignal[] C_bfInputKeys = con2.readGCSignalArray(pid);
		GCSignal[][] C_apInputKeys = con2.readDoubleGCSignalArray(pid);
		GCSignal[][] C_bpInputKeys = con2.readDoubleGCSignalArray(pid);
		timer.stop(pid, M.online_read + onoff);

		GCSignal[][] outKeys = predata.gp_circuit.execute(E_nInputKeys, C_nInputKeys, E_alInputKeys, C_alInputKeys,
				E_afInputKeys, C_afInputKeys, E_bfInputKeys, C_bfInputKeys, E_apInputKeys, C_apInputKeys, E_bpInputKeys,
				C_bpInputKeys);

		long p = GCUtil.evaOutKeys(outKeys[0], predata.gp_outKeyHashes[0]).longValue();
		Block A_prime = new Block(predata.getIndex(), md, (Random) null);
		byte[] AL = Util.padArray(GCUtil.evaOutKeys(outKeys[1], predata.gp_outKeyHashes[1]).toByteArray(),
				md.getLBytes(predata.getIndex()));
		A_prime.setL(AL);
		BigInteger AF = GCUtil.evaOutKeys(outKeys[2], predata.gp_outKeyHashes[2]);
		A_prime.setF(Block.toLongF(AF, md.getTwoTauPow()));
		BigInteger BF = GCUtil.evaOutKeys(outKeys[3], predata.gp_outKeyHashes[3]);
		for (int i = 0; i < md.getTwoTauPow(); i++)
			A_prime.setP(i,
					Util.rmSignBit(GCUtil.evaOutKeys(outKeys[4 + i], predata.gp_outKeyHashes[4 + i]).toByteArray()));

		OutGetPointer outgp = new OutGetPointer(p, A_prime, Block.toLongF(BF, md.getTwoTauPow()));

		timer.stop(pid, M.online_comp + onoff);
		return outgp;
	}

	public void runC(PreData predata, byte[] N, Block A, Block B, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return;

		timer.start(pid, M.online_comp + onoff);

		GCSignal[] nInputKeys = GCUtil.selectKeys(predata.gp_C_nKeyPairs, N);
		GCSignal[] alInputKeys = GCUtil.selectKeys(predata.gp_C_alKeyPairs, A.getL());
		GCSignal[] afInputKeys = GCUtil.selectKeys(predata.gp_C_afKeyPairs, A.getShortF());
		GCSignal[] bfInputKeys = GCUtil.selectKeys(predata.gp_C_bfKeyPairs, B.getShortF());
		GCSignal[][] apInputKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] bpInputKeys = new GCSignal[md.getTwoTauPow()][];
		for (int i = 0; i < md.getTwoTauPow(); i++) {
			apInputKeys[i] = GCUtil.selectKeys(predata.gp_C_apKeyPairs[i], A.getP(i));
			bpInputKeys[i] = GCUtil.selectKeys(predata.gp_C_bpKeyPairs[i], B.getP(i));
		}

		timer.start(pid, M.online_write + onoff);
		con2.write(pid, nInputKeys);
		con2.write(pid, alInputKeys);
		con2.write(pid, afInputKeys);
		con2.write(pid, bfInputKeys);
		con2.write(pid, apInputKeys);
		con2.write(pid, bpInputKeys);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	// for testing correctness
	@Override
	public void run(Party party, SqrtOram oram) {
		Timer timer = new Timer();
		byte[] N = null;
		Block A = null;
		Block B = null;
		PreData predata = null;
		PreGetPointer pregp = null;
		int levelIndex;

		for (int j = 0; j < 100; j++) {
			pregp = new PreGetPointer(con1, con2, md);

			if (party == Party.Eddie) {
				levelIndex = Crypto.sr.nextInt(md.getNumLevels() - 1);
				con1.write(levelIndex);
				con2.write(levelIndex);
				predata = new PreData(levelIndex);
				pregp.runE(predata, timer);

				N = Util.nextBytes((md.getTau() + 7) / 8, Crypto.sr);
				A = new Block(levelIndex, md, Crypto.sr);
				B = new Block(levelIndex, md, Crypto.sr);
				this.runE(predata, N, A, B, timer);

				con1.write(Util.getSubBits(new BigInteger(1, N), md.getTau(), 0).intValue());
				con1.write(A);
				con1.write(B);
				con1.write(predata.gp_A_prime);
				con1.write(predata.gp_BF_prime);

			} else if (party == Party.Debbie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				pregp.runD(predata, timer);

				OutGetPointer outgp = this.runD(predata, timer);

				int index = con1.readInt();
				A = con1.readBlock();
				B = con1.readBlock();
				predata.gp_A_prime = con1.readBlock();
				predata.gp_BF_prime = con1.read();

				outgp.A.setXor(predata.gp_A_prime);
				Util.setXor(outgp.BF, predata.gp_BF_prime);
				byte[] F = (A.getF(index) & 1) == 0 ? outgp.A.getF() : outgp.BF;
				Block block = (A.getF(index) & 1) == 0 ? A : B;
				long p = Util.getSubBits(new BigInteger(1, block.getP(index)), md.getPBits(levelIndex), 0).longValue();
				Block zero = A.xor(outgp.A);

				if (p == outgp.p && (F[index] & 1) == 1
						&& Util.getSubBits(new BigInteger(zero.getL()), md.getLBits(levelIndex), 0).intValue() == 0) {
					boolean pass = true;
					for (int i = 0; i < md.getTwoTauPow(); i++) {
						if (Util.getSubBits(new BigInteger(zero.getP(i)), md.getPBits(levelIndex), 0).intValue() != 0) {
							System.err.println(j + " " + i + ": GP test failed (1)");
							pass = false;
							break;
						}
					}
					if (pass)
						System.out.println(j + ": GP test passed");
				} else {
					System.err.println(j + ": GP test failed (0)");
				}

			} else if (party == Party.Charlie) {
				levelIndex = con1.readInt();
				predata = new PreData(levelIndex);
				pregp.runC(predata, timer);

				N = new byte[(md.getTau() + 7) / 8];
				A = new Block(levelIndex, md, (Random) null);
				B = new Block(levelIndex, md, (Random) null);
				this.runC(predata, N, A, B, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}
