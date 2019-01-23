package vue;

import com.itextpdf.text.BaseColor;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import dao.DAOAssociatedObjects;
import dao.DAOConcours;
import dao.DAOFilm;
import dao.DAOHoraire;
import dao.DAOProjection;
import dao.DAOQuery;
import dao.DAOSalle;
import dao.DAOSalle_Concours;
import java.util.Iterator;
import metier.Concours;
import metier.Film;
import metier.Horaire;
import metier.Projection;
import metier.ProjectionLendemain;
import metier.ProjectionOfficielle;
import metier.Salle;
import metier.Salle_Concours;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PlanningHelper
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
    private boolean planningFromBD;
 
    public PlanningHelper(Connection connection)
    {
        super(); 
        DAOQuery.setConnexion(connection);
        initElemFromBD();
        
        // on génère le planning si jamais on a pas trouvé de proj en BD
        if(listeProjection.isEmpty() && estGenerable())
        {
            generatePlanning();
            planningFromBD = false;
        } else {
            planningFromBD = true;
        }
        
    }
    
    private void initElemFromBD()
    {
        daoConcours = new DAOConcours();                // creation des objets dao
        daoSalle = new DAOSalle();
        daoFilm = new DAOFilm();
        daoHoraire = new DAOHoraire();
        daoSalle_Concours = new DAOSalle_Concours();
        daoAssObj = new DAOAssociatedObjects();
        daoProjection = new DAOProjection();

        listeConcours = daoConcours.select("SELECT * FROM Concours");           // remplissages des objets métiers 
        listeSalle = daoSalle.select("SELECT * FROM Salle");
        listeFilm = daoFilm.select("SELECT * FROM Film");
        listeHoraire = daoHoraire.select("SELECT * FROM Horaire");

        /* Dans les deux expressions fonctionnelles qui suivent :
            - on récupère les id d'objets métiers associés afin de respecter
                les relations entités entre les objets
        */
        
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

        // on récupère les objets métiers 
        // (on met les attributs composés en passant directement les listes adéquates aux dao)
        listeSalle_Concours = daoSalle_Concours.select("SELECT * FROM Salle_Concours", listeConcours, listeSalle);
        listeProjection = daoProjection.select("SELECT * FROM Projection", listeHoraire, listeSalle, listeFilm);
        structureProjection = new Projection[11][listeSalle.size()][listeHoraire.size()];
        
       // on remplit les listes de proj associées
       for(Projection p : listeProjection)
       {
           if(p instanceof ProjectionOfficielle)
           {
               for(Projection pr : listeProjection)
               {
                   if(pr instanceof ProjectionLendemain && pr.getFilm().getId() == p.getFilm().getId())
                   {
                       ProjectionOfficielle poTmp = (ProjectionOfficielle)p;
                       ProjectionLendemain plTmp = (ProjectionLendemain)pr;
                       ((ProjectionOfficielle) p).addProjectionLendemainAssociee(plTmp);
                       ((ProjectionLendemain) pr).addProjectionOfficielleAssociee(poTmp);
                   }
               }
           }
       }

       // on remplit la 2ème structure de données qui stocke les projections
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
    
    /* verifie si le planning peut être généré avec les contraintes initiales 
        On vérifie dans l'ordre:
            - que les coeff de répartitions concours/salles sont correctes
            - que le nombre de proj par salle est adéquat
    */
    public boolean estGenerable()
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
    
    /*
        Génération automatique du planning :
            - on génère une liste de films à projeter en SN
            - puis une à projeter en SL
            - Pour chaque Salle_Concours, on compte le nombre de films à projeter,
                on choisit la bonne liste de films à projeter en fonction du concours,
                on itère la liste jusqu'à trouver un film éligible,
                on crée une projection du bon type,
                on met à jour les listes de proj associées le cas échéant,
                on regarde à partir de quel créneau on peut placer la projection 
                (pour ça on regarde si le film est SL etc...)
                on la place et on update la liste 
    */
    public void generatePlanning()
    {
        listeProjection.removeAll(listeProjection);
        
        for(int indJour = 0; indJour < structureProjection.length; indJour++)
            for(int indSalle = 0; indSalle < structureProjection[0].length; indSalle++)
                for(int indHoraire = 0; indHoraire < structureProjection[0][0].length; indHoraire++)
                    structureProjection[indJour][indSalle][indHoraire] = null;
        
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
                                        // int idtmp = structureProjection[a][salleConcernee.getId()][c].getId();
                                        // structureProjection[a][salleConcernee.getId()][c] = ProjectionOfficielle.copie(structureProjection[a][salleConcernee.getId()][c]);
                                        ProjectionOfficielle tmp = (ProjectionOfficielle) structureProjection[a][salleConcernee.getId()][c];
                                        ProjectionLendemain tmp2 = (ProjectionLendemain) nouvProj;
                                        // tmp.setId(idtmp);
                                        tmp.addProjectionLendemainAssociee(tmp2);
                                        tmp2.addProjectionOfficielleAssociee(tmp);
                                        indJour = ++a;
                                    }
                    }
                    
                    
                    for(; indJour < structureProjection.length && !estPlace; indJour++)
                    {
                        for(int indHoraire = 0;  indHoraire < structureProjection[0][0].length && !estPlace; indHoraire++)
                        {
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
    }
    
    /*
        On génère la liste de film qu'il faut placer :
        On ajoute chaque film devant être projeté dans une liste 
        (une fois par projection) puis on compare cette liste à celle des 
        projection en faisant sauter les deux éléments à chaque match
        à la fin il doit ou non rester des films dans la liste de films à projeter.
        Ce qui reste est la liste de films à placer.
    */
    public ObservableList<String> generateListeFilmAPlacer()
    {
        List<Film> filmAProjeter = new ArrayList<>(listeFilm);
        List<Projection> listeCompProjection = new ArrayList<> (listeProjection);
        
        List<Integer> idConcoursSL = new ArrayList<>();
        
        listeSalle_Concours.stream().filter((sc) -> (sc.isSeanceLendemain())).filter((sc) -> (!idConcoursSL.contains(sc.getConcours().getId()))).forEachOrdered((sc) -> {
            idConcoursSL.add(sc.getConcours().getId());
        });
        
        idConcoursSL.forEach((Integer i) -> {
            listeFilm.stream().filter((f) -> (f.getConcours().getId() == i)).forEachOrdered((f) -> {
                filmAProjeter.add(f);
            });
        });
        
        
        Iterator<Film> iteratorFilm = filmAProjeter.iterator();

        while (iteratorFilm.hasNext()) {
            Film f = iteratorFilm.next();
            Iterator<Projection> iteratorProj = listeCompProjection.iterator();
            while (iteratorProj.hasNext()) {
                Projection p = iteratorProj.next();
                if (p.getFilm().getId() == f.getId())
                {
                    iteratorProj.remove();
                    iteratorFilm.remove();
                    
                    break;
                }
            } 
        }
        
        ObservableList<String> titre = FXCollections.observableArrayList ();
        
        for(Film film : filmAProjeter)
            if(!titre.contains(film.getTitre()))
                titre.add(film.getTitre());
        
        return titre;
    }
    
    // utilitaire, utilisation en dessous
    private List<String> getStringListFromFilmList(List<Film> lf)
    {
        List<String> ls = new ArrayList<>();
        for(Film f : lf)
            ls.add(f.getTitre());
        return ls;
    }
    
    /*
        On génère la liste de film qu'on peut placer à l'infini :
        En fct de l'affichage on sait quelle salle est sur l'écran,
        si un des concours associées à cette salle comporte des séances
        répétables alors on ajoute les films à la liste
        Enfin on ajoute (si pas déjà présents) les films à placer 
    */
    public ObservableList<String> generateListeFilmPlacable(boolean affichage, int arg1)
    {
        ObservableList<String> listeFilmP = FXCollections.observableArrayList ();
        List<Integer> listIdSalle = new ArrayList<>();
        if(affichage) 
            listIdSalle.add(arg1);
        else
            IntStream.range(0, listeSalle.size()).forEach(n -> {listIdSalle.add(n);});
        
        /*for(Salle_Concours sc : listeSalle_Concours)
            if(sc.isEstRepetable() && listIdSalle.contains(sc.getSalle().getId()))
                listeFilmP.addAll(getStringListFromFilmList(sc.getConcours().getAllFilms()));*/
        
        listeSalle_Concours.stream().filter((sc) -> (sc.isEstRepetable() 
                && listIdSalle.contains(sc.getSalle().getId()))).forEachOrdered((sc) -> {
            listeFilmP.addAll(getStringListFromFilmList(sc.getConcours().getAllFilms()));
        });
        
        if(affichage)
        {
            List<String> listeFilmAP = generateListeFilmAPlacer();
            List<String> listeFilmEligible = new ArrayList<>();
            
            for(Salle_Concours sc : listeSalle_Concours)
                if(sc.getSalle().getId() == arg1)
                    for(Film f : sc.getConcours().getAllFilms())
                        listeFilmEligible.add(f.getTitre());
            
            listeFilmAP.retainAll(listeFilmEligible);

            listeFilmP.addAll(listeFilmAP);
                            
            /*listeFilmP.addAll((String[])(generateListeFilmAPlacer().stream().filter((String title) -> 
                    (listeSalle_Concours.stream().filter((sc) -> sc.getSalle().getId() == arg1 
                            && sc.getConcours().getAllFilms().contains(listeFilm.stream().filter((Film f)
                                    -> f.getTitre().equals(title))))).iterator().hasNext())).toArray());*/
        } else
            listeFilmP.addAll(generateListeFilmAPlacer());
        
        Predicate<String> FilmPredicate = f -> Collections.frequency(listeFilmP, f) >= 2;      
        listeFilmP.removeIf(FilmPredicate);
        
        return listeFilmP;
    }
    
    //debugging
    public void debugPlanning()
    {
        System.out.println("Projection affichées depuis la liste : ");
        
        listeProjection.forEach(System.out::println);
        
        System.out.println("\n");
        System.out.println("Projection affichées depuis la structure : ");
        
        
        for(Projection[][] p3 : structureProjection)
            for(Projection[] p2 : p3)
                for(Projection p : p2)
                    if (p != null)
                        System.out.println(p);
        
        System.out.println("\nTest de la structure : projection jour 1, salle 3, horaire 3 : ");
        
        Projection p = structureProjection[0][2][2];
        if(p != null)
            System.out.println("id = " + p.getId() + " Jour = " + p.getJour() + " Film = " + p.getFilm().getTitre() + " Salle = " + p.getSalle().getNom() + " Horaire = " + p.getHoraire() + " est lend = " + p.isProjectionLendemain());
        else
            System.out.println("Pas de projection");
    }
    
    public List<Salle_Concours> getlisteSalle_Concours()
    {
        return listeSalle_Concours;
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
    
    // methode intermédiaire (facilite la vie à la classe Fenetre)
    public int isPossible(String f, int colonne, int jcb2, int h, int affichage)
    {
        int s, numJour;
        
        if(affichage == 0)
        {
            s = colonne;
            numJour = jcb2+1;
        } else {
            s = jcb2;
            numJour = colonne+1;
        }
        
        Film fi = null;
        Salle sa = null;
        Horaire ho = null;
        
        for(Salle salle : listeSalle)
            if(salle.getId() == s)
                sa = salle;
        
        for(Film film : listeFilm)
            if(film.getTitre().equals(f))
                fi = film;
        
        for(Horaire horaire : listeHoraire)
            if(horaire.getId() == h)
                ho = horaire;
        
        return isPossible(fi, sa, numJour, ho);
    }
    
    /*
        Vérifie si un certain créneau d'une certaine salle peut accueillir un certain film :
    returne :
        si succès :
            1 : oui et ce sera une projection normale
            2 : oui et ce sera une projection officielle
            3 : oui et ce sera une projection du lendemain
        si echec :
            -1 : ce serait une PL mal placée par rapport aux officielles
            -2 : ce serait une PN doublon alors que le concours est Non Répétable
            -3 : ce serait une PL sans projection officielle associéee
            -4 : inadéquation salle-concours
            -5 : Il y a déjà une projection à ce créneau
            -6 : ce serait une PO mal placée par rapport aux PL associées
            -7 : ce serait une PO doublon alors que le concours est non répétable
            -8 : ce serait une PL doublon alors que le concours est non répétable
        
        Pour le détail de l'algo, il faudra attendre une vraie documentation.
    */
    public int isPossible(Film film, Salle salle, int numJour, Horaire horaire)
    {
        if(structureProjection[numJour-1][salle.getId()][horaire.getId()] != null)
            return -5;
        
        boolean estValide = false;
        boolean estLendemain = false;
        boolean estRepetable = false;
        
        Concours concConcerne = film.getConcours();
        
        for(Salle_Concours sc : listeSalle_Concours)
        {
            if(sc.getConcours().getId() == concConcerne.getId() && sc.getSalle().getId() == salle.getId())
            {
                estValide = true;
                estRepetable = sc.isEstRepetable();
                estLendemain = sc.isSeanceLendemain();
            }
        }
                    
        if (!estValide)
            return -4;
        
        if(estLendemain)
        {
            List<Salle> sallesProjOfficielleAssociees = new ArrayList<>();
            
            listeSalle_Concours.stream().filter((sc) -> (sc.getConcours().getId() == concConcerne.getId() && !sc.isSeanceLendemain())).forEachOrdered((sc) -> {
                sallesProjOfficielleAssociees.add(sc.getSalle());
            });
            
            List<Projection> projectionOffciellesAssociees = new ArrayList<>();
            
            for(int indJour = 0; indJour < 11; indJour++)
            {
                for(int indHoraire = 0; indHoraire < listeHoraire.size(); indHoraire++)
                {
                    for(Salle s : sallesProjOfficielleAssociees)
                        if(structureProjection[indJour][s.getId()][indHoraire] != null
                                && structureProjection[indJour][s.getId()][indHoraire].getFilm().getId() == film.getId())
                            projectionOffciellesAssociees.add(structureProjection[indJour][s.getId()][indHoraire]);
                }
            }
            
            if(projectionOffciellesAssociees.isEmpty())
                return -3;
            
            boolean estBienPlacee = true;
            
            for(Projection p : projectionOffciellesAssociees)
                if(p.getJour() >= numJour)
                    estBienPlacee = false;
            
            if(!estBienPlacee)
                return -1;
            
            boolean estUnique = true;
            
            for(int indJour = 0; indJour < 11 && estUnique; indJour++)
            {
                for(int indHoraire = 0; indHoraire < listeHoraire.size() && estUnique; indHoraire++)
                {
                    if(structureProjection[indJour][salle.getId()][indHoraire] != null 
                            && structureProjection[indJour][salle.getId()][indHoraire].getFilm().getId() == film.getId())
                        estUnique = false;
                }
            }
            
            if(estUnique)
                return 3;
            
            if(estRepetable)
                return 3;
            
            return -8;
            
        } else { //estLendemain est false
            
            if(concConcerne.aSeanceLendemain())
            {
                List<Salle> sallesProjLendemainAssociees = new ArrayList<>();
            
                listeSalle_Concours.stream().filter((sc) -> (sc.getConcours().getId() == concConcerne.getId() && sc.isSeanceLendemain())).forEachOrdered((sc) -> {
                    sallesProjLendemainAssociees.add(sc.getSalle());
                });

                List<Projection> projectionLendemainAssociees = new ArrayList<>();

                for(int indJour = 0; indJour < 11; indJour++)
                {
                    for(int indHoraire = 0; indHoraire < listeHoraire.size(); indHoraire++)
                    {
                        for(Salle s : sallesProjLendemainAssociees)
                            if(structureProjection[indJour][s.getId()][indHoraire]!= null
                                    && structureProjection[indJour][s.getId()][indHoraire].getFilm().getId() == film.getId())
                                projectionLendemainAssociees.add(structureProjection[indJour][s.getId()][indHoraire]);
                    }
                }
                
                boolean estBienPlacee = true;
                
                for(Projection p : projectionLendemainAssociees)
                    if(p.getJour() <= numJour)
                        estBienPlacee = false;

                if(!estBienPlacee)
                    return -6;
                
                boolean estUnique = true;
            
                for(int indJour = 0; indJour < 11 && estUnique; indJour++)
                {
                    for(int indHoraire = 0; indHoraire < listeHoraire.size() && estUnique; indHoraire++)
                    {
                        if(structureProjection[indJour][salle.getId()][indHoraire] != null
                                && structureProjection[indJour][salle.getId()][indHoraire].getFilm().getId() == film.getId())
                            estUnique = false;
                    }
                }

                if(estUnique)
                    return 2;

                if(estRepetable)
                    return 2;

                return -7;
                
            } else { // concConcerne.aSeanceLendemain() est false
                
                boolean estUnique = true;
                
                for(int indJour = 0; indJour < 11 && estUnique; indJour++)
                {
                    for(int indHoraire = 0; indHoraire < listeHoraire.size() && estUnique; indHoraire++)
                    {
                        if(structureProjection[indJour][salle.getId()][indHoraire] != null 
                                && structureProjection[indJour][salle.getId()][indHoraire].getFilm().getId() == film.getId())
                            estUnique = false;
                    }
                }
                
                if(estUnique)
                    return 1;
                
                if(estRepetable)
                    return 1;
                
                return -2;
            }
        }
    }
    
    // méthode intermédiaire, facilite la vie de la classe Fenetre
    public int isPossible(String f, int numJour, int s,  int h)
    {
        Film film = null;
        for(Film fi : listeFilm)
            if(f.equals(fi.getTitre()))
            {
                film = fi;
                break;
            }
        
        Salle salle = listeSalle.get(s);
        Horaire horaire = listeHoraire.get(h);
        
        return isPossible(film, salle, numJour, horaire);
    }
    
    /* récupère les objets associés aux id en param,
        regarde si le projection est possible, 
        crée le bon type en fct du retour et 
        update les list de PA le cas échéant
    */
    public int newProjection(String f, int numJour, int s,  int h)
    {
        Film film = null;
        for(Film fi : listeFilm)
            if(f.equals(fi.getTitre()))
            {
                film = fi;
                break;
            }
        
        Salle salle = listeSalle.get(s);
        Horaire horaire = listeHoraire.get(h);
        
        int retour = isPossible(film, salle, numJour, horaire);
        
        if(retour > 0)
        {
            Projection p = null;
            switch(retour){
                case 1:
                    p = new Projection(numJour, false, horaire, salle, film);
                break;
                case 2:
                    p = new ProjectionOfficielle(numJour, false, horaire, salle, film);
                    ProjectionOfficielle tmpP = (ProjectionOfficielle)p;
                    for(Projection pl : listeProjection)
                        if(pl instanceof  ProjectionLendemain 
                                && pl.getFilm().getId() == p.getFilm().getId())
                        {
                            ProjectionLendemain tmp = (ProjectionLendemain) pl;
                            tmp.addProjectionOfficielleAssociee(tmpP);
                            tmpP.addProjectionLendemainAssociee(tmp);
                        }
                break;
                case 3:
                    p = new ProjectionLendemain(numJour, true, horaire, salle, film);
                    ProjectionLendemain tmpPl = (ProjectionLendemain)p;
                    for(Projection pl : listeProjection)
                        if(!(pl instanceof  ProjectionLendemain)
                                && pl.getFilm().getId() == p.getFilm().getId())
                        {
                            ProjectionOfficielle tmp = (ProjectionOfficielle) pl;
                            tmp.addProjectionLendemainAssociee(tmpPl);
                            tmpPl.addProjectionOfficielleAssociee(tmp);
                        }
                break;
            }
            listeProjection.add(p);
            structureProjection[numJour-1][salle.getId()][horaire.getId()] = p;
        }
        return retour;
    }
  
    // enregistre le planning et retourne un booléen pour indiquer s'il est valide ou non
    public boolean enregistrerPlanning()
    {
        List<Film> filmAProjeter = new ArrayList<>(listeFilm);
        List<Projection> listeCompProjection = new ArrayList<> (listeProjection);
        
        List<Integer> idConcoursSL = new ArrayList<>();
        
        listeSalle_Concours.stream().filter((sc) -> (sc.isSeanceLendemain())).filter((sc) -> (!idConcoursSL.contains(sc.getConcours().getId()))).forEachOrdered((sc) -> {
            idConcoursSL.add(sc.getConcours().getId());
        });
        
        idConcoursSL.forEach((Integer i) -> {
            listeFilm.stream().filter((f) -> (f.getConcours().getId() == i)).forEachOrdered((f) -> {
                filmAProjeter.add(f);
            });
        });
        
        
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
        
        daoProjection.saveAll(listeProjection);
        
        generationPDF();
        
        return programmeValide;
    }
    
    // on vérifie que la projection est supprimable (c'est pas une PO avec PL)
    // on met à jour les list de PA le cas échéant, on supprime
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
        
        if(p instanceof ProjectionLendemain)
        {
            for(Projection pro : listeProjection)
            {
                if(pro instanceof ProjectionOfficielle 
                        && pro.getFilm().getId() == p.getFilm().getId())
                {
                    ProjectionOfficielle tmp = (ProjectionOfficielle) pro;
                    tmp.getProjectionLendemainAssociee().remove(p);
                }
            }
        }
        
        
        listeProjection.remove(p);
        structureProjection[p.getJour()-1][p.getSalle().getId()][p.getHoraire().getId()] = null;
        
        return true;
    }
    
    // les 4 méthodes sont suivantes sont obsolètes mais conservées au cas où
    
    public boolean[][] placementFilm(Film f, int numJour, boolean jour)
    {
        Concours concAssocie = f.getConcours();
        List<Salle> sallesAutorisees = new ArrayList<>();
        
        listeSalle_Concours.stream().filter((sc) -> (sc.getConcours().getId() == concAssocie.getId())).forEachOrdered((sc) -> {
            sallesAutorisees.add(sc.getSalle());
        });
        
        boolean[][] tab = new boolean[listeSalle.size()][listeHoraire.size()];
        
        for(int indSalle = 0; indSalle < listeSalle.size(); indSalle++)
        {
            if(sallesAutorisees.contains(listeSalle.get(indSalle)))
            {
                for(int indHoraire = 0; indHoraire < listeHoraire.size(); indHoraire++)
                    tab[indSalle][indHoraire] = structureProjection[numJour-1][indSalle][indHoraire] == null;
            } else {
                for(int indHoraire = 0; indHoraire < listeHoraire.size(); indHoraire++)
                    tab[indSalle][indHoraire] = false;
            }
        }
        return tab;
    }
    
    public boolean[][] placementFilm(String f, int j, boolean jour)
    {
        Film fl = null;
        
        for(Film film : listeFilm)
            if(film.getTitre().equals(f))
                fl = film;
        
        boolean[][] tab = new boolean[j][listeHoraire.size()];
        
        return placementFilm(fl, j, true);
    }
     
    public boolean[][] placementFilm(Film f, Salle s)
    {
        boolean[][] tab = new boolean[11][listeHoraire.size()];
        
        for(int indJour = 0; indJour < 11; indJour++)
        {
                for(int indHoraire = 0; indHoraire < listeHoraire.size(); indHoraire++)
                    tab[indJour][indHoraire] = true;
        }
        
        return tab;
    }
    
    public boolean[][] placementFilm(String f, int s)
    {
        Film fl = null;
        
        for(Film film : listeFilm)
            if(film.getTitre().equals(f))
                fl = film;
        
        Salle sl = null;
        
        for(Salle salle : listeSalle)
            if(salle.getId() == s)
                sl = salle;
        
        
        return placementFilm(fl, sl);
    }
    
    // c'est déjà très sémantique je trouve
     public void generationPDF() 
    {
        String userprofile = System.getenv("USERPROFILE");
        String name = "Generation des projections.pdf";
        
        Rectangle pagesize = new Rectangle(680, 2024);
        Document document = new Document(pagesize,0,0,0,0);
        
        try
        {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(name));
            document.open();
            
            document.add(Image.getInstance("pdfneeded/projectionsCannes2018-001.jpg"));
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/2haut.jpg"));
            
            PdfPTable table = new PdfPTable(3);
      
            document.add(new Paragraph("   "));
            
            PdfPCell z = new PdfPCell(new Phrase("Jour"));
            z.setHorizontalAlignment(Element.ALIGN_CENTER);
            z.setVerticalAlignment(Element.ALIGN_MIDDLE);
            z.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(z);

            PdfPCell e = new PdfPCell(new Phrase("Horaire"));
            e.setHorizontalAlignment(Element.ALIGN_CENTER);
            e.setVerticalAlignment(Element.ALIGN_MIDDLE);
            e.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(e);    

            PdfPCell d = new PdfPCell(new Phrase("Titre"));
            d.setHorizontalAlignment(Element.ALIGN_CENTER);
            d.setVerticalAlignment(Element.ALIGN_MIDDLE);
            d.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(d);
                        
            BaseColor bc = new BaseColor(253,229,201);
            for (Projection p : listeProjection)
            {
                if (p.getSalle().getId() == 0)
                {
                    if (p.getFilm().getConcours().getId() == 1)
                    {
                        PdfPCell a = new PdfPCell(new Phrase(String.valueOf(p.getJour())));
                        a.setHorizontalAlignment(Element.ALIGN_CENTER);
                        a.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        a.setBackgroundColor(bc);
                        a.setFixedHeight(75);
                        table.addCell(a);
                        
                        PdfPCell b = new PdfPCell(new Phrase(p.getHoraire().getHoraire()));
                        b.setHorizontalAlignment(Element.ALIGN_CENTER);
                        b.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        b.setBackgroundColor(bc);
                        table.addCell(b);  
                        
                        PdfPCell c = new PdfPCell(new Phrase(p.getFilm().getTitre()));
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        c.setBackgroundColor(bc);
                        table.addCell(c);
                        
                        if(bc.getRed() == 253 && bc.getGreen() == 229 && bc.getBlue() == 201)
                            bc = new BaseColor(254,242,228);
                        else
                            bc = new BaseColor(253,229,201);
                    }
                }
            }
            
            document.add(table);
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/3haut.jpg"));
            
            PdfPTable table2 = new PdfPTable(3);
      
            document.add(new Paragraph("   "));
            
            PdfPCell a = new PdfPCell(new Phrase("Salle"));
            a.setHorizontalAlignment(Element.ALIGN_CENTER);
            a.setVerticalAlignment(Element.ALIGN_MIDDLE);
            a.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table2.addCell(a);

            PdfPCell b = new PdfPCell(new Phrase("Horaire"));
            b.setHorizontalAlignment(Element.ALIGN_CENTER);
            b.setVerticalAlignment(Element.ALIGN_MIDDLE);
            b.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table2.addCell(b);    

            PdfPCell c = new PdfPCell(new Phrase("Titre"));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table2.addCell(c);
                
            for (Projection p : listeProjection)
            {
                if (p.getSalle().getId() == 0 || p.getSalle().getId() == 3)
                {
                    if (p.getFilm().getConcours().getId() == 3)
                    {
                        PdfPCell w = new PdfPCell(new Phrase(p.getSalle().getNom()));
                        w.setHorizontalAlignment(Element.ALIGN_CENTER);
                        w.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        w.setFixedHeight(75);
                        w.setBackgroundColor(bc);
                        table2.addCell(w);
                        
                        PdfPCell x = new PdfPCell(new Phrase(p.getHoraire().getHoraire()));
                        x.setHorizontalAlignment(Element.ALIGN_CENTER);
                        x.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        x.setBackgroundColor(bc);
                        table2.addCell(x);  
                        
                        PdfPCell v = new PdfPCell(new Phrase(p.getFilm().getTitre()));
                        v.setHorizontalAlignment(Element.ALIGN_CENTER);
                        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        v.setBackgroundColor(bc);
                        table2.addCell(v);
                        
                        if(bc.getRed() == 253 && bc.getGreen() == 229 && bc.getBlue() == 201)
                            bc = new BaseColor(254,242,228);
                        else
                            bc = new BaseColor(253,229,201);
                    }
                }
            }
            
            document.add(table2);
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/4haut.jpg"));
            
            PdfPTable table3 = new PdfPTable(2);
      
            document.add(new Paragraph("   "));
            
            PdfPCell q = new PdfPCell(new Phrase("Horaire"));
            q.setHorizontalAlignment(Element.ALIGN_CENTER);
            q.setVerticalAlignment(Element.ALIGN_MIDDLE);
            q.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table3.addCell(q);

            PdfPCell s = new PdfPCell(new Phrase("Titre"));
            s.setHorizontalAlignment(Element.ALIGN_CENTER);
            s.setVerticalAlignment(Element.ALIGN_MIDDLE);
            s.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table3.addCell(s);    
            
            for (Projection p : listeProjection)
            {
                if (p.getSalle().getId() == 1)
                {
                    if (p.getFilm().getConcours().getId() == 2)
                    {
                        PdfPCell w = new PdfPCell(new Phrase(p.getHoraire().getHoraire()));
                        w.setHorizontalAlignment(Element.ALIGN_CENTER);
                        w.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        w.setFixedHeight(75);
                        w.setBackgroundColor(bc);
                        table3.addCell(w);
                        
                        PdfPCell x = new PdfPCell(new Phrase(p.getFilm().getTitre()));
                        x.setHorizontalAlignment(Element.ALIGN_CENTER);
                        x.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        x.setBackgroundColor(bc);
                        table3.addCell(x);  
                        
                        if(bc.getRed() == 253 && bc.getGreen() == 229 && bc.getBlue() == 201)
                            bc = new BaseColor(254,242,228);
                        else
                            bc = new BaseColor(253,229,201);
                    }
                }
            }
            
            document.add(table3);
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/5haut.jpg"));
            
            PdfPTable table4 = new PdfPTable(3);
      
            document.add(new Paragraph("   "));
            
            PdfPCell m = new PdfPCell(new Phrase("Jour"));
            m.setHorizontalAlignment(Element.ALIGN_CENTER);
            m.setVerticalAlignment(Element.ALIGN_MIDDLE);
            m.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table4.addCell(m);

            PdfPCell o = new PdfPCell(new Phrase("Horaire"));
            o.setHorizontalAlignment(Element.ALIGN_CENTER);
            o.setVerticalAlignment(Element.ALIGN_MIDDLE);
            o.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table4.addCell(o);    

            PdfPCell i = new PdfPCell(new Phrase("Titre"));
            i.setHorizontalAlignment(Element.ALIGN_CENTER);
            i.setVerticalAlignment(Element.ALIGN_MIDDLE);
            i.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table4.addCell(i);

            for (Projection p : listeProjection)
            {
                if (p.getSalle().getId() == 1 || p.getSalle().getId() == 2)
                {
                    if (p.getFilm().getConcours().getId() == 4)
                    {
                        PdfPCell w = new PdfPCell(new Phrase(String.valueOf(p.getJour())));
                        w.setHorizontalAlignment(Element.ALIGN_CENTER);
                        w.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        w.setFixedHeight(75);
                        w.setBackgroundColor(bc);
                        table4.addCell(w);
                        
                        PdfPCell x = new PdfPCell(new Phrase(p.getHoraire().getHoraire()));
                        x.setHorizontalAlignment(Element.ALIGN_CENTER);
                        x.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        x.setBackgroundColor(bc);
                        table4.addCell(x);  
                        
                        PdfPCell v = new PdfPCell(new Phrase(p.getFilm().getTitre()));
                        v.setHorizontalAlignment(Element.ALIGN_CENTER);
                        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        v.setBackgroundColor(bc);
                        table4.addCell(v);
                        
                        if(bc.getRed() == 253 && bc.getGreen() == 229 && bc.getBlue() == 201)
                            bc = new BaseColor(254,242,228);
                        else
                            bc = new BaseColor(253,229,201);
                    }
                }
            }
            
            document.add(table4);
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/6haut.jpg"));
            
            PdfPTable table5 = new PdfPTable(3);
      
            document.add(new Paragraph("   "));
            
            PdfPCell g = new PdfPCell(new Phrase("Jour"));
            g.setHorizontalAlignment(Element.ALIGN_CENTER);
            g.setVerticalAlignment(Element.ALIGN_MIDDLE);
            g.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table5.addCell(g);

            PdfPCell h = new PdfPCell(new Phrase("Horaire"));
            h.setHorizontalAlignment(Element.ALIGN_CENTER);
            h.setVerticalAlignment(Element.ALIGN_MIDDLE);
            h.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table5.addCell(h);    

            PdfPCell j = new PdfPCell(new Phrase("Titre"));
            j.setHorizontalAlignment(Element.ALIGN_CENTER);
            j.setVerticalAlignment(Element.ALIGN_MIDDLE);
            j.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table5.addCell(j);
              
            for (Projection p : listeProjection)
            {
                if (p.getSalle().getId() == 3 || p.getSalle().getId() == 4)
                {
                    if (p.isProjectionLendemain())
                    {
                        PdfPCell w = new PdfPCell(new Phrase(String.valueOf(p.getJour())));
                        w.setHorizontalAlignment(Element.ALIGN_CENTER);
                        w.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        w.setFixedHeight(75);
                        w.setBackgroundColor(bc);
                        table5.addCell(w);
                        
                        PdfPCell x = new PdfPCell(new Phrase(p.getHoraire().getHoraire()));
                        x.setHorizontalAlignment(Element.ALIGN_CENTER);
                        x.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        x.setBackgroundColor(bc);
                        table5.addCell(x);  
                        
                        PdfPCell v = new PdfPCell(new Phrase(p.getFilm().getTitre()));
                        v.setHorizontalAlignment(Element.ALIGN_CENTER);
                        v.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        v.setBackgroundColor(bc);
                        table5.addCell(v);
                        
                        if(bc.getRed() == 253 && bc.getGreen() == 229 && bc.getBlue() == 201)
                            bc = new BaseColor(254,242,228);
                        else
                            bc = new BaseColor(253,229,201);
                    }
                }
            }
            
            document.add(table5);
            
            document.newPage();
            document.add(Image.getInstance("pdfneeded/projectionsCannes2018-010.jpg"));
            
                    
            document.close();
            writer.close();
           
        } 
        catch (DocumentException e)
        {
           e.printStackTrace();
        } 
        catch (FileNotFoundException e)
        {
           e.printStackTrace();
        } catch (IOException ex) {
            Logger.getLogger(PlanningHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // file opening 
        if (Desktop.isDesktopSupported()) {
            try {
                File myFile = new File(name);
                Desktop.getDesktop().open(myFile);
            } catch (IOException ex) {
                // no application registered for PDFs
            }
        }

    }
     
    public boolean isPlanningFromBD() {
        return planningFromBD;
    }
    
        void deletePlanningFromBD() {
        daoProjection.deleteAll();
    }


}
