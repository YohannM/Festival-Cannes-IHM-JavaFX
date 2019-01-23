/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import metier.Film;
import metier.Horaire;
import metier.Projection;
import metier.ProjectionLendemain;
import metier.ProjectionOfficielle;
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
                        if(rset.getInt("idHoraire") == ho.getId())
                            h = ho;
                    
                    for(Salle sa : listeSalle)
                        if(rset.getInt("idSalle") == sa.getId())
                            s = sa;
                    
                    for(Film fi : listeFilm)
                        if(rset.getInt("idFilm") == fi.getId())
                            f = fi;
                    
                    if(f.getConcours().aSeanceLendemain() && !rset.getBoolean("ProjectionLendemain"))
                        list.add(new ProjectionOfficielle(rset.getInt("DateProjection"), rset.getBoolean("ProjectionLendemain"), h, s, f));
                    else if(rset.getBoolean("ProjectionLendemain"))
                        list.add(new ProjectionLendemain(rset.getInt("DateProjection"), rset.getBoolean("ProjectionLendemain"), h, s, f));
                    else
                        list.add(new Projection(rset.getInt("DateProjection"), rset.getBoolean("ProjectionLendemain"), h, s, f));
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

    public void saveAll(List<Projection> lp) {
        
        /*PreparedStatement stmt0;
    	stmt0 = DAOQuery.query("SELECT COUNT(*) FROM Projection");
        int nb = 0;
        try {
            ResultSet rset = stmt0.getResultSet();
            rset.next();
            nb = rset.getInt(1);
        } catch (SQLException ex) {
            Logger.getLogger(DAOProjection.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        
        DAOQuery.update("DELETE FROM Projection");
        for(Projection p : lp)
        {
            Statement stmt;
            DAOQuery.update("INSERT INTO Projection (DateProjection, ProjectionLendemain,"
                    + "idHoraire, idSalle, idFilm) VALUES (" + p.getJour() + "," + 
                    p.isProjectionLendemain() + "," + 
                    p.getHoraire().getId() + "," + 
                    p.getSalle().getId() + "," + 
                    p.getFilm().getId() + ")");
        }
    }
    
    public void deleteAll()
    {
        DAOQuery.update("DELETE FROM Projection");
    }
}
