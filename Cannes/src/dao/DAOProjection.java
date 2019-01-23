/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import metier.Concours;
import metier.Film;
import metier.Horaire;
import metier.Projection;
import metier.Salle;

/**
 *
 * @author p1702174
 */
public class DAOProjection {
    
    public List<Projection> select(String query, List<Horaire> listeHoraire, List<Salle> listeSalle, List<Film> listeFilm) 
    {
    	PreparedStatement stmt;
    	stmt = DAOQuery.query(query);
    	
        if (stmt != null)
        {
            try 
            {
                ResultSet rset = stmt.getResultSet();
                List<Projection> list = new ArrayList<>();

                while (rset.next()) 
                {
                    Horaire h = null;
                    Salle s = null;
                    Film f = null;
                    
                    for(Horaire ho : listeHoraire)
                        if(rset.getInt("id") == ho.getId())
                            h = ho;
                    
                    for(Salle sa : listeSalle)
                        if(rset.getInt("idSalle") == sa.getId())
                            s = sa;
                    
                    for(Film fi : listeFilm)
                        if(rset.getInt("idFilm") == fi.getId())
                            f = fi;
                    
                    list.add(new Projection(rset.getInt("id"), rset.getBoolean("aSeanceLendemain"), h, s, f));
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
