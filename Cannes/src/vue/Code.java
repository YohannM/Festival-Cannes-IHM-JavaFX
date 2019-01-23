package vue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import dao.DAOAssociatedObjects;
import dao.DAOConcours;
import dao.DAOFilm;
import dao.DAOHoraire;
import dao.DAOProjection;
import dao.DAOQuery;
import dao.DAOSalle;
import dao.DAOSalle_Concours;
import gui.Fenetre;
import java.util.Iterator;
import metier.Concours;
import metier.Film;
import metier.Horaire;
import metier.Projection;
import metier.ProjectionLendemain;
import metier.ProjectionOfficielle;
import metier.Salle;
import metier.Salle_Concours;

public class Code extends JFrame
{
    private DAOConcours daoConcours;
    private DAOSalle daoSalle;
    private DAOFilm daoFilm;
    private DAOHoraire daoHoraire;
    private DAOSalle_Concours  daoSalle_Concours;
    private DAOAssociatedObjects daoAssObj;
    private DAOProjection daoProjection;
    private List<Concours> listeConcours;
    private List<Salle> listeSalle;
    private List<Film> listeFilm;
    private List<Horaire> listeHoraire;
    private List<Salle_Concours> listeSalle_Concours;
    private List<Projection> listeProjection;
    private Projection[][][] structureProjection;

 
    public Code(Connection connection)
    {
        super(); 
        DAOQuery.setConnexion(connection);
        initElemFromBD();
        // debugElem();
        
        if(listeProjection.isEmpty() && estGenerable())
        {
            System.out.println("Le programme est générable");
            generatePlanning();
            // debugPlanning();
            System.out.println("Le programme est généré");
        } else
            System.out.println("Le programme non généré");
        
        System.out.println(enregistrerPlanning());
        
        
        Fenetre fen = new Fenetre(this);

        java.awt.EventQueue.invokeLater(() -> {
            fen.setVisible(true);
        } // lancement gui dans un new thread
        );
    }

        
    private void initElemFromBD()
    {
        daoConcours = new DAOConcours();
        daoSalle = new DAOSalle();
        daoFilm = new DAOFilm();
        daoHoraire = new DAOHoraire();
        daoSalle_Concours = new DAOSalle_Concours();
        daoAssObj = new DAOAssociatedObjects();
        daoProjection = new DAOProjection();

        listeConcours = daoConcours.select("SELECT * FROM Concours");
        listeSalle = daoSalle.select("SELECT * FROM Salle");
        listeFilm = daoFilm.select("SELECT * FROM Film");
        listeHoraire = daoHoraire.select("SELECT * FROM Horaire");

        listeConcours.forEach((c) -> {
            String req = "SELECT id FROM Film WHERE concours_id =" + c.getId();
            List<Integer> listeIdFilmsAssocie = daoAssObj.selectAssociatedId(req);

            ArrayList<Film> listeFilmsAssociees = new ArrayList<>();

            listeIdFilmsAssocie.forEach((i) -> {
                listeFilm.stream().filter((f) -> (f.getId() == i)).forEachOrdered((f) -> {
                    listeFilmsAssociees.add(f);
                });
            });
            c.setFilms(listeFilmsAssociees);
        });


        listeFilm.forEach((f) -> {
            String req = "SELECT concours_id FROM Film WHERE id =" + f.getId();

            Integer idConcoursAssocie = daoAssObj.selectAssociatedId(req).get(0);

            listeConcours.stream().filter((c) -> (idConcoursAssocie == c.getId())).forEachOrdered((c) -> {
                f.setConcours(c);
            });
        });

        listeSalle_Concours = daoSalle_Concours.select("SELECT * FROM Salle_Concours", listeConcours, listeSalle);
        listeProjection = daoProjection.select("SELECT * FROM Projection", listeHoraire, listeSalle, listeFilm);
        structureProjection = new Projection[11][listeSalle.size()][listeHoraire.size()];

        listeProjection.forEach((p) -> {
            structureProjection[p.getJour()-1][p.getSalle().getId()][p.getHoraire().getId()] = p;
        });
    }


