package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import metier.Film;

public class DAOFilm implements IClassesDAO<Film>
{
    public List<Film> select(String query) 
    {
    	Statement stmt;
    	
    	
    	stmt = DAOQuery.query(query);
    	
    	
    	if (stmt != null)
    	{
			try 
			{
				ResultSet rset = stmt.getResultSet();
				List<Film> list = new ArrayList<Film>();
				
				while (rset.next()) 
	            {
	            	list.add(new Film (rset.getInt("id"), rset.getString("Titre"), rset.getString("Duree"), null));
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
