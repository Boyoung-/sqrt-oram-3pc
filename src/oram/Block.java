package oram;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import exceptions.LengthNotMatchException;
import util.Util;

public class Block implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private byte[] l;
	private byte[] F;
	private byte[][] P;
	private byte[] rec;

	private int numBytes;

	public Block(int lBytes, int fBytes, int pBytes, int recBytes, int twoTauPow, Random rand) {
		l = new byte[lBytes];
		F = new byte[fBytes * twoTauPow];
		P = new byte[twoTauPow][pBytes];
		rec = new byte[recBytes];
		if (rand != null) {
			rand.nextBytes(l);
			rand.nextBytes(F);
			for (int i = 0; i < twoTauPow; i++)
				rand.nextBytes(P[i]);
			rand.nextBytes(rec);
		}

		numBytes = lBytes + (fBytes + pBytes) * twoTauPow + recBytes;
	}

	public Block(byte[] l, byte[] F, byte[][] P, byte[] rec) {
		assert (F.length == P.length);
		this.l = l;
		this.F = F;
		this.P = P;
		this.rec = rec;
		this.numBytes = l.length + rec.length;
		if (F.length > 0)
			this.numBytes += F.length + P.length * P[0].length;
	}

	public Block(int index, Metadata md, Random rand) {
		this(md.getLBytes(index), md.getFBytes(index), md.getPBytes(index), md.getRecBytes(index), md.getTwoTauPow(),
				rand);
	}

	public Block(int index, Metadata md, byte[] arr) {
		this(index, md, (Random) null);
		if (getNumBytes() != arr.length)
			throw new LengthNotMatchException(getNumBytes() + " != " + arr.length);
		int offset = 0;
		System.arraycopy(arr, offset, l, 0, l.length);
		offset += l.length;
		System.arraycopy(arr, offset, F, 0, F.length);
		offset += F.length;
		for (int i = 0; i < P.length; i++) {
			System.arraycopy(arr, offset, P[i], 0, P[i].length);
			offset += P[i].length;
		}
		System.arraycopy(arr, offset, rec, 0, rec.length);
	}

	// deep copy
	public Block(Block b) {
		l = b.getL().clone();
		F = b.getF().clone();
		P = new byte[b.getP().length][];
		for (int i = 0; i < P.length; i++)
			P[i] = b.getP(i).clone();
		rec = b.getRec().clone();
		numBytes = b.numBytes;
	}

	public byte[] getL() {
		return l;
	}

	public byte getF(int i) {
		return F[i];
	}

	public byte[] getF() {
		return F;
	}

	public byte[] getShortF() {
		return toShortF(F);
	}

	public static byte[] toShortF(byte[] f) {
		BigInteger shortF = BigInteger.ZERO;
		for (int i = 0; i < f.length; i++) {
			if ((f[f.length - 1 - i] & 1) == 1) {
				shortF = shortF.setBit(i);
			}
		}
		return Util.rmSignBit(shortF.toByteArray());
	}

	public static byte[] toLongF(BigInteger f, int len) {
		byte[] longF = new byte[len];
		for (int i = 0; i < len; i++) {
			longF[len - 1 - i] = (byte) (f.testBit(i) ? 1 : 0);
		}
		return longF;
	}

	public byte[] getP(int i) {
		return P[i];
	}

	public byte[][] getP() {
		return P;
	}

	public byte[] getRec() {
		return rec;
	}

	public int getNumBytes() {
		return numBytes;
	}

	public void setL(byte[] l) {
		if (this.l.length == l.length)
			this.l = l;
		else
			setByteArray(this.l, l);
	}

	public void setF(byte[] F) {
		if (this.F.length == F.length)
			this.F = F;
		else
			setByteArray(this.F, F);
	}

	public void setF(int i, byte b) {
		F[i] = b;
	}

	public void setP(int i, byte[] arr) {
		if (P[i].length == arr.length)
			P[i] = arr;
		else
			setByteArray(P[i], arr);
	}

	public void setP(byte[][] P) {
		if (this.P.length != P.length)
			throw new LengthNotMatchException(this.P.length + " != " + P.length);
		this.P = P;
		// for (int i=0; i<P.length; i++)
		// setP(i, P[i]);
	}

	public void setRec(byte[] rec) {
		if (this.rec.length == rec.length)
			this.rec = rec;
		else
			setByteArray(this.rec, rec);
	}

	private void setByteArray(byte[] oldArr, byte[] newArr) {
		if (oldArr.length < newArr.length)
			throw new LengthNotMatchException(oldArr.length + " < " + newArr.length);
		else {
			for (int i = 0; i < oldArr.length - newArr.length; i++)
				oldArr[i] = 0;
			System.arraycopy(newArr, 0, oldArr, oldArr.length - newArr.length, newArr.length);
		}
	}

	public Block xor(Block b) {
		if (getNumBytes() != b.getNumBytes())
			throw new LengthNotMatchException(getNumBytes() + " != " + b.getNumBytes());
		byte[] newL = Util.xor(l, b.getL());
		byte[] newF = Util.xor(F, b.getF());
		byte[][] newP = Util.xor(P, b.getP());
		byte[] newRec = Util.xor(rec, b.getRec());
		return new Block(newL, newF, newP, newRec);
	}

	public void setXor(Block b) {
		if (getNumBytes() != b.getNumBytes())
			throw new LengthNotMatchException(getNumBytes() + " != " + b.getNumBytes());
		Util.setXor(l, b.getL());
		Util.setXor(F, b.getF());
		Util.setXor(P, b.getP());
		Util.setXor(rec, b.getRec());
	}

	public boolean equals(Block b) {
		if (getNumBytes() != b.getNumBytes())
			return false;
		return Util.equal(toByteArray(), b.toByteArray());
	}

	public byte[] toByteArray() {
		byte[] block = new byte[getNumBytes()];
		int offset = 0;
		System.arraycopy(l, 0, block, offset, l.length);
		offset += l.length;
		System.arraycopy(F, 0, block, offset, F.length);
		offset += F.length;
		for (int i = 0; i < P.length; i++) {
			System.arraycopy(P[i], 0, block, offset, P[i].length);
			offset += P[i].length;
		}
		System.arraycopy(rec, 0, block, offset, rec.length);
		return block;
	}

	@Override
	public String toString() {
		StringBuffer str = new StringBuffer("Block: ");
		str.append("l=" + Util.byteArrayToString(l, 2) + ", ");
		str.append("F=");
		for (int i = 0; i < F.length; i++)
			str.append(F[i] & 1);
		str.append(", P=|");
		for (int i = 0; i < P.length; i++)
			str.append(Util.byteArrayToString(P[i], 2) + "|");
		str.append(", rec=" + Util.byteArrayToString(rec, 2));
		return str.toString();
	}

}