    @SuppressWarnings("unused")
    private void debugElem()
    {
        listeSalle_Concours.forEach((sc) -> {
            System.out.println(sc.toString());
        });

        listeSalle.forEach((s) -> {
            System.out.println(s);
        });
            
        listeConcours.forEach((c) -> {
            System.out.println(c);
        });
            
        listeHoraire.forEach((h) -> {
            System.out.println(h);
        });
    }


    
    private boolean estGenerable()
    {
        boolean estFaisable = true;
        
        int nbSeancesMaxDansUneSalle = listeHoraire.size() * 11;

        int[] nbFilmParSalle = new int[listeSalle.size()];
        float[][] sommePropParConcours = new float[listeConcours.size()][2];
        // deuxième dimension pour distinguer séance normale et du lendemain

        for(int i = 0; i < nbFilmParSalle.length; i++)
            nbFilmParSalle[i] = 0;

        for(int i = 0; i < sommePropParConcours.length; i++)
        {
            sommePropParConcours[i][0] = 0;
            sommePropParConcours[i][1] = (listeConcours.get(i).aSeanceLendemain() ? 0 : 1);
        }

        for(int i = 0; i < listeConcours.size() ; i++)
        {
            for(Salle_Concours sc : listeSalle_Concours)
            {
                if(sc.getConcours().getId() == listeConcours.get(i).getId())
                    sommePropParConcours[i][sc.isSeanceLendemain() ? 1 : 0] += sc.getProportion();
            }
        }
        
        for(int i = 0; estFaisable && i < sommePropParConcours.length; i++)
            estFaisable = sommePropParConcours[i][0] == 1 && (sommePropParConcours[i][1] == 1);
        
        if (estFaisable)
        {
            for(int i = 0; i < listeSalle.size() ; i++)
            {
                for(Salle_Concours sc : listeSalle_Concours)
                {
                        if(sc.getSalle() == listeSalle.get(i))
                            nbFilmParSalle[i] +=  (int) (Math.ceil(sc.getProportion() * sc.getConcours().getAllFilms().size()));
                }
            }

            for(int i = 0; estFaisable && i < nbFilmParSalle.length; i++)
                estFaisable = (nbFilmParSalle[i] < nbSeancesMaxDansUneSalle);
        }
     
        return estFaisable;
    }


