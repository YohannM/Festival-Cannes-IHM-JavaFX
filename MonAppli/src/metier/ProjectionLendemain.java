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
public class ProjectionLendemain extends Projection {
    
    private List<ProjectionOfficielle> projectionOfficielleAssociee;

    public ProjectionLendemain(Integer date, boolean projectionLendemain, 
            Horaire horaire, Salle salle, Film film) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionOfficielleAssociee = new ArrayList<>();
    }
    
    public ProjectionLendemain(Integer date, boolean projectionLendemain, Horaire horaire, 
            Salle salle, Film film, List<ProjectionOfficielle> projectionOfficielleAssociee) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionOfficielleAssociee = projectionOfficielleAssociee;
    }
    
    public List<ProjectionOfficielle> getProjectionOfficielleAssociee() {
        return projectionOfficielleAssociee;
    }

    public void setProjectionOfficielleAssociee(List<ProjectionOfficielle> projectionOfficielleAssociee) {
        this.projectionOfficielleAssociee = projectionOfficielleAssociee;
    }
    
    public void addProjectionOfficielleAssociee(ProjectionOfficielle p)
    {
        if(!projectionOfficielleAssociee.contains(p))
            projectionOfficielleAssociee.add(p);
    }

    @Override
    public String toString() {
        String str = "ProjectionLendemain{" + super.toString() + " idProjectionOfficielleAssociee=";
        for(Projection p : projectionOfficielleAssociee)
            str += p.getId() + " ";
        return str + "}";
    }
    
    @Override
    public String getInfo()
    {
        String str = super.getInfo() + "\n\tCette séance est rattachée aux projections officielles suivantes :";
        
        for(Projection p : projectionOfficielleAssociee)
            str += "\n\t\tJour : " + p.getJour() 
                + "\n\t\tHoraire : " + p.getHoraire().getHoraire() 
                + "\n\t\tSalle : " + p.getSalle().getNom()
                + "\n\t\tFilm : " + p.getFilm().getTitre() 
                + "\n\t\tDurée : " + p.getFilm().getDuree()
                + "\n";
        
        return str;
    }
    
    
}
