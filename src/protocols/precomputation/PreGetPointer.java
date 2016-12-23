package protocols.precomputation;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import gc.GCGetPointer;
import gc.GCUtil;
import oram.Metadata;
import oram.SqrtOram;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;

public class PreGetPointer extends Protocol {

	private int pid = P.GP;

	public PreGetPointer(Communication con1, Communication con2, Metadata md) {
		super(con1, con2, md);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		// GC
		int tau = md.getTau();
		int ttp = md.getTwoTauPow();
		int pBits = md.getPBits(predata.getIndex());
		
		predata.gp_E_nKeyPairs = GCUtil.genKeyPairs(tau);
		predata.gp_C_nKeyPairs = GCUtil.genKeyPairs(tau);
		predata.gp_E_afKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_C_afKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_E_bfKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_C_bfKeyPairs = GCUtil.genKeyPairs(ttp);
		predata.gp_E_apKeyPairs = new GCSignal[ttp][][];
		predata.gp_C_apKeyPairs = new GCSignal[ttp][][];
		predata.gp_E_bpKeyPairs = new GCSignal[ttp][][];
		predata.gp_C_bpKeyPairs = new GCSignal[ttp][][];
		for (int i=0; i<ttp; i++) {
			predata.gp_E_apKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_C_apKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_E_bpKeyPairs[i] = GCUtil.genKeyPairs(pBits);
			predata.gp_C_bpKeyPairs[i] = GCUtil.genKeyPairs(pBits);
		}
		
		GCSignal[] E_nZeroKeys = GCUtil.getZeroKeys(predata.gp_E_nKeyPairs);
		GCSignal[] C_nZeroKeys = GCUtil.getZeroKeys(predata.gp_C_nKeyPairs);
		GCSignal[] E_afZeroKeys = GCUtil.getZeroKeys(predata.gp_E_afKeyPairs);
		GCSignal[] C_afZeroKeys = GCUtil.getZeroKeys(predata.gp_C_afKeyPairs);
		GCSignal[] E_bfZeroKeys = GCUtil.getZeroKeys(predata.gp_E_bfKeyPairs);
		GCSignal[] C_bfZeroKeys = GCUtil.getZeroKeys(predata.gp_C_bfKeyPairs);
		GCSignal[][] E_apZeroKeys = new GCSignal[ttp][];
		GCSignal[][] C_apZeroKeys = new GCSignal[ttp][];
		GCSignal[][] E_bpZeroKeys = new GCSignal[ttp][];
		GCSignal[][] C_bpZeroKeys = new GCSignal[ttp][];
		for (int i=0; i<ttp; i++) {
			E_apZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_E_apKeyPairs[i]);
			C_apZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_C_apKeyPairs[i]);
			E_bpZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_E_bpKeyPairs[i]);
			C_bpZeroKeys[i] = GCUtil.getZeroKeys(predata.gp_C_bpKeyPairs[i]);
		}

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel, timer, pid, M.offline_write);
		GCSignal[][] outZeroKeys = new GCGetPointer<GCSignal>(gen).execute(E_nZeroKeys, C_nZeroKeys, E_afZeroKeys, C_afZeroKeys, E_bfZeroKeys, C_bfZeroKeys, E_apZeroKeys, C_apZeroKeys, E_bpZeroKeys, C_bpZeroKeys);
		((GCGen) gen).sendLastSetGTT();
		
		predata.gp_outKeyHashes = new byte[outZeroKeys.length][][];
		for (int i=0; i<outZeroKeys.length; i++) {
			predata.gp_outKeyHashes[i] = GCUtil.genOutKeyHashes(outZeroKeys[i]);
			//System.out.println(i + "  " + outZeroKeys[i].length + "  " + predata.gp_outKeyHashes[i].length);
		}
		
		timer.start(pid, M.offline_write);
		for (int i=0; i<outZeroKeys.length; i++)
			con1.write(predata.gp_outKeyHashes[i]);
		//con1.write(predata.gp_outKeyHashes[1]);
		//con1.write(predata.gp_outKeyHashes[2]);
		
		con2.write(predata.gp_C_nKeyPairs);
		con2.write(predata.gp_C_afKeyPairs);
		con2.write(predata.gp_C_bfKeyPairs);
		con2.write(predata.gp_C_apKeyPairs);
		con2.write(predata.gp_C_bpKeyPairs);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public long runD(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		// GC
		GCSignal[] E_nZeroKeys = GCUtil.genEmptyKeys(md.getTau());
		GCSignal[] C_nZeroKeys = GCUtil.genEmptyKeys(md.getTau());
		GCSignal[] E_afZeroKeys = GCUtil.genEmptyKeys(md.getTwoTauPow());
		GCSignal[] C_afZeroKeys = GCUtil.genEmptyKeys(md.getTwoTauPow());
		GCSignal[] E_bfZeroKeys = GCUtil.genEmptyKeys(md.getTwoTauPow());
		GCSignal[] C_bfZeroKeys = GCUtil.genEmptyKeys(md.getTwoTauPow());
		GCSignal[][] E_apZeroKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] C_apZeroKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] E_bpZeroKeys = new GCSignal[md.getTwoTauPow()][];
		GCSignal[][] C_bpZeroKeys = new GCSignal[md.getTwoTauPow()][];
		for (int i=0; i<md.getTwoTauPow(); i++) {
			E_apZeroKeys[i] = GCUtil.genEmptyKeys(md.getPBits(predata.getIndex()));
			C_apZeroKeys[i] = GCUtil.genEmptyKeys(md.getPBits(predata.getIndex()));
			E_bpZeroKeys[i] = GCUtil.genEmptyKeys(md.getPBits(predata.getIndex()));
			C_bpZeroKeys[i] = GCUtil.genEmptyKeys(md.getPBits(predata.getIndex()));
		}

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> eva = new GCEva(channel, timer, pid, M.offline_read);
		predata.gp_circuit = new GCGetPointer<GCSignal>(eva);
		GCSignal[][] out = predata.gp_circuit.execute(E_nZeroKeys, C_nZeroKeys, E_afZeroKeys, C_afZeroKeys, E_bfZeroKeys, C_bfZeroKeys, E_apZeroKeys, C_apZeroKeys, E_bpZeroKeys, C_bpZeroKeys);
		((GCEva) eva).receiveLastSetGTT();
		eva.setEvaluate();

		predata.gp_outKeyHashes = new byte[out.length][][];
		timer.start(pid, M.offline_read);
		for (int i=0; i<out.length; i++)
			predata.gp_outKeyHashes[i] = con1.readDoubleByteArray();
		//predata.gp_outKeyHashes[1] = con1.readDoubleByteArray();
		//predata.gp_outKeyHashes[2] = con1.readDoubleByteArray();
		//System.out.println("AAAAAAA  "  + predata.gp_outKeyHashes[1].length);
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
		return eva.numOfAnds;
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		// GC
		timer.start(pid, M.offline_read);
		predata.gp_C_nKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_afKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_bfKeyPairs = con1.readDoubleGCSignalArray();
		predata.gp_C_apKeyPairs = con1.readTripleGCSignalArray();
		predata.gp_C_bpKeyPairs = con1.readTripleGCSignalArray();
		timer.stop(pid, M.offline_read);		
		
		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, SqrtOram oram) {
	}
}