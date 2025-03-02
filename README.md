# Search Engine and Web Crawler (Java) 🔍🕷️

## Overview
This project is a **search engine and web crawler** written in Java.  
It crawls web pages, indexes content, computes rankings, and retrieves relevant search results efficiently.

## 🚀 Features
- **Web Crawler**: Extracts URLs and content using [Jsoup](https://jsoup.org/).
- **Indexing & Ranking**: Stores words and computes **influence-based rankings**.
- **Priority Queues**: Uses **custom comparators** to rank search results efficiently.
- **Fast Lookups**: Implements **SkipMap** for optimized searches.

## 🛠️ How to Install & Run

### 1️⃣ Install Dependencies
This project requires **[Jsoup](https://jsoup.org/)** for web crawling.  
**Download** the latest version and place `jsoup-1.x.x.jar` inside the `lib/` folder.

### 2️⃣ Compile the Code
Run the following command to compile Java files:
```sh
javac -cp lib/jsoup-1.15.4.jar -d bin src/**/*.java

