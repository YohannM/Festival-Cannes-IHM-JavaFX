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
public class ProjectionLendemain extends Projection {
    
    private ProjectionOfficielle projectionOfficielleAssociee;

    public ProjectionLendemain(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionOfficielleAssociee = null;
    }
    
    public ProjectionLendemain(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film, ProjectionOfficielle projectionOfficielleAssociee) {
        super(date, projectionLendemain, horaire, salle, film);
        this.projectionOfficielleAssociee = projectionOfficielleAssociee;
    }
    
    public ProjectionOfficielle getProjectionOfficielleAssociee() {
        return projectionOfficielleAssociee;
    }

    public void setProjectionOfficielleAssociee(ProjectionOfficielle projectionOfficielleAssociee) {
        this.projectionOfficielleAssociee = projectionOfficielleAssociee;
    }
    
    
}
