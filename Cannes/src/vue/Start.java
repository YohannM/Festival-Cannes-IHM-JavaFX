package vue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import dao.DataSource;

public class Start
{
    public static void main(String args[]) 
    {
        try {
            Connection connection = DataSource.getMysqlDataSource().getConnection();
            // System.out.println("connection �tablie");
            new Code(connection);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}


	

