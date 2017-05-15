package server_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import simpleServer.exception.IllegalPortException;
import simpleServer.server.InsecureServer;
import simpleServer.util.NetworkService;

public class ChatServer extends InsecureServer {
	
	private NetworkService worker;
	
	private final String DB_USER = "LoginServer";
	private final String DB_PASS = "LogChatv1sadioncorp";
	private final String DB_URL = "jdbc:mysql://localhost/simpleChat";
	private Logger logger = Logger.getLogger("Chat-Server");

	public ChatServer(final String name, final int port) throws NullPointerException, IllegalPortException {
		super(name, port);
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
	protected boolean stop_Handler_service() {

		try{
			worker.interrupt();
			//TODO: Wait until finished
		}catch(SecurityException ex){
			return false;
		}
		return true;
	}
	
	public void handler(final Socket sock){
		
		Objects.requireNonNull(sock);
		
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = new JSONObject();
		JSONObject responseObj = new JSONObject();
		Database db = new Database(this.DB_URL);
		
		BufferedReader in = null;
		PrintWriter printer = null;
		
		int err_code = 0;
		boolean success = true;
		String readBuf = "";
		
		try {
			db.openConnection(this.DB_USER, this.DB_PASS);
		} catch(SQLException ex){//TODO: add logger
			err_code = 20;
		} catch (ClassNotFoundException ex) {
			//TODO: same
			err_code = 21;
		}
		
		if(err_code == 0){
			
			try{
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				readBuf = in.readLine();
			}catch(IOException ex){
				err_code = 21;
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
				ChatServerAction action = null;
				
				try{
					String tmp = (String)jsonObj.get("cmd");
					if(tmp != null)
						action = ChatServerAction.fromString(tmp);
					else
						err_code = 51;
				}catch(ClassCastException ex){
					err_code = 51;
				}catch(NullPointerException ex){
					err_code = 21;
				}
				
				if(err_code == 0){
					responseObj.put("response", action.getText());
					
					String incomingToken = "";
					
					switch(action){
					case GET_USERS:
						
						incomingToken = "";
						
						try{
							incomingToken = Objects.requireNonNull((String)jsonObj.get("token"));
						}catch(ClassCastException ex){
							err_code = 21;
						}catch(NullPointerException ex){
							err_code = 51;
						}
						
						if(err_code == 0){
							int userId = 0;
							UserInfo[] infos = null;
							
							try {
								userId = db.getUserIDByToken(incomingToken);
							} catch (SQLException ex) {
								err_code = 20;
							}
							
							if(userId > 0){
								try{
									infos = db.getUserList(userId);
								} catch (SQLException ex) {
									ex.printStackTrace();
									err_code = 20;
								}
								
								success = (infos != null);
								if(infos != null){
									JSONArray arr = new JSONArray();
									for(int i = 0; i < infos.length; i++)
										arr.add(UserInfo.toMap(infos[i]));
									responseObj.put("users", arr);
								}
								else{
									responseObj.put("users", null);
								}
							}else{
								err_code = 3;
							}	
						}
						
						break;
					case GET_IP_USER:
						
						incomingToken = "";
						
						try{
							incomingToken = Objects.requireNonNull((String)jsonObj.get("token"));
						}catch(ClassCastException | NullPointerException ex){
							err_code = 21;
						}
						
						if(incomingToken == null)
							err_code = 51;
						
						if(err_code == 0){
							//TODO: Implement get IP
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
