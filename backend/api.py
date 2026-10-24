"""
Standalone FastAPI REST Microservice for Binance Trading & Strategy Engine
Allows headless execution, external integrations, local API connectivity,
and cross-platform synchronization with the Android Standalone Mobile App.
"""

from typing import Optional, List, Dict, Any
from pathlib import Path
from fastapi import FastAPI, HTTPException, Query, BackgroundTasks
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from datetime import datetime, timezone

from backend.config import DEFAULT_PAIRS, DEFAULT_TIMEFRAME
from backend.binance_client import binance_client
from backend.strategies import STRATEGY_REGISTRY, evaluate_market_wide_setup, analyze_manual_trade_risk, analyze_manual_trade_portfolio
from backend.db import (
    get_all_breakout_signals,
    get_active_breakout_signals,
    get_breakout_accuracy_stats,
    get_storage_stats,
    get_logs,
    log_event,
    save_manual_trade_analysis,
    get_manual_trade_analyses,
    get_completed_batch_evaluations
)
from backend.runner import trading_runner
from backend.evolution_engine import batch_tracker, strategy_evolution_core

# Initialize FastAPI app
api_app = FastAPI(
    title="Binance Autonomous Trading Engine API",
    description="High-performance REST API for market scanning, multi-strategy consensus, batch evolution, and real-time accuracy verification.",
    version="2.1.0"
)
app = api_app

# Enable CORS for cross-origin access (e.g. mobile apps, local web dev, electron)
api_app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =====================================================================
# MODELS
# =====================================================================

class ScannerToggleRequest(BaseModel):
    action: str  # "start" or "pause"

class ManualSweepRequest(BaseModel):
    pairs_count: Optional[int] = 30

class AutoTradeToggleRequest(BaseModel):
    enabled: Optional[bool] = None
    trade_size_usd: Optional[float] = None

class ManualTradeRiskRequest(BaseModel):
    symbol: str
    direction: str  # "LONG" or "SHORT"
    entry_price: float
    stop_loss: Optional[float] = None
    take_profit: Optional[float] = None
    position_size_usd: Optional[float] = 1.0
    timeframe: Optional[str] = "15m"

class BatchManualTradeRequest(BaseModel):
    trades: List[ManualTradeRiskRequest]

# =====================================================================
# ENDPOINTS
# =====================================================================

@api_app.get("/api/health")
def get_health() -> Dict[str, Any]:
    """Returns connectivity health, latency, and background runner state."""
    binance_ok = binance_client.ping()
    now_utc = datetime.now(timezone.utc).isoformat()
    return {
        "status": "online" if binance_ok else "degraded",
        "timestamp": now_utc,
        "binance_connected": binance_ok,
        "scanner_active": trading_runner.is_running,
        "auto_trade_enabled": trading_runner.auto_paper_trade,
        "trade_size_usd": trading_runner.trade_size_usd,
        "scanned_pairs_count": trading_runner.total_pairs_scanned_count,
        "market_universe_size": len(trading_runner.all_market_pairs),
        "last_scan_time": trading_runner.last_scan_time,
        "open_paper_positions": len(trading_runner.open_positions)
    }

@api_app.get("/api/markets/pairs")
@api_app.get("/api/pairs")
def get_all_pairs(quote: str = "USDT", force_refresh: bool = False) -> Dict[str, Any]:
    """Dynamically loads and returns every active trading pair available on Binance via CCXT."""
    pairs = binance_client.get_all_active_pairs(quote=quote, force_refresh=force_refresh)
    return {
        "count": len(pairs),
        "quote": quote,
        "pairs": pairs
    }

@api_app.get("/api/market/cross-exchange")
def get_cross_exchange_market(
    exchanges: Optional[str] = None,
    quote: str = "USDT"
) -> Dict[str, Any]:
    """Returns real-time tickers across Binance, Bybit, Bitget, and Gate.io with exchange tagging."""
    from backend.cross_exchange import multi_exchange_manager
    ex_list = [e.strip() for e in exchanges.split(",")] if exchanges else None
    tickers = multi_exchange_manager.get_all_cross_exchange_tickers(exchanges=ex_list, quote=quote)
    return {
        "count": len(tickers),
        "exchanges": ex_list or multi_exchange_manager.EXCHANGES,
        "tickers": tickers
    }

@api_app.get("/api/market/htf-analysis")
def get_htf_analysis(
    symbol: str = "BTC/USDT",
    exchange: str = "Binance"
) -> Dict[str, Any]:
    """Evaluates 4H and 1D market structure, outputting 'Long Triggered (4H)' or 'Short Active (1D)'."""
    from backend.cross_exchange import multi_exchange_manager
    return multi_exchange_manager.analyze_htf_structure(symbol=symbol, exchange=exchange)

