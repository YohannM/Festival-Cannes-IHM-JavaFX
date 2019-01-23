package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import metier.Concours;

public class DAOConcours implements IClassesDAO<Concours>
{
    
    public List<Concours> select(String query) 
    {
    	PreparedStatement stmt;
    	
    	stmt = DAOQuery.query(query);
    	
    	
    	if (stmt != null)
    	{
			try 
			{
				ResultSet rset = stmt.getResultSet();
				List<Concours> list = new ArrayList<Concours>();
				
				while (rset.next()) 
	            {
	            	list.add(new Concours (rset.getInt("id"), rset.getString("libelleConcours"), null, rset.getBoolean("aSeanceLendemain")));
	            }
	            return list;
			} 
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
    	}
    	
    	return null;
    }
    
}
