

class RepSalle:
    def __init__(self, salle, prop):
        self._salle = salle
        self._prop = prop 
        self._nb = 0

    def getProp(self):
        return self._prop

    def getSalle(self):
        return self._salle

    def setNb(self, nb):
        self._nb = nb

    def getNb(self):
        return self._nb

    def ajouterFilms(self, nb):
        self._nb += nb

class Film:
    def __init__(self, num, nom, duree):
        self._num = num
        self._nom = nom
        self._duree = duree

    def getNum(self):
        return self._num

    def getNom(self):
        return self._nom

    def getDuree(self):
        return self._duree



class Salle:
    def __init__(self, num, nom, capacite):
        self._num = num
        self._nom = nom
        self._capacite = capacite

    def getNom(self):
        return self._nom

    def getCapacite(self):
        return self._capacite

#class Projection:
    # à écrire

r1 = RepSalle(0,1)
r2 = RepSalle(3,1)

r3 = RepSalle(1,1)
r4 = RepSalle(3,1)

r5 = RepSalle(0,1)

r6 = RepSalle(1, 0.15)
r7 = RepSalle(2, 0.5)
r8 = RepSalle(3, 0.25)
r9 = RepSalle(5, 0.1)

s1 = Salle(0, "GTL", 2400)
s2 = Salle(1, "DEB", 1000)
s3 = Salle(2, "BUN", 500)
s4 = Salle(3, "SOI", 1000)
s5 = Salle(4, "BAZ", 500)

tabFilm = []

for i in range(18 + 10 + 5 + 23):
    tabFilm.append(Film(i+1, "Film1", "2h25"))

tabSalle = []
tabSalle.append(s1)
tabSalle.append(s2)
tabSalle.append(s3)
tabSalle.append(s4)
tabSalle.append(s5)

tabSalleConc = [[0,18,[r1],[r2]], [1,10,[r3],[r4]], [2,5,[r5],[]], [3,23, [r6,r7, r8, r9],[]]]
# généré à partir des infos en base de données -- table SalleConcours
# la forme de la structure de données est [numConcours, nbFilms, [salle1, salle2], [salle2, salle3]]
# avec la première liste de salle supportant les projections normales et la deuxième supportant les séances du lendemain

print("\n------ REPARTITION PRIMAIRE -------")

for c in tabSalleConc:
    print("\nPour le concours numero ", c[0], " :")
    nbFilms = c[1]
    for i in range(2,len(c)):
        sommeCoef = 0
        for rp in c[i]:
            sommeCoef += rp.getProp()
            assert(0 < rp.getProp() <= 1)
            rp.setNb(int(nbFilms * rp.getProp()))
            print("\t- la salle numero ", rp.getSalle(), " contiendra ", rp.getNb(), " films")
        assert(sommeCoef == 1 or len(c[i]) == 0)

print("\nLes affectations concours/salles sont correctes")

for i in range(len(tabSalleConc)):

    sommeFilms = 0

    for c in tabSalleConc[i][2]:
        sommeFilms += c.getNb()

    if sommeFilms != tabSalleConc[i][1]:
        tabSalleConc[i][2][0].ajouterFilms(tabSalleConc[i][1] - sommeFilms)
        print("Concours", tabSalleConc[i][0], ": on rajoute", (tabSalleConc[i][1] - sommeFilms), "films a la salle", tabSalleConc[i][2][0].getSalle())

    if len(tabSalleConc[i][3]) != 0:

        sommeFilmsLendemain = 0

        for c in tabSalleConc[i][3]:
            sommeFilmsLendemain += c.getNb()

        if sommeFilmsLendemain != tabSalleConc[i][1]:
            tab[i][3][0].ajouterFilms(tab[i][1] - sommeFilmsLendemain)
            print("\nConcours ", tabSalleConc[i][0], " : on rajoute ", (tabSalleConc[i][1] - sommeFilmsLendemain), " a la salle ", tabSalleConc[i][3][0].getSalle())


print("\n\n------ REPARTITION FINALE -------")

for c in tabSalleConc:
    print("\nPour le concours numero ", c[0], " :")
    for i in range(2,len(c)):
        for rp in c[i]:
            print("\t- la salle numero ", rp.getSalle(), " contiendra ", rp.getNb(), " films")


    # PROCHAINE ETAPE : on sait cb de films on a par salle pour chaque concours
    # il reste à créer une structure capable d'accueillir les projos à partir du tableau tabSalleConc
    # + donc création d'un tableau 3D, dont l'index de la première dim correspondra au num de salle
    # et l'index de la deuxième au num du jour 
    # et l'index de la troisième au num de l'horaire
    # ensuite on répartit les films de manière optimum dans ce tableau (sans bouger) de salle (<=> première dim)  


    tab_projection = [[[0 for x in range(5)] for y in range7] for z in range[5]]
    # donne un tableu tab_projection[nbSalle][nbJour][nbHoraire]