@api_app.get("/api/strategies")
def list_strategies() -> Dict[str, Any]:
    """Returns all registered institutional and AI trading strategies."""
    strats = []
    for key, meta in STRATEGY_REGISTRY.items():
        strats.append({
            "id": key,
            "name": meta["name"],
            "type": meta["type"],
            "description": meta["description"],
            "target_move": meta["target_move"],
            "ideal_timeframe": meta["ideal_timeframe"]
        })
    return {"count": len(strats), "strategies": strats}

@api_app.get("/api/signals")
def get_signals(
    limit: int = 50,
    direction: Optional[str] = None,
    status: Optional[str] = None,
    min_confidence: Optional[float] = None
) -> Dict[str, Any]:
    """Returns real-time trading signals logged by the continuous scanner with R:R tiers (1:2 to 1:500)."""
    signals = get_all_breakout_signals(
        limit=limit,
        direction=direction,
        status=status,
        min_confidence=min_confidence
    )
    return {
        "count": len(signals),
        "signals": signals
    }

@api_app.get("/api/signals/active")
def get_active_signals() -> Dict[str, Any]:
    """Returns all currently live monitored setups being tracked for 10%-20% breakouts and mega-moves."""
    active = get_active_breakout_signals()
    return {
        "count": len(active),
        "active_signals": active
    }

@api_app.get("/api/signals/{signal_id}")
def get_signal_detail(signal_id: int) -> Dict[str, Any]:
    """Returns detailed real-time tracking metrics for a specific signal."""
    all_sigs = get_all_breakout_signals(limit=500)
    for s in all_sigs:
        if s["id"] == signal_id:
            return s
    raise HTTPException(status_code=404, detail="Signal not found")

@api_app.get("/api/stats")
def get_accuracy_metrics() -> Dict[str, Any]:
    """Computes and returns comprehensive real-time strategy win rates and performance stats."""
    return get_breakout_accuracy_stats()

@api_app.get("/api/market/gainers-losers")
def get_market_gainers_losers(limit: int = 15) -> Dict[str, Any]:
    """Returns top 24hr gainers and losers from Binance."""
    return {
        "gainers": binance_client.get_top_gainers(limit=limit),
        "losers": binance_client.get_top_losers(limit=limit)
    }

@api_app.get("/api/market/candles")
def get_market_candles(
    symbol: str = Query("BTC/USDT", description="Trading pair"),
    timeframe: str = Query("15m", description="Candle interval (1m, 5m, 15m, 1h, 4h, 1d)"),
    limit: int = Query(60, ge=10, le=200)
) -> Dict[str, Any]:
    """Fetches real-time OHLCV candles from Binance for charting and live inspection."""
    df = binance_client.fetch_ohlcv(symbol=symbol, timeframe=timeframe, limit=limit)
    if df is None or df.empty:
        raise HTTPException(status_code=400, detail=f"Failed to fetch candles for {symbol}")

    setup = evaluate_market_wide_setup(df)
    candles = df.to_dict(orient="records")
    return {
        "symbol": symbol,
        "timeframe": timeframe,
        "count": len(candles),
        "candles": candles,
        "active_setup": setup
    }

@api_app.post("/api/scanner/sweep")
def trigger_sweep(req: ManualSweepRequest, background_tasks: BackgroundTasks) -> Dict[str, Any]:
    """Triggers an instant market sweep in the background."""
    def run_sweep(count: int):
        trading_runner._refresh_market_universe()
        for sym in trading_runner.all_market_pairs[:count]:
            trading_runner._scan_symbol_for_breakout(sym)
        trading_runner._verify_active_signals_accuracy()
        log_event("SUCCESS", "API", f"Manual sweep of {count} pairs completed via REST API.")

    background_tasks.add_task(run_sweep, req.pairs_count or 30)
    return {"status": "sweep_queued", "pairs_target": req.pairs_count or 30}

@api_app.post("/api/scanner/toggle")
def toggle_scanner(req: ScannerToggleRequest) -> Dict[str, Any]:
    """Starts or pauses the background market scanner."""
    if req.action.lower() == "start":
        trading_runner.start()
        return {"status": "started", "is_running": True}
    elif req.action.lower() == "pause":
        trading_runner.stop()
        return {"status": "paused", "is_running": False}
    else:
        raise HTTPException(status_code=400, detail="Action must be 'start' or 'pause'")

# =====================================================================
# BATCH ANALYTICS & AUTONOMOUS EVOLUTION
# =====================================================================

@api_app.get("/api/batches/current")
def get_current_batch() -> Dict[str, Any]:
    """Returns real-time progress and per-strategy metrics for the active 100-150 trade batch."""
    return batch_tracker.get_current_batch_metrics()

@api_app.get("/api/batches/history")
def get_batch_history(limit: int = 20) -> Dict[str, Any]:
    """Returns completed historical batch evaluations and diagnostic records."""
    batches = get_completed_batch_evaluations(limit=limit)
    return {"count": len(batches), "batches": batches}

@api_app.get("/api/evolution/status")
def get_evolution_status() -> Dict[str, Any]:
    """Returns self-learning evolution parameters, tuned mutations, and synthesized strategies."""
    return strategy_evolution_core.get_status()

