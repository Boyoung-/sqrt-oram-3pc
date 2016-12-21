package util;

public class P {

	public static final int INIT = 0;
	public static final int OP_ON = 1;
	public static final int OP_XOT_ON = 2;
	public static final int ACC = 3;
	public static final int COT = 4;
	public static final int ACC_XOT = 5;

	public static final int OP_OFF = 6;
	public static final int OP_XOT_OFF = 7;
	public static final int GPC = 8;
	public static final int IPM = 9;
	public static final int GPS = 10;

	public static final String[] names = { "INIT", "OP_ON", "OP_XOT_ON", "ACC", "COT", "ACC_XOT", "OP_OFF",
			"OP_XOT_OFF", "GPC", "IPM", "GPS" };
	public static final int size = names.length;
}
