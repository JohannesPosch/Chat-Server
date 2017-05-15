package server_chat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import simpleServer.util.Constant;
import simpleServer.util.Database_base;

public class Database extends Database_base {

	public Database(String db_url) {
		super(db_url);
		// TODO Auto-generated constructor stub
	}
	
	public UserInfo[] getUserList(final int userId) throws SQLException{
		final String SQLLoggedInGetUsers = "select distinct nickname from simplechat.account where id in (select user from simplechat.session where sessionkey is not null && user != ?)";
		final String SQLLoggedOutGetUsers = "select distinct nickname from simplechat.account where id not in (select user from simplechat.session where user = ?)";
		
		if(userId <= 0)
			return null;
		
		ArrayList<UserInfo> list = new ArrayList<UserInfo>();
		PreparedStatement stmnt = this.getConnection().prepareStatement(SQLLoggedInGetUsers);
		stmnt.setInt(1, userId);
		ResultSet result = stmnt.executeQuery();
		
		UserInfo info = null;
		while(result.next()){
			info = new UserInfo();
			info.active = true;
			info.user = result.getString("nickname");
			list.add(info);
		}
		
		result.close();
		stmnt.close();
		
		stmnt = this.getConnection().prepareStatement(SQLLoggedOutGetUsers);
		stmnt.setInt(1, userId);
		result = stmnt.executeQuery();
		
		while(result.next()){
			info = new UserInfo();
			info.active = false;
			info.user = result.getString("nickname");
			list.add(info);
		}
		
		result.close();
		stmnt.close();
		
		UserInfo[] tmp = new UserInfo[list.size()];
		
		tmp = list.toArray(tmp);
		return tmp;
	}
	
	public int getUserIDByToken(final String token) throws SQLException{
		final String SQLGetID = "select ID from simplechat.session where sessionkey=?";
		
		if(token == null || token.isEmpty())
			throw new IllegalArgumentException();

		boolean tokenValid = checkTokenValidity(token, 60); 
		
		if(!tokenValid)
			return -1;
		
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

	public boolean checkTokenValidity(final String token, final int valid_for_minutes) throws SQLException, NullPointerException{
		final String SQLGetTimeStamp = "select user, leastuse from simplechat.session where sessionkey=?";
		
		if(token == null)
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
			this.invalidadeToken(result.getInt("user"));
		return false;
	}
	
	public boolean invalidadeToken(final int user_id) throws SQLException{
		final String SQLInvalidade = "update simplechat.session set sessionkey=?, ip_address=?, leastUse=? where user=?";
		//TODO: delete the entry from the database
		if(user_id <= 0)
			return false;
		
		PreparedStatement stmnt = null;
		int result = 0;
		
		stmnt = this.getConnection().prepareStatement(SQLInvalidade);
		stmnt.setString(1, null);
		stmnt.setString(2, null);
		stmnt.setTimestamp(3, null);
		
		stmnt.setInt(4, user_id);
		result = stmnt.executeUpdate();
		
		stmnt.close();
		
		if(result == 0)
			return false;
		return true;
	}
}
