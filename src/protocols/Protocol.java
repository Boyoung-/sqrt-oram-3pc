package protocols;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Metadata;
import oram.SqrtOram;
import protocols.struct.Party;

public abstract class Protocol {
	protected Communication con1;
	protected Communication con2;

	protected Metadata md;

	/*
	 * Connections are alphabetized so:
	 * 
	 * For Eddie con1 = debbie con2 = charlie
	 * 
	 * For Debbie con1 = eddie con2 = charlie
	 * 
	 * For Charlie con1 = eddie con2 = debbie
	 */
	/*
	 * public Protocol(Communication con1, Communication con2) { this.con1 =
	 * con1; this.con2 = con2; }
	 */

	public Protocol(Communication con1, Communication con2, Metadata md) {
		this.con1 = con1;
		this.con2 = con2;
		this.md = md;
	}

	private static final boolean ENSURE_SANITY = true;

	public boolean ifSanityCheck() {
		return ENSURE_SANITY;
	}

	// Utility function will test for synchrony between the parties.
	public void sanityCheck() {
		if (ENSURE_SANITY) {

			// System.out.println("Sanity check");
			con1.write("sanity");
			con2.write("sanity");

			if (!con1.readString().equals("sanity")) {
				System.out.println("Sanity check failed for con1");
			}
			if (!con2.readString().equals("sanity")) {
				System.out.println("Sanity check failed for con2");
			}
		}
	}

	public void run(Party party, String forestFile) {
		SqrtOram oram = null;

		if (party == Party.Eddie) {
			// if (forestFile == null)
			// oram = SqrtOram.readFromFile(md.getDefaultSharesName1());
			// else
			// oram = SqrtOram.readFromFile(forestFile);
			oram = new SqrtOram(md, null);
			oram.initWithRecords();

		} else if (party == Party.Debbie) {
			// if (forestFile == null)
			// oram = SqrtOram.readFromFile(md.getDefaultSharesName2());
			// else
			// oram = SqrtOram.readFromFile(forestFile);
			oram = new SqrtOram(md, null);

		} else if (party == Party.Charlie) {

		} else {
			throw new NoSuchPartyException(party.toString());
		}

		run(party, oram);
	}

	/*
	 * This is mostly just testing code and may need to change for the purpose
	 * of an actual execution
	 */
	public abstract void run(Party party, SqrtOram oram);
}
