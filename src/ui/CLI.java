package ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Metadata;
import protocols.*;
import protocols.struct.Party;

public class CLI {
	public static final int DEFAULT_PORT = 8000;
	public static final String DEFAULT_IP = "localhost";

	public static void main(String[] args) {
		// Setup command line argument parser
		Options options = new Options();
		options.addOption("config", true, "Config file");
		options.addOption("forest", true, "Forest file");
		options.addOption("eddie_ip", true, "IP to look for eddie");
		options.addOption("debbie_ip", true, "IP to look for debbie");
		options.addOption("protocol", true, "Algorithim to test");
		options.addOption("pipeline", false, "Whether to do pipelined eviction");

		// Parse the command line arguments
		CommandLineParser cmdParser = new GnuParser();
		CommandLine cmd = null;
		try {
			cmd = cmdParser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		// Global.pipeline = cmd.hasOption("pipeline");

		String configFile = cmd.getOptionValue("config", "config.yaml");
		String forestFile = cmd.getOptionValue("forest", null);

		String party = null;
		String[] positionalArgs = cmd.getArgs();
		if (positionalArgs.length > 0) {
			party = positionalArgs[0];
		} else {
			try {
				throw new ParseException("No party specified");
			} catch (ParseException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		int extra_port = 1;
		int eddiePort1 = DEFAULT_PORT;
		int eddiePort2 = eddiePort1 + extra_port;
		int debbiePort = eddiePort2 + extra_port;

		String eddieIp = cmd.getOptionValue("eddie_ip", DEFAULT_IP);
		String debbieIp = cmd.getOptionValue("debbie_ip", DEFAULT_IP);

		Class<? extends Protocol> operation = null;
		String protocol = cmd.getOptionValue("protocol", "retrieve").toLowerCase();

		if (protocol.equals("acc")) {
			operation = Access.class;
		} else if (protocol.equals("cot")) {
			operation = SSCOT.class;
		} else if (protocol.equals("gpc")) {
			operation = GenPermConcat.class;
		} else if (protocol.equals("gps")) {
			operation = GenPermShare.class;
		} else if (protocol.equals("op")) {
			operation = OblivPermute.class;
		} else if (protocol.equals("ipm")) {
			operation = InitPosMap.class;
		} else if (protocol.equals("gp")) {
			operation = GetPointer.class;
		} else if (protocol.equals("xot")) {
			operation = SSXOT.class;
		} else if (protocol.equals("init")) {
			operation = Initialize.class;
		} else if (protocol.equals("run")) {
			operation = RunSqrtOram.class;
		} else if (protocol.equals("rtv")) {
			operation = RunSqrtOram.class;
		} else {
			System.out.println("Protocol " + protocol + " not supported");
			System.exit(-1);
		}

		Constructor<? extends Protocol> operationCtor = null;
		try {
			operationCtor = operation.getDeclaredConstructor(Communication.class, Communication.class, Metadata.class);
		} catch (NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
		}

		// For now all logic happens here. Eventually this will get wrapped
		// up in party specific classes.
		System.out.println("Starting " + party + "...");

		Metadata md = new Metadata(configFile);
		int numComs = 1; // Global.pipeline ? md.getNumLevels() + 1 : 1;
		Communication[] con1 = new Communication[numComs];
		Communication[] con2 = new Communication[numComs];

		if (party.equals("eddie")) {
			System.out.print("Waiting to establish debbie connections...");
			for (int i = 0; i < numComs; i++) {
				con1[i] = new Communication();
				con1[i].start(eddiePort1);
				eddiePort1 += 3;
				while (con1[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			System.out.print("Waiting to establish charlie connections...");
			for (int i = 0; i < numComs; i++) {
				con2[i] = new Communication();
				con2[i].start(eddiePort2);
				eddiePort2 += 3;
				while (con2[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			for (int i = 0; i < numComs; i++) {
				con1[i].setTcpNoDelay(true);
				con2[i].setTcpNoDelay(true);
			}

			try {
				Protocol p = operationCtor.newInstance(con1[0], con2[0], md);
				// if (protocol.equals("rtv"))
				// ((RunSqrtOram) p).setCons(con1, con2);
				p.run(Party.Eddie, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		} else if (party.equals("debbie")) {
			System.out.print("Waiting to establish eddie connections...");
			for (int i = 0; i < numComs; i++) {
				con1[i] = new Communication();
				InetSocketAddress addr = new InetSocketAddress(eddieIp, eddiePort1);
				con1[i].connect(addr);
				eddiePort1 += 3;
				while (con1[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			System.out.print("Waiting to establish charlie connections...");
			for (int i = 0; i < numComs; i++) {
				con2[i] = new Communication();
				con2[i].start(debbiePort);
				debbiePort += 3;
				while (con2[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			for (int i = 0; i < numComs; i++) {
				con1[i].setTcpNoDelay(true);
				con2[i].setTcpNoDelay(true);
			}

			try {
				Protocol p = operationCtor.newInstance(con1[0], con2[0], md);
				// if (protocol.equals("rtv"))
				// ((RunSqrtOram) p).setCons(con1, con2);
				p.run(Party.Debbie, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		} else if (party.equals("charlie")) {
			System.out.print("Waiting to establish eddie connections...");
			for (int i = 0; i < numComs; i++) {
				con1[i] = new Communication();
				InetSocketAddress addr = new InetSocketAddress(eddieIp, eddiePort2);
				con1[i].connect(addr);
				eddiePort2 += 3;
				while (con1[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			System.out.print("Waiting to establish debbie connections...");
			for (int i = 0; i < numComs; i++) {
				con2[i] = new Communication();
				InetSocketAddress addr = new InetSocketAddress(debbieIp, debbiePort);
				con2[i].connect(addr);
				debbiePort += 3;
				while (con2[i].getState() != Communication.STATE_CONNECTED)
					;
			}
			System.out.println(" done!");

			for (int i = 0; i < numComs; i++) {
				con1[i].setTcpNoDelay(true);
				con2[i].setTcpNoDelay(true);
			}

			try {
				Protocol p = operationCtor.newInstance(con1[0], con2[0], md);
				// if (protocol.equals("rtv"))
				// ((RunSqrtOram) p).setCons(con1, con2);
				p.run(Party.Charlie, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		} else {
			throw new NoSuchPartyException(party);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < numComs; i++) {
			con1[i].stop();
			con2[i].stop();
		}

		System.out.println(party + " exiting...");
	}
}
