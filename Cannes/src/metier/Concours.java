package metier;

import java.util.ArrayList;
import java.util.List;

public class Concours 
{
	private int id;
	private String libelle;
	private List<Film> films;
        private boolean aSeanceLendemain;

	public Concours(int id, String libelle, ArrayList<Film> films, boolean aSeanceLendemain)
	{
		this.id = id;
		this.libelle = libelle;
		this.films = films;
                this.aSeanceLendemain = aSeanceLendemain;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
	
	public void setFilms(ArrayList<Film> films) {
		this.films = films;
	}
	
	public List<Film> getAllFilms() {
		return films;
	}
	
	public Film getFilm(int id)
	{
		for(Film f : films)
		{
			if(f.getId() == id)
				return f;
		}
		
		return null;
	}

	public void ajouterFilm(Film f)
	{
		films.add(f);
	}
        
        public List<Film> getFilms() {
            return films;
        }

        public void setFilms(List<Film> films) {
            this.films = films;
        }

        public boolean aSeanceLendemain() {
            return aSeanceLendemain;
        }

        public void setaSeanceLendemain(boolean aSeanceLendemain) {
            this.aSeanceLendemain = aSeanceLendemain;
        }
	
	@Override
	public String toString() {
		
		String str = "Concours -> id=" + id + ", libelle=" + libelle + ", films=[";
		
		for(Film f : films)
			str += f.getId() + ",";
				
		str += "]" + "aSeanceLendemain = " + aSeanceLendemain;
		
		return str;
	}
	
}
