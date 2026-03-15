# 🤖 AI Database Assistant

An intelligent web application that converts natural language queries into SQL, powered by AI. Upload CSV files, ask questions in plain English, and get instant database insights with automatic chart generation.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ Features

- 🗣️ **Natural Language to SQL**: Ask questions in plain English, get SQL queries
- 📊 **Auto Chart Generation**: Automatic bar, line, and pie charts from query results
- 🔒 **Secure SELECT-Only**: Enforces read-only queries for safety
- 💡 **AI-Powered Insights**: Get business insights from your data
- 🔄 **Error Correction**: Automatic SQL error fixing
- 📥 **CSV Upload**: Import data from CSV files with type inference
- 📤 **Export**: Download results as CSV or Excel
- 🎙️ **Voice Input**: Chrome voice recognition support
- 💾 **Query Management**: Save and reuse favorite queries
- 🕘 **Query History**: Full audit trail with execution stats
- 👤 **Role-Based Access**: Admin and User roles
- 🗺️ **H2 Console**: Built-in database browser
- 🤖 **Multiple AI Providers**: Groq, Ollama, Anthropic, Gemini

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Internet connection (for AI APIs)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ai-database-assistant.git
   cd ai-database-assistant
   ```

2. **Configure AI Provider**

   Edit `src/main/resources/application.properties`:

   ```properties
   # Free option: Groq (recommended)
   ai.provider=groq
   ai.api.key=YOUR_GROQ_API_KEY
   ai.groq.model=llama-3.3-70b-versatile

   # Local option: Ollama (completely free)
   # ai.provider=ollama
   # ai.ollama.model=llama3

   # Paid option: Anthropic (best quality)
   # ai.provider=anthropic
   # ai.api.key=YOUR_ANTHROPIC_KEY
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Open in browser**
   ```
   http://localhost:8080
   ```

### Demo Credentials

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | Admin |
| user     | user123   | User  |
| analyst  | analyst123| User  |

## 🏗️ Architecture

```
src/main/java/com/aidb/
├── controller/         # REST APIs and web controllers
├── service/            # Business logic (AI, Query, CSV, Export)
├── model/              # JPA entities
├── repository/         # Data access layer
├── config/             # Security and initialization
├── utils/              # SQL validation utilities
└── dto/                # Data transfer objects
```

## 🔧 Configuration

### AI Providers

#### Groq (Free, Recommended)
```properties
ai.provider=groq
ai.api.key=your_groq_key_here
ai.groq.model=llama-3.3-70b-versatile
```

#### Ollama (Free, Local)
```properties
ai.provider=ollama
ai.ollama.url=http://localhost:11434
ai.ollama.model=llama3
```

#### Anthropic Claude (Paid)
```properties
ai.provider=anthropic
ai.api.key=your_anthropic_key_here
ai.model=claude-sonnet-4-20250514
```

#### Google Gemini (Free)
```properties
ai.provider=gemini
ai.api.key=your_gemini_key_here
```

### Database

#### H2 (Default, Embedded)
```properties
spring.datasource.url=jdbc:h2:file:./data/aidbassistant
spring.datasource.driver-class-name=org.h2.Driver
```

#### MySQL
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/aidbassistant
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

#### PostgreSQL
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/aidbassistant
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## 📚 API Documentation

### REST Endpoints

#### Chat & Query
- `POST /api/chat` - Convert natural language to SQL
- `POST /api/query` - Execute SQL query
- `GET /api/history` - Get query history
- `POST /api/saved-queries` - Save a query
- `GET /api/saved-queries` - List saved queries

#### CSV Upload
- `POST /api/csv/upload` - Upload and import CSV
- `GET /api/csv/tables` - List uploaded tables
- `DELETE /api/csv/tables/{name}` - Drop table

#### Export
- `GET /api/export/csv` - Export results as CSV
- `GET /api/export/excel` - Export results as Excel

### Example API Usage

```bash
# Convert natural language to SQL
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me all employees in the sales department"}'

# Execute SQL query
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM employees WHERE department = 'sales'"}'
```

## 🎯 Usage Examples

### 1. Upload Data
- Go to the web interface
- Upload a CSV file (e.g., employee data)
- The system automatically creates database tables

### 2. Query in Natural Language
- Type: "What is the average salary by department?"
- The AI converts this to SQL and executes it
- View results in table format with auto-generated charts

### 3. Get Insights
- After running a query, click "Generate Insight"
- AI analyzes the data and provides business insights

### 4. Export Results
- Download query results as CSV or Excel
- Perfect for further analysis in tools like Excel or Tableau

## 🔒 Security

- **Authentication**: Spring Security with in-memory users
- **Authorization**: Role-based access control (USER, ADMIN)
- **SQL Safety**: Only SELECT queries allowed
- **Input Validation**: Comprehensive validation on all inputs
- **CSRF Protection**: Enabled for web forms

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run with test profile
mvn spring-boot:run -Dspring.profiles.active=test
```

## 🚀 Deployment

### JAR File
```bash
mvn clean package
java -jar target/ai-database-assistant-1.0.0.jar
```

### Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ai-database-assistant-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
docker build -t ai-db-assistant .
docker run -p 8080:8080 ai-db-assistant
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot for the excellent framework
- H2 Database for embedded database support
- Groq, Anthropic, and Google for AI APIs
- OpenCSV and Apache POI for file processing

## 📞 Support

If you have any questions or issues:

1. Check the [Issues](https://github.com/yourusername/ai-database-assistant/issues) page
2. Create a new issue with detailed information
3. Contact the maintainers

---

**Made with ❤️ using Spring Boot and AI**
