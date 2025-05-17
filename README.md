# Stockico - Backend Service

Stockico is a competitive, fast-paced multiplayer game that simulates real-world stock trading. This repository contains the backend service responsible for managing game logic, user data, stock information, and transactions. Players join a game room, analyze stock data, and make strategic transactions over ten rounds to maximize their portfolio value. The game integrates real stock market data via the Alpha Vantage API and enforces financial constraints.

## Introduction

*   **Project Goal:** To create a robust backend service that powers the Stockico stock trading game, handling game state, user management, real-time stock data integration, and transaction processing.
*   **Motivation:** To provide a fun and engaging multiplayer experience that educates players about stock market dynamics in a simulated, competitive environment.

## Technologies Used

*   **Spring Boot:** Core framework for building the RESTful API.
*   **Java 17:** Programming language.
*   **Gradle:** Dependency management and build tool.
*   **Alpha Vantage API:** Source for real-time and historical stock data.
*   **Docker:** For containerization and simplified deployment.
*   **Spring Data JPA:** For database interaction.
*   **POSTGRES Database (Default for Dev):** Database for local development.
*   **JUnit & Mockito:** For unit and integration testing.
*   **MapStruct (Assumed for DTO mapping):** For mapping between entities and DTOs.

## High-level Components

The backend is structured into several key components that work together to deliver the Stockico game experience:

1.  **API Endpoints (Controllers):**
    *   **Role:** Handle incoming HTTP requests from the client, validate input, and delegate business logic processing to the appropriate service components. They define the contract for how the frontend interacts with the backend, often using DTOs for request and response bodies.
    *   **Correlation:** Controllers interact primarily with Service components and use DTOs to structure data exchanged with the client.
    *   **Main Path:** [`src/main/java/ch/uzh/ifi/hase/soprafs24/controller/`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/)
    *   **Main Files:**
        *   [`ChartDataController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/ChartDataController.java): Manages endpoints related to fetching and displaying chart data for stocks.
        *   [`GameController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameController.java): Handles game lifecycle operations like creating, starting, joining games, and game progression.
        *   [`LeaderBoardController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/LeaderBoardController.java): Provides endpoints for retrieving leaderboard information.
        *   [`LobbyController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/LobbyController.java): Manages game lobbies, player readiness, and countdowns.
        *   [`NewsController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/NewsController.java): Delivers news items or events relevant to the game or stocks.
        *   [`StockController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/StockController.java): Handles requests for stock information and potentially player stock holdings.
        *   [`TransactionController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/TransactionController.java): Processes player buy/sell stock transactions.
        *   [`UserController.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/controller/UserController.java): Manages user authentication (login, registration) and user profile information.

2.  **Business Logic (Services & Game Managers):**
    *   **Role:** Contain the core application logic, including game rules, user management, transaction processing, and coordination of data access. They encapsulate complex operations and business rules.
    *   **Correlation:** Services are called by Controllers and use Repositories for data persistence. They may also call other services or external APIs (like Alpha Vantage via a dedicated client). Game-specific managers handle detailed game mechanics.
    *   **Main Paths:** [`src/main/java/ch/uzh/ifi/hase/soprafs24/service/`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/) (for general services) and [`src/main/java/ch/uzh/ifi/hase/soprafs24/game/`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/) (for game-specific logic).
    *   **Main Files (Services):**
        *   [`ChartDataService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/ChartDataService.java): Provides business logic for chart data, possibly interacting with Alpha Vantage.
        *   [`GameService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java): Orchestrates overall game logic, player actions, and game state transitions, possibly using `GameManager`.
        *   [`LobbyService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/LobbyService.java): Handles logic for game lobbies, player joining/leaving, and game start preparations.
        *   [`NewsService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/NewsService.java): Manages the retrieval and provision of news items.
        *   [`StockService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/StockService.java): Contains business logic related to stocks, potentially including fetching data.
        *   [`UserService.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/service/UserService.java): Manages user registration, login, authentication, and profile operations.
    *   **Main Files (from `game/` directory - Game-specific Logic):**
        *   [`GameManager.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/GameManager.java): Likely responsible for managing the detailed state and flow of an active game, including rounds, player turns, and applying game rules.
        *   [`InMemoryGameRegistry.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/InMemoryGameRegistry.java): Possibly manages active game instances or lobbies in memory, providing quick access.

