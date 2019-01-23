package metier;

public class Horaire {

	private String horaire;
	private int id;

	public Horaire(int id, String horaire) {
		this.id = id;
		this.horaire = horaire;
	}

	public String getHoraire() {
		return horaire;
	}

	public void setHoraire(String horaire) {
		this.horaire = horaire;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

        /*@Override
        public String toString() {
            return "Horaire{" + "horaire=" + horaire + ", id=" + id + '}';
        }*/
        
        public String toString() 
        {
            return horaire;
        }
        
       
	
}
