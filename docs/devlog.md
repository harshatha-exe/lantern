ai-repository-analyzer/
│
├── backend/
│   ├── src/main/java/com/harshatha/repoanalyzer/
│   │   ├── analysis/             # Domain logic for repo parsing heuristics
│   │   ├── client/               # External API integrations (Gemini, GitHub API)
│   │   ├── config/               # General system configurations
│   │   ├── controller/           # REST Controllers (DTOs only, no business logic)
│   │   ├── dto/                  # Request/Response Data Transfer Objects
│   │   ├── entity/               # JPA Hibernate Entities
│   │   ├── exception/            # Global exception handlers and custom exceptions
│   │   ├── repository/           # Spring Data JPA Repositories
│   │   ├── security/             # JWT filters, Security Config, UserDetailsService
│   │   ├── service/              # Business logic & Orchestration services
│   │   ├── util/                 # Pure utility classes (e.g., File utilities)
│   │   └── RepoAnalyzerApplication.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── static/
│   │
│   ├── pom.xml
│   └── README.md
│
├── frontend/
│   ├── src/
│   │   ├── assets/               # Images, SVGs, global styles
│   │   ├── components/           # Reusable UI components (Buttons, Inputs, Cards)
│   │   ├── context/              # Global state management (Auth context)
│   │   ├── hooks/                # Custom React hooks
│   │   ├── layouts/              # Page layouts (Navbar, Sidebar wrappers)
│   │   ├── pages/                # High-level views (Login, Dashboard, AnalysisResult)
│   │   ├── services/             # Axios API client instances and endpoints
│   │   ├── utils/                # Helper functions
│   │   ├── App.jsx
│   │   └── main.jsx
│   │
│   ├── public/
│   ├── package.json
│   └── README.md
│
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── api-design.md
│   ├── database-schema.md
│   ├── roadmap.md
│   ├── devlog.md
│   ├── setup-guide.md
│   └── decisions/                # Architecture Decision Records (ADRs)
│
├── assets/
├── scripts/
├── .gitignore
├── README.md
└── docker-compose.yml