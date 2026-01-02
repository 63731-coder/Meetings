# 🤝 Rencontres - Application de Rencontres Sociales

**Groupe :** 62098-63731-63737 | **HE2B ESI - Q1 2025-2026**

---

# 1. Introduction - Notre Projet

## Qu'est-ce qu'on a fait ?
**Une application pour encourager les gens à se rencontrer dans la vraie vie**

### Les Fonctionnalités
✅ Créer un profil avec ses centres d'intérêt  
✅ Simuler des rencontres entre utilisateurs  
✅ Gagner des points quand on rencontre quelqu'un  
✅ Chercher des gens par intérêts  
✅ Avoir des suggestions de personnes à rencontrer

### Notre Architecture - 4 Bases de Données Différentes

```
                    SPRING BOOT
    ┌─────────────────────────────────────┐
    │ UserService │ MeetingService │ PointsService │
    └──────┬──────┴────────┬───────┴───────┬──────┘
           │               │               │
    ┌──────▼────┐   ┌──────▼────┐   ┌─────▼────┐   ┌──────────┐
    │  MongoDB  │   │   Neo4j   │   │  Redis   │   │Elasticsearch│
    │           │   │           │   │          │   │            │
    │ Stockage  │   │ Relations │   │Classement│   │ Recherche  │
    │utilisateurs│   │  sociales │   │          │   │            │
    └───────────┘   └───────────┘   └──────────┘   └──────────┘
```

**On vise la Phase 3 🥇** : 4 bases de données bien utilisées

---

# 2. Pourquoi 4 Bases de Données ?

## Notre Stratégie : Chaque Base Pour Sa Spécialité

| Ce qu'on veut faire | Quelle base ? | Pourquoi ? |
|---------------------|---------------|------------|
| **Créer/lire un utilisateur** | MongoDB | Simple et flexible, comme du JSON |
| **Chercher "cuisine" dans les profils** | Elasticsearch | Fait pour la recherche de texte |
| **Savoir qui a rencontré qui** | Neo4j | Parfait pour les relations entre personnes |
| **Afficher le Top 10 du classement** | Redis | Ultra rapide (en mémoire) |

### L'Idée Principale
> **On utilise la bonne base pour le bon travail** = Meilleure performance

---

## Modèles de Données
Comment On Stocke Les Données

### 1. MongoDB - Les Utilisateurs
```json
{
  "username": "Alice",
  "bio": "Passionnée de cuisine italienne",
  "interests": ["cuisine", "voyages", "photographie"],
  "localisation": "Bruxelles"
}
```
**C'est notre base principale** : toutes les infos des utilisateurs

### 2. Neo4j - Les Rencontres
```
(Alice) -[A RENCONTRÉ {date, lieu}]-> (Bob)
```
**Comme un graphe** : facile de voir qui connaît qui

### 3. Redis - Le Classement
```
Classement: Alice → 120 points
            Bob → 95 points
            Charlie → 80 points
```
**Super rapide** : parfait pour afficher le Top 10 en temps réel

### 4. Elasticsearch - La Recherche
```
Chercher "cuisine" → Trouve Alice (dans sa bio)
```
**Moteur de recherche** : comme Google mais pour nos utilisateurs
---
Les Requêtes Avancées

## MongoDB - Compter Les Centres d'Intérêt Populaires
```javascript
// Quel est le centre d'intérêt le plus populaire ?
db.users.aggregate([
  { $unwind: "$interests" },
  { $group: { _id: "$interests", count: { $sum: 1 } }},
  { $sort: { count: -1 } },
  { $limit: 5 }
])
```
**Résultat** : cuisine (145 personnes), voyages (132), photo (98)

## Neo4j - Suggérer Des Personnes à Rencontrer
```cypher
// Les amis de mes amis que je ne connais pas encore
MATCH (moi)-[:MET]-(ami)-[:MET]-(suggestion)
WHERE moi <> suggestion 
  AND NOT (moi)-[:MET]-(suggestion)
RETURN suggestion, count(ami) as amis_communs
ORDER BY amis_communs DESC
```

