# IRIS — Incident Response Intelligence System

IRIS is a self-hosted incident response platform built on [Mercury Composable](https://accenture.github.io/mercury-composable/), an event-driven Java framework by Accenture. When a GitHub Actions workflow fails, IRIS automatically triages it — fetching recent commits and service metrics in parallel, assessing severity, storing the incident, posting an alert to Microsoft Teams, and creating a structured report in Notion. All orchestration is defined entirely in YAML flow files with zero controller boilerplate.

---

## Features

- **Automatic incident detection** — GitHub Actions webhook triggers the full pipeline on workflow failure
- **Parallel triage** — commits and service metrics are fetched simultaneously using Mercury's fork-n-join execution
- **Severity assessment** — classifies incidents as `critical`, `high`, `medium`, or `low` based on branch, failure type, and error rates
- **Microsoft Teams alerts** — posts a rich card to your channel on incident creation and resolution
- **Notion incident docs** — creates a structured report page with commits, metrics, and resolution notes, and updates it when resolved
- **Live dashboard** — single-page UI with real-time incident list, stats, simulate and resolve buttons
- **Simulate mode** — trigger a realistic fake incident without needing a real GitHub failure
- **Scheduled health checks** — pings the database every 5 minutes via Mercury's mini-scheduler

---

## Architecture

Every piece of business logic is a stateless `@PreLoad` function. All orchestration lives in YAML flow files. No controllers, no service layers, no tight coupling.

### Incident Flow

```
POST /webhook/github
        │
        ▼
ParseGitHubWebhook        ← validates + extracts fields, returns 202 immediately
        │
      fork ───────────────────────────────────┐
        │                                     │
        ▼                                     ▼
FetchRecentCommits                    FetchServiceMetrics
(GitHub API → last 5 commits)         (Postgres → error rates)
        │                                     │
        └─────────────── join ────────────────┘
                              │
                              ▼
                       AssessSeverity       ← critical / high / medium / low
                              │
                              ▼
                       StoreIncident        ← persists to Postgres
                              │
               fork ──────────────────────────┐
                 │                            │
                 ▼                            ▼
           NotifyTeams                 CreateNotionPage
           (Adaptive Card)             (structured report)
                 │                            │
                 └──────────── join ──────────┘
                                   │
                                   ▼
                           UpdateIncidentRecord  ← writes Notion URL + Teams status
```

### Composable Functions

| Function | Route | Purpose |
|---|---|---|
| `ParseGitHubWebhook` | `v1.github.parse.webhook` | Validates and extracts webhook fields |
| `FetchRecentCommits` | `v1.github.fetch.commits` | Fetches last 5 commits via GitHub API |
| `FetchServiceMetrics` | `v1.metrics.fetch` | Queries error metrics from Postgres |
| `AssessSeverity` | `v1.severity.assess` | Computes severity from branch + metrics |
| `StoreIncident` | `v1.incident.store` | Inserts incident record into Postgres |
| `NotifyTeams` | `v1.teams.notify` | Posts alert card to Teams webhook |
| `CreateNotionPage` | `v1.notion.create.page` | Creates incident report page in Notion |
| `UpdateIncidentRecord` | `v1.incident.update` | Writes Notion URL + Teams status back |
| `ResolveIncident` | `v1.incident.resolve` | Marks incident resolved in Postgres |
| `UpdateNotionStatus` | `v1.notion.update.status` | Patches Notion page Status to Resolved |
| `NotifyTeamsResolved` | `v1.teams.notify.resolved` | Posts resolution card to Teams |
| `SimulateFailure` | `v1.simulate.failure` | Generates realistic fake webhook payload |
| `HealthCheck` | `iris.health` | DB connectivity + open incident count |

### REST Endpoints

| Method | URL | Description |
|---|---|---|
| `POST` | `/webhook/github` | Receive GitHub Actions failure webhook |
| `POST` | `/api/simulate/failure` | Trigger a fake incident (demo) |
| `GET` | `/api/incidents` | List all incidents |
| `GET` | `/api/incidents/{id}` | Get incident by ID |
| `POST` | `/api/incidents/{id}/resolve` | Resolve an incident |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Mercury Composable 4.4.2 |
| Runtime | Java 21 Virtual Threads |
| HTTP | Spring Boot 4.0 + Mercury REST Automation |
| Database | PostgreSQL (Spring JDBC) |
| Scheduling | Mercury Mini-Scheduler |
| Notifications | Microsoft Teams Incoming Webhook |
| Docs | Notion API |
| Code source | GitHub REST API |
| HTTP client | OkHttp 4 |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL
- Mercury Composable source (for local Maven install)

### 1. Install Mercury libraries

```bash
git clone https://github.com/Accenture/mercury-composable
cd mercury-composable
mvn install -pl "system/platform-core,system/rest-spring-4,system/event-script-engine,system/mini-scheduler" -am -DskipTests
```

### 2. Set up Postgres

```bash
createdb iris
```

The schema is created automatically on first startup.

### 3. Configure environment variables

Create a `.env` file in the project root:

```env
# Database (required)
IRIS_DB_URL=jdbc:postgresql://localhost:5432/iris
IRIS_DB_USER=your_postgres_user
IRIS_DB_PASSWORD=your_postgres_password

# GitHub (optional — falls back to demo commits if not set)
GITHUB_TOKEN=ghp_xxxx
GITHUB_OWNER=your-github-username
GITHUB_REPO=your-repo-name

# Microsoft Teams (optional — logs to console if not set)
TEAMS_WEBHOOK_URL=https://yourorg.webhook.office.com/webhookb2/...

# Notion (optional — logs to console if not set)
NOTION_TOKEN=secret_xxxx
NOTION_DATABASE_ID=your-database-id
```

### 4. Build

```bash
mvn package -DskipTests
```

### 5. Run

```bash
set -a && source .env && set +a
java -jar target/incident-response-platform-1.0.0.jar
```

Open `http://localhost:8200` in your browser.

---

## Wiring Up Integrations

### GitHub Webhook
1. Go to your repo → **Settings → Webhooks → Add webhook**
2. Payload URL: `http://your-server:8200/webhook/github`
3. Content type: `application/json`
4. Events: select **Workflow runs**

### Microsoft Teams
1. In Teams, open the channel → `...` → **Connectors**
2. Configure **Incoming Webhook**, name it `IRIS`
3. Copy the webhook URL into `TEAMS_WEBHOOK_URL` in your `.env`

### Notion
1. Go to `https://www.notion.so/profile/integrations` → **New integration**
2. Create a database with columns: `Name` (title), `Severity` (select), `Status` (select), `Repository` (text)
3. Connect your integration to the database via **Connections**
4. Copy the token into `NOTION_TOKEN` and the database ID into `NOTION_DATABASE_ID`

---

## Running Without Integrations

IRIS works fully without any API tokens. GitHub commits fall back to realistic demo data, and Teams/Notion actions log to the console instead. Use the **Simulate Failure** button on the dashboard to trigger the full pipeline end-to-end with no external accounts needed.

---

## Project Structure

```
src/main/
├── java/com/accenture/iris/
│   ├── start/
│   │   ├── MainApp.java          ← entry point, schema init
│   │   └── DbAccess.java         ← static Spring JDBC accessor
│   └── functions/                ← all composable @PreLoad functions
└── resources/
    ├── flows/                    ← Mercury YAML flow definitions
    │   ├── incident-triggered.yml
    │   ├── simulate-failure.yml
    │   ├── resolve-incident.yml
    │   ├── list-incidents.yml
    │   └── get-incident.yml
    ├── flows.yaml                ← flow index
    ├── rest.yaml                 ← REST endpoint configuration
    ├── cron.yaml                 ← scheduled health check
    ├── application.properties
    └── public/
        └── index.html            ← dashboard UI
```

---

## License

Apache 2.0