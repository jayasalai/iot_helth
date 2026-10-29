# IoT-Based Health Monitoring System
**College Database Systems Lab Project**  
**Technology Stack:** Java 17+ • JDBC (Oracle Thin Driver) • Oracle Database • HTML5 / CSS3 / JavaScript • Apache Tomcat / Embedded Runner • Maven

---

## 1. Project Overview

The **IoT-Based Health Monitoring System** simulates a real-time clinical healthcare monitoring and telemetry management platform. The application allows healthcare staff to authenticate securely, register in-patients, record continuous biometric sensor streams via an interactive **IoT Telemetry Simulator**, evaluate patient vitals against evidence-based clinical reference thresholds, and generate longitudinal health trends and reports.

All data persistence, transaction control, demographic queries, and clinical aggregates are executed directly on **Oracle Database** via **Java JDBC** (`java.sql.*`).

---

## 2. Architecture & Lab Requirements Visibly Demonstrated

This project is specifically designed for a second-year CSE Database Systems practical exam. Every mandatory requirement is clearly mapped to source files:

| Lab Requirement | Description | Java / SQL Implementation |
| :--- | :--- | :--- |
| **DDL (Data Definition)** | Tables, PK, FK, CHECK, UNIQUE, Indexes, Sequences | [`database/schema.sql`](file:///Users/salai/.gemini/antigravity-ide/scratch/iot-health-monitor/database/schema.sql) |
| **DML: INSERT** | Patient registration and IoT telemetry logging | `PatientDAO.java`, `HealthReadingDAO.java` |
| **DML: SELECT & WHERE** | Filtering by patient ID, demographics, date ranges | `PatientDAO.java`, `HealthReadingDAO.java` |
| **DML: UPDATE** | Modifying patient details with parameterized queries | `PatientDAO.java` (`updatePatient`) |
| **DML: DELETE & ACID TX** | Deleting patient and readings with `COMMIT` & `ROLLBACK` | `PatientDAO.java` (`deletePatient`) |
| **SQL JOINs** | Combining patient records with telemetry logs | `HealthReadingDAO.java`, `DashboardDAO.java` |
| **Aggregates (COUNT, AVG)** | Dashboard totals, triage metrics, population averages | `DashboardDAO.java` (`COUNT`, `AVG`, `ROUND`) |
| **Security & Passwords** | SHA-256 cryptographic hashing | `PasswordUtil.java` |
| **Session Authentication** | HTTP Session management & endpoint filter | `AuthFilter.java`, `AuthServlet.java` |

---

## 3. Directory Structure

```text
iot-health-monitor/
├── pom.xml                                      # Maven build file with Oracle JDBC & dependencies
├── README.md                                    # Comprehensive execution & viva guide
├── .env.example                                 # Environment variable overrides
├── database/
│   ├── schema.sql                               # Oracle DDL (USERS, PATIENTS, HEALTH_READINGS)
│   ├── sample_data.sql                          # Demo in-patients, users, and telemetry
│   └── queries_verification.sql                 # SQL queries for examiner verification
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   └── db.properties                    # Oracle connection settings (URL, user, password)
│   │   ├── java/com/college/iothealth/
│   │   │   ├── ServerMain.java                  # 1-command embedded server runner
│   │   │   ├── config/
│   │   │   │   ├── DatabaseConfig.java          # Configuration manager
│   │   │   │   └── DBConnection.java            # JDBC connection utility & ORA diagnostic
│   │   │   ├── model/
│   │   │   │   ├── User.java                    # User credentials & role
│   │   │   │   ├── Patient.java                 # In-patient demographics
│   │   │   │   ├── HealthReading.java           # IoT telemetry streams
│   │   │   │   └── DashboardStats.java          # Aggregated dashboard metrics
│   │   │   ├── dao/
│   │   │   │   ├── AuthDAO.java                 # Login & password verification
│   │   │   │   ├── PatientDAO.java              # CRUD & ACID transaction
│   │   │   │   ├── HealthReadingDAO.java        # Telemetry ingestion & JOINs
│   │   │   │   └── DashboardDAO.java            # SQL Aggregates & window queries
│   │   │   ├── service/
│   │   │   │   └── VitalsClinicalRules.java     # Thresholds & status priority engine
│   │   │   ├── filter/
│   │   │   │   └── AuthFilter.java              # Session protection filter
│   │   │   ├── servlet/
│   │   │   │   ├── AuthServlet.java             # /api/auth/*
│   │   │   │   ├── PatientServlet.java          # /api/patients/*
│   │   │   │   ├── HealthReadingServlet.java    # /api/vitals/*
│   │   │   │   ├── DashboardServlet.java        # /api/dashboard/*
│   │   │   │   └── HealthCheckServlet.java      # /api/health (Diagnostic)
│   │   │   └── util/
│   │   │       ├── PasswordUtil.java            # SHA-256 cryptographic utility
│   │   │       └── JsonUtil.java                # Gson JSON serialization helper
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── web.xml                      # Standard Servlet deployment descriptor
│   │       ├── index.html                       # Healthcare monitoring dashboard SPA
│   │       ├── login.html                       # Clinical authentication portal
│   │       ├── css/
│   │       │   ├── variables.css                # Color tokens & theme variables
│   │       │   ├── styles.css                   # Layout & typography
│   │       │   └── components.css               # Cards, badges, sliders, modals
│   │       └── js/
│   │           ├── api.js                       # AJAX client for Java servlets
│   │           ├── vitals-rules.js              # Client-side triage evaluator
│   │           ├── charts.js                    # HTML5 Canvas trend graphing engine
│   │           └── app.js                       # UI controller & event handlers
│   └── test/java/com/college/iothealth/
│       ├── VitalsClinicalRulesTest.java         # Threshold & priority unit tests
│       └── PasswordUtilTest.java                # SHA-256 hashing unit tests
```

---

## 4. Setup & Execution Guide (macOS / Linux / Windows)

### Step 1: Verify Java & Maven
Ensure Java 17 or higher and Maven are installed:
```bash
java -version
mvn -version
```

### Step 2: Configure Oracle Database

#### Option A: College Lab Remote Oracle Server
If your college provides an Oracle Database server over LAN:
1. Obtain the **Host IP**, **Port** (usually `1521`), **Service Name** (e.g., `ORCL` or `XE`), **Username**, and **Password**.
2. Update [`src/main/resources/db.properties`](file:///Users/salai/.gemini/antigravity-ide/scratch/iot-health-monitor/src/main/resources/db.properties):
   ```properties
   db.url=jdbc:oracle:thin:@//192.168.1.100:1521/ORCL
   db.username=your_college_username
   db.password=your_college_password
   ```

#### Option B: Local Oracle via Docker (Optional)
If running Oracle locally on macOS using Docker:
```bash
docker run -d -p 1521:1521 -e ORACLE_PASSWORD=oracle gvenzl/oracle-free:latest
```
Set JDBC URL in `db.properties`:
```properties
db.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
db.username=system
db.password=oracle
```

### Step 3: Run Database Scripts in Oracle

Connect to your Oracle instance via **SQL*Plus** or **Oracle SQL Developer**:

```bash
sqlplus system/oracle@localhost:1521/XEPDB1
```

Execute the schema and sample data scripts in order:
```sql
@database/schema.sql
@database/sample_data.sql
```
Verify that tables `USERS`, `PATIENTS`, and `HEALTH_READINGS` are created and populated.

### Step 4: Build and Test Application

Run JUnit tests and package the WAR file using Maven:
```bash
mvn clean test
mvn package
```
All unit tests will execute and verify the password hashing and clinical triage rules.

### Step 5: Start the Application Server

#### Mode 1: 1-Click Embedded Server Runner (Recommended for Examination)
No standalone Tomcat installation required:
```bash
mvn compile exec:java
```
The server will start on port `8080` (or `PORT` specified in `.env` or `db.properties`).

#### Mode 2: Standalone Apache Tomcat (Optional)
If your laboratory requires deploying to a standalone Apache Tomcat server:
1. Build the WAR: `mvn package`
2. Copy `target/iot-health-monitor.war` into your Tomcat `webapps/` directory:
   ```bash
   cp target/iot-health-monitor.war /path/to/tomcat/webapps/
   ```
3. Start Tomcat: `/path/to/tomcat/bin/startup.sh`
4. Access at `http://localhost:8080/iot-health-monitor/`

---

## 5. Demonstration Workflow for Lab Examination

Follow this step-by-step sequence in front of the lab examiner:

1. **Open Application**: Navigate to `http://localhost:8080/login.html`.
2. **Review DB Connection Pill**: Click **Diagnostic Check** or check `/api/health` to demonstrate the active Oracle JDBC thin connection and database product version.
3. **Log In**:
   - Username: `admin`
   - Password: `Admin@123`
   - Show that entering wrong credentials triggers an error banner, and that passwords in Oracle are stored as cryptographic SHA-256 hashes (`USERS` table).
4. **Dashboard Overview**:
   - Point out the 4 stat cards (Total Patients, Total Readings, Normal Vitals, Abnormal Alert Count).
   - Show that these are calculated dynamically using `SELECT COUNT(*)` and `SELECT AVG()` in `DashboardDAO.java`.
5. **Patient Management (CRUD)**:
   - Click **Patient Management** tab.
   - Click **➕ Register New Patient** and enter patient details (e.g., "Vikram Seth", 52, Male, B+, +91 9888877777).
   - Click **Save to Oracle** (demonstrates `INSERT` via `PreparedStatement` with generated keys).
   - Use the **Search bar** to search by name or ID (demonstrates `SELECT ... WHERE LIKE ...`).
   - Click **✏️ Edit** to update phone or notes (demonstrates `UPDATE`).
6. **IoT Telemetry Simulator**:
   - Click **IoT Sensor Simulator** tab.
   - Select the newly registered patient.
   - Move sliders to adjust Temperature, Heart Rate, SpO2, and Blood Pressure.
   - Show how the **Live Triage Preview** dynamically badges each vital based on the priority hierarchy.
   - Click **Preset: Hypoxia/Critical** (SpO2 drops to 87.5% -> Overall status switches to `CRITICAL`).
   - Click **Record Sensor Reading** (demonstrates `INSERT INTO HEALTH_READINGS`).
   - Return to Dashboard to show that the new critical reading appears instantly in the alerts queue.
7. **Clinical History & Trends**:
   - Click **Clinical History & Trends** tab and select the patient.
   - Show the 4 time-series trend graphs (Canvas multi-line graphs for Temperature, Heart Rate, SpO2, and BP).
   - Filter by date range (demonstrates `WHERE TRUNC(RECORDED_AT) BETWEEN ...`).
8. **ACID Transaction & Deletion**:
   - Go to Patient Management.
   - Click **🗑️ Delete** on a test patient.
   - The confirmation modal explains the atomic transaction.
   - Click **Confirm & Execute Transaction**.
   - Explain how `conn.setAutoCommit(false)`, deleting dependent records, deleting the patient, and `conn.commit()` / `conn.rollback()` guarantees database integrity!

---

## 6. Default Demo Credentials

| Role | Username | Password | Database Table |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | `USERS` |
| **Staff Nurse** | `nurse_priya` | `Nurse@123` | `USERS` |

---

## 7. SQL Verification Queries (For Examiner)

Run these queries in **Oracle SQL*Plus** or **SQL Developer** to verify persistent storage:

```sql
-- 1. Verify Patient Registry with Joined Vitals
SELECT p.PATIENT_ID, p.FULL_NAME, p.BLOOD_GROUP, h.TEMPERATURE, h.HEART_RATE, h.SPO2, h.OVERALL_STATUS
FROM PATIENTS p
JOIN HEALTH_READINGS h ON p.PATIENT_ID = h.PATIENT_ID
ORDER BY h.RECORDED_AT DESC;

-- 2. Verify Database Aggregates
SELECT 
    COUNT(DISTINCT p.PATIENT_ID) AS TOTAL_PATIENTS,
    COUNT(h.READING_ID) AS TOTAL_LOGS,
    ROUND(AVG(h.HEART_RATE), 0) AS MEAN_HR,
    ROUND(AVG(h.SPO2), 1) AS MEAN_SPO2
FROM PATIENTS p
LEFT JOIN HEALTH_READINGS h ON p.PATIENT_ID = h.PATIENT_ID;

-- 3. Verify Critical Alerts Requiring Immediate Triage
SELECT h.READING_ID, p.FULL_NAME, h.HEART_RATE, h.SPO2, h.OVERALL_STATUS
FROM HEALTH_READINGS h
JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID
WHERE h.OVERALL_STATUS IN ('WARNING', 'CRITICAL')
ORDER BY h.RECORDED_AT DESC;
```

---

## 8. Common Oracle Errors & Troubleshooting

| Oracle Error | Meaning | How to Fix |
| :--- | :--- | :--- |
| **ORA-01017** | `invalid username/password; logon denied` | Check `db.username` and `db.password` in `src/main/resources/db.properties`. |
| **ORA-12514** | `TNS:listener does not currently know of service requested` | Verify the Pluggable Database (PDB) service name in the JDBC URL (e.g., `XEPDB1` vs `FREEPDB1` vs `ORCL`). Check with `lsnrctl status`. |
| **ORA-12541** | `TNS:no listener` | The Oracle service or Docker container is not running on port 1521. Start the Oracle instance or listener. |
| **ORA-00001** | `unique constraint violated` | You attempted to register a duplicate email in `USERS` or `PATIENTS`. Emails must be unique. |
| **Port Conflict** | `Port 8080 already in use` | Change `server.port=8081` in `db.properties` or set `PORT=8081` in `.env`. |

---

## 9. PAT Viva Voce Cheat Sheet (Second-Year CSE)

### Q1: What is the JDBC architecture?
**Answer:** JDBC (Java Database Connectivity) consists of two layers:
1. **JDBC API:** Standard Java interfaces in `java.sql` (`Connection`, `Statement`, `PreparedStatement`, `ResultSet`, `DriverManager`).
2. **JDBC Driver:** Vendor-specific implementation. We use the **Oracle Thin Driver** (Type 4 pure Java network driver), which converts JDBC calls directly into Oracle SQL*Net network protocol packets without requiring client-side native libraries.

### Q2: Why use PreparedStatement instead of Statement?
**Answer:** 
1. **Security:** PreparedStatement prevents SQL Injection by precompiling the SQL structure and treating user input strictly as literal parameter data.
2. **Performance:** The Oracle database parses and compiles the query execution plan once, allowing efficient repeated executions with different parameter values.

### Q3: What is the difference between executeQuery() and executeUpdate()?
**Answer:**
- `executeQuery()`: Used for `SELECT` queries. It returns a `ResultSet` cursor representing database rows.
- `executeUpdate()`: Used for DML statements (`INSERT`, `UPDATE`, `DELETE`) or DDL. It returns an `int` indicating the number of affected rows.

### Q4: How do you handle transactions in JDBC?
**Answer:** By default, JDBC operates in auto-commit mode (`conn.getAutoCommit() == true`). To manage an atomic transaction across multiple queries:
1. Disable auto-commit: `conn.setAutoCommit(false)`
2. Execute related DML operations (e.g., delete telemetry readings, then delete the patient).
3. If all statements succeed, commit atomically: `conn.commit()`.
4. If an exception occurs, catch `SQLException` and undo all changes: `conn.rollback()`.
5. In a `finally` block, restore `conn.setAutoCommit(true)`.
*(Demonstrated in `PatientDAO.java` `deletePatient`)*.

### Q5: How are passwords stored securely?
**Answer:** Passwords are never stored in plaintext. In `PasswordUtil.java`, we apply the **SHA-256** cryptographic hash algorithm to the plaintext password, generating a 64-character hexadecimal digest. When logging in, the entered password is hashed with SHA-256 and compared with the stored hash using parameterized SQL.

### Q6: How does the application prioritize triage statuses?
**Answer:** Status priority rule: **CRITICAL > WARNING > NORMAL**. If any vital sign (e.g., SpO2 < 90% or Heart Rate > 120 BPM) breaches critical limits, the overall status is immediately flagged as `CRITICAL`, regardless of whether other vitals are normal.
