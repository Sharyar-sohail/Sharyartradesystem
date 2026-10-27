"""
Interactive Quizzes, Scenario Challenges & Certification Engine for Sharyar Trading Academy
Features:
- Dynamic multi-topic multiple-choice quizzes
- Chart-reading tactical scenario challenges (FVG, Order Block, Liquidity Sweeps)
- Instant scoring feedback with detailed educational explanations
- User XP tracking and unlockable achievement badges
"""

from typing import Dict, Any, List

QUIZ_TOPICS: List[Dict[str, Any]] = [
    {
        "id": "quiz_foundations",
        "title": "Module 1: Candlestick & Technical Analysis Exam",
        "module_id": "module_1",
        "badge": "🥉 Novice Chartist",
        "questions": [
            {
                "id": "q1_1",
                "question": "What does a long lower wick on a candle located at key support indicate?",
                "options": [
                    "Institutional sellers are preparing to dump the market further.",
                    "Sellers tried to drive price lower, but institutional buyers absorbed all selling and pushed price back up.",
                    "The market has zero liquidity and trading should be paused.",
                    "The candle is invalid and should be ignored."
                ],
                "correct_index": 1,
                "explanation": "A long lower wick represents heavy absorption and demand rejection. Sellers pushed price down, but institutional demand absorbed the orders, closing the candle well above the lows."
            },
            {
                "id": "q1_2",
                "question": "What is a 'Regular Bullish Divergence' on the RSI indicator?",
                "options": [
                    "Price makes a Higher High while RSI makes a Lower High.",
                    "Price makes a Lower Low while RSI makes a Higher Low.",
                    "Both price and RSI hit 70 at the exact same moment.",
                    "RSI falls below 10 while volume drops to zero."
                ],
                "correct_index": 1,
                "explanation": "Regular Bullish Divergence occurs when price prints a Lower Low (exhaustion), but the RSI momentum oscillator prints a Higher Low, signaling weakening selling momentum and an imminent upward reversal."
            },
            {
                "id": "q1_3",
                "question": "When Bollinger Bands contract into a tight, narrow corridor ('The Squeeze'), what typically follows?",
                "options": [
                    "The market enters permanent low volatility.",
                    "An explosive volatility breakout in one direction.",
                    "The broker halts trading for maintenance.",
                    "RSI automatically resets to 50."
                ],
                "correct_index": 1,
                "explanation": "Volatility is cyclical. Severe contraction (The Squeeze) compresses order flow until an energetic volatility expansion occurs, often initiating multi-day trending moves."
            }
        ]
    },
    {
        "id": "quiz_smc",
        "title": "Module 2: Institutional Smart Money Concepts (SMC & ICT) Exam",
        "module_id": "module_2",
        "badge": "🥈 Smart Money Apprentice",
        "questions": [
            {
                "id": "q2_1",
                "question": "What creates a 3-candlestick Fair Value Gap (FVG)?",
                "options": [
                    "Two candles having identical open and close prices.",
                    "A void where Candle 2 has explosive displacement and there is an unfilled price gap between Candle 1's wick and Candle 3's wick.",
                    "When price trades at the exact same level for 3 hours.",
                    "When the broker charges a weekend rollover fee."
                ],
                "correct_index": 1,
                "explanation": "An FVG is created during aggressive displacement. The void between the high of Candle 1 and the low of Candle 3 (in a bullish FVG) contains unfilled institutional limit orders that price magnetically revisits."
            },
            {
                "id": "q2_2",
                "question": "What defines a validated Bullish Order Block (+OB)?",
                "options": [
                    "Any green candle with high volume.",
                    "The last bearish (down-close) candle prior to an impulsive displacement that breaks market structure (BOS).",
                    "A doji candle forming in the middle of a consolidation.",
                    "The lowest price recorded over the past 365 days."
                ],
                "correct_index": 1,
                "explanation": "A Bullish Order Block is the specific footprint where institutional algorithms bought prior to an explosive move that broke structure. Price retesting this block offers high-probability discount entries."
            },
            {
                "id": "q2_3",
                "question": "What is the critical difference between a Break of Structure (BOS) and a Change of Character (CHoCH)?",
                "options": [
                    "BOS is only for Forex, while CHoCH is only for Crypto.",
                    "BOS signals trend continuation (breaking prior swing highs in an uptrend); CHoCH signals trend reversal (breaking the prior Higher Low in an uptrend).",
                    "There is no difference; they are identical terms.",
                    "BOS is a 1-minute pattern, while CHoCH is a daily pattern."
                ],
                "correct_index": 1,
                "explanation": "BOS confirms that the prevailing trend is continuing by breaking swing highs/lows. CHoCH is the critical early warning that the market structure has shifted against the prevailing trend."
            },
            {
                "id": "q2_4",
                "question": "What is the primary objective of an institutional 'London Judas Swing'?",
                "options": [
                    "To announce macroeconomic news releases.",
                    "To generate a false breakout that sweeps Asian range retail stop losses before launching the true trend.",
                    "To equalize broker spreads across London and New York.",
                    "To freeze retail trading accounts."
                ],
                "correct_index": 1,
                "explanation": "The Judas Swing traps breakout traders by briefly piercing Asian highs/lows to harvest liquidity, then reversing violently in the direction of smart money's true daily intention."
            }
        ]
    },
    {
        "id": "quiz_risk",
        "title": "Module 3: Advanced Risk Management & Capital Longevity Exam",
        "module_id": "module_4",
        "badge": "🥇 Risk & Discipline Master",
        "questions": [
            {
                "id": "q3_1",
                "question": "According to the institutional 1% Rule, what is the maximum dollar risk allowed on a $10,000 account?",
                "options": [
                    "$1,000.00 (10%)",
                    "$500.00 (5%)",
                    "$100.00 (1%)",
                    "$10.00 (0.1%)"
                ],
                "correct_index": 2,
                "explanation": "1% of $10,000 is exactly $100.00. Strictly capping risk to $100 per trade ensures you can easily withstand drawdown streaks without risking account ruin."
            },
            {
                "id": "q3_2",
                "question": "If you have a 40% win-rate system with a 1:3.0 Risk-to-Reward ratio, what is the net result over 100 trades risking $100 per trade?",
                "options": [
                    "You lose money because your win rate is below 50%.",
                    "Net Profit of +$6,000.00 (40 wins x $300 = $12,000 minus 60 losses x $100 = $6,000).",
                    "You break exactly even ($0).",
                    "Your broker shuts down your account."
                ],
                "correct_index": 1,
                "explanation": "With a 1:3 RR, 40 wins yield $12,000. 60 losses cost $6,000. Net profit is a massive +$6,000 despite losing 60% of all trades! Mathematical expectancy triumphs over win rate."
            },
            {
                "id": "q3_3",
                "question": "Why is an oversized $19+ Stop Loss on Gold (XAU/USD) hazardous compared to an institutional $2.50 - $3.50 SL?",
                "options": [
                    "Gold cannot move more than $5 in a day.",
                    "A wide $19+ Stop Loss requires an enormous $57+ move just to hit 1:3 RR, while a $2.80 micro-tight SL achieves 1:4.5 RR on a standard $12.60 intraday swing.",
                    "Stop losses are illegal in precious metals.",
                    "Brokers do not accept stop losses smaller than $20."
                ],
                "correct_index": 1,
                "explanation": "Micro-optimizing your Stop Loss to $2.50–$3.50 at institutional Order Blocks allows you to capture asymmetric 1:4.5+ RR ratios on normal session volatility rather than requiring once-in-a-month outlier moves."
            }
        ]
    },
    {
        "id": "quiz_psychology",
        "title": "Module 4: Trading Psychology & Tilt Protocol Exam",
        "module_id": "module_5",
        "badge": "💎 Emotional Fortitude Badge",
        "questions": [
            {
                "id": "q4_1",
                "question": "What is the recommended action when a trader experiences 2 consecutive Stop Loss hits in a single session?",
                "options": [
                    "Double the position size on the third trade to recover the losses quickly (Martingale).",
                    "Enforce the 2-Loss Tilt Protocol: close charts and step away for the next 24 hours.",
                    "Switch to 1-minute scalping to scalp small profits.",
                    "Post angrily in social media trading channels."
                ],
                "correct_index": 1,
                "explanation": "The 2-Loss Circuit Breaker prevents the emotional spiral of 'revenge trading'. Walking away preserves both mental clarity and account capital until market conditions reset."
            },
            {
                "id": "q4_2",
                "question": "How should a professional quantitative trader view a Stop Loss hit?",
                "options": [
                    "As a personal failure and proof that their analysis was completely wrong.",
                    "As an ordinary business expense and a vital safety mechanism that protected 99% of their capital.",
                    "As market manipulation directed specifically at them.",
                    "As a reason to abandon their strategy immediately."
                ],
                "correct_index": 1,
                "explanation": "In probabilistic trading, losses are unavoidable costs of doing business—just like inventory cost for a shopkeeper. Capital preservation allows you to execute the next high-probability setup."
            }
        ]
    }
]

def get_all_quizzes() -> List[Dict[str, Any]]:
    """Returns all quiz modules."""
    return QUIZ_TOPICS

def calculate_student_badges(completed_quiz_scores: Dict[str, float]) -> List[str]:
    """Evaluates student quiz scores and awards milestone badges."""
    badges = []
    total_score = 0.0
    count = 0

    for q in QUIZ_TOPICS:
        qid = q["id"]
        if qid in completed_quiz_scores:
            score = completed_quiz_scores[qid]
            count += 1
            total_score += score
            if score >= 70.0:
                badges.append(q["badge"])

    # Master badge if all completed with avg >= 85%
    if count == len(QUIZ_TOPICS) and (total_score / count) >= 85.0:
        badges.append("👑 Sharyar Certified Quantitative Trader")

    return badges
