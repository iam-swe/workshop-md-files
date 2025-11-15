---
description: 'Testing guidelines and patterns for Atlas AI Agent'
applyTo: '**/tests/**/*.py, **/integration_tests/**/*.py'
---

# Testing Guidelines for Atlas AI Agent

## Unit Testing

- Write focused, single-responsibility test methods
- Use descriptive test method names that explain the scenario
- Remove unnecessary docstrings from test methods
- Use proper mocking for external dependencies
- Include type annotations for test methods: `-> None`

## Integration Testing

- Simplify complex test execution methods by breaking them into smaller functions
- Use single response field instead of duplicating raw/structured responses
- Remove test description fields from both input and output CSV files
- Focus on essential validation logic without verbose documentation

## Test Structure and Organization

- Maintain clear separation between test and production code
- Group related test cases in dedicated test modules
- Use descriptive file names that clearly indicate what is being tested
- Follow the same code style and formatting rules as production code

## Test Quality Requirements

### Pre-commit Validation

The following checks must pass before any commit:

#### **Unit Tests** (`pytest`)
- All tests must pass
- Maintain test coverage for critical functionality
- Include edge case testing

### Error Handling in Tests

- Implement proper exception handling with informative error messages
- Use structured logging for debugging information
- Validate input parameters and provide clear error feedback
- Handle edge cases gracefully

## Testing Best Practices

- Test one thing per test method
- Use arrange-act-assert pattern
- Keep tests independent and isolated
- Use fixtures for common test setup
- Mock external dependencies appropriately
- Write tests that are easy to understand and maintain
