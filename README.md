

# Rencontres - Application de Création de Relations Humaines

## Description du Projet

Application de gestion de rencontres sociales visant à encourager les interactions humaines réelles. Le système permet aux utilisateurs de découvrir d'autres personnes partageant des centres d'intérêt communs, de simuler des rencontres, et d'accumuler des points de sociabilité.

## Architecture Générale

### Système Multi-Bases de Données

Le projet utilise **4 systèmes de gestion de données différents**, chacun optimisé pour un type spécifique d'opérations :

```
┌───────────────────────────────────────────────────────────┐
│                   Spring Boot Application                 │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ UserService  │  │MeetingService│  │ PointsService│     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────      │
│         │                 │                 │             │
└─────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │
    ┌─────┴───── ┐          │                 │
    │     |      │          │                 │
┌───▼───┐ | ┌────▼───┐  ┌───▼ ───┐        ┌───▼──┐
│MongoDB│ | │Neo4j   │  │Neo4j   │        │Redis │
└───────┘ | └────┬───┘  └────────┘        └──────┘
   (1)    |    (2)          (2)                (3)
          |
       ┌───
       │
   ┌───▼─────────
   │Elasticsearch│
   └─────────────┘
       (4)
```

### Services Métiers

Le système est structuré en **3 services métiers principaux** :

| Service | Responsabilité | Bases utilisées |
|---------|---------------|-----------------|
| **UserService** | Gestion des utilisateurs et centres d'intérêt | MongoDB, Neo4j, Elasticsearch |
| **MeetingService** | Gestion des rencontres entre utilisateurs | Neo4j |
| **PointsService** | Gestion des points et classements | Redis |

---

## 💾 Répartition des Données

### 1. **MongoDB** - Données Structurées des Utilisateurs

**Pourquoi MongoDB ?**
- Stockage de documents JSON flexibles
- Idéal pour les données utilisateur avec schéma évolutif

**Données stockées :**
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "username": "Alice",
  "email": "alice@example.com",
  "bio": "Passionnée de cuisine et de randonnée",
  "interests": ["cuisine", "randonnée", "photographie"],
  "localisation": "Bruxelles"
}
```

### 2. **Neo4j** - Graphe de Relations Sociales

**Pourquoi Neo4j ?**
- Modélisation naturelle des relations entre personnes
- Requêtes de graphe optimisées (recommandations, chemins)
- Analyse des réseaux sociaux

**Modèle de graphe :**
```cypher
(User1:User {id, username, bio, interests, localisation})
    -[:MET {meetingDate, location}]->
(User2:User {id, username, bio, interests, localisation})
```

**Requêtes avancées implémentées :**

1. **Comptage de rencontres** :
```cypher
MATCH (u:User {id: $userId})-[:MET]-() 
RETURN count(*) as meetingCount
```

2. **Utilisateurs les plus actifs** :
```cypher
MATCH (u:User)-[m:MET]-() 
RETURN u.id as userId, count(m) as meetingCount 
ORDER BY meetingCount DESC 
LIMIT 10
```

3. **Recommandations basées sur connexions communes** :
```cypher
MATCH (me:User {id: $userId})-[:MET]-(common)-[:MET]-(suggestion) 
WHERE me <> suggestion 
AND NOT (me)-[:MET]-(suggestion) 
RETURN DISTINCT suggestion.id, count(common) as commonMeetings 
ORDER BY commonMeetings DESC
```

4. **Rencontres récentes** :
```cypher
MATCH (u1:User)-[m:MET]-(u2:User) 
WHERE m.meetingDate > $since 
RETURN m, u1, u2 
ORDER BY m.meetingDate DESC
```

---

### 3. **Redis** - Cache et Leaderboard Temps Réel

**Pourquoi Redis ?**
- Performances ultra-rapides
- Structures de données optimisées (Sorted Sets pour classements)
- Idéal pour les données volatiles et fréquemment accédées

**Structures utilisées :**

1. **Scores individuels** (String) :
   - Clé : `score:{userId}`
   - Valeur : nombre de points

2. **Leaderboard** (Sorted Set - ZSET) :
   - Clé : `leaderboard`
   - Membres : userIds
   - Scores : points totaux

**Opérations avancées Redis :**

```java
// Top 10 utilisateurs
ZREVRANGE leaderboard 0 9 WITHSCORES

// Rang d'un utilisateur
ZREVRANK leaderboard {userId}

// Score total
GET score:{userId}
```

**Avantages :**
- Classement en temps réel sans requête SQL coûteuse
- Mise à jour atomique des scores
---

### 4. **Elasticsearch** - Recherche Plein Texte

**Pourquoi Elasticsearch ?**
- Moteur de recherche full-text optimisé
- Recherche floue et pertinence
- Indexation inversée pour performances

**Données indexées :**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "username": "Alice",
  "bio": "Passionnée de cuisine et de randonnée",
  "interests": ["cuisine", "randonnée", "photographie"],
  "localisation": "Bruxelles"
}
```

