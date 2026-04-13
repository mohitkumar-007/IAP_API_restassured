# IAP API Automation Framework

## Overview
Rest Assured API Automation Framework for **IAP Subscription Admin APIs** — End-to-End chaining tests for **Gems**, **Coins**, and **Tournament** Tabs & Passes.

Covers full CRUD lifecycle: Tab creation → Pass creation → Fetch & Validate → Update → Disable/Enable → HTML report generation.

## Tech Stack
| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Rest Assured | 5.4.0 |
| TestNG | 7.9.0 |
| Allure | 2.25.0 |
| Jackson | 2.17.0 |
| Log4j2 | 2.23.1 |
| Lombok | 1.18.32 |

## Project Structure
```
IAP API Automation/
├── pom.xml                              # Maven dependencies
├── testng.xml                           # TestNG suite config (Gems + Coins + Tournament)
├── Jenkinsfile                          # Jenkins CI/CD pipeline (parameterized)
├── src/
│   ├── main/java/com/iap/automation/
│   │   ├── base/
│   │   │   └── BaseTest.java           # Base test with RequestSpecification setup
│   │   ├── clients/
│   │   │   ├── TabApiClient.java       # Tab CRUD API client
│   │   │   └── PassApiClient.java      # Pass CRUD API client
│   │   ├── config/
│   │   │   └── ConfigManager.java      # Properties-based config with env/system override
│   │   ├── payloads/
│   │   │   ├── TabPayloadBuilder.java  # Tab payload builder (Gems, Coins, Tournament)
│   │   │   └── PassPayloadBuilder.java # Pass payload builder (all tab types)
│   │   ├── report/
│   │   │   └── TestReportGenerator.java # Custom HTML report generator
│   │   └── utils/
│   │       └── ResponseHelper.java     # Response logging and field extraction
│   ├── main/resources/
│   │   ├── config.properties            # Base URL, auth token, product config
│   │   └── log4j2.xml                   # Logging configuration
│   └── test/java/com/iap/automation/tests/
│       ├── GemsPassE2ETest.java         # Gems E2E suite (14 TCs)
│       ├── CoinsPassE2ETest.java        # Coins E2E suite (13 TCs)
│       └── TournamentPassE2ETest.java   # Tournament E2E suite (10 TCs)
├── reports/                             # Generated HTML test reports
└── allure-results/                      # Allure report data
```

## API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/subs-admin/api/v1/tab` | Create a new tab |
| PUT | `/subs-admin/api/v1/tab` | Update an existing tab |
| GET | `/subs-admin/api/v1/tabs` | Fetch all tabs |
| POST | `/subs-admin/api/v1/pass` | Create a new pass |
| PUT | `/subs-admin/api/v1/pass` | Update an existing pass |
| GET | `/subs-admin/api/v1/pass/{passId}` | Fetch a pass by ID |

---

## Test Suites

### 1. Gems Pass E2E Suite — `GemsPassE2ETest.java` (14 TCs)
| TC | Method | Description |
|----|--------|-------------|
| 1 | `TC1_CheckIfGemsTabExists` | Fetch all tabs → check if GEMS tab exists |
| 2 | `TC2_CreateGemsTabIfNotExists` | Create GEMS tab if not found → store `tabId` |
| 3 | `TC3_FetchGemsTabById` | Fetch tab by ID → validate tab details |
| 4 | `TC4_DuplicateGemsTabCreationShouldFail` | Attempt duplicate creation → expect 400 |
| 5 | `TC5_Create4GemsPasses` | Create 4 Gems passes under the tab |
| 6 | `TC6_FetchGemsPassesAfterCreation` | Fetch all passes → validate all 4 exist |
| 7 | `TC7_UpdateGemsPasses` | Update all passes (price increase) |
| 8 | `TC8_VerifyUpdatedPasses` | Re-fetch → validate updated prices |
| 10 | `TC10_DisablePassSaleEnabled` | Set `passSaleEnabled = false` |
| 11 | `TC11_VerifyPassSaleDisabled` | Verify sale is disabled |
| 12 | `TC12_EnablePassSaleEnabled` | Set `passSaleEnabled = true` |
| 13 | `TC13_VerifyPassSaleReEnabled` | Verify sale is re-enabled |
| 14 | `TC14_GenerateExecutionReport` | Generate HTML execution report |