**Exemple concret** :  
Alice connaît Bob et Charlie  
Bob et Charlie connaissent tous les deux David  
→ **On suggère David à Alice** (2 amis en commun David
* 4. Les Autres Requêtes Avancées

## Redis - Afficher Le Classement Rapidement
```redis
// Top 10 du classement
ZREVRANGE leaderboard 0 9 WITHSCORES
→ Alice: 120 points, Bob: 95 points, Charlie: 80 points...

// Quelle est ma position ?
ZREVRANK leaderboard Alice
→ Position #1 (première du classement)

// Ajouter 10 points à Alice
ZINCRBY leaderboard 10 Alice
```
**Super rapide** : 0.1ms même avec des millions d'utilisateurs

## Elasticsearch - Recherche Intelligente
```json
Chercher: "cuisine italienne"
Dans: username, bio, interests
```
**Résultat** : Trouve "Alice" même si on écrit "cuisne" (avec une faute)

---

## Comparaison Simple

| Opération | Meilleure Base | Temps |
|-----------|----------------|-------|
| Créer un utilisateur | MongoDB | 10ms |
| Chercher "cuisine" | Elasticsearch | 5ms |
| Voir les rencontres d'Alice | Neo4j | 2ms |
| Afficher Top 10 | Redis | 0.1ms ⚡ |
| Suggérer des amis | Neo4j | 10ms |

**Notre approche marche** : chaque base est rapide sur sa spécialité !
| Top 10 leaderboard | 100ms | N/A | **0.1ms** ✅ | N/A |
| Recommandations | ❌ | **10ms** ✅ | N/A | N/A |

**Conclusion** : Chaque système excelle dans son domaine 🎯
5. Comment On Synchronise Les Bases ?

## Le Problème : 4 Bases = 4 Fois Les Données

### Notre Solution : Écrire Au Bon Endroit Au Bon Moment

**Quand on crée un utilisateur** :
```java
public UserDoc registerUser(UserDoc user) {
    // 1. On sauvegarde dans MongoDB (c'est notre base principale)
    UserDoc saved = mongoRepository.save(user);
    
    // 2. On crée aussi un nœud dans Neo4j
    neo4jRepository.createNode(saved);
    
    // 3. On indexe dans Elasticsearch pour la recherche
    elasticsearchRepository.save(saved);
    
    return saved;
}
```

**Quand on crée une rencontre** :
```
1. On crée la relation dans Neo4j
2. On ajoute +10 points dans Redis pour chaque personne
```

### Schéma Simple
```
Créer Alice → 
  ├─→ MongoDB (toutes ses infos) ✅
  ├─→ Neo4j (nœud vide pour futures rencontres) ✅
  └─→ Elasticsearch (pour recherche) ✅
```

**Pourquoi ?** Chaque base est toujours à jour pour faire son travailOpération atomique Redis (thread-safe)
pointsService.addPoints(userId, 10);
// → INCRBY + ZINCRBY atomique
```

---

# 5. Démonstration - Scénario Complet

## Étape 1 : Créer Utilisateurs
```http
POST /api/users
{ "username": "Alice", "interests": ["cuisine", "voyages"] }
{ "username": "Bob", "interests": ["photographie", "voyages"] }
```
**Résultat** : Triple écriture MongoDB + Neo4j + Elasticsearch

## Étape 2 : Rechercher par Intérêt
```http
GET /api/users/search?interest=voyages
```
**6. Démonstration - Un Scénario Complet

## On Crée Deux Utilisateurs
```
Alice : intérêts = cuisine, voyages
Bob : intérêts = photographie, voyages
```
→ Sauvegardés dans MongoDB, Neo4j et Elasticsearch

## On Cherche Des Gens Qui Aiment Les Voyages
```
Recherche: "voyages"
```
→ Elasticsearch nous retourne Alice et Bob

## On Simule Une Rencontre
```
Alice rencontre Bob au Café Central
```
→ Neo4j crée une relation entre Alice et Bob  
→ Redis ajoute +10 points à Alice et +10 points à Bob

---

## On Regarde Le Classement
```
Top 10 du classement:
1. Alice - 120 points
2. Bob - 95 points  
3. Charlie - 80 points
```
→ Redis nous donne le résultat instantanément

## On Demande Des Suggestions Pour Alice
```
Qui Alice devrait-elle rencontrer ?
```
→ Neo4j analyse le graphe et suggère David (ami de Bob)

### Interface Web Simple
Page d'accueil | Recherche | Profils | Classement | Statistiquesque MongoDB pour classements
4. **Qualité du code** : Clean Code | Gestion d'erreurs | Tests

## Apprentissages Clés
💡 **Polyglot Persistence** : Pas besoin d'une base "universelle"
💡 **Performance = Bon choix techno** : Redis ZSET vs MongoDB $sort
💡 **Cohérence éventuelle OK** : Si le use case le permet

--7. Conclusion - Ce Qu'On a Appris

## Ce Qu'On a Réussi ✅

**4 bases de données différentes** bien utilisées  
**Chaque base fait ce qu'elle fait de mieux** :
- MongoDB pour stocker les utilisateurs
- Neo4j pour les relations
- Redis pour le classement rapide
- Elasticsearch pour la recherche

**Des requêtes avancées** dans chaque base :
- Agrégations MongoDB (centres d'intérêt populaires)
- Suggestions Neo4j (amis d'amis)
- Classement Redis ultra-rapide
- Recherche intelligente Elasticsearch

**Architecture propre** : 3 services métiers séparés (UserService, MeetingService, PointsService)

## Ce Qu'On a Compris

💡 **Une seule base ne suffit pas** : chaque base a sa spécialité  
💡 **Le bon outil pour le bon travail** : Redis est 1000x plus rapide que MongoDB pour les classements  
💡 **Synchroniser intelligemment** : écrire dans plusieurs bases au bon moment

---

# Merci ! Des Questions ?

**Groupe 62098-63731-63737**

On peut vous montrer :
- L'application en direct
- Les requêtes dans les bases de données
- Le code source
- La documentation complète (README.md)