3.  **Data Model (Entities):**
    *   **Role:** Define the structure of the data used within the application, representing users, games, stocks, transactions, and other core concepts. These are typically Plain Old Java Objects (POJOs) annotated for persistence (e.g., with JPA).
    *   **Correlation:** Entities are the primary objects managed by Repositories and often transformed into DTOs by Services or Mappers before being sent to Controllers.
    *   **Main Paths:** [`src/main/java/ch/uzh/ifi/hase/soprafs24/entity/`](src/main/java/ch/uzh/ifi/hase/soprafs24/entity/) (for general entities) and [`src/main/java/ch/uzh/ifi/hase/soprafs24/game/`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/) (for game-specific domain objects).
    *   **Main Files (Entities):**
        *   [`User.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/entity/User.java): Represents a player or user of the system.
    *   **Main Files (from `game/` directory - Game-specific Entities/Models):**
        *   [`Game.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/Game.java): Represents a single game instance, its settings, and status.
        *   [`Lobby.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/Lobby.java): Represents a game lobby before a game starts, holding players.
        *   [`News.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/News.java): Represents a news item or event affecting the game.
        *   [`Stock.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/Stock.java): Represents a tradable stock within the game.
        *   [`StockDataPoint.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/StockDataPoint.java): Represents a specific data point for a stock (e.g., price at a time).
        *   [`LeaderBoardEntry.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/LeaderBoardEntry.java): Represents an entry in the game's leaderboard, typically a player and their score.
        *   [`PlayerState.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/PlayerState.java): Represents the state of a player within a game (e.g., portfolio, cash, stocks owned).
        *   [`Transaction.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/game/Transaction.java): Represents a buy or sell transaction made by a player.

4.  **Data Transfer Objects (DTOs):**
    *   **Role:** Simple objects used to transfer data between layers, especially between the Service layer and the Controller/API layer. They help define the API contract and decouple it from the internal entity structure.
    *   **Correlation:** DTOs are used as request bodies for POST/PUT requests and as response bodies for GET requests. Mappers are often used to convert between Entities and DTOs.
    *   **Main Path:** [`src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/)
    *   **Main Files:**
        *   [`GameStatusDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/GameStatusDTO.java)
        *   [`LeaderBoardEntryDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/LeaderBoardEntryDTO.java)
        *   [`LobbyGetDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/LobbyGetDTO.java)
        *   [`LobbyPostDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/LobbyPostDTO.java)
        *   [`LobbyUserPostDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/LobbyUserPostDTO.java)
        *   [`NewsDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/NewsDTO.java)
        *   [`PlayerStateGetDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/PlayerStateGetDTO.java)
        *   [`RoundStatusDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/RoundStatusDTO.java)
        *   [`StockDataPointDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/StockDataPointDTO.java)
        *   [`StockHoldingDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/StockHoldingDTO.java)
        *   [`StockPriceGetDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/StockPriceGetDTO.java)
        *   [`TransactionRequestDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/TransactionRequestDTO.java)
        *   [`UserGetDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/UserGetDTO.java)
        *   [`UserLoginDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/UserLoginDTO.java)
        *   [`UserPostDTO.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/UserPostDTO.java)

5.  **Data Access (Repositories):**
    *   **Role:** Provide an abstraction layer for all database interactions. They translate service-layer calls into database queries using Spring Data JPA.
    *   **Correlation:** Repositories are interfaces extending Spring Data JPA interfaces. They are used by Service components to persist and retrieve entity data.
    *   **Main Path:** [`src/main/java/ch/uzh/ifi/hase/soprafs24/repository/`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/)
    *   **Main Files:**
        *   [`GameRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/GameRepository.java)
        *   [`LobbyRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/LobbyRepository.java)
        *   [`NewsRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/NewsRepository.java)
        *   [`StockDataPointRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/StockDataPointRepository.java)
        *   [`StockRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/StockRepository.java)
        *   [`UserRepository.java`](src/main/java/ch/uzh/ifi/hase/soprafs24/repository/UserRepository.java)

## Launch & Deployment

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

*   **Java 17 JDK:** Ensure Java 17 is installed. On Windows, also ensure your `JAVA_HOME` environment variable is correctly set to your Java 17 installation directory.
*   **IDE:** An Integrated Development Environment like IntelliJ IDEA (recommended, free educational license available), VS Code with Java extensions, or Eclipse.
*   **Alpha Vantage API Key:** You need a free API key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key) to fetch stock data.
*   **Git:** For cloning the repository.

### External Dependencies (for local development)

