package metier;

import java.sql.Date;

public class Projection {

	private static int cpt=0;
	private int id;
	private Integer numJour;
	private boolean projectionLendemain;
	private Horaire horaire;
	private Salle salle;
	private Film film;
	
	public Projection(Integer date, boolean projectionLendemain, Horaire horaire, Salle salle, Film film) {
		this.id = ++cpt; 
		this.numJour = date;
		this.projectionLendemain = projectionLendemain;
		this.horaire = horaire;
		this.salle = salle;
		this.film = film;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
        
        public int getJour()
        {
            return numJour;
        }

	public void setJour(int jour) {
		this.numJour = jour;
	}

	public boolean isProjectionLendemain() {
		return projectionLendemain;
	}

	public void setProjectionLendemain(boolean projectionLendemain) {
		this.projectionLendemain = projectionLendemain;
	}

	public Horaire getHoraire() {
		return horaire;
	}

	public void setHoraire(Horaire horaire) {
		this.horaire = horaire;
	}

	public Salle getSalle() {
		return salle;
	}

	public void setSalle(Salle salle) {
		this.salle = salle;
	}

	public Film getFilm() {
		return film;
	}

	public void setFilm(Film film) {
		this.film = film;
	}
        
        public String getInfo()
        {
            return "Informations sur cette séance :"
                        + "\n\tJour : " + this.getJour() 
                        + "\n\tHoraire : " + this.getHoraire().getHoraire() 
                        + "\n\tSalle : " + this.getSalle().getNom()
                        + "\n\tFilm : " + this.getFilm().getTitre() 
                        + "\n\tDurée : " + this.getFilm().getDuree();
        }

    @Override
    public String toString() {
        return "Projection{" + "id=" + id + ", numJour=" + numJour + ", projectionLendemain=" + projectionLendemain + ", horaire=" + horaire + ", salle=" + salle.getNom() + ", film=" + film.getTitre() + '}';
    }
    
    public boolean estAuMoinsPlaceeLaVeilleDe(Projection p)
    {
        return this.numJour < p.getJour();
    }
        
	
	
	
}
