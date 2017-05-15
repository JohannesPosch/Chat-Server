package server_login;

import java.net.InetAddress;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Objects;

import org.mindrot.jbcrypt.BCrypt;

import simpleServer.util.Database_base;
import util.KeyGen;

public class Database extends Database_base {
	
	public Database(String db_url) {
		super(db_url);
	}
	
	public boolean validateLogin(final String nickname, final String password) throws SQLException{
		
		final String SQLLookupUser = "select salt, pw from simplechat.account where nickname=?";
		
		Objects.requireNonNull(password);
		Objects.requireNonNull(nickname);

		PreparedStatement stmnt = null;
		ResultSet result = null;
		
		boolean successfullLogin = false;
		
		stmnt = this.getConnection().prepareStatement(SQLLookupUser);
		stmnt.setString(1, nickname);	
		result = stmnt.executeQuery();
		
		if(!result.next()){
			result.close();
			stmnt.close();
			return false;
		}
		
		String salt = result.getString("salt");
		
		String inputPWHash = BCrypt.hashpw(password, salt);
		
		if(inputPWHash.equals(result.getString("pw")))
			successfullLogin = true;
		
		result.close();
		stmnt.close();
		return successfullLogin;
	}
	
	public int getUserIDByNickname(final String nickname) throws SQLException{
		final String SQLGetID = "select ID from simplechat.account where nickname=?";
		
		if(nickname == null || nickname.isEmpty())
			throw new IllegalArgumentException();
		
		PreparedStatement stmnt = null;
		ResultSet result = null;
		int retVal = 0;
		
		stmnt = this.getConnection().prepareStatement(SQLGetID);
		stmnt.setString(1, nickname);
		result = stmnt.executeQuery();
		
		if(!result.next()){
			result.close();
			stmnt.close();
			return -1;
		}
		retVal = result.getInt("ID");
		
		result.close();
		stmnt.close();
		
		return retVal;
	}
	
	public int getUserIDByToken(final String token) throws SQLException{
		final String SQLGetID = "select ID from simplechat.session where sessionkey=?";
		
		if(token == null || token.isEmpty())
			throw new IllegalArgumentException();

		
		PreparedStatement stmnt = null;
		ResultSet result = null;
		int retVal = 0;
		
		stmnt = this.getConnection().prepareStatement(SQLGetID);
		stmnt.setString(1, token);
		result = stmnt.executeQuery();
		
		if(!result.next()){
			result.close();
			stmnt.close();
			return -1;
		}
		
		retVal = result.getInt("ID");
		
		result.close();
		stmnt.close();		
		return retVal;
	}
	
	public String generateSession (final InetAddress IP, final String nickname) throws SQLException{
//		final String SQLUpdateUserState = "update simplechat.session set sessionkey=?, leastUse=? where user=?";
//		final String SQLUpdateUserState = "insert into simplechat.session (IP, sessionkey, leastUse, user) values (?,?,?,?) where user=? ON DUPLICATE KEY UPDATE sessionkey=?, leastUse=?";
		final String SQLUpdateUserState = "{? = call UpdateSession(?,?,?,?)}";
		Objects.requireNonNull(IP);
		
		if(nickname == null || nickname.isEmpty())
			throw new IllegalArgumentException();
		
		CallableStatement  stmnt = null;
		
		String SessionKey = KeyGen.generateSessionKey(50);
		Timestamp time = new Timestamp(System.currentTimeMillis());
		int id = this.getUserIDByNickname(nickname);
		
		if(id == 0)
			return "";
		
		
		stmnt = this.getConnection().prepareCall(SQLUpdateUserState);
		stmnt.registerOutParameter(1, Types.INTEGER);
		stmnt.setString(2, SessionKey);
		stmnt.setTimestamp(3, time);
		stmnt.setInt(4, id);
		stmnt.setString(5, IP.getHostAddress());
		stmnt.execute();
		
		if(stmnt.getInt(1) == 0)
			return "";
		return SessionKey;
	}
	
	public boolean checkTokenValidity(final String token, final int valid_for_minutes) throws SQLException, NullPointerException{
		final String SQLGetTimeStamp = "select leastuse from simplechat.session where sessionkey=?";
		
		if(token != null)
			return false;
		
		if(valid_for_minutes <= 0)
			throw new IllegalArgumentException();
		
		Timestamp lastValidTime = new Timestamp(System.currentTimeMillis() - valid_for_minutes * 60000);
		
		PreparedStatement stmnt = null;
		ResultSet result = null;
		
		stmnt = this.getConnection().prepareStatement(SQLGetTimeStamp);
		stmnt.setString(1, token);
		result = stmnt.executeQuery();
		
		if(!result.next()){
			result.close();
			stmnt.close();
			return false;
		}
			
		Timestamp leastUse = result.getTimestamp("leastuse");
		
		result.close();
		stmnt.close();
		
		if(lastValidTime.compareTo(leastUse) == -1)
			return true;
		else
			this.invalidadeToken(token);
		return false;
	}
	
	public boolean invalidadeToken(final String token) throws SQLException{
		// final String SQLInvalidade = "update simplechat.session set sessionkey=?, ip_address=?, leastUse=? where user=?";
		final String SQLInvalidade = "delete from simplechat.session where sessionkey=?";
		
		if(token == null || token.isEmpty())
			return false;
		
		PreparedStatement stmnt = null;
		int result = 0;
		
		stmnt = this.getConnection().prepareStatement(SQLInvalidade);
//		stmnt.setString(1, null);
//		stmnt.setString(2, null);
//		stmnt.setTimestamp(3, null);
		
		stmnt.setString(1,  token);
		
//		int id = this.getUserIDByToken(token);
//		
//		if(id == -1){
//			stmnt.close();
//			return false;
//		}
//		
//		stmnt.setInt(4, id);
		
		result = stmnt.executeUpdate();
		
		stmnt.close();
		
		return (result == 1);
	}
	
	public boolean createNewUser(final String vn, final String sn, final String nickname, final String password) throws SQLException{
		
		if(vn == null || vn.isEmpty())
			return false;
		
		if(sn == null || sn.isEmpty())
			return false;
		
		if(nickname == null || nickname.isEmpty())
			return false;
		
		if(password == null || nickname.isEmpty())
			return false;
		
		if(this.getUserIDByNickname(nickname) != -1)
			return false;
		
		final String SQLCreateNew = "insert into simplechat.account (vn, sn, nickname, pw, salt) value (?,?,?,?,?)";
		final String SQLDropUser = "delete from simplechat.account where nickname=?";
		
		PreparedStatement stmnt = null;
		int result = 0;
		
		stmnt = this.getConnection().prepareStatement(SQLCreateNew);
		stmnt.setString(1, vn);
		stmnt.setString(2, sn);
		stmnt.setString(3, nickname);
		
		String salt = BCrypt.gensalt(5);
		
		stmnt.setString(4, BCrypt.hashpw(password, salt));
		stmnt.setString(5, salt);
		
		result = stmnt.executeUpdate();
		
		if(result == 0){
			stmnt.close();
			return false;
		}
		
		stmnt.close();
		return true;
		
	}
}
