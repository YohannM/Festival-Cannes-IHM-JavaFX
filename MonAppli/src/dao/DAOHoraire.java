package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import metier.Horaire;
import metier.Salle;

public class DAOHoraire implements IClassesDAO<Horaire>{

	
	public List<Horaire> select(String req) {

		PreparedStatement stmt;
    	
    	stmt = DAOQuery.query(req);
    	
    	
    	if (stmt != null)
    	{
			try 
			{
				ResultSet rset = stmt.getResultSet();
				List<Horaire> list = new ArrayList<Horaire>();
				
				while (rset.next()) 
	            {
	            	list.add(new Horaire (rset.getInt("idHoraire"), rset.getString("PlageHoraire")));
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