    private void generatePlanning()
    {
        
        List<Film> filmsAPlacerSN = new ArrayList<>(listeFilm);
        List<Film> filmsAPlacerSL = new ArrayList<>();
        
        listeSalle_Concours.stream().filter((sc) -> (sc.isSeanceLendemain())).forEachOrdered((Salle_Concours sc) -> {
            listeConcours.stream().filter((c) -> (c.getId() == sc.getConcours().getId())).forEachOrdered((c) -> {
                filmsAPlacerSL.addAll(c.getAllFilms());
            });
        });
        
        listeSalle_Concours.forEach((sc) -> {
            for(int i = 0; i < (int) (Math.ceil(sc.getProportion() * sc.getConcours().getAllFilms().size())); i++)
            {
                int j = 0;
                
                List<Film> listeAUtiliser = sc.isSeanceLendemain() ? filmsAPlacerSL : filmsAPlacerSN;
                
                while(j < listeAUtiliser.size() && listeAUtiliser.get(j).getConcours().getId() != sc.getConcours().getId())
                    j++;
                
                if(j < listeAUtiliser.size())
                {
                    boolean estProjectionOfficielle = listeAUtiliser.get(j).getConcours().aSeanceLendemain() && !sc.isSeanceLendemain();
                    
                    Projection nouvProj;
                    
                    if(estProjectionOfficielle)
                    {
                        nouvProj = new ProjectionOfficielle(null, sc.isSeanceLendemain(), null, sc.getSalle(), listeAUtiliser.get(j));
                    } else if(sc.isSeanceLendemain()) {
                        nouvProj = new ProjectionLendemain(null, sc.isSeanceLendemain(), null, sc.getSalle(), listeAUtiliser.get(j));
                    } else {
                        nouvProj = new Projection(null, sc.isSeanceLendemain(), null, sc.getSalle(), listeAUtiliser.get(j));
                    }
                    
                    
                    boolean estPlace = false;
                    
                    int indJour = 0;
                    
                    Salle salleConcernee = null;
                    
                    for(Salle_Concours sco : listeSalle_Concours)
                        if(sco.getConcours().getId() == sc.getConcours().getId() && !sco.isSeanceLendemain())
                            salleConcernee = sco.getSalle();
                    
                    if(sc.isSeanceLendemain())
                    {
                        for(int a = 0; a < structureProjection.length && indJour == 0; a++)
                                for(int c = 0;  c < structureProjection[0][0].length && indJour == 0; c++)
                                    if (structureProjection[a][salleConcernee.getId()][c] != null 
                                            && structureProjection[a][salleConcernee.getId()][c].getFilm().getId() == nouvProj.getFilm().getId())
                                    {
                                        int idtmp = structureProjection[a][salleConcernee.getId()][c].getId();
                                        structureProjection[a][salleConcernee.getId()][c] = ProjectionOfficielle.copieProfonde(structureProjection[a][salleConcernee.getId()][c]);
                                        ProjectionOfficielle tmp = (ProjectionOfficielle) structureProjection[a][salleConcernee.getId()][c];
                                        ProjectionLendemain tmp2 = (ProjectionLendemain) nouvProj;
                                        tmp.setId(idtmp);
                                        tmp.setProjectionLendemainAssociee(tmp2);
                                        tmp2.setProjectionOfficielleAssociee(tmp);
                                        indJour = ++a;
                                            
                                    }
                    }
                    
                    
                    for(; indJour < structureProjection.length && !estPlace; indJour++)
                    {
                        for(int indHoraire = 0;  indHoraire < structureProjection[0][0].length && !estPlace; indHoraire++)
                        {
                            // System.out.println(structureProjection[indJour][sc.getSalle().getId()][indHoraire]);
                            if(structureProjection[indJour][sc.getSalle().getId()][indHoraire] == null)
                            {
                                nouvProj.setJour(indJour + 1);
                                nouvProj.setHoraire(listeHoraire.get(indHoraire));
                                structureProjection[indJour][sc.getSalle().getId()][indHoraire] = nouvProj;
                                estPlace = true;
                            }
                        }
                    }
                    
                    
                    
                    
                    listeProjection.add(nouvProj);
                    listeAUtiliser.remove(j);
                }
            }
        });
        
        /*if(filmsAPlacerSN.isEmpty() && filmsAPlacerSL.isEmpty())
            System.out.println("Tous les films sont projetés");
        else
            System.out.println("Il reste " + filmsAPlacerSN.size() + "Films en séance normal et et " + filmsAPlacerSL.size() + " films en séance du lendemain");
        */
    }
    
    
    public void debugPlanning()
    {
        System.out.println("Projection affichées depuis la liste : ");
        
        listeProjection.forEach((p) -> {
            System.out.println("id = " + p.getId() + " Jour = " + p.getJour() + " Film = " + p.getFilm().getTitre() + " Salle = " + p.getSalle().getNom() + " Horaire = " + p.getHoraire() + " est lend = " + p.isProjectionLendemain());
        });
        
        System.out.println("\n");
        System.out.println("Projection affichées depuis la structure : ");
        
        
        for(Projection[][] p3 : structureProjection)
            for(Projection[] p2 : p3)
                for(Projection p : p2)
                    if (p != null)
                        System.out.println("id = " + p.getId() + " Jour = " + p.getJour() + " Film = " + p.getFilm().getTitre() + " Salle = " + p.getSalle().getNom() + " Horaire = " + p.getHoraire() + " est lend = " + p.isProjectionLendemain());
        
        System.out.println("\nTest de la structure : projection jour 1, salle 3, horaire 3 : ");
        
        Projection p = structureProjection[0][2][2];
        if(p != null)
            System.out.println("id = " + p.getId() + " Jour = " + p.getJour() + " Film = " + p.getFilm().getTitre() + " Salle = " + p.getSalle().getNom() + " Horaire = " + p.getHoraire() + " est lend = " + p.isProjectionLendemain());
        else
            System.out.println("Pas de projection");
    }
    
    
    public List<Salle> getListeSalle() {
        return listeSalle;
    }
    
