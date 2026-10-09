# CloudCostOptimizer — AWS Resource Monitoring & Cost Governance Platform

Enterprise-grade cloud cost optimization platform built with **Spring Boot 3 (Java 17)**, **AWS SDK v2**, **MySQL (Flyway 3NF schema)**, **Deterministic Rule Engine**, and a modern **Thymeleaf + Chart.js** dark glassmorphic dashboard.

---

## 🏗️ System Architecture

```text
               ┌──────────────────────────────────────────────┐
               │    Browser (Thymeleaf + Chart.js UI)         │
               │   • Responsive Glassmorphism • Dark Mode     │
               └───────────────────────┬──────────────────────┘
                                       │ HTTP / JWT
                                       ▼
               ┌──────────────────────────────────────────────┐
               │         Spring MVC REST & Page Layer         │
               │   • SecurityConfig (Stateless JWT Filter)   │
               │   • Thin Controllers (No AWS calls in HTTP)  │
               └───────────────────────┬──────────────────────┘
                                       │
                                       ▼
               ┌──────────────────────────────────────────────┐
               │             Service & Domain Layer           │
               │   • ScanService (Pipeline Orchestrator)      │
               │   • RecommendationEngine (5 Java Rules)      │
               │   • AlertService & ReportService (OpenPDF)   │
               │   • DashboardService (FinOps KPI Analytics)  │
               └───────────┬──────────────────────┬───────────┘
                           │                      │
                  AWS SDK v2 (Read-only)     Spring Data JPA
                           │                      │
                           ▼                      ▼
               ┌───────────────────────┐  ┌───────────────────┐
               │      Amazon Web       │  │    MySQL / RDS    │
               │       Services        │  │  (Flyway 3NF DB)  │
               │ • EC2   • EBS  • EIP  │  └───────────────────┘
               │ • S3    • IAM  • CW   │
               └───────────────────────┘
```

---

## ⚡ Key Features

1. **Deterministic Rule Engine (5 Rules)**:
   - `IDLE_EC2`: Detects running EC2 instances with 7-day average CPU < 10%.
   - `UNATTACHED_EBS`: Detects unattached EBS volumes in `available` state.
   - `UNUSED_EIP`: Detects unassociated Elastic IPs incurring idle hourly charges.
   - `STALE_S3_DATA`: Detects cold standard S3 buckets > 50 GB with objects > 90 days old to transition to Glacier.
   - `INACTIVE_IAM_USER`: Audits stale active access keys (> 90 days old) and dormant IAM users.

2. **Least Privilege Security**:
   - Uses `DefaultCredentialsProvider` for local CLI / Environment variables and IAM Instance Profiles in EC2 production.
   - Read-only AWS operations (`DescribeInstances`, `DescribeVolumes`, `DescribeAddresses`, `ListBuckets`, `GetMetricStatistics`, `ListUsers`).
   - Zero hardcoded access keys.

3. **Optimization Scoring Formula**:
   $$\text{Score} = 100 - \min(100, \text{wastePenalty})$$
   $$\text{wastePenalty} = (\text{Idle EC2} \times 12) + (\text{Unattached EBS} \times 8) + (\text{Unused EIP} \times 6) + (\text{Stale S3} \times 4)$$

4. **Executive PDF Reporting**:
   - Generates high-resolution PDF audit reports (`WEEKLY`, `MONTHLY`, `OPTIMIZATION`) with OpenPDF.

---

## 🗄️ Database Design (MySQL, 3NF)

- `users`: User entity with BCrypt password hashes and `ADMIN` / `USER` roles.
- `scan_jobs`: Execution record of each scan (Health score, estimated cost & savings, status, timing).
- `resource_snapshots`: System of record for discovered cloud state (EC2, EBS, EIP, S3, IAM) with JSON attributes.
- `resource_metrics`: 7-day CloudWatch time-series metrics.
- `recommendations`: Actionable findings with calculated monthly ROI and resolution lifecycle (`OPEN`, `ACKNOWLEDGED`, `DISMISSED`).
- `alerts`: Immediate notifications for high and critical cost anomalies.
- `reports`: Metadata and file paths for generated PDF reports.
- `activity_logs`: Immutable security audit trail.

---

## 🚀 Quickstart & Local Setup

### 1. Prerequisites
- **Java 17+**
- **Maven 3.9+**
- **MySQL 8.0+** (or default H2 for testing)

### 2. Configure Database
Set environment variables or use the defaults in `src/main/resources/application.yml`:

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="cloud_optimizer"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

### 4. Docker Deployment (Production)
```bash
docker compose up --build -d
```

### 5. Access the Platform
- Open **http://localhost:8080**
- Default Admin Account:
  - **Email**: `admin@cloudops.local`
  - **Password**: `Admin@123`

---

## 🔒 AWS Least-Privilege IAM Policy

Attach this read-only IAM policy to your AWS IAM User or EC2 Instance Role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CostOptimizationReadOnly",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeVolumes",
        "ec2:DescribeAddresses",
        "cloudwatch:GetMetricData",
        "cloudwatch:GetMetricStatistics",
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation",
        "s3:ListBucket",
        "iam:ListUsers",
        "iam:GetAccessKeyLastUsed"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## 🧭 How to Use the Platform

1. **Sign In**: Navigate to `http://localhost:8080/login` with default admin credentials (`admin@cloudops.local` / `Admin@123`).
2. **Run Cloud Discovery**: Click **"Run Scan Now"** on the Dashboard to trigger all 6 AWS scanners.
3. **Inspect Inventory**: Visit **Resources** to search and filter discovered instances, volumes, IPs, buckets, and IAM identities.
4. **Take Action on Waste**: Visit **Recommendations** to review high/critical findings and click **"Acknowledge"** or **"Dismiss"**.
5. **Monitor Anomalies**: Visit **Alerts** to review real-time budget overruns and resource warnings.
6. **Export Audits**: Visit **Reports** to generate and download multi-page executive PDF reports.
7. **Audit Trail**: Visit **Activity Log** for full compliance and tracking history.

---

## 📡 REST API Reference

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new user |
| `POST` | `/api/auth/login` | Public | Authenticate and obtain JWT |
| `GET` | `/api/dashboard` | USER+ | Fetch aggregated KPI cards & telemetry |
| `POST` | `/api/scans` | ADMIN | Trigger AWS resource scan |
| `GET` | `/api/scans` | USER+ | List scan history |
| `GET` | `/api/resources` | USER+ | Query snapshots (filter with `?type=EC2\|EBS\|EIP\|S3\|IAM`) |
| `GET` | `/api/recommendations` | USER+ | List open findings |
| `PATCH` | `/api/recommendations/{id}` | USER+ | Update status (`ACKNOWLEDGED` / `DISMISSED`) |
| `GET` | `/api/alerts` | USER+ | Fetch active alerts feed |
| `PATCH` | `/api/alerts/{id}/read` | USER+ | Mark alert as read |
| `POST` | `/api/reports` | USER+ | Generate PDF report (`OPTIMIZATION`, `WEEKLY`, `MONTHLY`) |
| `GET` | `/api/reports/{id}/pdf` | USER+ | Download binary PDF export |
| `GET` | `/api/activity` | ADMIN | System audit logs |
