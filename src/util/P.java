package util;

public class P {

	public static final int INIT = 0;
	public static final int INIT_OP_ON = 1;
	public static final int INIT_OP_XOT_ON = 2;
	public static final int ACC = 3;
	public static final int ACC_XOT = 4;
	public static final int COT = 5;
	public static final int GP = 6;

	public static final int INIT_OP_OFF = 7;
	public static final int INIT_OP_XOT_OFF = 8;
	public static final int IPM = 9;
	public static final int IPM_OP = 10;
	public static final int IPM_OP_XOT = 11;
	public static final int GPC = 12;
	public static final int GPS = 13;

	public static final int RUN = 14;

	public static final String[] names = { "INIT", "INIT_OP_ON", "INIT_OP_XOT_ON", "ACC", "ACC_XOT", "COT", "GP",
			"INIT_OP_OFF", "INIT_OP_XOT_OFF", "IPM", "IPM_OP", "IPM_OP_XOT", "GPC", "GPS", "RUN" };
	public static final int size = names.length;
}