    public List<Horaire> getListeHoraire() {
        return listeHoraire;
    }
    
    public List<Film> getListeFilm() {
        return listeFilm;
    }

    public Projection[][][] getStructureProjection() {
        return structureProjection;
    }
    
    public List<Projection> getProjections()
    {
        return listeProjection;
    }
    
    public List<Salle_Concours> getSalleConcours()
    {
        return listeSalle_Concours;
    }
    
    // renvoie 1 si valide et rajoute à la liste et à la structure
    // renvoie 0 si mauvaise salle pour le concours associé au film
    // renvoie -1 si le film est un doublon et qu'il ne doit pas être en projection du lendemain
    // renvoie -2 si le film est un doublon et que sa projection est mal placée par rapport à 
    // sa projection officielle ou que sa projection officielle n'existe pas
    
    public int newProjection(Film film, Salle salle, int numJour, Horaire horaire)
    {
        //boolean estValide = false;
        boolean estLendemain = false;
        
        Concours concConcerne = film.getConcours();
        
        for(Salle_Concours sc : listeSalle_Concours)
        {
            if(sc.getConcours().getId() == concConcerne.getId())
            {
                /* estValide = estValide || sc.getSalle().getId() == salle.getId();
                if(estValide)
                { */
                estLendemain = sc.isSeanceLendemain();
                break;
                //}
            }
        }
        
        /*if (!estValide)
            return 0;
        
        if(!estLendemain)
        {
            boolean estDoublon = false;
            
            for(Projection p : listeProjection)
            {
                if(p.getFilm().getId() == film.getId())
                {
                    estDoublon = true;
                    break;
                }
            }
            
            if(estDoublon)
                return -1;
            
        } else {
            boolean filmOfficielTrouve = false;
            
            int idSalleProjOfficielleAssociee = -1;
            
            for(Salle_Concours sc : listeSalle_Concours)
            {
                if(sc.getConcours().getId() == concConcerne.getId() && sc.isSeanceLendemain())
                    idSalleProjOfficielleAssociee = sc.getSalle().getId();
            }
            
            int indJour = numJour - 1;
            int indHoraire = horaire.getId() + 1;
            if (indHoraire == listeHoraire.size())
            {
                indHoraire = 0;
                indJour++;
            }
            
            for(; indJour < structureProjection.length && !filmOfficielTrouve; indJour++)
            {
                for(; indHoraire < structureProjection[0][0].length && !filmOfficielTrouve; indHoraire++)
                {
                    if(structureProjection[indJour][idSalleProjOfficielleAssociee][indHoraire] != null 
                            && structureProjection[indJour][idSalleProjOfficielleAssociee][indHoraire].getFilm().getId() == film.getId())
                    {
                        filmOfficielTrouve = true;
                    }
                }
            }
            
            if(!filmOfficielTrouve)
                return -2;
        }*/
        
        Projection nouvProj = new Projection(numJour, estLendemain, horaire, salle, film);
        
        listeProjection.add(nouvProj);
        structureProjection[numJour - 1][salle.getId()][horaire.getId()] = nouvProj;

        return 1;
    }
    
    
    public boolean enregistrerPlanning()
    {
        List<Film> filmAProjeter = new ArrayList<>(listeFilm);
        List<Projection> listeCompProjection = new ArrayList<> (listeProjection);
        
        List<Integer> idConcoursSL = new ArrayList<>();
        
        for(Salle_Concours sc : listeSalle_Concours)
        {
            if(sc.isSeanceLendemain())
            {
                if(!idConcoursSL.contains(sc.getConcours().getId()))
                    idConcoursSL.add(sc.getConcours().getId());
            }
        }
        
        for(Integer i : idConcoursSL)
            for(Film f : listeFilm)
                if(f.getConcours().getId() == i)
                    filmAProjeter.add(f);
        
        
        Iterator<Film> iteratorFilm = filmAProjeter.iterator();

        while (iteratorFilm.hasNext()) {
            Film f = iteratorFilm.next();
            Iterator<Projection> iteratorProj = listeCompProjection.iterator();
            while (iteratorProj.hasNext()) {
                Projection p = iteratorProj.next();
                if (p.getFilm().getId() == f.getId())
                {
                    iteratorFilm.remove();
                    iteratorProj.remove();
                    break;
                }
            } 
        }
        
        boolean programmeValide = filmAProjeter.isEmpty() && listeCompProjection.isEmpty();
        
        if(programmeValide)
        {
            // TODO : générer le programme dans un fichier
        }
        
        return programmeValide;
    }
    
    
    public boolean supprimerProjection(int j, int s, int h)
    {
        Projection p = structureProjection[j-1][s][h];
        
        if(!p.isProjectionLendemain() && p.getFilm().getConcours().aSeanceLendemain())
        {
            int indJour = j;
            int indHoraire = h;
            int numSalle = 0;
            
            for(Salle_Concours sc : listeSalle_Concours)
                if(sc.getConcours().getId() == p.getFilm().getConcours().getId() && sc.isSeanceLendemain())
                {
                    numSalle = sc.getSalle().getId();
                    break;
                }
            
            for(; indJour < structureProjection.length; indJour++)
                for(; indHoraire < structureProjection[0][0].length; indHoraire++)
                    if(structureProjection[indJour][numSalle][indHoraire] != null 
                            && structureProjection[indJour][numSalle][indHoraire].getFilm().getId() == p.getFilm().getId() 
                            && structureProjection[indJour][numSalle][indHoraire].isProjectionLendemain())
                        return false;
        }
        
        // TODO : proposer de supprimer toutes les projections du lendemain en même temps
        
        listeProjection.remove(p);
        structureProjection[p.getJour()-1][p.getSalle().getId()][p.getHoraire().getId()] = null;
        
        return true;
    }
    
