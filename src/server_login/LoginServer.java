package server_login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import server_chat.ChatServerAction;
import simpleServer.exception.IllegalPortException;
import simpleServer.server.InsecureServer;
import simpleServer.util.NetworkService;

public class LoginServer extends InsecureServer {
	
	private NetworkService worker;	
	
	private final String DB_USER = "LoginServer";
	private final String DB_PASS = "LogChatv1sadioncorp";
	private final String DB_URL = "jdbc:mysql://localhost/simpleChat";
	private static Logger logger = Logger.getLogger("Login-Server");
	
	public LoginServer(final String name, final int port) throws NullPointerException, IllegalPortException {
		super(name, port);
		
		worker = null;
	}

	@Override
	protected boolean start_Handler_service() {
		worker = new NetworkService(this.getSSocket(), this::handler);
		try{
			worker.start();
		}catch(IllegalThreadStateException ex){
			return false;
		}
		return true;
	}

	@Override
	protected boolean stop_Handler_service(){
		
		try{
			worker.interrupt();
			//TODO: Wait until thread is finished
		}catch(SecurityException ex){
			return false;
		}
		return true;
	}
	
	private void handler(final Socket sock){
		
		Objects.requireNonNull(sock);
		
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = new JSONObject();
		JSONObject responseObj = new JSONObject();
		Database db = new Database(this.DB_URL);
		
		BufferedReader in = null;
		PrintWriter printer = null;
		
		boolean success = false;
		int err_code = 0;
		String readBuf = "";
		
		try {
			db.openConnection(this.DB_USER, this.DB_PASS);
		} catch(SQLException ex){
			logger.log(Level.SEVERE, "could not connect to the database", ex);
			err_code = 20;
		} catch (ClassNotFoundException ex) {
			logger.log(Level.SEVERE, "server failure during connection to the database", ex);
			err_code = 21;
		}
		
		if(err_code == 0){
			try{
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				readBuf = in.readLine();
			}catch(IOException ex){
				logger.log(Level.SEVERE, "could not read from network stream at address: " + sock.getInetAddress(), ex);
			}
			
			if(!readBuf.isEmpty()){
				try {
					jsonObj = (JSONObject) parser.parse(readBuf);
				} catch (ParseException e1) {
					err_code = 50;
				} catch (NullPointerException ex){
					err_code = 21;
				}
			}
			
			if(err_code == 0){
				LoginServerAction action = null;
				
				try{
					String tmp = (String)jsonObj.get("cmd");
					if(tmp != null)
						action = LoginServerAction.fromString(tmp);
					else
						err_code = 51;
				}catch(ClassCastException ex){
					err_code = 51;
				}catch(NullPointerException ex){
					err_code = 21;
				}
	
				if(err_code == 0){
					responseObj.put("response", action.getText());
					switch(action){
					case LOGIN:
						
						String nickname = "";
						String pw = "";
						
						boolean login_success = false;
						String newKey = "";
						
						try{
							nickname = Objects.requireNonNull((String)jsonObj.get("user"));
							pw = Objects.requireNonNull((String)jsonObj.get("pw"));
						}catch(ClassCastException ex){
							err_code = 21;
						}catch(NullPointerException ex){
							err_code = 51;
						}
						
						if(err_code == 0){
							try{
								login_success = db.validateLogin(nickname, pw);
							}catch(SQLException ex){
								err_code = 20;
							}
							
							if(login_success){
								try {
									newKey = db.generateSession(sock.getInetAddress(), nickname);
								}catch(SQLException ex){
									ex.printStackTrace();
									err_code = 20;
								}
							}else
								err_code = 2;
							
							success = (err_code == 0 && login_success);
							if(err_code == 0 && login_success)
								responseObj.put("token", newKey);
							else
								responseObj.put("token", null);
						}
	
						break;
					case LOGOFF:
						
						String incomingToken = "";
						
						boolean logoff_success = false;
						
						try{
							incomingToken = Objects.requireNonNull((String)jsonObj.get("token"));
						}catch(ClassCastException ex){
							err_code = 21;
						}catch(NullPointerException ex){
							err_code = 51;
						}
						
						if(err_code == 0){
							try {
								logoff_success = db.invalidadeToken(incomingToken);
							} catch (SQLException ex) {
								err_code = 21;
							}
							
							success = (err_code == 0 && logoff_success);
						}
						
						break;
					case REGISTER:
						
						String vn = "";
						String nn = "";
						
						nickname = "";
						pw = "";
						
						boolean validNewUser = false;
						boolean creationSuccess = false;
						
						try{
							vn = Objects.requireNonNull((String)jsonObj.get("firstname"));
							nn = Objects.requireNonNull((String)jsonObj.get("secondname"));
							nickname = Objects.requireNonNull((String)jsonObj.get("user"));
							pw = Objects.requireNonNull((String)jsonObj.get("pw"));
						}catch(ClassCastException ex){
							err_code = 21;
						}catch(NullPointerException ex){
							err_code = 51;
						}
						
						if(err_code == 0){
							
							try {
								validNewUser = (db.getUserIDByNickname(nickname) == -1);
							} catch (SQLException ex) {
								err_code = 20;
							}
							
							if(validNewUser){
								try {
									creationSuccess = db.createNewUser(vn, nn, nickname, pw);
								} catch (SQLException ex) {
									ex.printStackTrace();
									err_code = 20;
								}
								
								success = (err_code == 0 && creationSuccess);
							}else{
								err_code = 1;
							}
						}
						break;
					default:
						err_code = 53;
						break;
					}
				}
			}
		}
		try {
			printer = new PrintWriter(sock.getOutputStream());
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Unable to open Outputstream", ex);
		}
		
		responseObj.put("error", err_code);
		responseObj.put("success", success);
		
		
		printer.println(responseObj.toString());
		printer.flush();
		printer.close();
		
		try {
			db.close();
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, "Failure at closing Database connection", ex);
		}
		try {
			sock.close();
			in.close();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failure at closing Socket connection", ex);
		}
	}
}
