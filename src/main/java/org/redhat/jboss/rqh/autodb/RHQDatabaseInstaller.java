/**
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.redhat.jboss.rqh.autodb;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;


/**
 * @author Romain PELISSE - belaran@gmail.com
 *
 */
public class RHQDatabaseInstaller {

	
	private static final String PROG_NAME = "rhq-db-installer";
	
	private static final String DB_PASSWORD_FIELDNAME = "propForm:databasepassword";
	private static final String DB_CONNECTION_URL_FIELDNAME = "propForm:databaseconnectionurl";
	private static final String DB_DRIVER_FIELDNAME = "propForm:databasedriverclass";
	private static final String DB_USERNAME_FIELDNAME = "propForm:databaseusername";
	private static final String DB_XA_DATASOURCE_FIELDNAME ="propForm:databasexadatasourceclass";
	
	private static final Map<String, String> DEFAULT_VALUES;

	private static final int DEFAULT_TIMEOUT = 3000; // 3s

	private static final int WAIT_FOR_JON_SETUP_TIMEOUT = 30000; // 30s
	
	static {
		DEFAULT_VALUES = new HashMap<String, String>();
		DEFAULT_VALUES.put(DB_DRIVER_FIELDNAME, "org.postgresql.Driver");
		DEFAULT_VALUES.put(DB_CONNECTION_URL_FIELDNAME, "jdbc:postgresql://127.0.0.1:5432/rhq");
		DEFAULT_VALUES.put(DB_XA_DATASOURCE_FIELDNAME, "org.postgresql.xa.PGXADataSource");
	}
	
	@Data
	final class Arguments {

		@Parameter(names = { "-s", "--server-url" }, description = "URL to RHQ Server")
		private String rhqServerUrl;

		@Parameter(names = { "-d", "--db-host-url" }, description = "Database URL (ex: jdbc:postgresql://127.0.0.1:5432/rhq)")
		private String dbURL;
		
		@Parameter(names = { "-u", "--db-username" }, description = "Database Username")
		private String dbUsername;
		
		@Parameter(names = { "-p", "--db-password" }, description = "Database password")
		private String dbPassword;
		
		@Parameter(names = { "-D", "--db-driver" }, description = "Driver class for database")		
		private String dbDriver;
		
		@Parameter(names = { "-x", "--db-xa-datasource-class" }, description = "Class for XA datasource.")
		private String dbXADatasourceClass;
		
		@Parameter(names = { "-t", "--test-db-connection" }, description = "Test if database connection is working before triggering the install")
		private boolean testConnection = false;
				
		@Parameter(names = { "-h", "--help", "-?" }, description = "Display help")
		private boolean help = false;
		
		@Parameter(names = { "-v", "--verbose", "--debug"}, description = "Verbose mode (obviously)")
		private boolean verbose = false;
	}

