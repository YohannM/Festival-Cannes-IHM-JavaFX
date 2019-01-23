/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metier;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author p1702174
 */
public class ProjectionOfficielle extends Projection {
    
    private List<ProjectionLendemain> projectionLendemainAssociee;
    
    public ProjectionOfficielle(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionLendemainAssociee = new ArrayList<>();
    }
    
    public ProjectionOfficielle(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film, List<ProjectionLendemain> projectionLendemainAssociee) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionLendemainAssociee = projectionLendemainAssociee;
    }

    public List<ProjectionLendemain> getProjectionLendemainAssociee() {
        return projectionLendemainAssociee;
    }

    public void setProjectionLendemainAssociee(List<ProjectionLendemain> projectionLendemainAssociee) {
        this.projectionLendemainAssociee = projectionLendemainAssociee;
    }
    
    public void addProjectionLendemainAssociee(ProjectionLendemain p)
    {
        if(!projectionLendemainAssociee.contains(p))
            projectionLendemainAssociee.add(p);
    }
    
    public static ProjectionOfficielle copie(Projection p)
    {
        return new ProjectionOfficielle(p.getJour(), p.isProjectionLendemain(), p.getHoraire(), p.getSalle(), p.getFilm());
    }
    
    @Override
    public String toString() {
        String str = "ProjectionOfficielle{" + super.toString() + " idProjectionLendemainAssociees=";
        for(Projection p : projectionLendemainAssociee)
            str += p.getId() + " ";
        return str + "}";
    }
    
    @Override
    public String getInfo()
    {
        String str = super.getInfo() + "\n\tCette séance est rattachée aux projections du lendemain suivantes :";
        
        for(Projection p : projectionLendemainAssociee)
            str += "\n\t\tJour : " + p.getJour() 
                + "\n\t\tHoraire : " + p.getHoraire().getHoraire() 
                + "\n\t\tSalle : " + p.getSalle().getNom()
                + "\n\t\tFilm : " + p.getFilm().getTitre() 
                + "\n\t\tDurée : " + p.getFilm().getDuree()
                + "\n";
        
        return str;
    }
    
}
