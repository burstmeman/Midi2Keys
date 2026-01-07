<!--
Sync Impact Report:
Version change: N/A → 1.0.0 (initial constitution)
Modified principles: N/A (new document)
Added sections: Core Principles (6 principles), Code Quality Standards, Testing Standards, Architecture Standards, Performance Standards, User Experience Standards, Governance
Removed sections: N/A
Templates requiring updates:
  ✅ plan-template.md - Constitution Check section aligns with new principles
  ✅ spec-template.md - User scenarios align with AAA testing and UX consistency
  ✅ tasks-template.md - Task organization aligns with clean architecture and UI/backend separation
Follow-up TODOs: None
-->

# Midi2Keys Constitution

## Core Principles

### I. Clean Code and Code Quality (NON-NEGOTIABLE)

All code MUST adhere to clean code principles: meaningful names, small functions (single responsibility), minimal complexity, clear intent. Code MUST be self-documenting through structure and naming. Comments explain "why" not "what". Code reviews MUST enforce these standards before merge. Technical debt MUST be tracked and addressed in subsequent iterations.

**Rationale**: Maintainable code reduces bugs, speeds development, and enables team collaboration. Clean code is the foundation for all other principles.

### II. Clean Architecture and Separation of Concerns

The application MUST strictly separate UI (JavaFX controllers/views) from business logic (services, domain models). Controllers handle only UI events and delegate to service layers. Business logic MUST be framework-agnostic and independently testable. Dependencies flow inward: UI → Services → Domain Models. External dependencies (MIDI, file I/O) MUST be abstracted behind interfaces.

**Rationale**: Separation enables independent testing, easier maintenance, and future framework migration. Business logic remains stable while UI evolves.

### III. AAA Testing Standards (NON-NEGOTIABLE)

All tests MUST follow Arrange-Act-Assert pattern: clear setup (Arrange), single action (Act), explicit verification (Assert). Unit tests MUST test one behavior per test. Integration tests MUST verify component interactions. Test names MUST describe the scenario: `methodName_condition_expectedResult`. Tests MUST be fast, isolated, repeatable, and self-validating. Code coverage MUST meet minimum thresholds: 80% for business logic, 60% for UI controllers.

**Rationale**: AAA pattern ensures tests are readable, maintainable, and clearly express intent. Consistent structure reduces cognitive load when debugging failures.

### IV. User Experience Consistency

UI components MUST follow consistent patterns: navigation, error handling, feedback mechanisms, visual styling. User actions MUST provide immediate feedback (loading states, success/error messages). Error messages MUST be user-friendly and actionable. Accessibility standards MUST be considered (keyboard navigation, screen reader support where applicable). UI state MUST be predictable and recoverable.

**Rationale**: Consistent UX reduces user confusion, training time, and support burden. Predictable interfaces build user trust and efficiency.

### V. Performance Requirements

Application startup MUST complete within 2 seconds on target hardware. UI interactions MUST remain responsive (<100ms perceived latency). Background operations MUST not block UI thread. Memory usage MUST be bounded and monitored. Long-running operations MUST show progress indicators. Performance regressions MUST be caught by automated benchmarks.

**Rationale**: Desktop applications must feel instant and responsive. Performance directly impacts user satisfaction and productivity.

### VI. Test-First Development

New features MUST start with failing tests (red phase). Implementation proceeds only after tests are written and approved (green phase). Refactoring occurs with tests in place (refactor phase). Tests MUST be written for business logic before implementation. UI tests MAY follow implementation but MUST exist before feature completion.

**Rationale**: Test-first ensures requirements are clear, prevents over-engineering, and provides safety net for refactoring.

## Architecture Standards

### UI/Backend Separation

**Controllers (JavaFX)**: Handle FXML binding, user input validation (format only), UI state management, and delegate to services. Controllers MUST NOT contain business logic.

**Services**: Implement business rules, orchestrate domain operations, handle transactions, and coordinate between domain models. Services are framework-agnostic.

**Domain Models**: Represent core business entities and rules. Domain models MUST be pure Java classes with no framework dependencies.

**Repositories/Adapters**: Abstract external dependencies (MIDI devices, file system, configuration). Implement interfaces defined in domain layer.

**Dependency Direction**: UI → Services → Domain Models. Services depend on abstractions (interfaces), not concrete implementations.

## Testing Standards

### Unit Tests

- Test one behavior per test method
- Use AAA pattern consistently
- Mock external dependencies
- Test edge cases and error conditions
- Fast execution (<1 second per test class)

### Integration Tests

- Test component interactions
- Use real implementations where feasible
- Test end-to-end workflows
- Verify data persistence and retrieval

### UI Tests

- Test user interactions and event handling
- Verify UI state changes
- Test error display and user feedback
- Use TestFX or similar JavaFX testing framework

## Code Quality Standards

### Code Review Checklist

- [ ] Functions are small and focused (single responsibility)
- [ ] Names are descriptive and domain-appropriate
- [ ] No code duplication (DRY principle)
- [ ] Complexity is minimized (cyclomatic complexity < 10)
- [ ] Error handling is explicit and user-friendly
- [ ] Logging is appropriate (not excessive, includes context)
- [ ] Dependencies are minimal and justified

### Static Analysis

- Code MUST pass static analysis tools (Checkstyle, PMD, SpotBugs)
- Code MUST compile without warnings
- Code MUST follow Java naming conventions
- Code MUST be formatted consistently (use formatter)

## Performance Standards

### Measurable Targets

- Application startup: < 2 seconds
- UI response time: < 100ms for user interactions
- Memory footprint: Monitor and bound to reasonable limits
- Background task progress: Visible to user

### Performance Testing

- Benchmark critical paths
- Profile memory usage
- Monitor thread usage and blocking operations
- Track performance metrics in CI/CD

## User Experience Standards

### Consistency Requirements

- Consistent navigation patterns across all screens
- Uniform error message format and placement
- Standardized loading and progress indicators
- Consistent visual styling (colors, fonts, spacing)
- Predictable keyboard shortcuts and interactions

### Feedback Requirements

- Immediate visual feedback for all user actions
- Clear success/error messages
- Progress indicators for long operations
- Undo/redo where applicable
- Graceful error recovery

## Development Workflow

### Feature Development Process

1. Write failing tests (AAA pattern)
2. Implement minimum code to pass tests
3. Refactor with tests in place
4. Code review (enforce constitution principles)
5. Integration and UI testing
6. Performance validation
7. Documentation update

### Code Review Process

All code changes MUST be reviewed for:
- Constitution compliance
- Test coverage and quality
- Architecture adherence (UI/backend separation)
- Performance impact
- UX consistency

## Governance

This constitution supersedes all other development practices. All team members MUST comply with these principles. Amendments require:

1. Documented rationale for change
2. Impact assessment on existing code
3. Team consensus
4. Version increment (semantic versioning)
5. Update to dependent templates and documentation

**Compliance**: Code reviews MUST verify constitution compliance. Violations MUST be addressed before merge. Exceptions require documented justification and approval.

**Version**: 1.0.0 | **Ratified**: 2025-01-27 | **Last Amended**: 2025-01-27
