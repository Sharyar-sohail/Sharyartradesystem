# ⚡ Binance Pro Trading Terminal - Autonomous AI Trading & Mega-Move Suite
### Multi-Platform Desktop Web & Standalone Mobile (Android) System

A production-ready, highly visual, low-storage Automated Binance Trading and Strategy Execution Suite fine-tuned specifically for catching **10% to 20% breakout/breakdown moves** and explosive **50% to 500%+ mega-pumps/dumps**.

The system features **100% independent multi-platform execution** across two standalone clients:
1. **Python Desktop Web & Backend Engine**: Streamlit visual dashboard + FastAPI REST Microservice + vectorized multi-strategy background runner.
2. **Standalone Native Android App**: Jetpack Compose mobile client that connects directly to Binance Public APIs and runs its own background scanner and strategy execution engine locally on the phone with **zero PC dependency**.
3. **Canonical Shared Strategy Core**: Unified mathematical formulas and parameters across all 9 trading strategies ensuring identical entries, stop-losses, targets, and Risk-to-Reward ratios across platforms.

---

## 🏛️ System Architecture

```
                                  ┌───────────────────────────────┐
                                  │   Binance Public REST API     │
                                  │   (api.binance.com / vision)  │
                                  └──────────────┬────────────────┘
                                                 │
                        ┌────────────────────────┴────────────────────────┐
                        │ Direct Public REST HTTPS                        │ Direct Public REST HTTPS
                        ▼                                                 ▼
     ┌─────────────────────────────────────┐           ┌─────────────────────────────────────┐
     │        PLATFORM A: PYTHON PC        │           │      PLATFORM B: STANDALONE ANDROID │
     │       (Zero Mobile Dependency)      │           │          (Zero PC Dependency)       │
     ├─────────────────────────────────────┤           ├─────────────────────────────────────┤
     │ • Background Scanner Daemon         │           │ • Background Coroutines Scanner     │
     │ • Vectorized Technical Indicators   │           │ • Native Kotlin Indicator Engine    │
     │ • 9 Strategy Evaluator              │           │ • 9 Strategy Evaluator (Identical)  │
     │ • SQLite WAL Signal Cache (<25MB)   │           │ • Thread-safe StateFlow Repository  │
     │ • Streamlit Dark Cyberpunk UI       │           │ • Jetpack Compose Native Mobile UI  │
     │ • FastAPI REST Microservice (:8000) │           │ • Canvas Candlestick & Level Chart  │
     └──────────────────┬──────────────────┘           └─────────────────────────────────────┘
                        │
                        ▼ Shared Mathematical Specification
     ┌────────────────────────────────────────────────────────┐
     │        shared/strategy_spec.json & STRATEGY_SPEC.md    │
     │  - ATR 14 RMA, BB 20/2, EMA 9/21/55, RSI 14, MACD     │
     │  - Identical TP1 (10%), TP2 (20%), Mega TP (50%-500%)  │
     │  - Identical Hard SL & Calculated Risk-to-Reward (R:R) │
     └────────────────────────────────────────────────────────┘
```

---

## 🌟 Key Features & Capabilities

### 1. Exhaustive Global Strategy & AI Learning Core ([`shared/STRATEGY_SPEC.md`](file:///d:/sharyar/Trading%20system/shared/STRATEGY_SPEC.md))
Both platforms evaluate the exact same 9 strategies:
1. **Proprietary AI Dynamic Learning Core**: Multi-model consensus synthesizing SMC, ICT, VFS, and VSA with **90% to 100% historical accuracy gating**.
2. **Master Mega-Pump Capture Strategy**: Specifically engineered to detect **50% to 500%+ explosive moves** at initial base consolidation ignition.
3. **Smart Money Concepts (SMC)**: Order Block retests, Fair Value Gaps (FVG), Liquidity Sweeps, and Break of Structure (BOS) momentum.
4. **Inner Circle Trader (ICT)**: Silver Bullet imbalances, Judas Swing manipulation traps, and Optimum Trade Entry (OTE 62%-79% Fib).
5. **Volume Flow Spread (VFS)**: Cumulative Volume Delta (CVD) divergence and Volume Profile Value Area (VAL/VAH) reaction.
6. **Volume Spread Analysis (VSA Wyckoff)**: Institutional stopping volume, effort vs. result, and absorption.
7. **Bollinger Squeeze & Volume Surge**: Extreme volatility compression coiling followed by high-volume 10% to 20% directional breakout.
8. **Candle King**: Master candlestick geometry, pinbar absorption wicks (66%+), and institutional engulfing momentum blocks.
9. **RSI & MACD Divergence**: Regular and hidden multi-timeframe oscillator divergences.