**Requêtes de recherche :**

1. **Recherche plein texte** :
```java
findByUsernameContainingOrBioContaining(query, query)
```

2. **Recherche par centre d'intérêt** :
```java
findByInterests(interest)
```

3. **Recherche multi-champs avancée** :
```java
@Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"username^2\", \"bio\", \"interests\"]}}")
```

---

## Stratégie de Synchronisation (Triple Écriture)

### Principe de la Triple Écriture

Lors de la création d'un utilisateur, les données sont répliquées dans les 3 systèmes :

```java
public UserDoc registerUser(UserDoc user) {
    // 1. Source de vérité : MongoDB (données complètes)
    UserDoc savedUser = userMongoRepository.save(user);
    
    // 2. Graphe de relations : Neo4j
    createNeo4jNode(savedUser);
    
    // 3. Moteur de recherche : Elasticsearch
    indexUserInElasticsearch(savedUser);
    
    return savedUser;
}
```

### Justification de l'Architecture

#### A. UserService (Gestion des profils et intérêts)

**Technologies utilisées :** MongoDB (1) + Neo4j (2) + Elasticsearch (4)

**Justifications :**

- **MongoDB** : Les profils utilisateurs et leurs centres d'intérêt sont des données semi-structurées qui peuvent évoluer. Le modèle Document permet de stocker des listes d'intérêts de tailles variables sans jointures complexes.

- **Neo4j** : Pour modéliser les liens sociaux entre utilisateurs. C'est ici que l'on gère qui connaît qui, facilitant ainsi la recommandation de nouvelles rencontres.

- **Elasticsearch** : Répond à la fonctionnalité "Recherche". Il permet une recherche plein texte performante sur les bios ou les intérêts, ce qu'une base classique fait mal.

---

#### B. MeetingService (Gestion des rencontres)

**Technologie utilisée :** Neo4j (2)

**Justification :** Une rencontre est par définition un graphe (un lien entre deux nœuds "Utilisateur" avec des propriétés comme le lieu ou la date). Utiliser Neo4j permet de parcourir facilement le réseau pour voir, par exemple, si deux personnes ont des "amis" communs ou ont déjà participé à des rencontres similaires. Cela répond à l'attente du client sur l'"analyse des liens créés".

---

#### C. PointsService (Points de sociabilité)

**Technologie utilisée :** Redis (3)

**Justification :** Le client demande un suivi des points de sociabilité. Redis est une base Clé-Valeur en mémoire, idéale pour des compteurs ultra-rapides. Comme les points peuvent être mis à jour très souvent lors de simulations, Redis offre une latence minimale et une performance maximale pour ces calculs atomiques.

---

## Installation et Configuration

### Prérequis

- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 1. Démarrer l'Infrastructure

```bash
# Démarrer tous les services de données
docker-compose up -d

# Vérifier que les services sont démarrés
docker ps
```

**Services lancés :**
- MongoDB
- Neo4j
- Redis
- Elasticsearch

### 2. Compiler et Lancer l'Application

```bash
cd rencontres/rencontres

# Compiler le projet
mvn clean install

# Lancer l'application
mvn spring-boot:run
```

**L'application sera accessible sur :**
- Interface Web : `http://localhost:8080`

### 3. Vérifier les Connexions

#### Neo4j Browser
```
URL: http://localhost:7474
Username: neo4j
Password: password
```

#### Elasticsearch
```bash
curl http://localhost:9200/_cluster/health
```

---

## Fonctionnalités Implémentées

### ✅ Gestion des Utilisateurs

- **Inscription** : Création d'utilisateur avec triple écriture
- **Centres d'intérêt** : Tableau de tags libres
- **Recherche** : 
  - Par nom (MongoDB)
  - Par centres d'intérêt (MongoDB + Elasticsearch)
  - Par localisation (Neo4j)

### ✅ Rencontres Simulées

- **Création de rencontre** : Relation Neo4j entre 2 utilisateurs
- **Attribution automatique de points** : +10 points de base + 5 par intérêt de l'utilisateur rencontré
- **Historique des rencontres** : Page dédiée affichant toutes les personnes rencontrées par l'utilisateur connecté
- **Validation** : Empêche les rencontres en double

### ✅ Points de Sociabilité

- **Attribution automatique** : 15 points par rencontre
- **Stockage Redis** : Scores individuels + leaderboard
- **Classement** : Top 10 en temps réel
- **Rang utilisateur** : Position dans le classement global

