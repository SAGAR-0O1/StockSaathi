# StockSaathi – AI-Powered Inventory & Business Intelligence System
## Download StockSaathi

Get the latest Android application APK.

\<p align="center">   \<a href="[https://drive.google.com/file/d/1U_5mF4UzGRnEl9DGi-NS_wTS5Mck15Un/view?usp=sharing](https://drive.google.com/file/d/1U_5mF4UzGRnEl9DGi-NS_wTS5Mck15Un/view?usp=sharing)">     \<b>⬇️ Download StockSaathi APK\</b>   \</a> \</p>

**Installation Instructions:**

1. Download the APK.
2. Open the downloaded file on your Android device.
3. Allow installation from the relevant source if prompted.
4. Install and start using StockSaathi.

**Note:** This is an Android application. Download and install it at your own discretion.


\<p align="center">
&#x20; \<b>Smart Inventory Management | Business Analytics | Demand Forecasting\</b>
\</p>

\<p align="center">
&#x20; An intelligent Android application designed to simplify retail store operations through inventory tracking, purchase and sales management, financial analytics, and data-driven business insights.
\</p>

---

## About the Project

**StockSaathi** is an AI-assisted inventory and business intelligence application developed to help small and medium-sized store owners manage their daily business operations efficiently.

It provides a centralized platform to manage products, purchases, sales, customers, suppliers, expenses, and financial records. Its business intelligence engines analyze stored transaction data to generate useful insights, monitor stock levels, and estimate future product demand.

The goal is to reduce manual work, improve inventory accuracy, and help store owners make better business decisions.

## Key Features

### 1. Dashboard

- Overview of store performance.
- Sales and purchase summaries.
- Inventory valuation.
- Low-stock and out-of-stock alerts.
- Business performance indicators.

### 2. Inventory Management

- Add, update, search, and manage products.
- Product barcode and SKU management.
- Purchase price and selling price tracking.
- Current stock and minimum stock thresholds.
- Batch and expiry information.

### 3. Purchase Management

- Record product purchases.
- Add multiple products to a purchase invoice.
- Track suppliers and purchase costs.
- Manage quantities, discounts, and taxes.
- Automatically update inventory after successful purchases.

### 4. Sales & Billing

- Barcode-based product selection.
- Multi-product sales invoices.
- Quantity, discount, and tax calculations.
- Cash, UPI, card, and credit payment modes.
- Hold and resume bills.
- Sales returns and transaction cancellation.
- Generate printable transaction receipts.

### 5. Customer & Supplier Management

- Maintain customer and supplier records.
- Track customer outstanding balances.
- Manage supplier payments and outstanding amounts.
- Maintain payment history.

### 6. Expense Management

- Record daily business expenses.
- Categorize expenses.
- Track payment methods and expense history.

### 7. Reports & Business Analytics

- Sales and purchase analysis.
- Revenue and profit calculations.
- Cost of goods sold (COGS).
- Stock valuation.
- Top-selling product analysis.
- Supplier spending analysis.
- CSV report exports.

### 8. AI Business Assistant

- Business-related queries.
- Sales and purchase summaries.
- Profit and loss analysis.
- Low-stock product identification.
- Product stock information.
- Hindi, English, and Hinglish query modes.
- Voice-style purchase and sales command parsing.

### 9. Demand Forecasting

- Analyze historical sales velocity.
- Estimate upcoming 14-day and 30-day demand.
- Calculate remaining inventory days.
- Identify stockout risks.
- Recommend product reorder quantities.

### 10. Additional Features

- Expiry management.
- Smart offers and promotional rules.
- Stock adjustments.
- Audit logs.
- Daily cash closing.
- Local backup and restore utilities.

---

## Technology Stack

| Technology        | Purpose                           |
| ----------------- | --------------------------------- |
| Kotlin            | Android application development   |
| Jetpack Compose   | Modern declarative UI             |
| Material 3        | UI components and design          |
| MVVM Architecture | Application architecture          |
| Room Database     | Local database abstraction        |
| SQLite            | Persistent local data storage     |
| CameraX           | Camera integration                |
| Google ML Kit     | Barcode recognition               |
| Kotlin Coroutines | Asynchronous operations           |
| StateFlow         | Reactive application state        |
| Gradle Kotlin DSL | Build and dependency management   |
| CSV / JSON / HTML | Data export, backup, and receipts |

## Application Architecture

StockSaathi follows the MVVM architecture with a repository-based data layer.

