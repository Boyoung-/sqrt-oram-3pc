package oram;

import java.math.BigInteger;

import crypto.Crypto;
import util.Util;

public class TestSqrtOram {

	public static void main(String[] args) {
		Metadata md = new Metadata();
		// SqrtOram oram = new SqrtOram(md, null);
		// oram.initWithRecords();
		// oram.writeToFile();
		SqrtOram oram = SqrtOram.readFromFile(md.getDefaultOramFileName());

		int tau = md.getTau();
		int addrBits = md.getAddrBits();
		int numLevels = md.getNumLevels();
		long numRecords = md.getNumBlocks(numLevels - 1);
		int firstLBits = md.getLBits(0);

		long numTests = 100;
		for (long n = 0; n < numTests; n++) {
			// address of record we want to test
			long testAddr = Util.nextLong(numRecords, Crypto.sr);

			long p = Util.getSubBits(testAddr, addrBits, addrBits - firstLBits);
			for (int i = 0; i < numLevels - 1; i++) {
				Block target = oram.getLevel(i).getFreshBlock(p);
				assert (new BigInteger(1, target.getL()).longValue() == p);
				int index = (int) Util.getSubBits(testAddr, addrBits - firstLBits - i * tau,
						addrBits - firstLBits - (i + 1) * tau);
				p = new BigInteger(1, target.getP(index)).longValue();
			}
			Block target = oram.getLevel(numLevels - 1).getFreshBlock(p);
			assert (new BigInteger(1, target.getL()).longValue() == p);
			long rec = new BigInteger(1, target.getRec()).longValue();

			// verify correctness
			if (testAddr == rec)
				System.out.println(n + ": Success on address " + BigInteger.valueOf(testAddr).toString(2));
			else
				System.err.println(n + ": Error on address " + BigInteger.valueOf(testAddr).toString(2));
		}
	}

}
