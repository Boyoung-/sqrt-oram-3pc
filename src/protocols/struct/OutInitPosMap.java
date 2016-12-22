package protocols.struct;

import oram.Block;
import util.Array64;

public class OutInitPosMap {
	public Array64<Block> fresh_a;
	public Array64<Block> fresh_b;
	public Array64<Long> pi_prime_D;
	public Array64<Long> pi_prime_E;

	public OutInitPosMap(Array64<Block> fresh_a, Array64<Block> fresh_b, Array64<Long> pi_D, Array64<Long> pi_E) {
		this.fresh_a = fresh_a;
		this.fresh_b = fresh_b;
		this.pi_prime_D = pi_D;
		this.pi_prime_E = pi_E;
	}
}
