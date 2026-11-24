# GradFinder

Search graduate programs at Canadian universities. Spring Boot (Java 25) and React (TypeScript, Bootstrap).

## Technology stack

| Part | Technology | Version | Used for |
|---|---|---|---|
| Backend | Java (JDK) | 25 | All features |
| | Spring Boot (Web MVC, Validation) | 4.2.0 | REST API and request checks |
| | Selenium with Google Chrome | 4.54.0 | Web crawler |
| | Jsoup | 1.24.1 | HTML parser |
| | Jackson | 3.1.8 | Reading and writing JSON |
| | Maven (wrapper included) | 3.10.1 | Build |
| Tests | JUnit, Spring Boot Test | Set by Spring Boot | Backend tests |
| Frontend | React | 19.3.1 | Displaying results |
| | TypeScript | 7.0.2 | Frontend code, with type checking |
| | Bootstrap | 5.3.9 | Styling |
| | Vite | 8.3.8 | Development server and build |
| Storage | JSON and HTML files in `backend/data` | | Programs, crawled pages and search history; no database |

The features are implemented in the backend. The frontend calls its REST API under `/api` and displays the answers.

## Run

Needs JDK 25, Google Chrome and Node.js LTS.

Start the backend:

    cd backend
    .\mvnw.cmd spring-boot:run

Start the frontend in a second terminal:

    cd frontend
    npm install
    npm run dev

Open http://localhost:5173, or the address that Vite prints if that port is in use.

### IntelliJ IDEA

Run `GradfinderApplication`. In **Run > Edit Configurations**, set **Working directory** to the `backend` folder.
From any other folder the app starts but finds no data and shows 0 programs.

### Eclipse

Use an Eclipse version that supports Java 25.

1. Choose **File > Import > Maven > Existing Maven Projects**, select the `backend` folder and click **Finish**.
   Wait for Maven to download the dependencies.
2. In **Window > Preferences > Java > Installed JREs**, add JDK 25 and tick it.
3. Right-click the project and choose **Maven > Update Project**, then **OK**.
4. Open `src/main/java/ca/gradfinder/GradfinderApplication.java`, right-click it and choose **Run As > Java Application**.
5. If the app shows 0 programs, open **Run > Run Configurations > Arguments** and set **Working directory** to the
   `backend` folder. The default, the project folder, is correct.

To run the tests, right-click the project and choose **Run As > JUnit Test** or **Run As > Maven test**.
Start the frontend from a terminal as shown above.

### Check and fix

- http://localhost:8080/api/stats shows how many programs are loaded.
- "Port 8080 was already in use" means another copy of the backend is running. Stop it first.

### Test and build

    cd backend
    .\mvnw.cmd test

    cd frontend
    npm run build

The frontend build also checks the TypeScript types.

## Project structure

| Folder | Contents |
|---|---|
| `backend/src/main/java/ca/gradfinder` | Backend code, one package per area |
| `backend/data/programs` | Program data, one JSON file per university |
| `backend/data/html` | Saved copies of crawled pages (local only) |
| `backend/data/search-history.json` | Search frequency (local only) |
| `frontend/src` | React pages: `SearchPage`, `ToolsPage`, `CrawlerPage`, `DatasetPage` |

## User interface

| Tab | Shows |
|---|---|
| Search | Search, word completion, spelling suggestions, filters, most searched words, program details |
| Text tools | Frequency count, spell check, inverted index, patterns, validation |
| Crawler | Starts the crawler of the chosen university and shows its progress |
| Dataset | Combined dataset, filled columns, values to check, CSV download |

## Where each feature is

Paths are under `backend/src/main/java/ca/gradfinder`.

| Feature | Code | Method |
|---|---|---|
| Web crawler | `crawler/UniversityCrawler`, `crawler/SeleniumCrawler`, `crawler/AlbertaCrawler` | Selenium, robots.txt, delay between pages |
| HTML parser | `parser/ProgramParser`, `parser/HtmlProgramParser`, `parser/AlbertaParser` | Jsoup |
| Spell checking | `algorithms/SpellChecker`, `algorithms/EditDistance` | Dynamic programming |
| Word completion | `algorithms/Trie` | Prefix tree |
| Frequency count | `algorithms/WordCounter` | Knuth-Morris-Pratt |
| Search frequency | `service/SearchHistory` | Hash map saved as JSON |
| Page ranking | `algorithms/PageRanker` | Min-heap on occurrences |
| Inverted indexing | `algorithms/InvertedIndex` | Hash map of word to page counts |
| Data validation | `service/InputValidator` | Regular expressions |
| Finding patterns | `service/PatternFinder` | Regular expressions |
| Data processing and organization | `processing/ProgramProcessor`, `processing/ProgramDataset` | One normalizer per kind of value |

