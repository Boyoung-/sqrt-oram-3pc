package protocols.struct;

public class OutGetPointer {
	public long p;
	public byte[] AF;
	public byte[] BF;

	public OutGetPointer(long p, byte[] AF, byte[] BF) {
		this.p = p;
		this.AF = AF;
		this.BF = BF;
	}
}
