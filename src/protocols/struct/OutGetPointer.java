package protocols.struct;

import oram.Block;

public class OutGetPointer {
	public long p;
	public Block A;
	public byte[] BF;

	public OutGetPointer(long p, Block A, byte[] BF) {
		this.p = p;
		this.A = A;
		this.BF = BF;
	}
}