## Data processing and organization

1. Each university's parser saves its records in `backend/data/programs`, with the values as the website writes them.
2. `ProgramProcessor` puts every value into the common format below.
3. `ProgramDataset` combines all universities. Every row has the same columns in the same order; missing values are empty.

Search, filters, the Dataset tab and the CSV all use this dataset. It is rebuilt at startup and after a crawl.

| Kind of value | Common format | Example |
|---|---|---|
| Money | Canadian dollars, two decimals; other details go to Notes | `$7,600.00 (2026-27)` becomes `7600.00` |
| Dates | `YYYY-MM-DD`, keeping labels | `2-Nov-26` becomes `2026-11-02` |
| Intake | Term and year (September = Fall, January = Winter, May = Summer) | `Sep-27` becomes `Fall 2027` |
| Mode of delivery | In person, Online, Hybrid | `On Campus or Online` becomes `In person; Online` |
| Program type, degree level | Fixed names; the level is taken from the title if not given | `Course Based, Thesis Based` becomes `Course Based; Thesis Based` |
| Counts | Whole numbers | `1,250` becomes `1250` |
| Text | Same spaces, apostrophes and accents | |

A value that does not fit is kept as it is and listed as needing a check; nothing is guessed.

## Object-oriented design

| Principle | Where |
|---|---|
| Abstraction | `UniversityCrawler`, `ProgramParser`, `StringDistance` and `FieldNormalizer` interfaces |
| Inheritance | `AlbertaCrawler` extends `SeleniumCrawler`; `AlbertaParser` extends `HtmlProgramParser` |
| Polymorphism | `CrawlService` uses any crawler and parser; `SpellChecker` uses any `StringDistance` |
| Encapsulation | Private state and rules, for example in `Trie`, `InvertedIndex` and `InputValidator` |
| Dependency injection | Objects are passed to constructors, mostly by Spring |
| Design patterns | Template method (`SeleniumCrawler`, `HtmlProgramParser`), strategy (`StringDistance`, `FieldNormalizer`) |

## REST API

The frontend uses these, and they also work without it. Paths start with `/api`.

| Feature | Request |
|---|---|
| Search and page ranking | `GET /search?q=anthropology` (add `&record=true` to count the search) |
| Filter options | `GET /filters` |
| Word completion | `GET /complete?prefix=anth` |
| Spell checking | `GET /spell?word=reserch` |
| Frequency count | `GET /frequency?word=capstone&url=<page URL>` |
| Search frequency | `GET /history` |
| Inverted index | `GET /index?word=thesis` |
| Finding patterns | `GET /patterns?type=money` |
| Data validation | `POST /validate` with `{"type": "postalCode", "value": "T6G 2E1"}` |
| Crawler | `POST /crawl` with `{"university": "ualberta", "limit": 10, "refresh": false}`; `GET /crawl` for progress; `GET /crawlers` for the universities |
| Combined dataset | `GET /dataset`, `GET /dataset.csv`, `GET /dataset/report` |
| Crawled pages and totals | `GET /pages`, `GET /stats` |
| Pattern and validation types | `GET /pattern-types`, `GET /validation-types` |

## Current data

Only the University of Alberta is included, and only part of its program list.
Most tuition, deadline and enrolment columns are empty because its program pages do not give them.

## Adding another university

Write two classes and mark both with `@Component`. Nothing else changes: the backend finds them when it starts
and the Crawler tab lists the university.

| Interface | Class to write | Job |
|---|---|---|
| `crawler/UniversityCrawler` | `WindsorCrawler` | Find the program pages and download their HTML |
| `parser/ProgramParser` | `WindsorParser` | Turn one page into one or more `ProgramRecord` objects |

Both return the same `code()`, for example `uwindsor`, and use the same university name.
Usually it is easiest to extend `SeleniumCrawler` and `HtmlProgramParser`, which already do the shared steps
(browser, robots.txt, delay, page text). `AlbertaCrawler` and `AlbertaParser` are the examples to follow.
If one page has several degrees, give each record its own URL by adding a fragment: `url + "#ma"`.

### Without a crawler

Put a JSON array in `backend/data/programs/<name>.json` and restart the backend:

    [{
      "university": "University of Windsor",
      "title": "Master of ...",
      "url": "https://...",
      "crawledAt": "2026-11-24T15:00:00Z",
      "fields": { "faculty": "...", "degreeLevel": "Master's" },
      "text": "All visible text of the page"
    }]

Use the keys in `model/ProgramFields` for the agreed columns; extra keys are allowed.
Each record needs its own URL; if two share one, only the last is kept.
Leave out fields that the website does not give. Records that fail validation are skipped and logged.