# =====================================================================
# SYNCHRONIZED AUTO-TRADING & DEMO STATION
# =====================================================================

@api_app.get("/api/autotrade/status")
def get_autotrade_status() -> Dict[str, Any]:
    """Returns current automated trade execution state and position sizing."""
    return {
        "auto_paper_trade": trading_runner.auto_paper_trade,
        "trade_size_usd": trading_runner.trade_size_usd,
        "paper_balance": round(trading_runner.paper_balance, 2),
        "open_positions_count": len(trading_runner.open_positions),
        "open_positions": list(trading_runner.open_positions.values())
    }

@api_app.post("/api/autotrade/toggle")
def toggle_autotrade(req: AutoTradeToggleRequest) -> Dict[str, Any]:
    """Synchronously toggles autonomous trade execution and updates position sizing ($1 default)."""
    if req.enabled is not None:
        trading_runner.auto_paper_trade = req.enabled
        state_str = "ENABLED" if req.enabled else "PAUSED"
        log_event("TRADE", "AUTOTRADE", f"Autonomous Trade Execution {state_str} (Size: ${trading_runner.trade_size_usd:.2f} per trade).")

    if req.trade_size_usd is not None and req.trade_size_usd > 0:
        trading_runner.trade_size_usd = req.trade_size_usd
        log_event("INFO", "AUTOTRADE", f"Autonomous Trade Size updated to ${req.trade_size_usd:.2f} per trade.")

    return {
        "status": "updated",
        "auto_paper_trade": trading_runner.auto_paper_trade,
        "trade_size_usd": trading_runner.trade_size_usd
    }

# =====================================================================
# ACTIVE POSITION RISK MANAGEMENT & MANUAL TRADE LOSS-MINIMIZATION
# =====================================================================

@api_app.post("/api/manual-trade/analyze")
def analyze_manual_position(req: ManualTradeRiskRequest) -> Dict[str, Any]:
    """
    Analyzes an active manual position against live Binance structure (SMC, FVGs, EMAs, ATR).
    Computes data-driven loss-minimization exit plans, breakeven advice, and optimal re-entry zones.
    Persists evaluation to SQLite.
    """
    sym = req.symbol.upper()
    if not sym.endswith("/USDT") and not sym.endswith("USDT"):
        sym = f"{sym}/USDT"
    elif sym.endswith("USDT") and "/" not in sym:
        sym = f"{sym[:-4]}/USDT"

    tf = req.timeframe or "15m"
    df = binance_client.fetch_ohlcv(symbol=sym, timeframe=tf, limit=80)
    if df is None or len(df) < 20:
        raise HTTPException(status_code=400, detail=f"Insufficient candle data for {sym} ({tf})")

    result = analyze_manual_trade_risk(
        symbol=sym,
        direction=req.direction,
        entry_price=req.entry_price,
        current_sl=req.stop_loss,
        current_tp=req.take_profit,
        position_size_usd=req.position_size_usd or 1.0,
        df=df
    )

    if not result:
        raise HTTPException(status_code=500, detail="Risk calculation failed")

    # Persist analysis
    save_manual_trade_analysis(result)
    log_event("TRADE", "RISK_MGR", f"Analyzed active position {sym} ({req.direction.upper()}): {result['risk_label']} | Exit Target: ${result['exit_price']:,.4f}")

    return result

@api_app.post("/api/manual-trade/analyze-portfolio")
def analyze_manual_portfolio(req: BatchManualTradeRequest) -> Dict[str, Any]:
    """
    Analyzes multiple active manual positions simultaneously against live market structure.
    Determines Right/Wrong side of momentum, optimal relief exit points, and recalculated SL/TP levels.
    """
    trades_dicts = [t.model_dump() for t in req.trades]
    analyses = analyze_manual_trade_portfolio(trades_dicts)
    for a in analyses:
        save_manual_trade_analysis(a)
    return {
        "count": len(analyses),
        "portfolio": analyses
    }

@api_app.get("/api/manual-trade/history")
def get_manual_trade_history(limit: int = 50) -> Dict[str, Any]:
    """Returns historical manual trade risk analyses."""
    analyses = get_manual_trade_analyses(limit=limit)
    return {"count": len(analyses), "analyses": analyses}

# =====================================================================
# LOGS & APK DOWNLOAD
# =====================================================================

@api_app.get("/api/logs")
def get_system_logs(limit: int = 100) -> Dict[str, Any]:
    """Returns fast RAM ring buffer logs."""
    logs = get_logs(limit=limit)
    return {"count": len(logs), "logs": logs}

@api_app.get("/download/apk")
def download_android_apk():
    """Direct APK download endpoint for mobile devices."""
    apk_file = Path(__file__).resolve().parent.parent / "android_app" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk_file.exists():
        raise HTTPException(status_code=404, detail="APK not found")
    return FileResponse(
        path=str(apk_file),
        filename="BinanceTerminal-debug.apk",
        media_type="application/vnd.android.package-archive"
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.api:api_app", host="0.0.0.0", port=8000, reload=False)