    public boolean[][] placementFilm(Film f, int numJour)
    {
        
        
        return null;
    }
        /*if(!estLendemain)
        {
            boolean estDoublon = false;
            
            for(Projection p : listeProjection)
            {
                if(p.getFilm().getId() == film.getId())
                {
                    estDoublon = true;
                    break;
                }
            }
            
            if(estDoublon)
                return -1;
            
        } else {
            boolean filmOfficielTrouve = false;
            
            int idSalleProjOfficielleAssociee = -1;
            
            for(Salle_Concours sc : listeSalle_Concours)
            {
                if(sc.getConcours().getId() == concConcerne.getId() && sc.isSeanceLendemain())
                    idSalleProjOfficielleAssociee = sc.getSalle().getId();
            }
            
            int indJour = numJour - 1;
            int indHoraire = horaire.getId() + 1;
            if (indHoraire == listeHoraire.size())
            {
                indHoraire = 0;
                indJour++;
            }
            
            for(; indJour < structureProjection.length && !filmOfficielTrouve; indJour++)
            {
                for(; indHoraire < structureProjection[0][0].length && !filmOfficielTrouve; indHoraire++)
                {
                    if(structureProjection[indJour][idSalleProjOfficielleAssociee][indHoraire] != null 
                            && structureProjection[indJour][idSalleProjOfficielleAssociee][indHoraire].getFilm().getId() == film.getId())
                    {
                        filmOfficielTrouve = true;
                    }
                }
            }
    }*/
    
    public boolean[][] placementFilm(Film f, Salle s)
    {
        return null;
    }
    
}
