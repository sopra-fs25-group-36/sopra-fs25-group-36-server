# Contributing to Stockico Backend

First off, thank you for considering contributing to Stockico! We welcome contributions from everyone. By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

This document provides guidelines for contributing to the Stockico backend service.

## Table of Contents

-   [How Can I Contribute?](#how-can-i-contribute)
    -   [Reporting Bugs](#reporting-bugs)
    -   [Suggesting Enhancements](#suggesting-enhancements)
    -   [Your First Code Contribution](#your-first-code-contribution)
    -   [Pull Requests](#pull-requests)
-   [Development Setup](#development-setup)
-   [Coding Conventions](#coding-conventions)
-   [Testing](#testing)
-   [Commit Message Guidelines](#commit-message-guidelines)
-   [Code of Conduct](#code-of-conduct)
-   [Questions?](#questions)

## How Can I Contribute?

There are many ways to contribute, from reporting bugs and suggesting features to writing code.

### Reporting Bugs

If you find a bug, please ensure the bug was not already reported by searching on GitHub under [Issues](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/issues).

If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample or an executable test case** demonstrating the expected behavior that is not occurring.

Provide the following information:
*   A clear and descriptive title.
*   Steps to reproduce the bug.
*   What you expected to happen.
*   What actually happened.
*   Screenshots or logs (if applicable).
*   Your environment details (e.g., OS, Java version, any relevant local configurations).

### Suggesting Enhancements

If you have an idea for an enhancement or a new feature, please first check the [Issues](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/issues) to see if there's already a similar suggestion.

If not, [open a new issue](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/issues/new) to propose your idea. Provide:
*   A clear and descriptive title.
*   A detailed description of the proposed enhancement and its benefits.
*   Why this enhancement would be useful to Stockico users.
*   Any potential drawbacks or alternative solutions.
*   (Optional) Mockups or specific examples of how it might look or work.

### Your First Code Contribution

Unsure where to begin contributing to Stockico? You can start by looking through `good first issue` or `help wanted` issues:
*   [Good first issues](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/labels/good%20first%20issue) - issues which should only require a few lines of code, and a test or two.
*   [Help wanted issues](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server/labels/help%20wanted) - issues which should be a bit more involved than `good first issue` issues.

For larger contributions, it's best to discuss your plans in an issue first to ensure it aligns with the project's goals and to avoid duplicate work.

### Pull Requests

We use the "Fork & Pull" model for contributions.

1.  **Fork the Repository:** Click the "Fork" button at the top right of the [Stockico Backend repository page](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server). This creates your own copy of the project.
2.  **Clone Your Fork:**
    ```bash
    git clone https://github.com/YOUR_USERNAME/YOUR_BACKEND_REPOSITORY_NAME.git
    cd YOUR_BACKEND_REPOSITORY_NAME
    ```
3.  **Create a Branch:** Create a new branch for your changes. Choose a descriptive branch name (e.g., `feat/add-player-achievements`, `fix/login-error-handling-123` where `123` is the issue number).
    ```bash
    git checkout -b name-of-your-new-branch
    ```
4.  **Make Your Changes:** Write your code and add tests for your changes.
5.  **Test Your Changes:** Ensure all existing tests pass, and your new tests cover your changes.
    ```bash
    ./gradlew test
    ```
6.  **Commit Your Changes:** Write clear, concise commit messages (see [Commit Message Guidelines](#commit-message-guidelines)).
    ```bash
    git add .
    git commit -m "Feat: Add feature X"
    ```
7.  **Push to Your Fork:**
    ```bash
    git push origin name-of-your-new-branch
    ```
8.  **Open a Pull Request (PR):**
    *   Go to the original [Stockico Backend repository](https://github.com/sopra-fs25-group-36/sopra-fs25-group-36-server) on GitHub.
    *   Click on the "Pull requests" tab and then "New pull request".
    *   Select your fork and the branch containing your changes as the "compare" branch.
    *   The "base" branch should typically be `main` (or `develop` if that's your primary development branch).
    *   **Title:** Write a clear and descriptive title for your PR (e.g., "Feat: Implement user achievement system").
    *   **Description:**
        *   Provide a detailed description of the changes.
        *   Explain the "why" behind your changes.
        *   Link to any relevant GitHub issues (e.g., "Closes #123", "Fixes #456").
        *   Describe how you tested your changes.
    *   Ensure all CI checks (GitHub Actions) pass.
    *   Collaborate with reviewers to address any feedback. Your PR will be merged once it's approved.

## Development Setup

Please refer to the [Launch & Deployment section in our README.md](README.md#launch--deployment) for detailed instructions on how to set up your development environment, build the project, and run it locally.

Ensure you have all prerequisites installed and can run the application and its tests before starting to code.

## Coding Conventions

To maintain code consistency, we use automated tools and follow general best practices:

*   **Formatting:** We use Spotless with a predefined style. Please run `./gradlew spotlessApply` before committing your changes to ensure your code is formatted correctly.
*   **Style:** Follow the existing code style in the project.
*   **Clarity:** Write clear, readable code. Add comments where necessary to explain complex logic.
*   **Java Best Practices:** Adhere to standard Java naming conventions and design principles.

## Testing

*   All new features must include corresponding tests.
*   All bug fixes should include a test that demonstrates the bug and verifies the fix.
*   Ensure all tests pass before submitting a pull request by running:
    ```bash
    ./gradlew test
    ```

## Commit Message Guidelines

We aim for clear and descriptive commit messages. A good commit message should be able to stand on its own.
Consider using a convention like [Conventional Commits](https://www.conventionalcommits.org/) for consistency, but at a minimum:

*   Separate subject from body with a blank line.
*   Limit the subject line to 50 characters.
*   Capitalize the subject line.
*   Do not end the subject line with a period.
*   Use the imperative mood in the subject line (e.g., "Add feature" not "Added feature" or "Adds feature").
*   Wrap the body at 72 characters.
*   Use the body to explain *what* and *why* vs. *how*.

```
Example:

This commit introduces a new GET endpoint /users/{userId}/profile
that allows clients to fetch detailed profile information for a
specific user. This is necessary for the new user profile page
in the frontend.

Closes #78
```
## Code of Conduct

This project and everyone participating in it is governed by the [Stockico Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Questions?

If you have questions about contributing, feel free to:
*   Open an issue on GitHub with the `question` label.
*   Reach out to the maintainers (list specific channels if you have them, e.g., a team Slack channel, otherwise GitHub issues are fine for a SoPra project).

Thank you for contributing to Stockico!
