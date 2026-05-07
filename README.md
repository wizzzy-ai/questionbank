# Question Bank

Spring Boot quiz platform with:

- student registration and login
- email verification and password reset development flows
- quiz history and per-attempt breakdown
- bookmark-to-practice flow
- resumable quiz drafts
- leaderboard and profile analytics
- admin question management and bulk import

## Local setup

1. Create a MySQL database or let the app create one from the configured JDBC URL.
2. Copy values from `.env.example` into your environment.
3. Start the app with the Maven wrapper once Maven is available in your environment.

## Environment variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SHOW_SQL`
- `DEFAULT_ADMIN_FULL_NAME`
- `DEFAULT_ADMIN_EMAIL`
- `DEFAULT_ADMIN_PASSWORD`

## Admin question import format

Use one question per line:

```text
Category|Difficulty|Question|Option A|Option B|Option C|Option D|CorrectOption
```

Example:

```text
Spring Boot|MEDIUM|Which annotation marks a service?|@Entity|@Service|@Controller|@Bean|B
```

## Default admin bootstrap

On startup, if no admin exists, the app creates (or promotes) a default admin account.

- email: `DEFAULT_ADMIN_EMAIL` (default: `admin@questionbank.local`)
- password: `DEFAULT_ADMIN_PASSWORD` (default: `Admin@12345`)

Change these values before running in any shared or production environment.

## Security notes

- Email verification and password reset links are surfaced in the UI for development because SMTP is not configured yet.
- Failed login attempts trigger a temporary lockout.
- Spring Security headers are enabled alongside the existing session-based flow.

## Database migrations

Flyway migrations live in `src/main/resources/db/migration`.