*   **Alpha Vantage API:** The application requires an active internet connection and a valid Alpha Vantage API key to fetch stock data.
*   **Database:**
    *   By default, for local development, the application uses an **in-memory H2 database**. This database is created when the application starts and destroyed when it stops, requiring no separate setup. The H2 console can usually be accessed at `http://localhost:8080/h2-console` (check your `application.properties` for exact path and credentials if enabled).
    *   If you configure the application to use an **external database** (e.g., PostgreSQL, MySQL) by modifying `src/main/resources/application.properties`, you must ensure that database server is running and accessible with the configured credentials *before* starting the application.

### Setting up API Key

The application reads the Alpha Vantage API key from an environment variable named `ALPHAVANTAGE_API_KEY`.

**Windows (PowerShell - for current session):**
```powershell
$env:ALPHAVANTAGE_API_KEY="YOUR_SECRET_API_KEY"
```

**Linux/macOS (Terminal - for current session):**

```bash   
export ALPHAVANTAGE_API_KEY="YOUR_SECRET_API_KEY"
```

**Windows (Command Prompt - for current session):**

```shell
set ALPHAVANTAGE_API_KEY=YOUR_SECRET_API_KEY
```
*For persistent storage, add this variable to your system's environment variables or your shell's profile script (e.g., `.bashrc`, `.zshrc`, or IDE run configuration).*

### Installing & Running Locally

**1. Clone the repository:**
 *  ```bash
    git clone https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server.git
    cd sopra-fs25-group-36-server
    ```

**2. Build the project:**

This step compiles your code and downloads all necessary dependencies using the Gradle Wrapper.
*   macOS/Linux:
    ```bash
    ./gradlew build
    ```
*   Windows (Command Prompt/PowerShell):
    ```bash
    .\gradlew.bat build
    ```

**3. Run the application:**

This starts the Spring Boot application.
*   macOS/Linux:
    ```bash
    ./gradlew bootRun
    ```
*   Windows (Command Prompt/PowerShell):
    ```bash
    .\gradlew.bat bootRun
    ```
The server will typically start on http://localhost:8080. You can verify this by opening the URL in your browser or by sending a request to a known API endpoint (e.g., using Postman or curl).

**Example Demo (using curl to check a hypothetical health endpoint):**
```bash
curl http://localhost:8080/users
```

## Launch & Deployment

### Development Mode (Faster Feedback Loop)

For a more efficient development workflow where changes are automatically reloaded:
1.  Open two terminal windows/tabs in the project root directory.
2.  In the **first terminal**, run the continuous build (skipping tests for speed):
    *   macOS/Linux:
        ```bash
        ./gradlew build --continuous -xtest
        ```
    *   Windows (Command Prompt/PowerShell):
        ```bash
        .\gradlew.bat build --continuous -xtest
        ```
3.  In the **second terminal**, run the application:
    *   macOS/Linux:
        ```bash
        ./gradlew bootRun
        ```
    *   Windows (Command Prompt/PowerShell):
        ```bash
        .\gradlew.bat bootRun
        ```
    Spring Boot DevTools (if included as a dependency) should automatically restart the application when class files change, reflecting your code updates without a manual restart.

### Debugging

To debug the application using your IDE:
1.  In your IDE (e.g., IntelliJ IDEA), go to **Run/Edit Configurations**.
2.  Add a new **Remote JVM Debug** configuration.
    *   **Name:** Give it a descriptive name (e.g., "Backend Debug").
    *   **Host:** Set to `localhost`.
    *   **Port:** Set to `5005` (this is a common default, but can be any available port).
3.  Start the application in debug mode from your terminal. This tells the JVM to listen for a debugger connection:
    *   macOS/Linux:
        ```bash
        ./gradlew bootRun --debug-jvm
        ```
    *   Windows (Command Prompt/PowerShell):
        ```bash
        .\gradlew.bat bootRun --debug-jvm
        ```
    *(This command usually makes Spring Boot start the JVM with arguments like `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`, causing it to listen on port 5005).*
4.  In your IDE, run the "Backend Debug" configuration you created (often by pressing `Shift + F9` or a debug icon). The IDE will attach to the running Java process.
5.  Set breakpoints in your Java code where you want to pause execution and inspect variables or step through the code.

## Running the tests

To execute all automated tests (unit and integration tests) defined in the project:
*   macOS/Linux:
    ```bash
    ./gradlew test
    ```
*   Windows (Command Prompt/PowerShell):
    ```bash
    .\gradlew.bat test
    ```
