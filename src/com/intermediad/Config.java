package com.intermediad;

import java.io.Console;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Config {
	private String dbhost;
	private String dbname;
	private String dbuser;
	private String dbpass;
	private int dbport = 5432;
	private String path;
	private boolean log = false;
	private boolean delete = false;
	
	private boolean isValid = false;
	
	private Options options;
	
	public Config() {
		setOptions();
	}
	
	public Config(String[] args) {
		setOptions();
		processArgs(args);
	}
	
	public String getDbhost() {
		return dbhost;
	}
	public String getDbname() {
		return dbname;
	}
	public String getDbuser() {
		return dbuser;
	}
	public String getDbpass() {
		return dbpass;
	}
	public int getDbport() {
		return dbport;
	}
	public String getPath() {
		return path;
	}
	public boolean isValid() {
		return isValid;
	}
	public boolean getLog() {
		return log;
	}
	public boolean getDelete() {
		return delete;
	}
				
	@Override
	public String toString() {
		return "Config [dbhost=" + dbhost + ", dbname=" + dbname + ", dbuser=" + dbuser + ", dbpass=" + dbpass
				+ ", dbport=" + dbport + ", path=" + path + "]";
	}

	public void processArgs(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.setWidth(100);	    
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			dbhost = cmd.getOptionValue("h");
			dbname = cmd.getOptionValue("d");
			dbuser = cmd.getOptionValue("u");
			path = cmd.getOptionValue("path");
			if (cmd.hasOption("P")) {
				dbport = Integer.parseInt(cmd.getOptionValue("P"));
			}
			log = cmd.hasOption("log");
			delete = cmd.hasOption("delete");
			
			Console console = System.console();
	        String password = new String(console.readPassword("Password: "));
		
			if (password.equalsIgnoreCase("")) {
				isValid = false;
			} else {
				dbpass = password;
				isValid = true;
			}			
			
		} catch (ParseException pe) {
			System.out.println(pe.getMessage());
			formatter.printHelp("MendixFSCleaner", options);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void setOptions() {
		options = new Options();
		
		options.addOption(Option.builder( "h" )
                .hasArg()
                .desc( "Database hostname" )
                .required()
                .argName( "dbhost" )
                .build());
		options.addOption(Option.builder( "d" )
                .hasArg()
                .desc( "Database name" )
                .required()
                .argName( "dbname" )
                .build());
		options.addOption(Option.builder( "u" )
                .hasArg()
                .desc( "Database username" )
                .required()
                .argName( "dbuser" )
                .build());
		options.addOption(Option.builder( "P" )
                .hasArg()
                .desc( "Database port - defaults to 5432" )
                .argName( "dbport" )
                .build());
		options.addOption(Option.builder( "path" )
                .hasArg()
                .desc( "Path to Mendix files (ie c:\\mx\\someproject\\deployment\\data\\files" )
                .required()
                .argName( "filepath" )
                .build());
		options.addOption(Option.builder( "log" )
                .desc( "Log missing files to missing_files.log?" )
                .argName( "log" )
                .build());
		options.addOption(Option.builder( "delete" )
                .desc( "Delete file missing in database from filesystem" )
                .hasArg(false)
                .argName( "delete" )
                .build());
	}
}
