# Eclipse IDE Setup Instructions

## For Your Teammate

### Prerequisites
- Eclipse IDE for Enterprise Java and Web Developers (latest version)
- Java 17 or higher
- Maven 3.6+ (Eclipse has embedded Maven support)
- MySQL database (or let the app create one)

### Step 1: Clone the Repository
```bash
git clone https://github.com/wizzzy-ai/questionbank.git
cd questionbank
```

### Step 2: Import into Eclipse
1. Open Eclipse IDE
2. Go to **File → Import → Maven → Existing Maven Projects**
3. Browse to the cloned project directory
4. Select the `pom.xml` file
5. Click **Finish**

### Step 3: Configure Environment Variables
1. Copy `.env.example` to `.env` (or create `.env` file)
2. Set the following environment variables:
   - `DB_URL` (e.g., `jdbc:mysql://localhost:3306/questionbank?createDatabaseIfNotExist=true`)
   - `DB_USERNAME` (your MySQL username)
   - `DB_PASSWORD` (your MySQL password)
   - `SHOW_SQL` (optional, set to `true` for debugging)
   - `DEFAULT_ADMIN_FULL_NAME`
   - `DEFAULT_ADMIN_EMAIL`
   - `DEFAULT_ADMIN_PASSWORD`

### Step 4: Run the Application
1. Right-click on the project in Eclipse
2. Go to **Run As → Spring Boot App**
   OR
   Right-click on `QuestionbankApplication.java` → **Run As → Java Application**

### Step 5: Access the Application
- Open your browser and navigate to: `http://localhost:8080`

### Default Admin Account
- Email: `admin@questionbank.local` (or your configured `DEFAULT_ADMIN_EMAIL`)
- Password: `Admin@12345` (or your configured `DEFAULT_ADMIN_PASSWORD`)

### Troubleshooting
- **Maven Build Issues**: Right-click project → **Maven → Update Project**
- **Port 8080 in use**: Change `server.port` in `application.properties`
- **Database connection issues**: Verify MySQL is running and credentials are correct

### Project Structure
- `src/main/java/com/example/questionbank/` - Java source code
- `src/main/resources/` - Configuration, templates, and static files
- `src/test/java/` - Test files
- `pom.xml` - Maven configuration

This is a Spring Boot application with Maven build system, fully compatible with Eclipse IDE.