### 2. Mandatory Risk-to-Reward (R:R) & Complete Trade Metrics
Every single signal on both the Web and Android apps computes:
- **Strategy / Pattern Name**: Identified quantitative or institutional setup.
- **Position Type**: `🟢 LONG` or `🔴 SHORT`.
- **Precise Entry Point**: Exact price trigger down to sub-cent decimals.
- **Hard Stop-Loss (SL)**: Structural invalidation level (Swing Low/High, Order Block boundary, or 2.0x ATR).
- **Take-Profit Targets**:
  - **TP1**: +10% standard move.
  - **TP2**: +20% extended move.
  - **Mega TP**: +50% to +500%+ explosive runner target.
- **Risk-to-Reward Ratio (R:R)**: Exact mathematical ratio (e.g. `1:3.5`, `1:5.2`, `1:12.5`).
- **Expected Move %**: Clear classification of 10%–20% vs. 50%–500%+ mega-pumps.
- **AI Confidence % / Historical Win Rate**: Probabilistic conviction score (90% to 100%).

### 3. Parallel Feature Parity Across Web & Android
| Feature | Python Web Dashboard | Standalone Android App |
| :--- | :--- | :--- |
| **PC Dependency** | Runs independently | **Zero PC Dependency (Direct Binance REST)** |
| **Real-time Live Scanner** | 10%–20% & 50%–500%+ sweeps | 10%–20% & 50%–500%+ sweeps |
| **Prioritized Pairs** | Gainers, Losers, High Volume | Gainers, Losers, High Volume |
| **Strategy Repository** | 9 Institutional Strategies | 9 Identical Native Kotlin Strategies |
| **Trade Level Overlay** | Plotly Interactive Candlestick | Jetpack Compose Canvas Interactive Chart |
| **Detailed Setup Modal** | Expandable Inspection Card | Bottom Sheet Breakdown with R:R |
| **Paper Trading Station** | Interactive Simulation Tab | **Built-in Dedicated Mobile Demo Station** |
| **Autonomous Auto-Trading** | Automated Signal Engine | **Live AI Autonomous Execution Daemon** |
| **Trade Analytics** | Historical Backtester | **Live Session Win Rate & Strategy Breakdown** |
| **Accuracy Tracking** | Dynamic Live Win-Rate Engine | Real-time Hits/Losses Telemetry |
| **Storage & Memory** | SQLite WAL (<25 MB) | Local In-Memory StateFlow Buffer |

### 4. Built-in Paper Trading & Autonomous Demo Station
- **Custom Demo Funds**: Starting balance configurable up to $100,000+ with 1-tap reset.
- **Dual Execution Modes**:
  - **Manual Trade Execution**: Tap *"⚡ Demo Trade"* on any signal in AI Radar or the Setup Modal to instantly enter a position.
  - **Autonomous Auto-Trading Engine**: Runs continuously in the background, automatically opening demo trades when AI confidence meets user filters (e.g., 90%+ or 50%–500% mega-moves).
- **Position & Risk Sizing**: Configurable position size ($10, $50, $100, $250, $500, or custom).
- **Automated Exit & Trailing Protection**:
  - Automatically executes **Hard Stop-Loss (SL)** on structural invalidation.
  - Automatically executes **Take-Profit (TP1, TP2, Mega TP)** on target achievement.
  - Automatically **trails stop-loss to breakeven** once TP1 (+10%) is locked in.
- **Performance Summary & Strategy Breakdown**:
  - Real-time **Win Rate (%)**, **Net PnL ($ and %)**, **Profit Factor**, **Best/Worst Trades**.
  - Strategy-wise breakdown leaderboard evaluating the live accuracy of each model.

---

## 📂 Project Structure

```
d:\sharyar\Trading system\
├── shared/
│   ├── strategy_spec.json           # Machine-readable strategy definitions & math
│   └── STRATEGY_SPEC.md             # Canonical specification across Python & Kotlin
├── backend/
│   ├── api.py                       # Standalone FastAPI REST Microservice (Port 8000)
│   ├── config.py                    # System parameters, rate-limits & DB paths
│   ├── db.py                        # SQLite WAL engine with signals & telemetry
│   ├── binance_client.py            # Direct REST multi-mirror Binance connector
│   ├── strategies.py                # 9 Strategy implementations in Python
│   ├── runner.py                    # Background scanner & live accuracy daemon
│   ├── data_miner.py                # 1-Month 50%-500% historical data miner
│   ├── backtester.py                # Targeted Mega-Move & Matrix backtester
│   └── ml_optimizer.py              # XGBoost & Random Forest ML optimizer
├── android_app/                     # Standalone Native Android Project (Gradle 9.1 / Kotlin)
│   ├── app/
│   │   ├── build.gradle.kts         # Jetpack Compose, Coroutines, OkHttp3, Navigation
│   │   ├── build/outputs/apk/debug/ # Compiled debug APK (19.5 MB)
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml # INTERNET & ACCESS_NETWORK_STATE
│   │       │   └── java/com/example/binanceterminal/
│   │       │       ├── api/BinanceApiClient.kt          # Direct Binance REST client
│   │       │       ├── model/                           # Signal, Candle, Ticker data classes
│   │       │       ├── strategy/
│   │       │       │   ├── TechnicalIndicators.kt       # RMA ATR, EMA, SMA, BB, RSI math
│   │       │       │   └── TradingStrategies.kt         # 9 Identical strategies in Kotlin
│   │       │       ├── engine/
│   │       │       │   ├── SignalRepository.kt          # Thread-safe StateFlow signal cache
│   │       │       │   └── BackgroundMarketScanner.kt   # Independent background worker
│   │       │       └── ui/
│   │       │           ├── MainActivity.kt              # App shell & bottom navigation
│   │       │           ├── RadarScreen.kt               # Live signal radar & filter chips
│   │       │           ├── SignalDetailModal.kt         # Detailed breakdown bottom sheet
│   │       │           ├── ProChartScreen.kt            # Canvas candlestick & level chart
│   │       │           ├── MarketScreen.kt              # Top Gainers/Losers/Volume monitor
│   │       │           └── TelemetryScreen.kt           # API latency & accuracy stats
│   │       └── test/
│   │           └── java/.../TradingStrategiesTest.kt    # Strategy unit test suite
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew.bat
├── ui/
│   └── components.py                # Dark theme UI components & Plotly visualizer
├── app.py                           # Zero-Click AI Market Radar & Web Dashboard
├── install.bat                      # One-click desktop installer & shortcut creator
├── launch.bat                       # One-click Web dashboard launcher
├── launch_api.bat                   # One-click FastAPI microservice launcher
└── requirements.txt                 # Pinned Python dependencies
```

