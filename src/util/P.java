package util;

public class P {

	public static final int RUN = 0;
	public static final int INIT = 1;
	public static final int INIT_OP_ON = 2;
	public static final int INIT_OP_XOT_ON = 3;
	public static final int ACC = 4;
	public static final int ACC_XOT = 5;
	public static final int COT = 6;
	public static final int GP = 7;

	public static final int INIT_OP_OFF = 8;
	public static final int INIT_OP_XOT_OFF = 9;
	public static final int IPM = 10;
	public static final int IPM_OP = 11;
	public static final int IPM_OP_XOT = 12;
	public static final int GPC = 13;
	public static final int GPS = 14;

	public static final int cutoff = INIT_OP_OFF;

	public static final String[] names = { "RUN", "INIT", "INIT_OP_ON", "INIT_OP_XOT_ON", "ACC", "ACC_XOT", "COT", "GP",
			"INIT_OP_OFF", "INIT_OP_XOT_OFF", "IPM", "IPM_OP", "IPM_OP_XOT", "GPC", "GPS" };
	public static final int size = names.length;
}
