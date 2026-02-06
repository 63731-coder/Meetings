# Rencontres – Human Relationship Creation Application

* Nicoleta Opre
* Fabiola Prenga
* Alessian Noje

## Project Description

A social meeting management application aimed at encouraging real human interactions. The system allows users to discover other people who share common interests, simulate meetings, and accumulate sociability points.

## General Architecture

### Multi-Database System

The project uses **4 different data management systems**, each optimized for a specific type of operation:

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
    ┌─────┴─────┐           │                 │
    │     |     │           │                 │
┌───▼───┐ | ┌────▼───┐  ┌───▼────┐        ┌───▼───┐
│MongoDB│ | │Neo4j   │  │Neo4j   │        │Redis  │
└───────┘ | └────┬───┘  └────────┘        └───────┘
   (1)    |    (2)          (2)                (3)
          |
       ┌───
       │
   ┌───▼─────────┐
   │Elasticsearch│
   └─────────────┘
       (4)
```

### Business Services

The system is structured around **3 main business services**:

| Service            | Responsibility                       | Databases Used                |
| ------------------ | ------------------------------------ | ----------------------------- |
| **UserService**    | User and interest management         | MongoDB, Neo4j, Elasticsearch |
| **MeetingService** | Management of meetings between users | Neo4j                         |
| **PointsService**  | Points and leaderboard management    | Redis                         |

---

## Data Distribution

### 1. **MongoDB** – Structured User Data

**Why MongoDB?**

* Flexible JSON document storage
* Ideal for user data with an evolving schema

**Stored data:**

```json
{
  "_id": "507f1f77bcf86cd799439011",
  "username": "Alice",
  "email": "alice@example.com",
  "bio": "Passionate about cooking and hiking",
  "interests": ["cooking", "hiking", "photography"],
  "location": "Brussels"
}
```

### 2. **Neo4j** – Social Relationship Graph

**Why Neo4j?**

* Natural modeling of relationships between people
* Optimized graph queries (recommendations, paths)
* Social network analysis

**Graph model:**

```cypher
(User1:User {id, username, bio, interests, location})
    -[:MET {meetingDate, location}]->
(User2:User {id, username, bio, interests, location})
```

**Implemented advanced queries:**

1. **Meeting count:**

```cypher
MATCH (u:User {id: $userId})-[:MET]-()
RETURN count(*) as meetingCount
```

2. **Most active users:**

```cypher
MATCH (u:User)-[m:MET]-()
RETURN u.id as userId, count(m) as meetingCount
ORDER BY meetingCount DESC
LIMIT 10
```

3. **Recommendations based on common connections:**

```cypher
MATCH (me:User {id: $userId})-[:MET]-(common)-[:MET]-(suggestion)
WHERE me <> suggestion
AND NOT (me)-[:MET]-(suggestion)
RETURN DISTINCT suggestion.id, count(common) as commonMeetings
ORDER BY commonMeetings DESC
```

4. **Recent meetings:**

```cypher
MATCH (u1:User)-[m:MET]-(u2:User)
WHERE m.meetingDate > $since
RETURN m, u1, u2
ORDER BY m.meetingDate DESC
```

---

### 3. **Redis** – Real-Time Cache and Leaderboard

**Why Redis?**

* Ultra-fast performance
* Optimized data structures (Sorted Sets for leaderboards)
* Ideal for volatile and frequently accessed data

**Used structures:**

1. **Individual scores** (String):

   * Key: `score:{userId}`
   * Value: number of points

2. **Leaderboard** (Sorted Set – ZSET):

   * Key: `leaderboard`
   * Members: userIds
   * Scores: total points

**Advanced Redis operations:**

```java
// Top 10 users
ZREVRANGE leaderboard 0 9 WITHSCORES

// User rank
ZREVRANK leaderboard {userId}

