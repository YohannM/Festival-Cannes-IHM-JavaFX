package metier;

public class Salle_Concours {
	
	private Salle salle;
	private Concours concours;
	private boolean seanceLendemain;
	private float proportion;
        
	
	public Salle_Concours(Salle salle, Concours concours, boolean seanceLendemain, float proportion) {
		
		this.salle = salle;
		this.concours = concours;
		this.seanceLendemain = seanceLendemain;
		this.proportion = proportion;
	}

	public Salle getSalle() {
		return salle;
	}

	public void setSalle(Salle salle) {
		this.salle = salle;
	}

	public Concours getConcours() {
		return concours;
	}

	public void setConcours(Concours concours) {
		this.concours = concours;
	}

	public boolean isSeanceLendemain() {
		return seanceLendemain;
	}

	public void setSeanceLendemain(boolean seanceLendemain) {
		this.seanceLendemain = seanceLendemain;
	}

	public float getProportion() {
		return proportion;
	}

	public void setProportion(float proportion) {
		this.proportion = proportion;
	}

	@Override
	public String toString() {
		return "Salle_Concours [salle=" + salle.getId() + ", concours=" + concours.getId() + ", seanceLendemain=" + seanceLendemain
				+ ", proportion=" + proportion + "]";
	}
	
	
	
	
}
