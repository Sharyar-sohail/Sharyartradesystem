"""
Sharyar Ultimate Trading Masterclass & Quantitative Academy
Exhaustive Knowledge Base & Curriculum covering:
1. Foundations & Technical Analysis (Candlestick Dictionary, Chart Patterns, Support/Resistance, Indicators)
2. Institutional Smart Money Concepts (SMC & ICT: FVG, Order Blocks, BOS, CHoCH, Liquidity Sweeps, OTE)
3. Volume Spread Analysis (VSA) & Market Flow (Wyckoff Accumulation/Distribution, Volume Profiles, Footprints)
4. Advanced Risk Management & Position Sizing (The 1% Rule, Tight SL, Large TP, RR Mastery, Compounding)
5. Trading Psychology & Emotional Discipline (FOMO, Greed, Fear, Tilt Protocol, Probabilistic Mindset)
6. Macroeconomics & Fundamental Data (CPI, NFP, Interest Rates, DXY, Bitcoin Dominance)
7. Algorithmic & Quantitative Trading Basics (Pine Script v5, MT5 EAs, AI Swarm Concepts)
"""

from typing import Dict, Any, List

CURRICULUM_MODULES: List[Dict[str, Any]] = [
    {
        "id": "module_1",
        "title": "Module 1: Foundations & Technical Analysis",
        "icon": "🕯️",
        "tag": "Core Foundations",
        "duration": "45 mins",
        "summary": "Master price action from the ground up: complete candlestick anatomy, single/double/triple patterns, classic chart geometries, and essential technical indicators.",
        "lessons": [
            {
                "id": "m1_l1",
                "title": "1.1 The Anatomy of a Japanese Candlestick & Market Psychology",
                "content": """
### 🕯️ Anatomy of a Candlestick & Order Flow Psychology

A candlestick is not just a visual shape; it is an **auction battle record** between buyers (bulls) and sellers (bears) across a specified timeframe.

#### Four Primary Price Points:
1. **Open (O)**: The price at the start of the timeframe.
2. **High (H)**: The absolute maximum price reached (buying exhaustion or supply rejection).
3. **Low (L)**: The absolute minimum price reached (selling exhaustion or demand injection).
4. **Close (C)**: The final settled price when the candle timeframe expires.

#### The Psychology of Wicks vs. Real Bodies:
- **Long Upper Wick**: Bull trap / Heavy selling pressure. Buyers pushed price upward, but institutional sellers stepped in with aggressive market orders, overwhelming demand before the close.
- **Long Lower Wick**: Bear trap / Heavy absorption. Sellers pushed price downward, but institutional buyers absorbed liquidity at a discount, driving price back up.
- **Large Full Body (Marubozu)**: Extreme directional conviction. Little to no wicks indicate one-sided market aggression.
- **Tiny Body with Long Dual Wicks (Doji / Spinning Top)**: Complete equilibrium / Market indecision. A volatility expansion breakout typically follows.

```text
       HIGH                          HIGH
        │                             │
    ┌───────┐ ◄── Upper Wick      ┌───────┐ ◄── Upper Wick
    │       │                     │       │
    │ BODY  │ ◄── Bullish Body    │ BODY  │ ◄── Bearish Body
    │       │     (Close > Open)  │       │     (Open > Close)
    └───────┘                     └───────┘
        │                             │
       LOW                           LOW
```
"""
            },
            {
                "id": "m1_l2",
                "title": "1.2 Complete Candlestick Dictionary (Single, Double & Triple Patterns)",
                "content": """
### 📖 Complete Candlestick Dictionary

#### 🔹 Single Candlestick Patterns:
- **Hammer (Bullish Reversal)**:
  - *Location*: Bottom of a downtrend.
  - *Anatomy*: Small body at the top, lower wick at least **2x the length of the real body**, minimal or no upper wick.
  - *Psychology*: Bears attempted to make a new low, but institutional demand absorbed all selling.
- **Shooting Star (Bearish Reversal)**:
  - *Location*: Peak of an uptrend.
  - *Anatomy*: Small body at the bottom, upper wick at least **2x the length of the real body**.
  - *Psychology*: Buyers tried to break higher, but met heavy institutional distribution.
- **Dragonfly Doji**: Open, High, and Close are virtually equal with a very long lower shadow. Strong demand signal at support.
- **Gravestone Doji**: Open, Low, and Close are virtually equal with a very long upper shadow. Strong supply signal at resistance.

#### 🔹 Double Candlestick Patterns:
- **Bullish Engulfing**: A small bearish candle immediately followed by a large green candle whose body completely engulfs the previous candle's body. Signals an aggressive momentum shift.
- **Bearish Engulfing**: A green candle followed by a massive red candle that completely engulfs the prior body. High-probability top reversal.
- **Tweezer Bottom**: Two consecutive candles sharing the exact same low price level with matching lower wicks. Confirms solid support test.
- **Tweezer Top**: Two consecutive candles sharing the exact same high price level with matching upper wicks. Confirms solid resistance test.

#### 🔹 Triple Candlestick Patterns:
- **Morning Star (3-Candle Bullish Reversal)**:
  1. Candle 1: Strong bearish impulse candle.
  2. Candle 2: Small indecision candle (star/doji) gapping down or stalling.
  3. Candle 3: Powerful bullish candle closing well past the midpoint of Candle 1.
- **Evening Star (3-Candle Bearish Reversal)**:
  1. Candle 1: Strong bullish impulse candle.
  2. Candle 2: Small indecision candle stalling at the top.
  3. Candle 3: Aggressive bearish displacement candle closing below the midpoint of Candle 1.
- **Three White Soldiers**: Three consecutive long-bodied green candles closing near their highs with small wicks, demonstrating sustained institutional accumulation.
- **Three Black Crows**: Three consecutive long-bodied red candles closing near their lows, indicating aggressive liquidation.
"""
            },
            {
                "id": "m1_l3",
                "title": "1.3 Classic Chart Geometries & Support / Resistance Dynamics",
                "content": """
### 📐 Classic Chart Geometries & Key Horizontal Levels

#### Support & Resistance Dynamics:
- **Support**: A price zone where buying interest is strong enough to overcome selling pressure.
- **Resistance**: A price zone where selling interest is strong enough to halt or reverse an advance.
- **The Polarity Principle**: Broken support becomes new resistance; broken resistance becomes new support.

#### High-Conviction Reversal Patterns:
1. **Head & Shoulders (Bearish)**:
   - Left Shoulder (Swing High) ➔ Head (Higher High) ➔ Right Shoulder (Lower High).
   - Break of the **Neckline** triggers entry. Stop Loss placed above Right Shoulder.
2. **Inverse Head & Shoulders (Bullish)**:
   - Left Shoulder (Swing Low) ➔ Head (Lower Low) ➔ Right Shoulder (Higher Low).
   - Break above Neckline confirms accumulation bottom.
3. **Double Top ('M' Pattern)** & **Double Bottom ('W' Pattern)**:
   - Two tests of a critical horizontal level with momentum divergence on the second test.

#### Continuation Patterns:
- **Bullish Flag & Pennant**: Sharp impulsive flagpole followed by a shallow, low-volume downward consolidation channel. Breakout resumes the primary upward trend.
- **Ascending Triangle**: Flat horizontal resistance with rising swing lows (higher lows). Indicates buyers are aggressively absorbing sellers at the ceiling.
- **Falling Wedge**: Converging downward-sloping trendlines where downward momentum is dying. Often breaks out violently upward.
"""
            },
            {
                "id": "m1_l4",
                "title": "1.4 Essential Indicators Encyclopedia (RSI, MACD, Bollinger Bands, ATR, VWAP)",
                "content": """
### 📊 Essential Technical Indicators Encyclopedia

Indicators are mathematical derivatives of price and volume. They should be used for **confluence and confirmation**, never as standalone blind triggers.

#### 1. RSI (Relative Strength Index - Momentum Oscillator):
- **Period**: Standard 14 periods.
- **Baseline Levels**: 30 (Oversold), 50 (Momentum Midpoint), 70 (Overbought).
- **Institutional Secret - Divergences**:
  - *Regular Bullish Divergence*: Price makes a **Lower Low**, but RSI forms a **Higher Low**. Signals imminent upward reversal!
  - *Hidden Bullish Divergence*: Price makes a **Higher Low**, but RSI makes a **Lower Low**. Strong trend continuation signal.

#### 2. MACD (Moving Average Convergence Divergence):
- **Components**: Fast EMA (12), Slow EMA (26), Signal Line (9), and Histogram.
- **Usage**: Histogram expanding above zero indicates accelerating bullish momentum. Look for histogram tick changes as early warning triggers.

#### 3. Bollinger Bands (Volatility Envelopes):
- **Calculation**: 20-period SMA middle band $\\pm 2$ standard deviations.
- **The Squeeze**: When bands contract to their tightest width in 50 bars, a massive volatility explosion is imminent.

#### 4. ATR (Average True Range - Volatility Ruler):
- Measures the average market movement over $N$ candles.
- **Critical Risk Rule**: Never use fixed-pip stop losses! Always set Stop Loss as a dynamic multiple of ATR (e.g. $1.5 \\times \\text{ATR}$) to adjust for market volatility.

#### 5. VWAP (Volume-Weighted Average Price):
$$\\text{VWAP} = \\frac{\\sum (\\text{Typical Price} \\times \\text{Volume})}{\\sum \\text{Volume}}$$
- Institutional benchmark: Day-trading algorithms look to buy below VWAP (discount) and sell above VWAP (premium).
"""
            }
        ]
    },
    {
        "id": "module_2",
        "title": "Module 2: Institutional Smart Money Concepts (SMC & ICT)",
        "icon": "🏛️",
        "tag": "Institutional Trading",
        "duration": "60 mins",
        "summary": "Trade how central banks and hedge funds trade. Uncover Fair Value Gaps, Order Blocks, Liquidity Sweeps, BOS, CHoCH, and Optimal Trade Entry (OTE).",
        "lessons": [
            {
                "id": "m2_l1",
                "title": "2.1 Fair Value Gaps (FVG) & Market Inefficiencies",
                "content": """
### ⚡ Fair Value Gaps (FVG) & Liquidity Inefficiencies

A **Fair Value Gap (FVG)** is a 3-candlestick price pattern where one side of the market moves with such immense speed and volume that it creates an auction imbalance.

#### Identifying a Bullish FVG (BISI - Buy-Side Imbalance Sell-Side Inefficiency):
- **Candle 1**: Initial reference candle.
- **Candle 2**: Massive, aggressive upward expansion candle.
- **Candle 3**: Subsequent candle.
- **The Gap**: A void between the **High of Candle 1** and the **Low of Candle 3**. Candle 2's body spans this gap with no overlapping wicks!

```text
  Price ($)
     │                     ┌───┐ (Candle 3 Low: $4,018)
     │                     │   │
     │      ┌───┐          └───┘
     │      │   │ ◄── Candle 2 (Displacement Impulse)
     │      │   │
     │      │   │ ◄══════ FAIR VALUE GAP (FVG) ZONE: $4,012 - $4,018
     │      │   │
     │  ┌───┘   └───┐
     │  │ (Candle 1 │
     │  │  High:    │
     │  │  $4,012)  │
     └──┴───────────┴───────────► Time
```

#### Why Does Price Return to an FVG?
Central banks and market makers cannot fill their entire multi-million dollar positions in one go without causing excessive slippage. When an FVG is formed, unfilled limit orders remain inside the imbalance. Price magnetically returns to **mitigate** (fill) the FVG before continuing the true institutional trend.

#### The 50% Consequent Encroachment (CE):
The exact midpoint ($50\\%$) of the FVG box is known as **Consequent Encroachment (CE)**. High-probability institutional reaction wicks frequently bounce precisely at the CE level.
"""
            },
            {
                "id": "m2_l2",
                "title": "2.2 Institutional Order Blocks (OB) & Mitigation",
                "content": """
### 🧱 Institutional Order Blocks (OB)

An **Order Block** represents the specific footprint where institutional algorithms placed their massive block orders before initiating an explosive directional rally or crash.

#### Types of Order Blocks:
1. **Bullish Order Block (+OB)**:
   - *Definition*: The **last downward (bearish) candle** prior to an aggressive upward displacement that breaks prior market structure (BOS).
   - *Validation Criteria*: Must be followed by strong momentum that creates a Fair Value Gap (FVG) and breaks a swing high.
   - *Trade Trigger*: Wait for price to pull back into the upper 50% body of the bearish candle. Set a micro-tight Stop Loss just below the Order Block low.
2. **Bearish Order Block (-OB)**:
   - *Definition*: The **last upward (bullish) candle** prior to an aggressive downward collapse that shatters support.
   - *Trade Trigger*: Short on the retest of the Order Block body with SL placed strictly above its high.

#### Breaker Blocks & Mitigation Blocks:
- **Breaker Block**: A failed Order Block that resulted in a liquidity sweep of a swing high/low before failing. When price reverses back through it, the failed Order Block flips into an ultra-high-conviction support/resistance level.
"""
            },
            {
                "id": "m2_l3",
                "title": "2.3 Market Structure: Break of Structure (BOS) vs. Change of Character (CHoCH)",
                "content": """
### 🔄 BOS vs. CHoCH - The True Language of Market Structure

Market structure is the skeleton of all price action. Understanding when a trend is merely continuing versus when it has structurally shifted is the hallmark of professional traders.

#### 1. Break of Structure (BOS) - Trend Continuation:
- In an **uptrend**, price forms Higher Highs (HH) and Higher Lows (HL).
- When a new candle body closes **above the previous swing high**, an official **BOS** has occurred.
- *Rule*: A wick break is NOT a confirmed BOS; it can simply be a liquidity hunt! A full candle body close is mandatory.

#### 2. Change of Character (CHoCH) - Early Reversal Warning:
- When price in an uptrend suddenly breaks and closes **below the most recent Higher Low (HL)**, it signals the first sign of institutional trend reversal.
- The CHoCH is where smart money shifts from buying discounts to distributing premium supply.

```text
  [BOS vs CHoCH Illustrated]
  
  Higher High (HH) ───────────┐ (BOS: Continuation)
                             /│
            Higher Low (HL) / │
                           /  │
  Higher High (HH) ───────┘   │
                             /│
            Higher Low (HL) / │
                           /  ▼ 
  Prior Higher Low (HL) ──┴────── ◄── [CHoCH: Body Close BELOW = REVERSAL!]
                                   \\
                                    ▼ Lower Low (LL)
```
"""
            },
            {
                "id": "m2_l4",
                "title": "2.4 Liquidity Pools, Judas Swings & The Optimal Trade Entry (OTE)",
                "content": """
### 🌊 Liquidity Engineering, Judas Swings & OTE

Smart Money cannot buy unless someone else is selling, and cannot sell unless someone else is buying. Therefore, institutions actively **engineer liquidity**.

#### Where Does Retail Liquidity Hide?
- **Buy-Side Liquidity (BSL)**: Clustered above obvious double tops, equal highs (EQH), and swing resistance (retail stop losses and breakout buy-stops).
- **Sell-Side Liquidity (SSL)**: Clustered below double bottoms, equal lows (EQL), and key swing support (retail stop losses and breakdown sell-stops).

#### The London Judas Swing Trap:
- **Phase 1 (Asian Session)**: Price consolidates in a narrow 30-pip range, creating equal highs and equal lows.
- **Phase 2 (London Open 07:00-09:00 UTC)**: Algorithms create a sudden false spike upwards to sweep Buy-Side Liquidity above the Asian High. Retail traders chase the breakout!
- **Phase 3 (The Reversal & True Move)**: Once retail stop orders are absorbed, smart money slams the market downward into the true session trend.

#### Optimal Trade Entry (OTE) Fibonacci Grid:
When calculating retracements from Swing Low to Swing High:
- **0.0% - 50.0%**: Premium Zone (Never buy here; you are paying retail premium!).
- **50.0%**: Equilibrium (Fair value baseline).
- **50.0% - 100.0%**: Discount Zone (Institutions buy here!).
- **61.8% to 79.0%**: The **Optimal Trade Entry (OTE) Corridor**. This is the highest probability entry zone in all institutional trading.
"""
            }
        ]
    },
    {
        "id": "module_3",
        "title": "Module 3: Volume Spread Analysis (VSA) & Market Flow",
        "icon": "📊",
        "tag": "Volume Intelligence",
        "duration": "50 mins",
        "summary": "Read between the bars. Unpack Wyckoff accumulation and distribution phases, volume footprint anomalies, Point of Control (POC), and order book heatmaps.",
        "lessons": [
            {
                "id": "m3_l1",
                "title": "3.1 The Wyckoff Method: Accumulation & Distribution Phases",
                "content": """
### 🏛️ The Wyckoff Method: The Master Blueprint

Richard Wyckoff proved that market cycles are driven by the **Composite Operator** (smart money institutions) in four distinct phases:

#### 1. Accumulation Phase:
- Smart money silently buys large quantities without letting the price rise.
- **Key Events**:
  - *Preliminary Support (PS)* & *Selling Climax (SC)*: Panic retail selling absorbed on massive volume.
  - *Automatic Rally (AR)*: Relief bounce creating the upper boundary of the range.
  - *The Spring (Phase C)*: A deliberate false breakdown below support that traps bears and shakes out weak longs before the true markup begins!

#### 2. Markup Phase:
- Steady trend upward. Price respects Higher Lows with increasing volume on rallies and diminishing volume on pullbacks.

#### 3. Distribution Phase:
- Smart money unloads their inventory to retail buyers who are experiencing extreme FOMO.
- **Key Events**:
  - *Buying Climax (BC)*: Euphoric news and retail rush absorbed by institutional limit sells.
  - *UTAD (Upthrust After Distribution)*: False breakout above the range high to harvest final liquidity before the collapse.

#### 4. Markdown Phase:
- Unrelenting downward trend driven by institutional liquidation and panic stop cascades.
"""
            },
            {
                "id": "m3_l2",
                "title": "3.2 Volume Profiles, Value Area & Point of Control (POC)",
                "content": """
### 📈 Volume Profile & Auction Market Theory

Standard volume bars tell you **when** volume occurred (time-based). The **Volume Profile** tells you **at what exact price level** transactions were executed (price-based).

#### Crucial Volume Profile Components:
1. **Point of Control (POC)**: The single price level with the highest traded volume in the selected timeframe. Acts as an ultra-powerful price magnet.
2. **Value Area (VA)**: The price range where **70% of total volume** occurred.
   - **Value Area High (VAH)**: Upper boundary of institutional acceptance.
   - **Value Area Low (VAL)**: Lower boundary of institutional acceptance.
3. **High Volume Nodes (HVN)**: Prices where extensive consolidation took place; act as strong support and resistance.
4. **Low Volume Nodes (LVN)**: Price zones where price moved so fast that very little volume was transacted. Price will slice through LVNs rapidly when revisited.

#### The Value Area Trading Rule:
If price opens outside the Value Area and then re-enters the Value Area with an hourly candle close, there is an **80% statistical probability** that price will rotate entirely across the range to the opposite Value Area boundary!
"""
            }
        ]
    },
    {
        "id": "module_4",
        "title": "Module 4: Advanced Risk Management & Position Sizing",
        "icon": "🛡️",
        "tag": "Capital Preservation",
        "duration": "45 mins",
        "summary": "The math behind long-term trading longevity: the strict 1% rule, micro-tight Stop Losses (<$3.50 Gold), 1:3+ Risk-to-Reward ratio mastery, and compounding equations.",
        "lessons": [
            {
                "id": "m4_l1",
                "title": "4.1 The Ironclad 1% Rule & Dynamic Position Sizing Formula",
                "content": """
### 🛡️ The Ironclad 1% Rule

Professional trading is not about how much money you can make on a single trade; it is about **capital preservation** so you can survive statistical drawdown streaks.

#### The Golden Rule of Survival:
> **Never risk more than 1% of your total account equity on any single trade.**

If you have a $10,000 account:
$$\\text{Max Dollar Risk per Trade} = \\$10,000 \\times 0.01 = \\$100.00$$

Even if you experience **10 consecutive losing trades**, your account will only draw down to ~$9,043 (less than a 10% drawdown!). If you risk 10% per trade, 10 consecutive losses would wipe out over **65% of your account**, requiring a 185% gain just to break even!

#### The Universal Position Sizing Formula:
$$\\text{Position Size (Units)} = \\frac{\\text{Account Balance} \\times \\text{Risk \\%}}{|\\text{Entry Price} - \\text{Stop Loss Price}|}$$

#### Step-by-Step Example (Spot Gold XAU/USD):
- Account Balance: **$10,000**
- Risk Budget (1%): **$100.00**
- Current Gold Price (Entry): **$4,020.00**
- Micro-Tight Stop Loss: **$4,017.20** (Stop Distance: **$2.80**)
- Lot Calculation:
$$\\text{Units} = \\frac{\\$100.00}{\\$2.80} = 35.71 \\text{ oz} \\approx 0.35 \\text{ Standard Lots}$$

If Gold hits your Stop Loss at $4,017.20, you lose **exactly $100.00** (1% of your account). Your risk is 100% mathematically capped!
"""
            },
            {
                "id": "m4_l2",
                "title": "4.2 Risk-to-Reward (RR) Mastery & Micro-Tight Stop Loss Math",
                "content": """
### 🎯 The Math of Asymmetric Risk-to-Reward (RR)

Why do retail traders blow up while hedge funds thrive with a 45% win rate? The answer is **Asymmetric Risk-to-Reward Ratios**.

#### The Profitability Matrix:
| Win Rate | Risk-Reward Ratio (RR) | Result Over 100 Trades ($100 Risk) | Net Profit |
| :---: | :---: | :---: | :---: |
| 35% | 1 : 1.0 | 35 wins ($3,500) - 65 losses ($6,500) | **-$3,000 (Loss)** |
| 40% | 1 : 3.0 | 40 wins ($12,000) - 60 losses ($6,000) | **+$6,000 (Profit!)** |
| 50% | 1 : 4.5 | 50 wins ($22,500) - 50 losses ($5,000) | **+$17,500 (Massive!)** |
| 65% | 1 : 4.5 | 65 wins ($29,250) - 35 losses ($3,500) | **+$25,750 (Elite!)** |

#### Micro-Tight Stop Loss vs. Oversized SL Hits:
- **The Amateur Mistake**: Setting a huge $18-$25 Stop Loss on Gold or 5% on Crypto. If you lose, your recovery takes weeks.
- **The Institutional Approach**: Enter strictly at the discount Order Block boundary with a micro-tight **$2.50 to $3.50 Stop Loss** on Gold, and **1.1% on Crypto**.
- When the trade works, it expands into TP1 (1:2.5 RR), TP2 (1:4.5 RR), and Mega Target (1:7.5+ RR).

#### The 1:1 Breakeven Ratchet Protocol:
1. When price reaches **TP1 (1:2.5 RR)**, close 50% of the position to bank guaranteed profit.
2. Immediately **shift the Stop Loss of the remaining 50% position to the exact Entry Price**.
3. The trade is now **100% risk-free**. Let the runner ride to TP2 and Mega TP with zero downside exposure!
"""
            }
        ]
    },
    {
        "id": "module_5",
        "title": "Module 5: Trading Psychology & Emotional Discipline",
        "icon": "🧠",
        "tag": "Psychology & Tilt",
        "duration": "40 mins",
        "summary": "Master the inner game. Eliminate FOMO, greed, and fear; defeat revenge trading, and develop the probabilistic mindset of top quantitative traders.",
        "lessons": [
            {
                "id": "m5_l1",
                "title": "5.1 Defeating FOMO, Revenge Trading & The Tilt Protocol",
                "content": """
### 🧠 Conquering the 4 Emotional Killers of Trading

More trading accounts are destroyed by psychological breakdowns than by flawed strategies.

#### 1. FOMO (Fear Of Missing Out):
- *Symptom*: Buying a green candle that has already pumped 8% because you fear missing the train.
- *Antidote*: **The Market Runs 24/7**. Missed moves are not losses; chasing extended moves guarantees buying at institutional distribution peaks. If you missed the entry, wait patiently for the next discount retest.

#### 2. Revenge Trading:
- *Symptom*: Taking an immediate, oversized trade right after a loss to 'get your money back'.
- *Antidote*: **The 2-Loss Tilt Circuit Breaker**. If you suffer **2 consecutive losses in a single day**, your trading terminal must be locked for the next 24 hours. No exceptions!

#### 3. Fear of Pulling the Trigger:
- *Symptom*: Freezing up when a valid, verified setup matches every rule in your trading plan.
- *Antidote*: Accept that any individual trade outcome is random and unpredictable. Profitability comes from the **law of large numbers over a series of 50+ trades**, not from any single trade.

#### The Professional Trader's Daily Creed:
1. I am a risk manager first, a trader second.
2. The market does not owe me anything.
3. A stop loss is an honorable business expense that protects my capital.
4. I will never enter a trade without an established exit plan.
"""
            }
        ]
    },
    {
        "id": "module_6",
        "title": "Module 6: Macroeconomics & Fundamental Catalysts",
        "icon": "🌐",
        "tag": "Macro Intelligence",
        "duration": "35 mins",
        "summary": "Understand global liquidity drivers: CPI inflation, Non-Farm Payrolls (NFP), FOMC interest rate decisions, DXY inverse correlations, and Bitcoin dominance cycles.",
        "lessons": [
            {
                "id": "m6_l1",
                "title": "6.1 Economic News Protocol: Trading CPI, NFP & Interest Rate Decisions",
                "content": """
### 🌐 Macroeconomic Catalysts & Economic Calendar Execution

High-impact macroeconomic data releases represent moments of peak institutional volatility where broker spreads widen and slippage surges.

#### Tier-1 High-Impact Economic Releases:
1. **US CPI (Consumer Price Index)**:
   - Measures inflation. Higher-than-expected CPI leads to expectations of interest rate hikes, strengthening the US Dollar (DXY) and putting downward pressure on Gold and Crypto.
2. **US NFP (Non-Farm Payrolls)**:
   - Released the first Friday of every month. Massive volatility driver across all USD currency pairs and Gold.
3. **FOMC Rate Decisions & Powell Press Conferences**:
   - Sets federal borrowing rates. The single largest liquidity valve in global capital markets.

#### The 15-Minute News Blackout Rule:
- **Never enter new positions 15 minutes before or 15 minutes after a Red-Folder (Tier-1) news release**.
- Automated algorithmic market-making quotes withdraw liquidity seconds prior to the announcement, causing bid-ask spreads to blow out by 5x-10x. Entering during this window invites devastating slippage stop-outs.
- Let the initial whipsaw sweep both sides of liquidity, wait for the dust to settle, and trade the subsequent structural continuation.
"""
            },
            {
                "id": "m6_l2",
                "title": "6.2 Intermarket Correlations: US Dollar Index (DXY) & Bitcoin Dominance (BTC.D)",
                "content": """
### 🔄 Intermarket Macro Correlations

No financial asset trades in isolation. Understanding currency and commodity inter-relationships gives you an unfair institutional edge.

#### 1. The US Dollar Index (DXY) vs. Gold & Crypto:
- **Inverse Correlation**: When the US Dollar strengthens ($DXY \\uparrow$), priced-in-dollar assets like Gold ($XAU/USD \\downarrow$), EUR/USD ($\\downarrow$), and Bitcoin ($\\downarrow$) typically experience downward headwinds.
- **Confluence Check**: Before longing Gold or Bitcoin, check the 4-hour chart of the DXY. If DXY is breaking down below support, your long trade has massive macro tailwinds!

#### 2. Bitcoin Dominance (BTC.D) & Altcoin Season Cycles:
- **BTC.D Rising**: Bitcoin is outperforming the broader market. Keep your crypto capital parked in BTC.
- **BTC.D Stalling / Dropping with BTC Price Stable or Rising**: **Altseason Activated**! Capital rotates aggressively into high-beta layer-1s, meme coins, and DeFi tokens for 50%-500% ROI runs.
"""
            }
        ]
    },
    {
        "id": "module_7",
        "title": "Module 7: Algorithmic & Quantitative Trading Basics",
        "icon": "🤖",
        "tag": "Algorithmic & Quant",
        "duration": "40 mins",
        "summary": "Bridge manual trading into automated quant bots. Understand TradingView Pine Script v5 architecture, MetaTrader 5 Expert Advisors (.ex5 / .mq5), and AI Swarm self-testing.",
        "lessons": [
            {
                "id": "m7_l1",
                "title": "7.1 TradingView Pine Script v5 & MT5 Expert Advisors",
                "content": """
### 🤖 Algorithmic Trading: From Manual Discretion to Automated Code

Algorithmic trading removes human emotional error, executes in milliseconds, and enforces mathematical discipline 24/7.

#### 1. TradingView Pine Script v5 Basics:
- Pine Script is TradingView's proprietary cloud-executed programming language.
- **Structure**:
  - `//@version=5`: Declares modern v5 syntax.
  - `indicator(...)` or `strategy(...)`: Defines whether script is a visual study or an automated backtesting algorithm.
  - Built-in functions: `ta.sma()`, `ta.rsi()`, `ta.atr()`, `ta.highest()`, `ta.lowest()`.
  - Alerts: `alertcondition()` allows connecting webhooks to Telegram or execution brokers.

#### 2. MetaTrader 5 (MT5) Expert Advisors (EA):
- **Source Code (.mq5)**: High-performance C++ based syntax compiled inside MetaEditor.
  - `OnInit()`: Executed once on chart attach. Initializes symbol subscriptions and parameters.
  - `OnTick()`: Executed every millisecond when a new price quote arrives from the broker. Evaluates entry triggers, lot sizes, and Stop Loss management.
  - `OnDeinit()`: Cleanup routine on chart removal.
- **Compiled Executable (.ex5)**: A pre-compiled binary container executable directly by MT5 terminals without requiring manual source editing.
"""
            }
        ]
    }
]

def get_all_modules() -> List[Dict[str, Any]]:
    """Returns the full masterclass curriculum."""
    return CURRICULUM_MODULES

def get_module_by_id(mod_id: str) -> Dict[str, Any]:
    """Retrieves a specific curriculum module."""
    for m in CURRICULUM_MODULES:
        if m["id"] == mod_id:
            return m
    return CURRICULUM_MODULES[0]