### 2. Coins Pass E2E Suite — `CoinsPassE2ETest.java` (13 TCs)
| TC | Method | Description |
|----|--------|-------------|
| 1 | `TC1_CheckIfCoinsTabExists` | Fetch all tabs → check if COINS tab exists |
| 2 | `TC2_CreateCoinsTabIfNotExists` | Create COINS tab if not found → store `tabId` |
| 3 | `TC3_FetchCoinsTabById` | Fetch tab by ID → validate tab details |
| 4 | `TC4_DuplicateCoinsTabCreationShouldFail` | Attempt duplicate creation → expect 400 |
| 5 | `TC5_Create4CoinsPasses` | Create 4 Coins passes under the tab |
| 6 | `TC6_FetchCoinsPassesAfterCreation` | Fetch all passes → validate all 4 exist |
| 7 | `TC7_UpdateCoinsPasses` | Update all passes (price increase) |
| 8 | `TC8_VerifyUpdatedPasses` | Re-fetch → validate updated prices |
| 10 | `TC10_DisablePassSaleEnabled` | Set `passSaleEnabled = false` |
| 11 | `TC11_VerifyPassSaleDisabled` | Verify sale is disabled |
| 12 | `TC12_EnablePassSaleEnabled` | Set `passSaleEnabled = true` |
| 13 | `TC13_VerifyPassSaleReEnabled` | Verify sale is re-enabled |
| 14 | `TC14_GenerateExecutionReport` | Generate HTML execution report |

### 3. Tournament Pass E2E Suite — `TournamentPassE2ETest.java` (10 TCs)
| TC | Method | Description |
|----|--------|-------------|
| 1 | `TC1_CheckIfTournamentTabExists` | Fetch all tabs → check if TOURNAMENT tab exists |
| 2 | `TC2_CreateTournamentTabIfNotExists` | Create TOURNAMENT tab if not found → store `tabId` |
| 3 | `TC3_FetchTournamentTabById` | Fetch tab by ID → validate tab details |
| 4 | `TC4_DuplicateTournamentTabCreationShouldFail` | Attempt duplicate creation → expect 400 |
| 5 | `TC5_Create3TournamentPasses` | Create 3 passes: Tournament-Only, Ad-Free, Tournament+AdFree |
| 6 | `TC6_FetchTournamentPassesAfterCreation` | Fetch all passes → detailed HTML report (11 columns) |
| 7 | `TC7_UpdateTournamentPassPrices` | Update prices (+25 increase) |
| 8 | `TC8_DisablePassSaleEnabled` | Set `passSaleEnabled = false` |
| 9 | `TC9_EnablePassSaleEnabled` | Set `passSaleEnabled = true` |
| 10 | `TC10_GenerateExecutionReport` | Generate HTML execution report |

**Tournament Offering Types:**
- **TOURNAMENT** — `tournamentLimit`, `templates` (IDs: 6003, 6004)
- **ADFREE** — `placesToHideAds` (15 ad locations)
- **TOURNAMENT + ADFREE** — combined offerings

---

## Jenkins CI/CD Pipeline

The `Jenkinsfile` provides a parameterized pipeline with suite selection:

| Parameter | Options | Description |
|-----------|---------|-------------|
| `TEST_SUITE` | `all`, `gems`, `coins`, `tournament` | Which suite to run |
| `BASE_URL` | (configurable) | Backend base URL for target environment |

**Pipeline Stages:**
1. **Clean Old Reports** — Remove previous reports and build artifacts
2. **Checkout** — Pull latest code from SCM
3. **Build & Compile** — `mvn clean compile -DskipTests`
4. **Run Tests** — Execute selected suite

**Post Actions:** Archive HTML reports, publish TestNG results, email report to team.

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Update Auth Token
Edit `src/main/resources/config.properties` and set a valid `auth.token`.

### Run Tests
```bash
# Run all suites
mvn clean test

# Run specific suite
mvn clean test -Dtest="GemsPassE2ETest" -DfailIfNoTests=false
mvn clean test -Dtest="CoinsPassE2ETest" -DfailIfNoTests=false
mvn clean test -Dtest="TournamentPassE2ETest" -DfailIfNoTests=false

# Override base URL
mvn clean test -Dbase.url=http://your-server-url

# Override auth token
mvn clean test -Dauth.token=your-jwt-token
```

### View Allure Report
```bash
mvn allure:serve
```

### Custom HTML Reports
After test execution, HTML reports are generated in the `reports/` directory.

---

## Key Design Decisions
- **API Chaining**: Tests use `dependsOnMethods` to enforce execution order. State (`tabId`, `passIds`) is shared via instance variables.
- **Idempotent Tab Check**: TC1 reuses an existing tab if found, making re-runs safe.
- **Config Override**: Properties can be overridden via `-D` system properties or environment variables.
- **Custom HTML Reports**: Each suite generates a detailed HTML report with pass details (name, price, offering, tags, etc.).
- **Jenkins Integration**: Parameterized pipeline supports selective suite execution with email notifications.
