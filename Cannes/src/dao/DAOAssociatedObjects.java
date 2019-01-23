package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DAOAssociatedObjects {
	

	// on appelle cette méthode avec des requêtes de type "SELECT idSalles FROM Salles_Concours GROUP BY idConcours HAVING idConcours = X" 
	// elle sert à compléter la création d'objets au lancement du logiciel
    public List<Integer> selectAssociatedId(String query) 
    {
    	
    	PreparedStatement stmt;
    
    	stmt = DAOQuery.query(query);
    	
    	
    	if (stmt != null)
    	{
			try 
			{
				ResultSet rset = stmt.getResultSet();
				
				List<Integer> listInterne = new ArrayList<Integer>();
				
				while (rset.next()) 
	            {
	            	listInterne.add(rset.getInt(1));
	            }
				
	            return listInterne;
			} 
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
    	}
    	
    	return null;
    }
}
