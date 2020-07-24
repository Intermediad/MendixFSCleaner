package com.intermediad;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MendixFSCleaner {
	
	private static final String FILECHECK_SQL = "SELECT id FROM system$filedocument WHERE ";
	private static Config config;
	private static BufferedWriter writer = null;
	private static Pattern numberPattern = Pattern.compile("-?\\d+(\\.\\d+)?");
	
	public static void main(String[] args) {
		
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {}
		
		System.out.println("Starting MendixFSCleaner 1.1.0 - 2020-07-24 13:56\n");
		
		// check for elevated permissions
		try {
			File adminFile = new File(System.getenv("programfiles")+"/test.tst");
			boolean isAdmin = adminFile.createNewFile();
			if (isAdmin) {
				adminFile.delete();
			}
		} catch (IOException ioe) {
			System.out.println("Please run in command prompt with elevated permissions. \n\nUse: Start > Run > Command prompt (right click 'Run as Administrator')");
			return;
		}
		
		// check commandline args
		config = new Config(args);		
		if (!config.isValid()) return;
		System.out.println("Logging enabled: " + (config.getLog()));
		System.out.println("Delete enabled: " + (config.getDelete()));
		System.out.println();
		
		// get database connection
		Connection conn = getDbConnection();
		if (conn == null) return;
				
		long totalFiles = 0, processedFiles = 0, foundDBFiles = 0, missingDBFiles = 0;
		double fileSize = 0;
		
		try {
			// open stream for all files in path recursively
			Stream<Path> pathStream = Files
					.walk(Paths.get(config.getPath()))
			        .filter(Files::isRegularFile);
			
			// get total file count
			totalFiles = Files
					.walk(Paths.get(config.getPath()))
			        .filter(Files::isRegularFile)
			        .count();
			
			if (config.getDelete()) {				
				Console console = System.console();
				boolean confirmDelete = false;
				
				System.out.println("Found a total of " + totalFiles + " files.\n");
				
				while (!confirmDelete) {
					String answer = new String(console.readLine("> Are you sure you want to delete orphaned files? (Y/N) "));
					if (answer.toUpperCase().equalsIgnoreCase("Y")) {
						confirmDelete = true;
						System.out.println();
					} else if (answer.toUpperCase().equalsIgnoreCase("N")) {
						System.out.println("Cleanup cancelled\n");
						return;
					} else {
						System.out.print('\r');
					}
				}		        
			}			
						
		    for (Path path : (Iterable<Path>) pathStream::iterator) {
		    	if (checkFileInDB(conn, path)) {
		    		foundDBFiles++;
		    	} else {
		    		missingDBFiles++;
		    		String filename = path.getParent().getFileName() + "/" + path.getParent().getParent().getFileName() + "/" + path.getFileName();
		    		if (!writeFileToLog(filename)) return;		    		
		    		File file = path.toFile();
		    		fileSize += file.length();		    		
		    		// delete file if enabled
		    		if (config.getDelete()) {
		    			file.delete();
		    		}
		    	}
		    	processedFiles++;
		    	if (processedFiles % 500 == 0) {
		    		System.out.print('\r');
		    		System.out.printf("Processed %6d of %6d files", processedFiles, totalFiles);
		    	}
		    }
		    
		    if (missingDBFiles == 0) writeFileToLog("No missing file associations found");
		    if (writer != null) writer.close();
		    
		    System.out.print('\r');
		    System.out.println("Finished processing\n");
		    System.out.println("================================");
		    
		    System.out.printf("Total on filsystem:\t%6d\n", totalFiles);
		    System.out.printf("Found in database:\t%6d\n", foundDBFiles);
		    System.out.printf("Missing in database:\t%6d\n", missingDBFiles);
		    
		    fileSize = fileSize / 1024 / 1024;
		    
		    System.out.printf("Size of missing files:\t%6.2f mb\n", fileSize);

		    System.out.println("================================");
		} catch (Exception e) {
			System.out.println("Error occurred: " + e.getMessage());
			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
			  for (int i = 1; i < elements.length; i++) {
			    StackTraceElement s = elements[i];
			    System.out.println("\tat " + s.getClassName() + "." + s.getMethodName()
			        + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
			  }
		}
	}
	
	public static boolean checkFileInDB(Connection conn, Path path) {
		String filename = path.getFileName().toString();
		boolean result = false;		
		int fileId = -1;		
		String sql = FILECHECK_SQL;
		
		if (isNumeric(filename)) {
			fileId = Integer.parseInt(filename);
			sql += "fileid = ?";
		} else {
			sql += "__uuid__ = ?";
		}
		
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			
			if (fileId != -1) {
				stmt.setInt(1, fileId);
			} else {
				stmt.setString(1, filename);
			}
			
			ResultSet resultSet = stmt.executeQuery();
			
			result = resultSet.next();
			
		} catch (SQLException sqle) {
			System.out.println(sqle.getMessage());
		}
		return result;
	}
	
	public static Connection getDbConnection() {
		Connection conn = null;
		String url = "jdbc:postgresql://" + config.getDbhost() + ":" + config.getDbport() + "/" + config.getDbname();
		
		try {
        	conn = DriverManager.getConnection(url, config.getDbuser(), config.getDbpass());
        	
        	if (conn.isValid(5)) {
        		DatabaseMetaData mtdt = conn.getMetaData();        	
            	System.out.println("Connected to: " + mtdt.getURL() + '\n');
        	}
		} catch (Exception e) {
			System.out.println("Eror while trying to connect to database: " + e.getMessage());
		}
				
		return conn;
	}
	
	public static boolean writeFileToLog(String filename) {
		// open logfile if enabled				
		if (config.getLog()) {
			// initate log if enabled
			try {
				if (writer == null) {
					writer = new BufferedWriter(new FileWriter("missing_files.log"));
				}
				writer.write(filename + "\n");
			} catch (IOException ioe) {
				System.out.println("Error occurred while trying to write to logfile: " + ioe.getMessage());
				return false;
			}	
		}
		return true;
	}
		
	public static boolean testSqlConnection() {
        // auto close connection

    	Connection conn = getDbConnection();
        if (conn != null) {
            System.out.println("Connected to the database!");
            return true;
        } else {
            System.out.println("Failed to make connection!");
        }
        
        return false;
	}
	
	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return numberPattern.matcher(strNum).matches();
	}
}


	