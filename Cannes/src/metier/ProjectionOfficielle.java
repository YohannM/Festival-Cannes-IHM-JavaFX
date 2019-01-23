/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metier;

/**
 *
 * @author p1702174
 */
public class ProjectionOfficielle extends Projection {
    
    private ProjectionLendemain projectionLendemainAssociee;
    
    public ProjectionOfficielle(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionLendemainAssociee = null;
    }
    
    public ProjectionOfficielle(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film, ProjectionLendemain projectionLendemainAssociee) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionLendemainAssociee = projectionLendemainAssociee;
    }

    public ProjectionLendemain getProjectionLendemainAssociee() {
        return projectionLendemainAssociee;
    }

    public void setProjectionLendemainAssociee(ProjectionLendemain projectionLendemainAssociee) {
        this.projectionLendemainAssociee = projectionLendemainAssociee;
    }
    
    public static ProjectionOfficielle copieProfonde(Projection p)
    {
        return new ProjectionOfficielle(p.getJour(), p.isProjectionLendemain(), p.getHoraire(), p.getSalle(), p.getFilm());
    }
    
}
