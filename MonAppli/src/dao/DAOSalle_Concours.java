package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import metier.Concours;
import metier.Salle;
import metier.Salle_Concours;

public class DAOSalle_Concours
{
    
    public List<Salle_Concours> select(String query, List<Concours> listeConcours, List<Salle> listeSalle) 
    {
    	PreparedStatement stmt;
    	
    	stmt = DAOQuery.query(query);
    	
    	List<Salle_Concours> sc = new ArrayList<Salle_Concours>();
    	
    	if (stmt != null)
    	{
    		
			try 
			{
				ResultSet rset = stmt.getResultSet();
				
				while (rset.next()) 
	            {
					Concours concAssocie = null;
					Salle salleAssociee = null;
					
					for(Concours c : listeConcours)
					{
						if(c.getId() == rset.getInt("idConcours"))
							concAssocie = c;
					}
					
					for(Salle s : listeSalle)
					{
						if(s.getId() == rset.getInt("idSalle"))
							salleAssociee = s;
					}
					
	            	sc.add(new Salle_Concours(salleAssociee, concAssocie, rset.getBoolean("seanceLendemain"), rset.getFloat("Proportion"), rset.getBoolean("estRepetable")));
	            }
			} 
			catch (SQLException e) 
			{
				e.printStackTrace();
			}
    	}
    	
    	return sc;
    }
    
}
