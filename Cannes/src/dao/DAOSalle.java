package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import metier.Salle;

public class DAOSalle implements IClassesDAO<Salle> {
	
	public List<Salle> select(String query) 
    {
    	PreparedStatement stmt;
    	
    	
    	stmt = DAOQuery.query(query);
    	
    	
    	if (stmt != null)
    	{
			try 
			{
				ResultSet rset = stmt.getResultSet();
				List<Salle> list = new ArrayList<Salle>();
				
				while (rset.next()) 
	            {
	            	list.add(new Salle (rset.getInt("idSalle"), rset.getString("LibelleSalle"), rset.getInt("Capacite")));
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
