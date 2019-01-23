package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DAOAssociatedObjects {
	

	// on appelle cette m�thode avec des requ�tes de type "SELECT idSalles FROM Salles_Concours GROUP BY idConcours HAVING idConcours = X" 
	// elle sert � compl�ter la cr�ation d'objets au lancement du logiciel
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
