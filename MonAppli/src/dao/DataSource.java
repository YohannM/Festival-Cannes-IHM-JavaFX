package dao;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import com.mysql.cj.jdbc.MysqlDataSource;

public class DataSource extends MysqlDataSource
{	
    public static MysqlDataSource getMysqlDataSource() throws IOException 
    {
    	MysqlDataSource dataSource = new MysqlDataSource();
        Properties props = new Properties();
        FileInputStream fichier = new FileInputStream(".\\src\\dao\\connexion.properties");
        props.load(fichier);
        
        dataSource.setServerName(props.getProperty("server")); 
        dataSource.setPortNumber(Integer.parseInt(props.getProperty("port")));
        dataSource.setDatabaseName(props.getProperty("dataBaseName"));
        dataSource.setUser(props.getProperty("user"));
        dataSource.setPassword(props.getProperty("pwd"));
        
        /*dataSource.setServerName("iutdoua-web.univ-lyon1.fr"); 
        dataSource.setPortNumber(3306);
        dataSource.setDatabaseName("p1700402");
        dataSource.setUser("p1700402");
        dataSource.setPassword("294357");*/
        
        return dataSource;
    }
}


