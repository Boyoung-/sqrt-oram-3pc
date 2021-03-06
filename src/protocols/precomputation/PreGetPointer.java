package protocols.precomputation;

import java.util.Random;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import crypto.Crypto;
import gc.GCGetPointer;
import gc.GCUtil;
import oram.Block;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreGetPointer extends Protocol {

	private int pid = P.GP;
	private int onoff = 3;

	public PreGetPointer(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return;

		timer.start(pid, M.online_comp + onoff);

		int tau = md.getTau();
		int ttp = md.getTwoTauPow();
		int lBits = md.getLBits(predata.getIndex());
		int pBits = md.getPBits(predata.getIndex());

		// GC
		predata.gp_A_prime = new Block(predata.getIndex(), md, Crypto.sr);
		predata.gp_BF_prime = Util.nextBytes(ttp, Crypto.sr);

		predata.gp_E_nKeyPairs = GCUtil.genKeyPairs(tau);
		predata.gp_C_nKeyPairs = GCUtil.genKeyPairs(tau);
		predata.gp_E_alKeyPairs = GCUtil.genKeyPairs(lBits);
		predata.gp_C_alKeyPairs = GCUtil.genKeyPairs(lBits);
		predata.gp_E_afKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_C_afKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_E_bfKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_C_bfKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_E_apKeyPairs = new GCSignal[ttp][][];
		predata.gp_C_apKeyPairs = new GCSignal[ttp][][];
		predata.gp_E_bpKeyPairs = new GCSignal[ttp][][];
		predata.gp_C_bpKeyPairs = new GCSignal[ttp][][];
		for (int i = 0; i < ttp; i++) {
			predata.gp_E_apKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_C_apKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_E_bpKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_C_bpKeyPairs[i] = GCUtil.genKeyPairs(pBits);
		}

		GCSignal[] E_nZeroKeys = GCUtil.getZeroKeys(predata.gp_E_nKeyPairs);
		GCSignal[] C_nZeroKeys = GCUtil.getZeroKeys(predata.gp_C_nKeyPairs);
		GCSignal[] E_alZeroKeys = GCUtil.getZeroKeys(predata.gp_E_alKeyPairs);
		GCSignal[] C_alZeroKeys = GCUtil.getZeroKeys(predata.gp_C_alKeyPairs);
		GCSignal[] E_afZeroKeys = GCUtil.getZeroKeys(predata.gp_E_afKeyPairs);
		GCSignal[] C_afZeroKeys = GCUtil.getZeroKeys(predata.gp_C_afKeyPairs);
		GCSignal[] E_bfZeroKeys = GCUtil.getZeroKeys(predata.gp_E_bfKeyPairs);
		GCSignal[] C_bfZeroKeys = GCUtil.getZeroKeys(predata.gp_C_bfKeyPairs);
		GCSignal[][] E_apZeroKeys = new GCSignal[ttp][];
		GCSignal[][] C_apZeroKeys = new GCSignal[ttp][];
		GCSignal[][] E_bpZeroKeys = new GCSignal[ttp][];
		GCSignal[][] C_bpZeroKeys = new GCSignal[ttp][];
		for (int i = 0; i < ttp; i++) {
			E_apZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_E_apKeyPairs[i]);
			C_apZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_C_apKeyPairs[i]);
			E_bpZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_E_bpKeyPairs[i]);
			C_bpZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_C_bpKeyPairs[i]);
		}

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel, timer, pid, M.online_write + onoff);
		GCSignal[][] outZeroKeys = new GCGetPointer<GCSignal>(gen, predata.gp_A_prime, predata.gp_BF_prime, md,
				predata.getIndex()).execute(E_nZeroKeys, C_nZeroKeys, E_alZeroKeys, C_alZeroKeys, E_afZeroKeys,
						C_afZeroKeys, E_bfZeroKeys, C_bfZeroKeys, E_apZeroKeys, C_apZeroKeys, E_bpZeroKeys,
						C_bpZeroKeys);
		((GCGen) gen).sendLastSetGTT();

		predata.gp_outKeyHashes = new byte[outZeroKeys.length][][];
		for (int i = 0; i < outZeroKeys.length; i++) {
			predata.gp_outKeyHashes[i] = GCUtil.genOutKeyHashes(outZeroKeys[i]);
		}

		timer.start(pid, M.online_write + onoff);
		for (int i = 0; i < outZeroKeys.length; i++)
			con1.write(predata.gp_outKeyHashes[i]);

		con2.write(predata.gp_C_nKeyPairs);
		con2.write(predata.gp_C_alKeyPairs);
		con2.write(predata.gp_C_afKeyPairs);
		con2.write(predata.gp_C_bfKeyPairs);
		con2.write(predata.gp_C_apKeyPairs);
		con2.write(predata.gp_C_bpKeyPairs);
		timer.stop(pid, M.online_write + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	public long runD(PreData predata, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return 0;

		timer.start(pid, M.online_comp + onoff);

		int ttp = md.getTwoTauPow();

		// GC
		Block A_prime = new Block(predata.getIndex(), md, (Random) null);
		byte[] BF_prime = new byte[ttp];

		GCSignal[] E_nZeroKeys = GCUtil.genEmptyKeys(md.getTau());
		GCSignal[] E_alZeroKeys = GCUtil.genEmptyKeys(md.getLBits(predata.getIndex()));
		GCSignal[] E_afZeroKeys = GCUtil.genEmptyKeys(ttp);
		GCSignal[][] E_apZeroKeys = new GCSignal[ttp][];
		for (int i = 0; i < md.getTwoTauPow(); i++) {
			E_apZeroKeys[i] = GCUtil.genEmptyKeys(md.getPBits(predata.getIndex()));
		}

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> eva = new GCEva(channel, timer, pid, M.online_read + onoff);
		predata.gp_circuit = new GCGetPointer<GCSignal>(eva, A_prime, BF_prime, md, predata.getIndex());
		GCSignal[][] out = predata.gp_circuit.execute(E_nZeroKeys, E_nZeroKeys, E_alZeroKeys, E_alZeroKeys,
				E_afZeroKeys, E_afZeroKeys, E_afZeroKeys, E_afZeroKeys, E_apZeroKeys, E_apZeroKeys, E_apZeroKeys,
				E_apZeroKeys);
		((GCEva) eva).receiveLastSetGTT();
		eva.setEvaluate();

		predata.gp_outKeyHashes = new byte[out.length][][];

		timer.start(pid, M.online_read + onoff);
		for (int i = 0; i < out.length; i++)
			predata.gp_outKeyHashes[i] = con1.readDoubleByteArray();
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
		return eva.numOfAnds;
	}

	public void runC(PreData predata, Timer timer) {
		if (predata.getIndex() >= md.getNumLevels() - 1)
			return;

		timer.start(pid, M.online_comp + onoff);

		// GC
		timer.start(pid, M.online_read + onoff);
		predata.gp_C_nKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_alKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_afKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_bfKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_apKeyPairs = con1.readTripleGCSignalArray();
		predata.gp_C_bpKeyPairs = con1.readTripleGCSignalArray();
		timer.stop(pid, M.online_read + onoff);

		timer.stop(pid, M.online_comp + onoff);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}
