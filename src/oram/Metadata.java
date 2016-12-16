package oram;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Metadata {
	private String configFolder = "config/";
	private String configFileName = "config.yaml";
	private String defaultOramFileName;

	private String TAU = "tau";
	private String ADDRBITS = "addrBits";
	private String DBYTES = "dBytes";

	private int tau;
	private int addrBits;
	private int dBytes;

	private int twoTauPow;
	private int period;
	private int numLevels;

	private int[] lBits;
	private int fBits;
	private int[] pBits;

	private int[] lBytes;
	private int fBytes;
	private int[] pBytes;
	private int FBytes;
	private int[] PBytes;
	private int[] blockBytes;

	private long[] numBlocks;
	private long[] levelBytes;

	private long oramBytes;

	public Metadata() {
		setup(configFileName);
	}

	public Metadata(String filename) {
		setup(filename);
	}

	private void setup(String filename) {
		Yaml yaml = new Yaml();
		InputStream input = null;
		try {
			input = new FileInputStream(new File(configFolder + filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> configMap = (Map<String, Object>) yaml.load(input);

		tau = Integer.parseInt(configMap.get(TAU).toString());
		addrBits = Integer.parseInt(configMap.get(ADDRBITS).toString());
		dBytes = Integer.parseInt(configMap.get(DBYTES).toString());

		init();
		setDefaultForestFileName();
	}

	private void init() {
		twoTauPow = (int) Math.pow(2, tau);
		int logT = (addrBits + 1) / 2 - tau;
		period = (int) Math.pow(2, logT);
		numLevels = (addrBits - logT) / tau + 1;

		lBits = new int[numLevels];
		fBits = 1;
		pBits = new int[numLevels];

		lBytes = new int[numLevels];
		fBytes = (fBits + 7) / 8;
		pBytes = new int[numLevels];
		FBytes = fBytes * twoTauPow;
		PBytes = new int[numLevels];
		blockBytes = new int[numLevels];

		numBlocks = new long[numLevels];
		levelBytes = new long[numLevels];

		oramBytes = 0;

		for (int i = 0; i < numLevels - 1; i++) {
			lBits[i] = addrBits - tau * (numLevels - 1 - i);
			pBits[i] = lBits[i] + tau;

			lBytes[i] = (lBits[i] + 7) / 8;
			pBytes[i] = (pBits[i] + 7) / 8;
			PBytes[i] = pBytes[i] * twoTauPow;
			blockBytes[i] = lBytes[i] + FBytes + PBytes[i];

			numBlocks[i] = (long) Math.pow(2, lBits[i]);
			levelBytes[i] = blockBytes[i] * numBlocks[i];

			oramBytes += levelBytes[i];
		}

		int i = numLevels - 1;
		lBits[i] = addrBits;
		lBytes[i] = (lBits[i] + 7) / 8;
		blockBytes[i] = lBytes[i] + dBytes;
		numBlocks[i] = (long) Math.pow(2, lBits[i]);
		levelBytes[i] = blockBytes[i] * numBlocks[i];
		oramBytes += levelBytes[i];
	}

	public void print() {
		System.out.println("===== ORAM Forest Metadata =====");
		System.out.println();
		System.out.println("tau:				" + tau);
		System.out.println("address bits:		" + addrBits);
		System.out.println("D bytes:			" + dBytes);
		System.out.println("shuffle period:		" + period);
		System.out.println();
		System.out.println("levels:				" + numLevels);
		System.out.println("oram bytes:		" + oramBytes);
		System.out.println();

		for (int i = 0; i < numLevels; i++) {
			System.out.println("[Level " + i + "]");
			System.out.println("	lBits		-> " + getLBits(i));
			System.out.println("	fBits		-> " + getFBits(i));
			System.out.println("	pBits		-> " + getPBits(i));
			System.out.println("	lBytes		-> " + getLBytes(i));
			System.out.println("	fBytes		-> " + getFBytes(i));
			System.out.println("	pBytes		-> " + getPBytes(i));
			System.out.println("	FBytes		-> " + getFarrBytes(i));
			System.out.println("	PBytes		-> " + getParrBytes(i));
			System.out.println("	blockBytes	-> " + getBlockBytes(i));
			System.out.println("	numBlocks	-> " + getNumBlocks(i));
			System.out.println("	levelBytes	-> " + getLevelBytes(i));
			System.out.println();
		}
		System.out.println("===== End of Metadata =====");
		System.out.println();
	}

	public void writeToFile(String filename) {
		Yaml yaml = new Yaml();
		FileWriter writer = null;
		try {
			writer = new FileWriter(configFolder + filename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, String> configMap = new HashMap<String, String>();
		configMap.put(TAU, "" + tau);
		configMap.put(ADDRBITS, "" + addrBits);
		configMap.put(DBYTES, "" + dBytes);

		yaml.dump(configMap, writer);
	}

	public void writeToFile() {
		writeToFile(configFileName);
	}

	private void setDefaultForestFileName() {
		defaultOramFileName = "sqrt_";
		defaultOramFileName += "t" + tau;
		defaultOramFileName += "m" + addrBits;
		defaultOramFileName += "d" + dBytes;
		defaultOramFileName += "_T" + period;
		defaultOramFileName += ".bin";
	}

	public String getDefaultForestFileName() {
		return defaultOramFileName;
	}

	public String getDefaultSharesName1() {
		return defaultOramFileName + ".share1";
	}

	public String getDefaultSharesName2() {
		return defaultOramFileName + ".share2";
	}

	public int getTau() {
		return tau;
	}

	public int getTwoTauPow() {
		return twoTauPow;
	}

	public int getAddrBits() {
		return addrBits;
	}

	public int getDBytes() {
		return dBytes;
	}

	public int getPeriod() {
		return period;
	}

	public int getNumLevels() {
		return numLevels;
	}

	public int getLBits(int i) {
		return lBits[i];
	}

	public int getFBits(int i) {
		assert (0 <= i && i < numLevels);
		if (i == numLevels - 1)
			return 0;
		return fBits;
	}

	public int getPBits(int i) {
		return pBits[i];
	}

	public int getLBytes(int i) {
		return lBytes[i];
	}

	public int getFBytes(int i) {
		assert (0 <= i && i < numLevels);
		if (i == numLevels - 1)
			return 0;
		return fBytes;
	}

	public int getPBytes(int i) {
		return pBytes[i];
	}

	public int getFarrBytes(int i) {
		assert (0 <= i && i < numLevels);
		if (i == numLevels - 1)
			return 0;
		return FBytes;
	}

	public int getParrBytes(int i) {
		return PBytes[i];
	}

	public int getBlockBytes(int i) {
		return blockBytes[i];
	}

	public long getNumBlocks(int i) {
		return numBlocks[i];
	}

	public long getLevelBytes(int i) {
		return levelBytes[i];
	}

	public long getForestBytes() {
		return oramBytes;
	}
}
