package metier;

public class Film
{
	private int id;
	private String titre;
	private int duree;
	private String dureeStr;
	private Concours concours;
	
	public Film (int id, String titre, String dureeStr, Concours concours)
	{
		this.id = id;;
		this.titre = titre;
		this.dureeStr = dureeStr;
		this.concours = concours;
		
		// TODO : init duree from dureeStr
	}

	public String getTitre()
	{
		return this.titre;
	}

	public Concours getConcours() {
		return concours;
	}

	public void setConcours(Concours concours) {
		this.concours = concours;
	}

	public int getId() {
		return id;
	}

	public void setId(int idFilm) {
		this.id = idFilm;
	}

	public int getDuree() {
		return duree;
	}

	public void setDuree(int duree) {
		this.duree = duree;
	}
        
        public String toString()
        {
            return titre;
        }
}
	