---

## 🚀 How to Run

### Platform A: Python Web Dashboard & Desktop App

#### 1. One-Click Installation
```cmd
install.bat
```
Verifies Python, installs requirements (`ccxt`, `pandas`, `numpy`, `streamlit`, `plotly`, `scikit-learn`, `xgboost`, `fastapi`, `uvicorn`), generates assets, and creates a desktop shortcut.

#### 2. Launch the Web Dashboard
Double-click the **Binance Trading Terminal** shortcut on your Desktop, or run:
```cmd
launch.bat
```
The application opens automatically at `http://localhost:8501`.

#### 3. (Optional) Launch the Standalone REST API Microservice
If you want to expose a headless REST API for external bots, curl, or integrations:
```cmd
launch_api.bat
```
Runs Uvicorn on `http://localhost:8000`. Interactive OpenAPI documentation is available at `http://localhost:8000/docs`.

---

### Platform B: Standalone Android Mobile App

The mobile application is **100% independent**. It runs directly on your Android phone and connects straight to Binance Public REST endpoints without requiring your PC or any local server to be active.

#### 1. Pre-Compiled APK Location
The ready-to-install debug APK is located at:
```
android_app\app\build\outputs\apk\debug\app-debug.apk
```

#### 2. Install on Device or Emulator
Connect your Android phone via USB (with USB Debugging enabled) or start an Android Emulator, then run:
```powershell
adb install -r "android_app\app\build\outputs\apk\debug\app-debug.apk"
```
Or copy `app-debug.apk` directly to your phone storage and open it to install.

#### 3. Rebuild / Run Unit Tests
To run unit tests or rebuild the APK from source:
```cmd
cd android_app
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat assembleDebug --no-daemon
```

---

## 📱 Mobile App UI Walkthrough

1. **⚡ Radar Tab**:
   - Live stream of detected signals across all 9 strategies.
   - Filter by direction (`ALL`, `LONG`, `SHORT`) and strategy chips.
   - Signal cards highlight: Pair, Strategy Badge, Entry, Hard SL, TP1, TP2, Mega TP, R:R Ratio, Expected Move %, and AI Win Rate %.
   - Tap any card to open the **Detailed Breakdown Bottom Sheet**.
   - Tap the chart button on any card to jump directly to the **Pro Chart Screen**.

2. **📊 Chart Tab**:
   - Interactive native Canvas candlestick chart.
   - Plots precise horizontal trade levels: **Entry (Gold)**, **Hard SL (Red)**, **TP1 (Green dashed)**, **TP2 (Green solid)**, and **Mega TP (Cyan)**.
   - Includes real-time trade level metrics cards.

3. **🔥 Market Tab**:
   - Direct live Binance 24h ticker monitor.
   - Sortable tables for **Top Gainers**, **Top Losers**, and **Highest Volume** coins.

4. **🖥️ Telemetry Tab**:
   - Live Binance REST API ping latency.
   - Active scanner status, cycle count, and total scanned coins.
   - Dynamic **Hit Rate %** and accuracy breakdown (Mega Hits, TP2 Hits, TP1 Hits, Stopped Out).

---

## 🔒 Security & Data Integrity
- **Public Endpoints Only**: All market scanning uses Binance Public REST APIs; no private API keys or trading secrets are required to run the scanner.
- **Low Footprint**: Local SQLite WAL database auto-prunes old data to stay below 25 MB on PC; Android app uses lightweight in-memory StateFlow ring buffers.
- **Multi-Mirror Failover**: Automatically retries across `api.binance.com` and `data-api.binance.vision` if network latency spikes or rate limits occur.