// Total score
GET score:{userId}
```

**Advantages:**

* Real-time ranking without costly SQL queries
* Atomic score updates

---

### 4. **Elasticsearch** – Full-Text Search

**Why Elasticsearch?**

* Optimized full-text search engine
* Fuzzy search and relevance scoring
* Inverted indexing for high performance

**Indexed data:**

```json
{
  "id": "507f1f77bcf86cd799439011",
  "username": "Alice",
  "bio": "Passionate about cooking and hiking",
  "interests": ["cooking", "hiking", "photography"],
  "location": "Brussels"
}
```

**Search queries:**

1. **Full-text search:**

```java
findByUsernameContainingOrBioContaining(query, query)
```

2. **Search by interest:**

```java
findByInterests(interest)
```

3. **Advanced multi-field search:**

```java
@Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"username^2\", \"bio\", \"interests\"]}}")
```

---

## Synchronization Strategy (Triple Write)

### Triple Write Principle

When a user is created, the data is replicated across the 3 systems:

```java
public UserDoc registerUser(UserDoc user) {
    // 1. Source of truth: MongoDB (complete data)
    UserDoc savedUser = userMongoRepository.save(user);
    
    // 2. Relationship graph: Neo4j
    createNeo4jNode(savedUser);
    
    // 3. Search engine: Elasticsearch
    indexUserInElasticsearch(savedUser);
    
    return savedUser;
}
```

### Architecture Justification

#### A. UserService (Profile and Interest Management)

**Technologies used:** MongoDB (1) + Neo4j (2) + Elasticsearch (4)

**Justifications:**

* **MongoDB**: User profiles and interests are semi-structured data that may evolve. The document model allows storing variable-length interest lists without complex joins.

* **Neo4j**: Used to model social links between users. This is where "who knows whom" is managed, making it easier to recommend new meetings.

* **Elasticsearch**: Supports the "Search" feature by providing efficient full-text search on bios and interests, which traditional databases handle poorly.

---

#### B. MeetingService (Meeting Management)

**Technology used:** Neo4j (2)

**Justification:** A meeting is inherently a graph (a link between two "User" nodes with properties such as location or date). Using Neo4j makes it easy to traverse the network to check, for example, whether two people share mutual connections or have already participated in similar meetings. This directly addresses the client’s requirement for "analysis of created links".

---

#### C. PointsService (Sociability Points)

**Technology used:** Redis (3)

**Justification:** The client requires tracking sociability points. Redis is an in-memory key-value store, ideal for ultra-fast counters. Since points can be updated very frequently during simulations, Redis provides minimal latency and maximum performance for these atomic operations.

---

## Installation and Configuration

### Prerequisites

* Java 17+
* Maven 3.6+
* Docker & Docker Compose

### 1. Start the Infrastructure

```bash
# Start all data services
docker-compose up -d

# Verify that services are running
docker ps
```

**Started services:**

* MongoDB
* Neo4j
* Redis
* Elasticsearch

### 2. Build and Run the Application

```bash
cd rencontres/rencontres

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

**The application will be available at:**

* Web Interface: `http://localhost:8080`

### 3. Verify Connections

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

## Implemented Features

### ✅ User Management

* **Registration**: User creation with triple write
* **Interests**: Free-tag interest array
* **Search**:

  * By name (MongoDB)
  * By interests (MongoDB + Elasticsearch)
  * By location (Neo4j)

### ✅ Simulated Meetings

* **Meeting creation**: Neo4j relationship between 2 users
* **Automatic point allocation**: +10 base points + 5 per interest of the met user
* **Meeting history**: Dedicated page showing all users met by the logged-in user
* **Validation**: Prevents duplicate meetings

### ✅ Sociability Points

* **Automatic allocation**: 15 points per meeting
* **Redis storage**: Individual scores + leaderboard
* **Ranking**: Real-time Top 10
* **User rank**: Position in the global leaderboard

### ✅ Advanced Queries (Phase 2)

#### Neo4j

* Meeting count per user (node degree)
* Most active users (Top N)
* Recommendations based on common connections (distance 2)
* Recent meetings with time filtering

#### Redis

* Leaderboard with Sorted Set (ZSET)
* User rank (ZREVRANK)
* Top N users (ZREVRANGE)

#### Elasticsearch

* Multi-field full-text search
* Relevance boosting (username^2)
* Fuzzy search on bio and interests

#### MongoDB

* Aggregations on popular interests
* User count by location
* Search within arrays (interests)

### ✅ Advanced Queries (Phase 3)

#### Multi-Database Synchronization

* **Triple write**: When a user is created, data is automatically replicated in MongoDB (source of truth), Neo4j (social graph), and Elasticsearch (search engine)
* **Transactional consistency**: Rollback mechanism in case of a write failure in one of the databases
* **Idempotency**: Synchronization operations can be replayed without side effects

#### Cross-Database Queries

* **Enriched search**: Elasticsearch search combined with MongoDB details and Neo4j relationships
* **Smart recommendations**: Use of the Neo4j graph to suggest users sharing common interests (MongoDB)
* **Contextual leaderboard**: Redis leaderboard filtered by location (Neo4j) or interests (MongoDB)

#### Advanced Optimizations

* **Redis cache**: Caching frequent search results to reduce Elasticsearch load
* **Selective indexing**: Only relevant fields are indexed in Elasticsearch (username, bio, interests)
* **Controlled denormalization**: Strategic data replication to avoid costly joins

### ✅ Statistics

* Top interests
* User distribution by location
* Most sociable users (number of interests)
* Global statistics (users, meetings, total points)