After the tests complete, detailed HTML reports are typically generated in the `build/reports/tests/test/index.html` file. Open this file in a web browser to see the test results.

### Unit & Integration Tests

*   **What these tests test and why:**
    *   **Unit Tests:** These tests verify the correctness of individual, isolated components or methods (e.g., a specific calculation in a `Service` class). They often use mocking frameworks like Mockito to simulate dependencies, ensuring that the unit under test behaves as expected independently. This helps catch bugs early and makes refactoring safer.
    *   **Integration Tests:** These tests verify the interaction and collaboration between multiple components of the application (e.g., a full request flow from a `Controller` through a `Service` to a `Repository`). In Spring Boot, `@SpringBootTest` is commonly used to load the application context, and `@AutoConfigureMockMvc` can be used to test controller endpoints via mock HTTP requests. Integration tests ensure that different parts of the system work together correctly.

*   **Example (Conceptual):** For actual implementations, refer to the test classes located in `src/test/java/ch/uzh/ifi/hase/soprafs24/`. For instance, you might find tests for `UserServiceTest.java` (unit test) or `GameControllerIntegrationTest.java` (integration test).

### Releases

Releases provide stable, versioned snapshots of your application. They are typically managed using Git tags and often automated with CI/CD pipelines (like GitHub Actions).

1.  **Preparation:** Ensure the `main` branch is stable, all tests are passing, and it contains all the features intended for the new release.
2.  **Create an Annotated Git Tag:** Follow Semantic Versioning (SemVer - `MAJOR.MINOR.PATCH`).
    ```bash
    git tag -a v1.0.0 -m "Release version 1.0.0: Initial stable release with core game features and user authentication."
    ```
    (Replace `v1.0.0` with the appropriate version number and write a meaningful annotation message summarizing the release).
3.  **Push the Tag to Remote:**
    ```bash
    git push origin v1.0.0
    ```
    (Or `git push origin --tags` to push all local tags).
4.  **Automated Release Process (via GitHub Actions - if configured):**
    Pushing a new tag (especially one matching a pattern like `v*.*.*`) can trigger a GitHub Actions workflow that:
    *   Checks out the tagged commit.
    *   Builds the application.
    *   Runs all tests.
    *   Optionally, creates a corresponding "Release" on GitHub, attaching release notes (which can be automatically generated from commit messages or manually written).
      
## Roadmap

Top features that new developers who want to contribute to this project could add:

1.  **Advanced Player Statistics & History:** Implement endpoints and service logic to track and retrieve detailed player performance metrics over time. This could include profit/loss per stock, overall win/loss ratio, average game score, and a persistent history of trades across multiple games. This would enhance player engagement by allowing them to track their progress and compare with others.
2.  **Real-time Game Event Notifications (WebSockets):** Extend the backend with WebSocket support (e.g., using Spring WebSockets) to push real-time notifications to connected clients about game events. Examples: round start/end, significant stock price changes, notifications when other players make trades (if game rules allow for public visibility), or instant leaderboard updates. This would make the game feel more dynamic and interactive.
3.  **Achievement System:** Design and implement an achievement system where players can earn badges or points for accomplishing specific milestones or performing notable actions within the game (e.g., "First Million Made," "Diversified Investor," "Day Trader Pro," "Survived a Market Crash"). This involves defining achievements, creating logic to track player progress towards these achievements, and providing API endpoints for clients to display earned achievements.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, the process for submitting pull requests to us, and other guidelines for contributing to the Stockico backend project.

## Versioning

We use [Semantic Versioning (SemVer)](http://semver.org/) for versioning our releases. For the versions available, see the [tags on this repository](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/tags).

## Authors

*   **SoPra FS24 Group 36** - *Initial work & Development*
    *   [Shirley Lau](https://github.com/shirleyl1220)
    *   [SeungJu Paek](https://github.com/sing-git)
    *   [Jianwen Cao](https://github.com/JianwenCao)
    *   [Ilias Karagiannakis](https://github.com/LiakosKari)
    *   [Julius Landes](https://github.com/JuliusLhamo)

## License

This project is licensed under the **MIT License**.
See the [LICENSE.md](LICENSE.md) file for the full license text.

## Acknowledgments

*   Our gratitude to the **SoPra FS24 Teaching Team** at the University of Zurich for their guidance, support, and providing the initial project templates and framework.
*   Appreciation for the open-source community and the creators of the many libraries and frameworks that made this project possible, including Spring Boot, Gradle, and others.
*   Inspiration from various stock market simulation games and real-world trading platforms.
