# IAP API Automation Framework

## Overview
Rest Assured API Automation Framework for IAP Subscription Admin APIs — End-to-End chaining test for Gems Tab and Passes.

## Project Structure
```
IAP API Automation/
├── pom.xml                          # Maven dependencies (RestAssured, TestNG, Allure, Jackson)
├── testng.xml                       # TestNG suite configuration
├── src/
│   ├── main/java/com/iap/automation/
│   │   ├── base/
│   │   │   └── BaseTest.java        # Base test with RequestSpecification setup
│   │   ├── clients/
│   │   │   ├── TabApiClient.java    # Tab CRUD API client
│   │   │   └── PassApiClient.java   # Pass CRUD API client
│   │   ├── config/
│   │   │   └── ConfigManager.java   # Properties-based config with env/system override
│   │   ├── payloads/
│   │   │   ├── TabPayloadBuilder.java   # Tab JSON payload builder
│   │   │   └── PassPayloadBuilder.java  # Pass JSON payload builder (timestamp names)
│   │   └── utils/
│   │       └── ResponseHelper.java  # Response logging and field extraction
│   └── main/resources/
│       ├── config.properties        # Base URL, auth token, product config
│       └── log4j2.xml               # Logging configuration
│   └── test/java/com/iap/automation/tests/
│       └── GemsPassE2ETest.java     # E2E chaining test (7 steps)
```

## E2E Test Flow (API Chaining)
| Step | Test Method | Description |
|------|------------|-------------|
| 1 | `testCheckAndCreateGemsTab` | Fetch all tabs → check if GEMS tab exists → create if missing → store `tabId` |
| 2 | `testCreate4GemsPasses` | Create 4 Gems passes with unique timestamp names under the tab |
| 3 | `testFetchAllPassesAfterCreation` | Fetch all passes by `tabId` → validate all 4 passes exist |
| 4 | `testFetchEachPassById` | Fetch each pass individually by `passId` → validate name, tabId |
| 5 | `testUpdateAll4Passes` | Update all 4 passes (append `_UPDATED` to name, increase price) |
| 6 | `testRefetchAndValidateUpdatedPasses` | Re-fetch all passes → validate updated names and prices |
| 7 | `testFinalIndividualPassValidation` | Final individual pass fetch → validate `_UPDATED` suffix, tabType |

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Update Auth Token
Edit `src/main/resources/config.properties` and set a valid `auth.token`.

### Run Tests
```bash
# Run all tests
mvn clean test

# Run with a different base URL
mvn clean test -Dbase.url=http://your-server-url

# Run with a different auth token
mvn clean test -Dauth.token=your-jwt-token
```

### View Allure Report
```bash
mvn allure:serve
```

## Key Design Decisions
- **API Chaining**: Tests use `dependsOnMethods` to enforce execution order. State (tabId, passIds) is shared via instance variables.
- **Unique Pass Names**: `PassPayloadBuilder.generateUniquePassName()` uses epoch millis to guarantee unique names per run.
- **Config Override**: Properties can be overridden via `-D` system properties or environment variables.
- **Idempotent Tab Check**: Step 1 reuses an existing GEMS tab if found, making re-runs safe.