### ✅ Requêtes Avancées (Phase 2)

#### Neo4j
- Comptage de rencontres par utilisateur (degré du nœud)
- Utilisateurs les plus actifs (top N)
- Recommandations basées sur connexions communes (distance 2)
- Rencontres récentes avec filtrage temporel

#### Redis
- Leaderboard avec Sorted Set (ZSET)
- Rang d'un utilisateur (ZREVRANK)
- Top N utilisateurs (ZREVRANGE)

#### Elasticsearch
- Recherche plein texte multi-champs
- Boost de pertinence (username^2)
- Recherche floue sur bio et intérêts

#### MongoDB
- Agrégations sur centres d'intérêt populaires
- Comptage d'utilisateurs par localisation
- Recherche dans tableaux (interests)

### ✅ Requêtes Avancées (Phase 3)

#### Synchronisation Multi-Bases
- **Triple écriture** : Lors de la création d'un utilisateur, les données sont automatiquement répliquées dans MongoDB (source de vérité), Neo4j (graphe social), et Elasticsearch (moteur de recherche)
- **Cohérence transactionnelle** : Mécanisme de rollback en cas d'échec d'écriture dans l'une des bases
- **Idempotence** : Les opérations de synchronisation peuvent être rejouées sans effet de bord

#### Requêtes Cross-Database
- **Recherche enrichie** : Recherche Elasticsearch combinée avec détails MongoDB et relations Neo4j
- **Recommandations intelligentes** : Utilisation du graphe Neo4j pour suggérer des utilisateurs partageant des centres d'intérêt communs (MongoDB)
- **Leaderboard contextuel** : Classement Redis filtré par localisation (Neo4j) ou centres d'intérêt (MongoDB)

#### Optimisations Avancées
- **Cache Redis** : Mise en cache des résultats de recherche fréquents pour réduire la charge sur Elasticsearch
- **Indexation sélective** : Seuls les champs pertinents sont indexés dans Elasticsearch (username, bio, interests)
- **Dénormalisation contrôlée** : Réplication stratégique des données pour éviter les jointures coûteuses

### ✅ Statistiques

- Top centres d'intérêt
- Répartition des utilisateurs par localisation
- Utilisateurs les plus sociables (nombre d'intérêts)
- Statistiques globales (users, meetings, total points)

---

## 📂 Structure du Projet

```
rencontres/
├── src/main/java/be/esi/rencontres/
│   ├── RencontresApplication.java
│   │
│   ├── auth/
│   │   └── controller/
│   │       └── AuthController.java          # Login/Logout
│   │
│   ├── user/
│   │   ├── controller/
│   │   │   ├── UserController.java          # CRUD utilisateurs
│   │   │   └── StatisticsController.java    # Statistiques
│   │   ├── service/
│   │   │   └── UserService.java             # Logique métier + triple écriture
│   │   ├── repository/
│   │   │   ├── UserMongoRepository.java     # MongoDB
│   │   │   ├── UserNeo4jRepository.java     # Neo4j
│   │   │   └── UserElasticsearchRepository.java  # Elasticsearch
│   │   ├── model/
│   │   │   ├── mongo/UserDoc.java           # Document MongoDB
│   │   │   ├── neo4j/UserNode.java          # Nœud Neo4j
│   │   │   └── elasticsearch/UserDocument.java   # Document ES
│   │   └── dto/
│   │
│   ├── meeting/
│   │   ├── controller/
│   │   │   ├── MeetingController.java       # CRUD rencontres
│   │   │   └── MeetingAdvancedController.java  # Requêtes avancées
│   │   ├── service/
│   │   │   └── MeetingService.java          # Logique métier
│   │   ├── repository/
│   │   │   └── MeetingNeo4jRepository.java  # Relations Neo4j
│   │   └── model/
│   │       └── neo4j/MeetingRelationship.java
│   │
│   ├── points/
│   │   ├── controller/
│   │   │   └── PointsController.java        # Leaderboard
│   │   ├── service/
│   │   │   └── PointsService.java           # Logique métier
│   │   └── repository/
│   │       └── PointsRedisRepository.java   # Opérations Redis
│   │
│   └── infrastructure/
│       └── config/
│           ├── RedisConfig.java             # Configuration Redis
│           └── ElasticsearchConfig.java     # Configuration Elasticsearch
│
├── src/main/resources/
│   ├── application.properties               # Configuration globale
│   └── templates/
│       ├── login.html
│       ├── register.html
│       ├── users.html
│       ├── search.html
│       ├── detail.html
│       ├── leaderboard.html
│       └── statistics.html
│
├── docker-compose.yml                       # Infrastructure Docker
└── pom.xml                                  # Dépendances Maven
```

---