```text
             USER
              |
              v
       ANDROID APPLICATION
              |
              v
    PRESENTATION LAYER
    Jetpack Compose UI
              |
              v
       VIEWMODEL LAYER
       MainViewModel
              |
              v
      REPOSITORY LAYER
    InventoryRepository
              |
       +------+------+
       |             |
       v             v
  BUSINESS LOGIC   DATA ACCESS
  AI Engines       Room DAOs
  Forecasting          |
  Financial Math       v
                   ROOM DATABASE
                       |
                       v
                     SQLITE
```

## Database & Data Storage

StockSaathi currently uses a local database architecture.

**Room Database + SQLite** are used for persistent storage inside the Android application's private internal storage.

Major database entities include:

- Products
- Suppliers
- Transactions
- Transaction Items
- Customers
- Customer Payments
- Supplier Payments
- Expenses
- Stock Adjustments
- Cash Closings
- Held Bills
- Audit Logs
- Promotion Rules

**Important:** The current version stores its primary business data locally on the Android device. A centralized cloud database and multi-device synchronization are planned future improvements.

## AI & Business Intelligence

StockSaathi includes dedicated business logic engines:

| Engine                  | Responsibility                        |
| ----------------------- | ------------------------------------- |
| StockSaathiAiEngine     | Business metrics and query processing |
| DemandForecastingEngine | Sales-based demand estimation         |
| SmartOfferEngine        | Promotion evaluation and smart offers |
| FinancialMath           | Financial calculations and reporting  |

The current forecasting implementation uses weighted historical sales velocity.

It gives more importance to recent sales activity while considering 30-day and 90-day sales history.

The system uses these calculations to estimate future demand, identify stockout risks, and recommend reorder quantities.

**Note:** The current business intelligence implementation primarily uses local rule-based logic and mathematical calculations. It should not be confused with a fully generative AI chatbot or a trained machine-learning forecasting model.

## Barcode Scanning

Barcode scanning is implemented using:

- CameraX for camera access.
- Google ML Kit for barcode detection.

The detected barcode is used to identify matching products from the local inventory database.

Supported barcode formats include common retail formats such as EAN-13, EAN-8, UPC-A, UPC-E, Code 128, and Code 39.

## Project Structure

```text
StockSaathi/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── data/
│       │   │   │   ├── ai/
│       │   │   │   ├── dao/
│       │   │   │   ├── database/
│       │   │   │   ├── model/
│       │   │   │   └── repository/
│       │   │   │
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   ├── screens/
│       │   │   │   └── MainViewModel.kt
│       │   │   │
│       │   │   ├── util/
│       │   │   └── MainActivity.kt
│       │   │
│       │   └── res/
│       │
│       └── test/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── .gitignore
```

## Getting Started

### Prerequisites

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Android Emulator or a physical Android device

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/SAGAR-0O1//StockSaathi.git
```

**2. Open the project**

Open Android Studio and select the cloned StockSaathi project folder.

**3. Sync Gradle**

Allow Android Studio to download the required dependencies and complete Gradle synchronization.

**4. Configure local secrets if required**

If using the configured Gemini integration, create a local `.env` file using `.env.example` as a reference and provide your own API key.

Never upload your actual API keys or secret credentials to GitHub.

**5. Run the application**

Connect an Android device or start an emulator and run the application using Android Studio.

## Screenshots

Add actual screenshots of your running StockSaathi application in the `screenshots/` folder.

| Dashboard | Inventory |
| --------- | --------- |
|           |           |

| Sales & Billing | Purchase |
| --------------- | -------- |
|                 |          |

| AI Assistant | Reports |
| ------------ | ------- |
|              |         |

## Future Improvements

- Cloud-based backend architecture.
- Centralized PostgreSQL database.
- Multi-store and multi-device synchronization.
- Secure authentication and role-based access control.
- Generative AI business assistant integration.
- Advanced machine-learning demand forecasting.
- Cloud backup and recovery.
- Scalable deployment infrastructure.

## Project Goals

StockSaathi aims to make business management easier for small retailers by combining inventory management, financial analysis, and intelligent decision-support tools in one application.

## Author

**Sagar**

B.Tech – Footwear Technology

Interested in Software Development, Data Structures & Algorithms, Artificial Intelligence, and building practical technology solutions.

---

\<p align="center">
&#x20; \<b>StockSaathi – Smarter Inventory. Better Business Decisions.\</b>
\</p>

\<p align="center">
&#x20; Made with ❤️ for smarter retail management.
\</p>
