"""
Institutional Alpha/Beta Audit Verification Suite for Sharyar Trading System
Runs automated end-to-end tests across Rounds 1 through 6.
"""

import sys
import os
import json
import traceback

# Ensure root directory is in sys.path
ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

results = {}

def test_round_1_import_integrity():
    print("\n--- [ROUND 1] PACKAGE & IMPORT INTEGRITY AUDIT ---")
    try:
        import backend
        import backend.db
        import backend.config
        import backend.binance_client
        import backend.crypto_engine
        import backend.forex_engine
        import backend.forex_metal_engine
        import backend.market_structure
        import backend.liquidity_heatmap
        import backend.pinescript_generator
        import backend.ai_agent_swarm
        import backend.runner
        import strategies.base_strategy
        import strategies.mega_pump_scanner
        import strategies.candle_king
        import strategies.divergence_momentum
        import strategies.ict_killzone
        import strategies.ma_confluence
        import strategies.smc_orderblock
        import strategies.volatility_contraction
        import strategies.wyckoff_vsa
        import ui.components
        print("✅ Round 1 Passed: All 20+ modules and submodules imported cleanly with zero errors.")
        results["Round 1: Imports"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 1 Failed: {e}")
        traceback.print_exc()
        results["Round 1: Imports"] = f"FAIL: {e}"
        return False

def test_round_2_feature_reconciliation():
    print("\n--- [ROUND 2] FEATURE RECONCILIATION & RISK SIDE-EVALUATION AUDIT ---")
    try:
        from strategies.mega_pump_scanner import MegaPumpScanner
        from backend.market_structure import analyze_market_structure
        from app import analyze_manual_trade_risk
        import pandas as pd
        import numpy as np

        # 1. Test Mega Pump Scanner instantiation and structure
        scanner = MegaPumpScanner()
        assert scanner.name == "Mega Pump 50%-500% ROI Coin Scanner"
        print("  - Mega Pump Scanner verified.")

        # 2. Test Market Structure Analysis
        df = pd.DataFrame({
            "timestamp": pd.date_range("2026-01-01", periods=100, freq="15min"),
            "open": np.linspace(100, 150, 100) + np.random.normal(0, 1, 100),
            "high": np.linspace(102, 153, 100) + np.random.uniform(0.5, 2, 100),
            "low": np.linspace(98, 147, 100) - np.random.uniform(0.5, 2, 100),
            "close": np.linspace(101, 151, 100) + np.random.normal(0, 1, 100),
            "volume": np.random.uniform(1000, 5000, 100)
        })
        ms = analyze_market_structure(df)
        assert "trend" in ms
        assert "structure" in ms
        assert "equilibrium" in ms
        assert "is_discount" in ms
        assert "is_ote" in ms
        assert "order_blocks" in ms
        assert "fvgs" in ms
        print(f"  - Market Structure Engine verified: Trend={ms['trend']}, Structure={ms['structure']}, OTE={ms['is_ote']}")

        # 3. Test Trade Risk & Side-Evaluation Engine
        risk_eval = analyze_manual_trade_risk(
            symbol="BTCUSDT",
            lane="Crypto",
            direction="LONG",
            entry_price=100000.0,
            stop_loss=98000.0,
            take_profit=106000.0,
            account_balance=10000.0,
            risk_pct=1.5
        )
        assert risk_eval["side_status"] in ("RIGHT SIDE OF MARKET", "WRONG SIDE OF MARKET")
        assert risk_eval["rr_ratio"] == 3.0
        assert risk_eval["risk_amount_usd"] == 150.0
        print(f"  - Risk Side-Evaluation verified: Status={risk_eval['side_status']}, Alignment={risk_eval['alignment_score']}%, RR=1:{risk_eval['rr_ratio']}")

        print("✅ Round 2 Passed: Historical Mega Pump features & Side-Evaluation system fully operational.")
        results["Round 2: Features & Risk"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 2 Failed: {e}")
        traceback.print_exc()
        results["Round 2: Features & Risk"] = f"FAIL: {e}"
        return False

def test_round_3_agent_swarm():
    print("\n--- [ROUND 3] MULTI-AGENT AI SWARM & SIGNAL QUALITY AUDIT ---")
    try:
        from backend.ai_agent_swarm import ai_agent_swarm, UNIVERSE_50_PAIRS
        assert len(UNIVERSE_50_PAIRS) >= 50
        print(f"  - Swarm universe verified: {len(UNIVERSE_50_PAIRS)} pairs monitored across Crypto & Forex/Metals.")

        # Run 2 synchronous swarm learning cycles
        res1 = ai_agent_swarm.run_swarm_learning_cycle()
        assert "asset" in res1
        assert "win_rate" in res1
        print(f"  - Swarm Cycle 1: {res1['asset']} | Strategy: {res1['strategy']} | Tested WR: {res1['win_rate']}%")

        res2 = ai_agent_swarm.run_swarm_learning_cycle()
        assert "asset" in res2
        print(f"  - Swarm Cycle 2: {res2['asset']} | Strategy: {res2['strategy']} | Tested WR: {res2['win_rate']}%")

        telemetry = ai_agent_swarm.get_swarm_telemetry()
        assert telemetry["total_cycles"] >= 2
        assert telemetry["active_agents"] == 5
        assert len(telemetry["recent_dialogue"]) > 0
        print(f"  - Telemetry verified: Active Agents={telemetry['active_agents']}, Total Cycles={telemetry['total_cycles']}")

        print("✅ Round 3 Passed: 5 Specialized Swarm Agents operating seamlessly over 50+ pairs.")
        results["Round 3: AI Swarm"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 3 Failed: {e}")
        traceback.print_exc()
        results["Round 3: AI Swarm"] = f"FAIL: {e}"
        return False

def test_round_4_liquidity_heatmap_pinescript():
    print("\n--- [ROUND 4] COINGLASS LIQUIDITY HEATMAP & PINE SCRIPT V5 AUDIT ---")
    try:
        from backend.liquidity_heatmap import generate_liquidity_heatmap_model, build_coinglass_liquidity_heatmap_chart
        from backend.pinescript_generator import generate_tradingview_pinescript_v5
        import pandas as pd
        import numpy as np

        df = pd.DataFrame({
            "timestamp": pd.date_range("2026-01-01", periods=60, freq="15min"),
            "open": np.linspace(2600, 2750, 60),
            "high": np.linspace(2610, 2760, 60),
            "low": np.linspace(2590, 2740, 60),
            "close": np.linspace(2605, 2755, 60),
            "volume": np.random.uniform(500, 2500, 60)
        })

        # 1. Test Liquidity Heatmap Model
        model = generate_liquidity_heatmap_model(df, current_price=2755.0)
        assert len(model["clusters"]) > 0
        assert "summary" in model
        print(f"  - Liquidity Model generated: Total Liquidation Pool=${model['summary']['total_pool_volume_usd']:,.0f}")

        # 2. Test Plotly Chart Generator
        fig = build_coinglass_liquidity_heatmap_chart(df, model, symbol="XAUUSD")
        assert fig is not None
        print("  - CoinGlass-style Plotly Liquidity Heatmap Chart successfully constructed.")

        # 3. Test Pine Script v5 Generator
        pine_code = generate_tradingview_pinescript_v5("Smart Money Concepts (SMC)", "Crypto", "BTCUSDT")
        assert "//@version=5" in pine_code
        assert "indicator(" in pine_code
        assert "[Sharyar Quant v5]" in pine_code
        assert "alertcondition(" in pine_code
        print(f"  - Pine Script v5 Generator validated: Generated {len(pine_code.splitlines())} lines of code.")

        print("✅ Round 4 Passed: Liquidity Heatmap model and Pine Script v5 generation verified.")
        results["Round 4: Heatmap & PineScript"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 4 Failed: {e}")
        traceback.print_exc()
        results["Round 4: Heatmap & PineScript"] = f"FAIL: {e}"
        return False

def test_round_5_forex_and_crypto_engines():
    print("\n--- [ROUND 5] MODULAR CRYPTO & FOREX ENGINES AUDIT ---")
    try:
        from backend.forex_engine import ForexEngine, MAJOR_FOREX_PAIRS
        from backend.crypto_engine import crypto_engine

        assert "XAU/USD" in MAJOR_FOREX_PAIRS
        assert "XAG/USD" in MAJOR_FOREX_PAIRS
        assert "EUR/USD" in MAJOR_FOREX_PAIRS
        print(f"  - Forex Engine major pairs confirmed ({len(MAJOR_FOREX_PAIRS)} pairs).")

        # Test ForexEngine synthetic/cached fallback reliability
        fe = ForexEngine()
        board = fe.get_board_of_high()
        assert len(board) > 0
        print(f"  - Forex Board of High verified: {len(board)} active assets analyzed.")

        mt_data = fe.get_metatrader_depth("XAU/USD")
        assert "bid" in mt_data and "ask" in mt_data
        print(f"  - MetaTrader Depth Feed verified: XAU/USD Bid={mt_data['bid']} Ask={mt_data['ask']}")

        print("✅ Round 5 Passed: Crypto and Forex/Metals modular backends verified.")
        results["Round 5: Modular Engines"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 5 Failed: {e}")
        traceback.print_exc()
        results["Round 5: Modular Engines"] = f"FAIL: {e}"
        return False

def test_round_6_full_compilation():
    print("\n--- [ROUND 6] FULL COMPILATION & SYNTAX INTEGRITY AUDIT ---")
    try:
        import py_compile
        files_to_test = [
            "app.py",
            "backend/ai_agent_swarm.py",
            "backend/crypto_engine.py",
            "backend/forex_engine.py",
            "backend/forex_metal_engine.py",
            "backend/market_structure.py",
            "backend/liquidity_heatmap.py",
            "backend/pinescript_generator.py",
            "backend/runner.py",
            "backend/db.py",
            "ui/components.py"
        ]
        for f in files_to_test:
            fpath = os.path.join(ROOT_DIR, f)
            py_compile.compile(fpath, doraise=True)
            print(f"  - Compiled cleanly: {f}")

        print("✅ Round 6 Passed: 100% of core application files compile with zero syntax errors.")
        results["Round 6: Compilation"] = "PASS"
        return True
    except Exception as e:
        print(f"❌ Round 6 Failed: {e}")
        traceback.print_exc()
        results["Round 6: Compilation"] = f"FAIL: {e}"
        return False

if __name__ == "__main__":
    t1 = test_round_1_import_integrity()
    t2 = test_round_2_feature_reconciliation()
    t3 = test_round_3_agent_swarm()
    t4 = test_round_4_liquidity_heatmap_pinescript()
    t5 = test_round_5_forex_and_crypto_engines()
    t6 = test_round_6_full_compilation()

    print("\n==========================================")
    print("      FINAL AUDIT SUMMARY & VERDICT       ")
    print("==========================================")
    all_passed = True
    for test_name, status in results.items():
        print(f"{test_name:35}: {status}")
        if not status.startswith("PASS"):
            all_passed = False

    if all_passed:
        print("\n🎉 ALL 6 INTERNAL TESTING ROUNDS PASSED!")
        sys.exit(0)
    else:
        print("\n⚠️ SOME AUDIT ROUNDS FAILED.")
        sys.exit(1)
