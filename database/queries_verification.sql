-- ============================================================================
-- IoT-Based Health Monitoring System
-- Laboratory Examination SQL Verification Queries
-- Run these queries directly in Oracle SQL*Plus or Oracle SQL Developer
-- to verify database state, relationships, constraints, and transactions.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. VERIFY TABLE CREATION & DDL METADATA
-- ----------------------------------------------------------------------------
SELECT TABLE_NAME, STATUS, NUM_ROWS 
FROM USER_TABLES 
WHERE TABLE_NAME IN ('USERS', 'PATIENTS', 'HEALTH_READINGS')
ORDER BY TABLE_NAME;

-- ----------------------------------------------------------------------------
-- 2. VERIFY CONSTRAINTS (Primary, Foreign Key, Check, Unique)
-- ----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME, SEARCH_CONDITION, STATUS
FROM USER_CONSTRAINTS
WHERE TABLE_NAME IN ('USERS', 'PATIENTS', 'HEALTH_READINGS')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

-- ----------------------------------------------------------------------------
-- 3. JOIN QUERY: PATIENT DEMOGRAPHICS WITH LATEST HEALTH TELEMETRY
-- Demonstrates INNER JOIN, WHERE, and ORDER BY
-- ----------------------------------------------------------------------------
SELECT 
    p.PATIENT_ID,
    p.FULL_NAME,
    p.AGE,
    p.GENDER,
    p.BLOOD_GROUP,
    h.READING_ID,
    h.TEMPERATURE || ' °C' AS TEMP,
    h.HEART_RATE || ' BPM' AS PULSE,
    h.SPO2 || ' %' AS SPO2_LEVEL,
    h.SYSTOLIC_BP || '/' || h.DIASTOLIC_BP AS BLOOD_PRESSURE,
    h.RESPIRATORY_RATE || ' BPM' AS RESP_RATE,
    h.OVERALL_STATUS,
    TO_CHAR(h.RECORDED_AT, 'YYYY-MM-DD HH24:MI:SS') AS TIMESTAMP
FROM PATIENTS p
JOIN HEALTH_READINGS h ON p.PATIENT_ID = h.PATIENT_ID
ORDER BY h.RECORDED_AT DESC;

-- ----------------------------------------------------------------------------
-- 4. AGGREGATE METRICS & CLINICAL SUMMARY (COUNT, AVG, MIN, MAX)
-- ----------------------------------------------------------------------------
SELECT 
    COUNT(DISTINCT p.PATIENT_ID) AS TOTAL_REGISTERED_PATIENTS,
    COUNT(h.READING_ID) AS TOTAL_TELEMETRY_LOGS,
    ROUND(AVG(h.TEMPERATURE), 1) AS AVG_TEMPERATURE_C,
    ROUND(AVG(h.HEART_RATE), 0) AS AVG_HEART_RATE_BPM,
    MIN(h.HEART_RATE) AS MIN_HEART_RATE,
    MAX(h.HEART_RATE) AS MAX_HEART_RATE,
    ROUND(AVG(h.SPO2), 1) AS AVG_SPO2_PCT,
    MIN(h.SPO2) AS MIN_SPO2_RECORDED
FROM PATIENTS p
LEFT JOIN HEALTH_READINGS h ON p.PATIENT_ID = h.PATIENT_ID;

-- ----------------------------------------------------------------------------
-- 5. TRIAGE SEVERITY BREAKDOWN (Aggregates + GROUP BY)
-- ----------------------------------------------------------------------------
SELECT 
    OVERALL_STATUS,
    COUNT(*) AS TOTAL_COUNT,
    ROUND(AVG(HEART_RATE), 1) AS MEAN_HR,
    ROUND(AVG(SPO2), 1) AS MEAN_SPO2
FROM HEALTH_READINGS
GROUP BY OVERALL_STATUS
ORDER BY 
    CASE OVERALL_STATUS 
        WHEN 'CRITICAL' THEN 1 
        WHEN 'WARNING' THEN 2 
        WHEN 'NORMAL' THEN 3 
        ELSE 4 
    END;

-- ----------------------------------------------------------------------------
-- 6. ABNORMAL READINGS REQUIRING IMMEDIATE CLINICAL ATTENTION
-- Demonstrates WHERE with multiple conditions and ORDER BY
-- ----------------------------------------------------------------------------
SELECT 
    h.READING_ID,
    p.FULL_NAME,
    p.CONTACT_NUMBER,
    h.HEART_RATE,
    h.SPO2,
    h.SYSTOLIC_BP,
    h.DIASTOLIC_BP,
    h.OVERALL_STATUS,
    TO_CHAR(h.RECORDED_AT, 'YYYY-MM-DD HH24:MI:SS') AS ALERT_TIME
FROM HEALTH_READINGS h
JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID
WHERE h.OVERALL_STATUS IN ('WARNING', 'CRITICAL')
ORDER BY h.RECORDED_AT DESC;

-- ----------------------------------------------------------------------------
-- 7. TRANSACTION VERIFICATION (DML: INSERT, UPDATE, DELETE with COMMIT/ROLLBACK)
-- ----------------------------------------------------------------------------
-- Savepoint demonstration
SAVEPOINT before_test_insert;

INSERT INTO PATIENTS (FULL_NAME, AGE, GENDER, CONTACT_NUMBER, EMAIL, BLOOD_GROUP, MEDICAL_HISTORY)
VALUES ('Test Transaction Patient', 40, 'Male', '+91 9000000000', 'test.tx@example.com', 'O+', 'Testing Rollback');

-- Verify insertion exists in current transaction
SELECT PATIENT_ID, FULL_NAME FROM PATIENTS WHERE EMAIL = 'test.tx@example.com';

-- Roll back the transaction
ROLLBACK TO SAVEPOINT before_test_insert;

-- Verify rollback succeeded (no rows returned)
SELECT PATIENT_ID, FULL_NAME FROM PATIENTS WHERE EMAIL = 'test.tx@example.com';