	private static Arguments extractParameters(String[] args) {
		Arguments arguments = new RHQDatabaseInstaller().new Arguments();
		JCommander jcommander = null;
		try {
			jcommander = new JCommander(arguments, args);
			jcommander.setProgramName(PROG_NAME);
			if (arguments.isHelp()) {
				jcommander.usage();
				System.exit(0);
			}
		} catch (ParameterException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		return arguments;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException, IOException {
		Arguments arguments = setDefaultValues(validateArgs(RHQDatabaseInstaller.extractParameters(args)));
		consolePrint("Setting up database for RHQ Server running on: " + arguments.getRhqServerUrl() );
		if ( arguments.isVerbose() )
			printSetting(arguments);
	    final HtmlForm form = ((HtmlPage) new WebClient().getPage(arguments.getRhqServerUrl())).getFormByName("propForm");
	    updatePasswordFieldInput(DB_PASSWORD_FIELDNAME,arguments.getDbPassword(), form);
	    updateTextFieldInput(DB_CONNECTION_URL_FIELDNAME,arguments.getDbURL(), form);
	    updateTextFieldInput(DB_DRIVER_FIELDNAME,arguments.getDbDriver(), form);
	    updateTextFieldInput(DB_USERNAME_FIELDNAME, arguments.getDbUsername(), form);			    
	    updateTextFieldInput(DB_XA_DATASOURCE_FIELDNAME,arguments.getDbXADatasourceClass(),form);
	    consolePrint("Waiting for installer to finish... ",true);
	    long startTime = System.currentTimeMillis();
	    WebResponse response = form.getInputByName("propForm:save").click().getWebResponse();	    
	    consolePrint("[execution time: " + (System.currentTimeMillis() - startTime) + "ms]");
	    consolePrint("Finished with HTTP code:" +  response.getStatusCode());
	    consolePrint("Waiting for RHQ Server to finish its installation [" + WAIT_FOR_JON_SETUP_TIMEOUT + "]... ", true);
	    if ( response.getStatusCode() != 200 ) {
	    	consolePrint("Error occured during DB setup");
	    	System.exit(1);
	    }
	    System.exit(checkIfServerIsInstalled(arguments.getRhqServerUrl()));
	}

	private static int checkIfServerIsInstalled(String rhqServerUrl) {
		try {
			Document doc = Jsoup.parse(new URL(rhqServerUrl), DEFAULT_TIMEOUT);
			Element firstTitle = doc.select("h1").first();
			if ( firstTitle != null && "".equals(firstTitle.text()) ) {
				consolePrint("DB setup for server " + rhqServerUrl + " done.");
				return 0;
			} else {
				consolePrint("DB setup for server " + rhqServerUrl + " failed.");
				return 1;
			}
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Malformed URL to RHQ Server - something quite wrong has happened here... " + rhqServerUrl);
		} catch (IOException e) {
			throw new IllegalStateException("Can't connect to RHQ Server: " + e.getMessage());
		}		
	}
	
	private static Arguments validateArgs(Arguments arguments) {
		valideValue(arguments.getRhqServerUrl(),"URL to RHQ Server is required.");
		checkRhqServerAvailability(arguments.getRhqServerUrl());
		valideValue(arguments.getDbUsername(),"DB Username is required.");
		valideValue(arguments.getDbPassword(),"DB Password is required.");				
		return arguments;
	}

	private static void checkRhqServerAvailability(String rhqServerUrl) {
		// TODO Implements this		
	}

	private static void valideValue(String value, String message) {
		if ( "".equals(value) ) {
			consolePrint(message);
			System.exit(1);
		}
	}
	
	private static void printSetting(Arguments arguments) {
		consolePrint("Database URL: " + arguments.getDbURL());
		consolePrint("Database Username: " + arguments.getDbUsername());
		consolePrint("Database Password: " + arguments.getDbPassword());
		consolePrint("Database Driver: " + arguments.getDbDriver());
		consolePrint("Database XA Datasource: " + arguments.getDbXADatasourceClass());		
	}

	private static Arguments setDefaultValues(Arguments arguments) {
		if ( isEmptyOrNull(arguments.getDbDriver()) )
			arguments.setDbDriver(DEFAULT_VALUES.get(DB_DRIVER_FIELDNAME));
		if ( isEmptyOrNull(arguments.getDbXADatasourceClass()) )
			arguments.setDbXADatasourceClass(DEFAULT_VALUES.get(DB_XA_DATASOURCE_FIELDNAME));
		if ( isEmptyOrNull(arguments.getDbURL()) )
			arguments.setDbURL(DEFAULT_VALUES.get(DB_CONNECTION_URL_FIELDNAME));
		
		if ( arguments.isTestConnection() )
			throw new UnsupportedOperationException("Test connection is not yet supported.");
		return arguments;		
	}

	private static boolean isEmptyOrNull(String value) {
		return ( value == null || "".equals(value) ) ? true : false;
	}
	
	private static void consolePrint(String string, boolean printCarriageReturn) {
		if ( printCarriageReturn )
			System.out.println(string);	//NOPMD
		else
			System.out.print(string);	//NOPMD
	}
	
	private static void consolePrint(String string) {
		consolePrint(string, true);
	}

	public static void updateTextFieldInput(String textInputId, String newValue, HtmlForm form) {
		final HtmlInput input = form.getInputByName(textInputId);
	    input.setValueAttribute(newValue);
	}

	public static void updatePasswordFieldInput(String textInputId, String newValue, HtmlForm form) {
		final HtmlPasswordInput input =  (HtmlPasswordInput) form.getInputByName(textInputId);
	    input.setValueAttribute(newValue);
	}	
}
