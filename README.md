<<<<<<< HEAD
# 🖼️ ImageFinder 

Hi Team,

Thank you for the opportunity to participate in the Eulerity Hackathon! This project is my implementation of the multi-threaded image crawler challenge. Below, I’ve outlined what I’ve built, how to run it, and how it aligns with your requirements.

---

## ✅ What I’ve Implemented

### Required Functionality
- **Image extraction** from the given URL using JSoup.
- **Sub-page crawling** within the same domain.
- **Multithreading** using `ExecutorService` for concurrent crawling.
- **Domain-limiting logic** to avoid external navigation.
- **Visited page tracking** with thread-safe sets to prevent redundant work.

### Extra Functionality
- **Real-time image streaming** via Server-Sent Events (SSE) to the frontend.
- **Polite crawling** with a configurable delay between requests.
- **Basic logo detection** using filename heuristics (e.g., checking for "logo" in the name).
- **Responsive frontend** with filters, progress indicators, and download links.
- **JUnit + Mockito** test coverage for crawler logic, servlet input validation, and robots.txt parsing.

---

## 🧰 Tech Stack

- Java 8, Maven, Jetty
- JSoup, Gson, SLF4J
- HTML, CSS, Vanilla JavaScript
- JUnit 4 & 5, Mockito (for testing)
- Docker (multi-stage build using Jetty)

---

## 🖥️ How to Run It Locally

### Requirements
- Java 8 (exact version)
- Maven 3.5+
- Internet connection for live crawling

### Run with Maven
```bash
cd imagefinder
mvn clean package -DskipTests
mvn jetty:run
```

### Open in your browser
```
http://localhost:8080
```

---

## 🐳 How to Run It with Docker

```bash
# Build the image
docker build -t imagefinder .

# Run the container
docker run -p 8080:8080 imagefinder
```

---

## 🔍 Sample URLs I Tested With

```txt
https://www.visionsfcu.org/
https://www.binghamton.edu/
https://www.pinterest.com/ideas/
https://en.wikipedia.org/wiki/Main_Page
```

These are included in `test-links.txt`.

---

## 📜 About `robots.txt`

I did not include a static `robots.txt` file in the repo. Instead, I dynamically fetch each site’s robots.txt using JSoup and parse rules for both wildcard `*` and bot-specific user-agents. The crawler respects disallowed paths accordingly.

---

## 🧪 Running Tests

```bash
mvn test
```

Includes unit tests for:
- `ImageCrawlerService`
- `ImageFinderServlet`
- `RobotsTxtParser`

Thank you again for the opportunity!

— Prasad Kulkarni
=======
# imagefinder
>>>>>>> 5c999b3a24b0e32b53e2301d1837dc9277ebf411
