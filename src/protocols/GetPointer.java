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

	public GetPointer(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, byte[] N, Block A, Block B, Timer timer) {
		timer.start(pid, M.online_comp);
		
		GCSignal[] nInputKeys = GCUtil.selectKeys(predata.gp_E_nKeyPairs, N);
		GCSignal[] afInputKeys = GCUtil.selectKeys(predata.gp_E_afKeyPairs, A.getShortF());
		GCSignal[] bfInputKeys = GCUtil.selectKeys(predata.gp_E_bfKeyPairs, B.getShortF());
		GCSignal[][] apInputKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] bpInputKeys = new GCSignal[md.getTwoTauPow()][];
		for (int i=0; i<md.getTwoTauPow(); i++) {
			apInputKeys[i] = GCUtil.selectKeys(predata.gp_E_apKeyPairs[i], A.getP(i));
			bpInputKeys[i] = GCUtil.selectKeys(predata.gp_E_bpKeyPairs[i], B.getP(i));
		}

		timer.start(pid, M.online_write);
		con1.write(pid, nInputKeys);
		con1.write(pid, afInputKeys);
		con1.write(pid, bfInputKeys);
		con1.write(pid, apInputKeys);
		con1.write(pid, bpInputKeys);
		timer.stop(pid, M.online_write);		

		timer.stop(pid, M.online_comp);
	}

	public OutGetPointer runD(PreData predata, Timer timer) {
		timer.start(pid, M.online_comp);

		timer.start(pid, M.online_read);
		GCSignal[] E_nInputKeys = con1.readGCSignalArray(pid);
		GCSignal[] E_afInputKeys = con1.readGCSignalArray(pid);
		GCSignal[] E_bfInputKeys = con1.readGCSignalArray(pid);
		GCSignal[][] E_apInputKeys = con1.readDoubleGCSignalArray(pid);
		GCSignal[][] E_bpInputKeys = con1.readDoubleGCSignalArray(pid);
		
		GCSignal[] C_nInputKeys = con2.readGCSignalArray(pid);
		GCSignal[] C_afInputKeys = con2.readGCSignalArray(pid);
		GCSignal[] C_bfInputKeys = con2.readGCSignalArray(pid);
		GCSignal[][] C_apInputKeys = con2.readDoubleGCSignalArray(pid);
		GCSignal[][] C_bpInputKeys = con2.readDoubleGCSignalArray(pid);		
		timer.stop(pid, M.online_read);

		GCSignal[][] outKeys = predata.gp_circuit.execute(E_nInputKeys, C_nInputKeys, E_afInputKeys, C_afInputKeys, E_bfInputKeys, C_bfInputKeys, E_apInputKeys, C_apInputKeys, E_bpInputKeys, C_bpInputKeys);
		
		long p = GCUtil.evaOutKeys(outKeys[0], predata.gp_outKeyHashes[0]).longValue();
		//System.out.println(predata.gp_outKeyHashes[1].length);
		byte[] AF = Util.rmSignBit(GCUtil.evaOutKeys(outKeys[1], predata.gp_outKeyHashes[1]).toByteArray());
		byte[] BF = Util.rmSignBit(GCUtil.evaOutKeys(outKeys[2], predata.gp_outKeyHashes[2]).toByteArray());
		OutGetPointer outgp = new OutGetPointer(p, AF, BF);
		
		System.out.println("BBBBBBB  " + GCUtil.evaOutKeys(outKeys[3], predata.gp_outKeyHashes[3]).intValue());
		
		timer.stop(pid, M.online_comp);
		return outgp;
	}

	public void runC(PreData predata, byte[] N, Block A, Block B, Timer timer) {
		timer.start(pid, M.online_comp);

		GCSignal[] nInputKeys = GCUtil.selectKeys(predata.gp_C_nKeyPairs, N);
		GCSignal[] afInputKeys = GCUtil.selectKeys(predata.gp_C_afKeyPairs, A.getShortF());
		GCSignal[] bfInputKeys = GCUtil.selectKeys(predata.gp_C_bfKeyPairs, B.getShortF());
		GCSignal[][] apInputKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] bpInputKeys = new GCSignal[md.getTwoTauPow()][];
		for (int i=0; i<md.getTwoTauPow(); i++) {
			apInputKeys[i] = GCUtil.selectKeys(predata.gp_C_apKeyPairs[i], A.getP(i));
			bpInputKeys[i] = GCUtil.selectKeys(predata.gp_C_bpKeyPairs[i], B.getP(i));
		}

		timer.start(pid, M.online_write);
		con2.write(pid, nInputKeys);
		con2.write(pid, afInputKeys);
		con2.write(pid, bfInputKeys);
		con2.write(pid, apInputKeys);
		con2.write(pid, bpInputKeys);
		timer.stop(pid, M.online_write);		

		timer.stop(pid, M.online_comp);
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
					
					N = Util.nextBytes((md.getTau()+7)/8, Crypto.sr);
					A = new Block(levelIndex, md, Crypto.sr);
					A.setF(Util.nextBytes(A.getF().length, Crypto.sr));
					//A.setF(new byte[A.getF().length]);
					B = new Block(levelIndex, md, (Random) null);
					this.runE(predata, N, A, B, timer);
					
					con1.write(Util.getSubBits(new BigInteger(1, N), md.getTau(), 0).intValue());
					con1.write(A);
					con1.write(B);

				} else if (party == Party.Debbie) {
					levelIndex = con1.readInt();
					predata = new PreData(levelIndex);
					pregp.runD(predata, timer);
					
					OutGetPointer outgp = this.runD(predata, timer);
					
					int index = con1.readInt();
					A = con1.readBlock();
					B = con1.readBlock();
					Block block = (A.getF(index) & 1) == 0 ? A : B;
					long p = Util.getSubBits(new BigInteger(1, block.getP(index)), md.getPBits(levelIndex), 0).longValue();
					byte[] F = (A.getF(index) & 1) == 0 ? outgp.AF : outgp.BF;
					
					if (p == outgp.p)// && new BigInteger(F).testBit(md.getTwoTauPow()-1-index))
						;//System.out.println(j + ": GP test passed" + " " + (A.getF(index) & 1));
					else {
						System.err.println(j + ": GP test failed" + " " + (A.getF(index) & 1) + " " + (Util.getSubBits(new BigInteger(1, B.getP(index)), md.getPBits(levelIndex), 0).longValue()) + " " + outgp.p);
						for (int i=0; i<md.getTwoTauPow(); i++)
							System.out.print((Util.getSubBits(new BigInteger(1, A.getP(index)), md.getPBits(levelIndex), 0).longValue()) + " ");
						System.out.println();
					}

				} else if (party == Party.Charlie) {
					levelIndex = con1.readInt();
					predata = new PreData(levelIndex);
					pregp.runC(predata, timer);
					
					N = new byte[(md.getTau()+7)/8];
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
