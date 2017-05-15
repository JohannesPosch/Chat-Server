create database SimpleChat;
use SimpleChat;

create table Account(
ID int auto_increment not null,
VN varchar(100) null,
SN varchar(100) not null,
Nickname varchar(100) not null,
PW varchar(60) not null,
salt varchar(60) not null,

primary key (ID),
Index iNickname (Nickname),
Index iID (ID)
)engine=InnoDB;

create table Session(
ID int auto_increment not null,
User int not null,
SessionKey varchar(50) null,
leastUse timestamp null,
IP varchar(15) not null,

primary key (ID),
unique key (IP),
foreign key (User) references Account(ID),
index iSessionKey (SessionKey),
index iUser (User)
)engine=InnoDB;

delimiter //
create function UpdateSession(s_key varchar(50), l_use timestamp, u_name int, ip_address varchar(15))
	returns int
	begin
		update simplechat.session set sessionkey=s_key, leastUse=l_use where user=u_name && ip = ip_address;
		
		if ROW_COUNT() = 0 then insert into simplechat.session (ip, sessionkey, leastUse, user) values (ip_address,s_key,l_use,u_name);
		end if;
        
        return ROW_COUNT();
end //
delimiter ;

delimiter //
CREATE DEFINER='LoginServer'@'localhost' PROCEDURE `UpdateSession`(
   IN s_key varchar(50),
   IN l_use timestamp,
   IN u_name INT,
   IN ip_address varchar(15),

   OUT result TINYINT(1)
)
BEGIN
    update simplechat.session set sessionkey=s_key, leastUse=l_use where user=u_name && ip = ip_address;
		
	if ROW_COUNT() = 0 then insert into simplechat.session (ip, sessionkey, leastUse, user) values (ip_address,s_key,l_use,u_name);
	end if;
        
    set result = ROW_COUNT();
END //
delimiter ;


create user 'LoginServer'@'localhost' identified by 'LogChatv1sadioncorp';
grant select,insert on simplechat.account to 'LoginServer'@'localhost';
grant select, insert, update on simplechat.session to 'LoginServer'@'localhost';
grant execute on PROCEDURE simplechat.UpdateSession to 'LoginServer'@'localhost';

