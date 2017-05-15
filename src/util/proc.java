package util;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;

import server_chat.ChatServer;
import server_login.LoginServer;
import simpleMenu.MenuCmd;
import simpleMenu.MenuStruct;
import simpleServer.exception.IllegalPortException;
import simpleServer.exception.RunningServerException;
import simpleServer.server.Server;
import simpleServer.util.Constant;
import simpleServer.util.ServerControl;


public class proc {

	public static Scanner proj_scan = new Scanner(System.in);
	
	public static void main(String[] args) {
		
		MenuCmd menu = new MenuCmd(proj_scan);
		ServerControl server_controller = new ServerControl();
		LoginServer login = null;
		ChatServer testSrv = null;
		
		final MenuStruct mainMenu = new MenuStruct();
		final MenuStruct tmpMenu = new MenuStruct();
		
		
		
		mainAction menuChoose = mainAction.ChangePort;
		boolean runSoftware = true;
		boolean stopFail = false;
		
		menu.setPrintUnderline(true);
		menu.setUnderlineChar('=');
		
		mainMenu.Title = "Menu for SAP-Server";
		mainMenu.entries = new String[]{"Change serverport", "Server status", "Start all servers", "Start one server", "Stop all servers", "Stop one Server", "Quit"};
		mainMenu.minMenuPoint = mainAction.firstIndex;
		
		
		try {
			login = new LoginServer("LoginServer", 9123);
			testSrv = new ChatServer("TestServer", 9124);
		} catch (NullPointerException | IllegalPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		server_controller.addServer(login);
		server_controller.addServer(testSrv);
		
		do{
			menuChoose = mainAction.fromInteger(menu.printMenu(mainMenu));
			
			switch(menuChoose){
			case ChangePort:
				tmpMenu.Title = "Choose a server";
				tmpMenu.entries = server_controller.getServerNameList();
				tmpMenu.minMenuPoint = 0;
				
				try{
					setPort(server_controller.getServer(menu.printMenu(tmpMenu)));
				}catch(IndexOutOfBoundsException ex){
					System.out.println("[WARNING] Invalid Server choice");
				}
				break;
			case ServerStatus:
				server_controller.printServerInformation();
				break;
			case startAll:
				try {
					server_controller.startAllServers();
				} catch (IOException e1) {
					System.err.println("[ERROR] Could not start servers");
				}
				break;
			case startOne:
				tmpMenu.Title = "Choose which server to start";
				tmpMenu.entries = server_controller.getServerNameList();
				tmpMenu.minMenuPoint = 0;
				
				try{
					server_controller.startSpecificServer(menu.printMenu(tmpMenu));
				}catch(IndexOutOfBoundsException ex){
					System.out.println("[WARNING] Invalid Server choice");
				} catch (IOException e) {
					System.err.println("[ERROR] Could not start server");
				}
				
				break;
			case stopAll:
				try {
					server_controller.stopAllServers();
				} catch (IOException e) {
					System.err.println("[ERROR] Could not stop servers");
					runSoftware = false;
					stopFail = true;
				}
				break;
			case stopOne:
				tmpMenu.Title = "Choose which server to stop";
				tmpMenu.entries = server_controller.getServerNameList();
				tmpMenu.minMenuPoint = 0;
				
				try{
					server_controller.stopSpecificServer(menu.printMenu(tmpMenu));
				}catch(IndexOutOfBoundsException ex){
					System.out.println("[WARNING] Invalid Server choice");
				}catch(IOException ex){
					System.err.println("[ERROR] Could not stop server");
					runSoftware = false;
					stopFail = true;
				}
				break;
				
			case quit:
				runSoftware = false;
				break;
			case FAULT:
				runSoftware = false;
				System.err.println("[ERROR] Error occured during programm");
				break;
			case UNDEFINED:
				System.out.println("[WARNING] Undefined input");
				break;
			default:
				runSoftware = false;
				System.err.println("[ERROR] Error undefined behaviour");
				break;
			};
		System.out.println();
		}while(runSoftware);
		
		try {
			server_controller.stopAllServers();
		} catch (IOException e) {
			if(!stopFail)
				System.err.println("[ERROR] Could not stop server");
		}
		
		proj_scan.close();
	}
	
	public static void setPort(Server srv){
		
		int port = 0;
		boolean rerun = false;
		
		do{
			rerun = false;
			System.out.println("Set new Serverport Range: <" + Constant.getMinServerPort() + " - " + Constant.getMaxServerPort() + ">");
			System.out.print(">> ");
			
			try{
				port = proj_scan.nextInt();
			}catch(InputMismatchException ex){
				System.err.println("[ERROR] Input not of type integer");
			}catch(Exception ex){
				System.err.println("[ERROR] Unable to open inputStream");
			};
				proj_scan.nextLine();
			try{
				srv.setPort(port);
			}catch(IllegalPortException ex){
				System.out.println("[ERROR] Invalid Port number");
				rerun = true;
			}catch(RunningServerException ex){
				System.out.println("[ERROR] Server running, unable to change port");
				rerun = false;
			}
			
		}while(rerun);
	}
}
