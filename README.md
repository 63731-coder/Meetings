# 🤝 Rencontres - Application de Création de Relations Humaines

## 📋 Description du Projet

Application de gestion de rencontres sociales visant à encourager les interactions humaines réelles. Le système permet aux utilisateurs de découvrir d'autres personnes partageant des centres d'intérêt communs, de simuler des rencontres, et d'accumuler des points de sociabilité.

**⚠️ Note importante** : Ce projet est avant tout une démonstration d'architecture logicielle et de gestion multi-bases de données. L'interface utilisateur est volontairement simple pour mettre l'accent sur la qualité de la structure informatique sous-jacente.

---

## 🏗️ Architecture Générale

### Système Multi-Bases de Données

Le projet utilise **4 systèmes de gestion de données différents**, chacun optimisé pour un type spécifique d'opérations :

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ UserService  │  │MeetingService│  │ PointsService│      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
└─────────┼──────────────────┼──────────────────┼──────────────┘
          │                  │                  │
    ┌─────┴─────┐      ┌─────┴─────┐      ┌─────┴─────┐
    │           │      │           │      │           │
┌───▼──┐  ┌────▼───┐  │  ┌────▼───┐  ┌───▼──┐  ┌────▼────┐
│MongoDB│  │Neo4j   │  └──│Neo4j   │  │Redis │  │ElasticS.│
└───────┘  └────────┘     └────────┘  └──────┘  └─────────┘
   (1)        (2)            (2)        (3)         (4)
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
- Requêtes sur des tableaux (centres d'intérêt)

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

**Requêtes spécifiques :**
- Recherche par centres d'intérêt : `findByInterestsContainingIgnoreCase()`
- Statistiques sur les centres d'intérêt populaires (agrégations)
- Comptage d'utilisateurs par localisation

---

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
- Performances ultra-rapides (in-memory)
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
// Top 10 utilisateurs (O(log(N)+M) - très rapide)
ZREVRANGE leaderboard 0 9 WITHSCORES

// Rang d'un utilisateur (O(log(N)))
ZREVRANK leaderboard {userId}

// Score total
GET score:{userId}
```

**Avantages :**
- ⚡ Classement en temps réel sans requête SQL coûteuse
- 🔄 Mise à jour atomique des scores
- 📊 Récupération du top N en O(log(N)+M)

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

## 🔄 Stratégie de Synchronisation (Triple Écriture)

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

| Besoin | Système | Raison |
|--------|---------|--------|
| Lecture complète d'un utilisateur | **MongoDB** | Source de vérité, schéma flexible |
| Analyse de relations sociales | **Neo4j** | Requêtes de graphe optimisées |
| Recherche plein texte | **Elasticsearch** | Indexation inversée, pertinence |
| Classement temps réel | **Redis** | In-memory, Sorted Sets |

**Cohérence des données :**
- Consistance éventuelle acceptée (use case social, non critique)
- Retry possible en cas d'échec d'écriture
- MongoDB comme source de vérité pour reconstruction

---

## 🚀 Installation et Configuration

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
- MongoDB : `localhost:27017`
- Neo4j : `localhost:7474` (interface), `localhost:7687` (bolt)
- Redis : `localhost:6379`
- Elasticsearch : `localhost:9200`

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
- API Swagger : `http://localhost:8080/swagger-ui.html`

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

## 📚 Fonctionnalités Implémentées

### ✅ Gestion des Utilisateurs

- **Inscription** : Création d'utilisateur avec triple écriture
- **Centres d'intérêt** : Tableau de tags libres
- **Recherche** : 
  - Par nom (MongoDB)
  - Par bio (Elasticsearch)
  - Par centres d'intérêt (MongoDB + Elasticsearch)
  - Par localisation (Neo4j)

### ✅ Rencontres Simulées

- **Création de rencontre** : Relation Neo4j entre 2 utilisateurs
- **Attribution automatique de points** : +10 points par rencontre
- **Historique** : Liste des rencontres passées d'un utilisateur
- **Validation** : Empêche les rencontres en double

### ✅ Points de Sociabilité

- **Attribution automatique** : 10 points par rencontre
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

## 🔌 API REST - Endpoints Principaux

### Utilisateurs

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/users` | Créer un utilisateur (triple écriture) |
| GET | `/api/users/search?interest={interest}` | Rechercher par centre d'intérêt |
| GET | `/api/users/fulltext-search?query={text}` | Recherche plein texte (ES) |
| GET | `/api/users/{userId}/meetings` | Rencontres d'un utilisateur |

### Rencontres

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/meetings` | Créer une rencontre (Neo4j) |
| GET | `/api/meetings/user/{userId}` | Rencontres d'un utilisateur |
| GET | `/api/meetings/between?user1={id1}&user2={id2}` | Vérifier une rencontre |
| GET | `/api/meetings-advanced/count/{userId}` | Nombre de rencontres |
| GET | `/api/meetings-advanced/most-active?limit=10` | Utilisateurs les plus actifs |
| GET | `/api/meetings-advanced/suggestions/{userId}` | Recommandations |

### Points & Leaderboard

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/points/leaderboard?limit=10` | Top N utilisateurs |
| GET | `/api/points/rank/{userId}` | Rang d'un utilisateur |
| GET | `/api/points/{userId}` | Points d'un utilisateur |

### Statistiques

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/statistics/top-interests` | Centres d'intérêt populaires |
| GET | `/api/statistics/users-by-location` | Répartition par ville |
| GET | `/api/statistics/global` | Statistiques globales |

---

## 🎯 Démonstration - Scénario d'Utilisation

### 1. Création d'Utilisateurs

```bash
# Alice - Passionnée de cuisine
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Alice",
    "email": "alice@example.com",
    "bio": "Passionnée de cuisine italienne",
    "interests": ["cuisine", "voyages", "photographie"],
    "localisation": "Bruxelles"
  }'

# Bob - Amateur de randonnée
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Bob",
    "email": "bob@example.com",
    "bio": "Amateur de randonnée et nature",
    "interests": ["randonnée", "photographie", "vélo"],
    "localisation": "Bruxelles"
  }'
```

**Résultat :**
- ✅ Données sauvegardées dans **MongoDB**
- ✅ Nœuds créés dans **Neo4j**
- ✅ Indexation dans **Elasticsearch**

### 2. Recherche d'Utilisateurs

```bash
# Recherche par centre d'intérêt commun
curl "http://localhost:8080/api/users/search?interest=photographie"

# Recherche plein texte
curl "http://localhost:8080/api/users/fulltext-search?query=cuisine"
```

### 3. Simulation d'une Rencontre

```bash
curl -X POST http://localhost:8080/api/meetings \
  -H "Content-Type: application/json" \
  -d '{
    "userId1": "67918e3b1e42df01e87e5db5",
    "userId2": "67918e501e42df01e87e5db6",
    "meetingDate": "2026-01-15T14:30:00",
    "location": "Café Central, Bruxelles"
  }'
```

**Résultat :**
- ✅ Relation `[:MET]` créée dans **Neo4j**
- ✅ +10 points ajoutés à Alice dans **Redis**
- ✅ +10 points ajoutés à Bob dans **Redis**
- ✅ Mise à jour du leaderboard (ZSET)

### 4. Consultation du Leaderboard

```bash
curl "http://localhost:8080/api/points/leaderboard?limit=10"
```

### 5. Recommandations

```bash
# Suggestions pour Alice basées sur connexions communes
curl "http://localhost:8080/api/meetings-advanced/suggestions/67918e3b1e42df01e87e5db5?limit=5"
```

---

## 🎓 Justifications Techniques (Phase 3)

### Architecture Évolutive

✅ **Séparation des préoccupations**
- Services métiers découplés
- Repositories abstraits (pattern Repository)
- DTOs pour la communication API

✅ **Polyglot Persistence**
- Chaque base de données utilisée pour ses forces
- Pas de redondance inutile des données
- Synchronisation maîtrisée

✅ **Scalabilité**
- Neo4j : Scalabilité horizontale pour graphe
- Redis : In-memory pour performances extrêmes
- Elasticsearch : Sharding natif pour recherche distribuée
- MongoDB : Sharding pour données utilisateur

### Optimisations Implémentées

#### Neo4j
- Index sur `User.id` pour requêtes rapides
- Requêtes Cypher optimisées (pas de `MATCH` imbriqués inutiles)
- Relations bidirectionnelles évitées (une seule direction)

#### Redis
- Utilisation de Sorted Sets (ZSET) pour leaderboard O(log N)
- Clés préfixées pour éviter collisions
- TTL non nécessaire (données persistantes)

#### Elasticsearch
- Mapping explicite avec boost de pertinence
- Recherche multi-champs avec pondération
- Indexation asynchrone pour performances

#### MongoDB
- Index sur `interests` pour requêtes de recherche
- Agrégations optimisées (pipeline MongoDB)
- Documents légers (pas de données binaires)

### Gestion des Erreurs

```java
@Transactional  // Rollback MongoDB si échec
public UserDoc registerUser(UserDoc user) {
    try {
        UserDoc savedUser = userMongoRepository.save(user);
        createNeo4jNode(savedUser);
        indexUserInElasticsearch(savedUser);
        return savedUser;
    } catch (Exception e) {
        log.error("Erreur lors de l'enregistrement", e);
        throw new RuntimeException("Échec de l'enregistrement");
    }
}
```

---

## 📊 Comparaison des Systèmes

| Critère | MongoDB | Neo4j | Redis | Elasticsearch |
|---------|---------|-------|-------|---------------|
| **Type** | Document | Graphe | Clé-Valeur | Moteur recherche |
| **Utilisation** | Source de vérité | Relations sociales | Classements | Recherche texte |
| **Requête typique** | `find({interests: "X"})` | `MATCH (a)-[:MET]-(b)` | `ZREVRANGE` | `match query` |
| **Performance** | Bonne | Excellente (graphe) | Extrême | Très bonne |
| **Complexité** | Faible | Moyenne | Très faible | Moyenne |
| **Cas d'usage** | CRUD utilisateurs | Recommandations | Leaderboard | Barre de recherche |

---

## 🧪 Tests et Vérification

### Vérifier la Triple Écriture

```bash
# 1. Créer un utilisateur via API
USER_ID=$(curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" -d '{"username":"Test","email":"test@test.com","interests":["test"]}' | jq -r '.id')

# 2. Vérifier dans MongoDB
curl "http://localhost:8080/api/users/$USER_ID"

# 3. Vérifier dans Neo4j (Neo4j Browser)
MATCH (u:User {id: "$USER_ID"}) RETURN u

# 4. Vérifier dans Elasticsearch
curl "http://localhost:8080/api/users/fulltext-search?query=Test"
```

### Vérifier le Leaderboard Redis

```bash
# Via Redis CLI
docker exec -it projet_redis redis-cli
> ZREVRANGE leaderboard 0 -1 WITHSCORES
```

---

## 🎯 Phase du Projet

**Phase Actuelle : Phase 3 🥇 (Projet Excellent)**

### Critères Phase 3 Satisfaits

✅ **Architecture évolutive**
- Services métiers découplés
- Repositories abstraits
- Configuration externalisée

✅ **Répartition réfléchie des données**
- Justification documentée pour chaque choix
- Triple écriture maîtrisée
- Consistance éventuelle acceptable

✅ **Modèles optimisés**
- Index Neo4j sur User.id
- Sorted Sets Redis pour leaderboard
- Mapping Elasticsearch avec boost

✅ **Requêtes complexes**
- Recommandations par connexions communes (Neo4j)
- Agrégations MongoDB
- Recherche multi-champs pondérée (ES)
- Classements Redis O(log N)

✅ **Documentation exemplaire**
- README complet
- Commentaires Javadoc
- Justifications techniques
- Schémas d'architecture

✅ **Démonstration fluide**
- Interface web fonctionnelle
- API REST documentée (Swagger)
- Scénarios d'usage clairs

---

## 👥 Équipe

**Groupe :** 62098-63731-63737

---

## 📝 Licence

Projet académique - HE2B ESI - Architecture & Bases de Données - 2025-2026

---

## 📞 Support

Pour toute question concernant le projet :
- Consulter la documentation API : `http://localhost:8080/swagger-ui.html`
- Vérifier les logs de l'application
- Inspecter les bases de données via leurs interfaces respectives

---

**Date de livraison :** 04 janvier 2026 (23h55)
