"""
Lightweight Database & Memory Ring Buffer Engine
Features WAL Mode, auto-pruning, and zero-latency RAM ring buffers.
"""

import sqlite3
import json
import time
import os
from collections import deque
from datetime import datetime, timezone, timedelta
from threading import Lock
from typing import List, Dict, Any, Optional

from backend.config import (
    DB_PATH,
    MAX_LOG_BUFFER_SIZE,
    MAX_LOG_RECORDS,
    MAX_CACHE_DAYS
)

# Thread-safe in-memory ring buffers
_log_lock = Lock()
_log_buffer: deque = deque(maxlen=MAX_LOG_BUFFER_SIZE)

_db_lock = Lock()

def get_connection() -> sqlite3.Connection:
    """Returns an optimized SQLite connection with WAL mode enabled."""
    conn = sqlite3.connect(str(DB_PATH), check_same_thread=False, timeout=15.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA temp_store=MEMORY;")
    conn.execute("PRAGMA cache_size=-8000;")  # 8MB memory cache
    return conn

def init_db():
    """Initializes tables and indexes with low-storage footprint."""
    with _db_lock:
        conn = get_connection()
        cursor = conn.cursor()
        
        # System Logs Table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS system_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                level TEXT NOT NULL,
                module TEXT NOT NULL,
                message TEXT NOT NULL
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON system_logs(timestamp);")

        # Market Cache Table (Lightweight OHLCV cache)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS market_cache (
                symbol TEXT NOT NULL,
                timeframe TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                open REAL NOT NULL,
                high REAL NOT NULL,
                low REAL NOT NULL,
                close REAL NOT NULL,
                volume REAL NOT NULL,
                PRIMARY KEY (symbol, timeframe, timestamp)
            )
        """)

        # Backtest Leaderboard Results
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS backtest_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id TEXT NOT NULL,
                strategy_name TEXT NOT NULL,
                symbol TEXT NOT NULL,
                timeframe TEXT NOT NULL,
                win_rate REAL NOT NULL,
                profit_factor REAL NOT NULL,
                total_return REAL NOT NULL,
                max_drawdown REAL NOT NULL,
                sharpe_ratio REAL NOT NULL,
                total_trades INTEGER NOT NULL,
                win_trades INTEGER NOT NULL,
                loss_trades INTEGER NOT NULL,
                timestamp TEXT NOT NULL,
                equity_curve_json TEXT,
                trade_log_json TEXT,
                params_json TEXT
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_bt_winrate ON backtest_results(win_rate DESC);")

        # Paper Trading Open Positions
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS paper_positions (
                symbol TEXT PRIMARY KEY,
                strategy TEXT NOT NULL,
                side TEXT NOT NULL,
                entry_price REAL NOT NULL,
                entry_time TEXT NOT NULL,
                quantity REAL NOT NULL,
                stop_loss REAL,
                take_profit REAL,
                highest_price REAL,
                trailing_active INTEGER DEFAULT 0
            )
        """)

        # Paper Trading Completed Trades
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS paper_trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                strategy TEXT NOT NULL,
                side TEXT NOT NULL,
                entry_price REAL NOT NULL,
                exit_price REAL NOT NULL,
                entry_time TEXT NOT NULL,
                exit_time TEXT NOT NULL,
                pnl REAL NOT NULL,
                pnl_pct REAL NOT NULL,
                reason TEXT NOT NULL,
                ai_analysis TEXT DEFAULT ''
            )
        """)
        try:
            cursor.execute("ALTER TABLE paper_trades ADD COLUMN ai_analysis TEXT DEFAULT '';")
        except Exception:
            pass

        # Terminal Key-Value Settings
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS terminal_settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)

        # Smart Alerts (Reversals, Early Exits, Milestone Triggers)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS smart_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                direction TEXT NOT NULL,
                alert_type TEXT NOT NULL,
                message TEXT NOT NULL,
                price REAL NOT NULL,
                pnl_pct REAL NOT NULL,
                timestamp TEXT NOT NULL,
                is_active INTEGER DEFAULT 1
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_alerts_active ON smart_alerts(is_active, id DESC);")

        # Historical Mega-Moves Table (50% to 500%+ Pumps & Dumps)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS mega_moves (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                move_type TEXT NOT NULL,
                peak_gain_pct REAL NOT NULL,
                start_price REAL NOT NULL,
                peak_price REAL NOT NULL,
                start_time TEXT NOT NULL,
                peak_time TEXT NOT NULL,
                duration_hours REAL NOT NULL,
                vol_multiplier REAL NOT NULL,
                pattern TEXT NOT NULL,
                pre_pump_data_json TEXT,
                timestamp TEXT NOT NULL
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_mega_gain ON mega_moves(peak_gain_pct DESC);")

        # Live Real-Time Multi-Strategy & Mega-Move Breakout Signals Table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS live_breakout_signals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                symbol TEXT NOT NULL,
                direction TEXT NOT NULL,
                entry_price REAL NOT NULL,
                current_price REAL NOT NULL,
                stop_loss REAL NOT NULL,
                target_10pct REAL NOT NULL,
                target_20pct REAL NOT NULL,
                target_mega REAL DEFAULT 0.0,
                risk_reward_ratio REAL DEFAULT 3.0,
                expected_move_pct REAL DEFAULT 20.0,
                ai_confidence_pct REAL DEFAULT 90.0,
                strategy_name TEXT DEFAULT 'Bollinger Squeeze & Vol Surge',
                is_mega_move INTEGER DEFAULT 0,
                max_favorable_pct REAL DEFAULT 0.0,
                status TEXT NOT NULL,
                bandwidth REAL NOT NULL,
                vol_surge REAL NOT NULL,
                timeframe TEXT NOT NULL,
                signal_reason TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                target_rr_2 REAL DEFAULT 0.0,
                target_rr_5 REAL DEFAULT 0.0,
                target_rr_10 REAL DEFAULT 0.0,
                target_rr_50 REAL DEFAULT 0.0,
                target_rr_100 REAL DEFAULT 0.0,
                target_rr_500 REAL DEFAULT 0.0,
                rr_hit_tier TEXT DEFAULT 'NONE'
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_breakout_status ON live_breakout_signals(status);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_breakout_sym_status ON live_breakout_signals(symbol, status);")

        # Batch Evaluations Table (Blocks of 100 to 150 trades)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS batch_evaluations (
                batch_id TEXT PRIMARY KEY,
                batch_number INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                target_trades INTEGER DEFAULT 100,
                completed_trades INTEGER DEFAULT 0,
                winning_trades INTEGER DEFAULT 0,
                losing_trades INTEGER DEFAULT 0,
                win_rate_pct REAL DEFAULT 0.0,
                total_capital_allocated REAL DEFAULT 0.0,
                net_pnl_usd REAL DEFAULT 0.0,
                profit_factor REAL DEFAULT 0.0,
                strategy_stats_json TEXT,
                rr_tier_stats_json TEXT,
                status TEXT DEFAULT 'IN_PROGRESS'
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_batch_num ON batch_evaluations(batch_number DESC);")

        # Autonomous Evolved Strategies Table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS evolved_strategies (
                strategy_id TEXT PRIMARY KEY,
                parent_strategy TEXT NOT NULL,
                strategy_name TEXT NOT NULL,
                generation INTEGER NOT NULL,
                creation_time TEXT NOT NULL,
                mutation_reason TEXT NOT NULL,
                adapted_parameters_json TEXT NOT NULL,
                win_rate_pct REAL DEFAULT 0.0,
                total_trades INTEGER DEFAULT 0,
                status TEXT DEFAULT 'ACTIVE'
            )
        """)

        # Manual Trade Analysis & Active Position Risk Management Table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS manual_analyzed_trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                symbol TEXT NOT NULL,
                direction TEXT NOT NULL,
                entry_price REAL NOT NULL,
                current_price REAL NOT NULL,
                stop_loss REAL,
                take_profit REAL,
                position_size_usd REAL,
                current_pnl_usd REAL,
                current_pnl_pct REAL,
                risk_status TEXT NOT NULL,
                exit_recommendation TEXT NOT NULL,
                exit_price REAL,
                reentry_recommendation TEXT NOT NULL,
                reentry_price_min REAL,
                reentry_price_max REAL,
                analysis_json TEXT
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_manual_trades_sym ON manual_analyzed_trades(symbol);")

        # Persistent System Settings & Admin Secret Key Table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS system_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
        """)

        # Initialize Default Secret Key 'SHARYAR' if not present
        now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
        cursor.execute("""
            INSERT OR IGNORE INTO system_settings (setting_key, setting_value, updated_at)
            VALUES ('secret_key', 'SHARYAR', ?)
        """, (now_iso,))

        # Auto-migration for existing databases
        cursor.execute("PRAGMA table_info(live_breakout_signals);")
        existing_cols = {col[1] for col in cursor.fetchall()}
        migration_cols = {
            "strategy_name": "TEXT DEFAULT 'Bollinger Squeeze & Vol Surge'",
            "target_mega": "REAL DEFAULT 0.0",
            "risk_reward_ratio": "REAL DEFAULT 3.0",
            "expected_move_pct": "REAL DEFAULT 20.0",
            "ai_confidence_pct": "REAL DEFAULT 90.0",
            "is_mega_move": "INTEGER DEFAULT 0",
            "target_rr_2": "REAL DEFAULT 0.0",
            "target_rr_5": "REAL DEFAULT 0.0",
            "target_rr_10": "REAL DEFAULT 0.0",
            "target_rr_50": "REAL DEFAULT 0.0",
            "target_rr_100": "REAL DEFAULT 0.0",
            "target_rr_500": "REAL DEFAULT 0.0",
            "rr_hit_tier": "TEXT DEFAULT 'NONE'"
        }
        for col_name, col_def in migration_cols.items():
            if col_name not in existing_cols:
                try:
                    cursor.execute(f"ALTER TABLE live_breakout_signals ADD COLUMN {col_name} {col_def};")
                except Exception:
                    pass

        conn.commit()
        conn.close()

def log_event(level: str, module: str, message: str):
    """
    Appends a log to the fast RAM ring buffer and asynchronously persists to SQLite.
    Levels: 'INFO', 'WARNING', 'ERROR', 'SUCCESS', 'TRADE'
    """
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    entry = {
        "timestamp": now_iso,
        "level": level.upper(),
        "module": module,
        "message": message
    }
    
    # 1. Thread-safe in-memory ring buffer (Instant UI feed)
    with _log_lock:
        _log_buffer.append(entry)

    # 2. Persist to DB with auto-pruning
    try:
        with _db_lock:
            conn = get_connection()
            conn.execute(
                "INSERT INTO system_logs (timestamp, level, module, message) VALUES (?, ?, ?, ?)",
                (now_iso, level.upper(), module, message)
            )
            # Auto-prune older logs if count exceeds MAX_LOG_RECORDS
            conn.execute(f"""
                DELETE FROM system_logs WHERE id IN (
                    SELECT id FROM system_logs ORDER BY id DESC LIMIT -1 OFFSET {MAX_LOG_RECORDS}
                )
            """)
            conn.commit()
            conn.close()
    except Exception as e:
        print(f"[Log Error] Failed to write log to DB: {e}")

def get_logs(limit: int = 150) -> List[Dict[str, Any]]:
    """Returns the most recent system logs from the RAM ring buffer or DB."""
    with _log_lock:
        if len(_log_buffer) >= limit:
            return list(_log_buffer)[-limit:]
        
    # If buffer is starting up, query SQLite
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute(
                "SELECT timestamp, level, module, message FROM system_logs ORDER BY id DESC LIMIT ?",
                (limit,)
            )
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in reversed(rows)]
    except Exception:
        return list(_log_buffer)

def save_backtest_result(
    run_id: str,
    strategy_name: str,
    symbol: str,
    timeframe: str,
    win_rate: float,
    profit_factor: float,
    total_return: float,
    max_drawdown: float,
    sharpe_ratio: float,
    total_trades: int,
    win_trades: int,
    loss_trades: int,
    equity_curve: list,
    trade_log: list,
    params: dict = None
) -> int:
    """Stores backtest metrics and trade history into leaderboard."""
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    equity_json = json.dumps(equity_curve)
    trade_json = json.dumps(trade_log)
    params_json = json.dumps(params or {})

    with _db_lock:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO backtest_results (
                run_id, strategy_name, symbol, timeframe, win_rate,
                profit_factor, total_return, max_drawdown, sharpe_ratio,
                total_trades, win_trades, loss_trades, timestamp,
                equity_curve_json, trade_log_json, params_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            run_id, strategy_name, symbol, timeframe, win_rate,
            profit_factor, total_return, max_drawdown, sharpe_ratio,
            total_trades, win_trades, loss_trades, now_iso,
            equity_json, trade_json, params_json
        ))
        row_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return row_id

def get_backtest_leaderboard(min_accuracy: Optional[float] = None) -> List[Dict[str, Any]]:
    """Retrieves sorted backtest leaderboard rankings."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            query = """
                SELECT id, run_id, strategy_name, symbol, timeframe, win_rate,
                       profit_factor, total_return, max_drawdown, sharpe_ratio,
                       total_trades, win_trades, loss_trades, timestamp
                FROM backtest_results
            """
            params = []
            if min_accuracy is not None:
                query += " WHERE win_rate >= ?"
                params.append(min_accuracy)
            query += " ORDER BY win_rate DESC, profit_factor DESC, total_return DESC LIMIT 200"

            cursor.execute(query, params)
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to retrieve leaderboard: {e}")
        return []

def get_backtest_detail(result_id: int) -> Optional[Dict[str, Any]]:
    """Fetches full backtest record including equity curve and individual trade logs."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM backtest_results WHERE id = ?", (result_id,))
            row = cursor.fetchone()
            conn.close()
            if row:
                d = dict(row)
                d["equity_curve"] = json.loads(d["equity_curve_json"] or "[]")
                d["trade_log"] = json.loads(d["trade_log_json"] or "[]")
                d["params"] = json.loads(d["params_json"] or "{}")
                return d
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to load backtest detail #{result_id}: {e}")
    return None

def clear_backtest_results():
    """Clears previous backtest records."""
    with _db_lock:
        conn = get_connection()
        conn.execute("DELETE FROM backtest_results;")
        conn.commit()
        conn.close()

def save_mega_move(
    symbol: str,
    move_type: str,
    peak_gain_pct: float,
    start_price: float,
    peak_price: float,
    start_time: str,
    peak_time: str,
    duration_hours: float,
    vol_multiplier: float,
    pattern: str,
    pre_pump_data: list = None
) -> int:
    """Stores a mined 50%-500%+ mega-pump or dump event."""
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    pre_pump_json = json.dumps(pre_pump_data or [])
    with _db_lock:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO mega_moves (
                symbol, move_type, peak_gain_pct, start_price, peak_price,
                start_time, peak_time, duration_hours, vol_multiplier, pattern,
                pre_pump_data_json, timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            symbol, move_type, round(peak_gain_pct, 2), start_price, peak_price,
            start_time, peak_time, round(duration_hours, 1), round(vol_multiplier, 2), pattern,
            pre_pump_json, now_iso
        ))
        row_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return row_id

def get_mega_moves(min_gain: float = 40.0, move_type: Optional[str] = None, limit: Optional[int] = 150) -> List[Dict[str, Any]]:
    """Retrieves mined mega-moves sorted by peak percentage gain/loss."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            query = "SELECT id, symbol, move_type, peak_gain_pct, start_price, peak_price, start_time, peak_time, duration_hours, vol_multiplier, pattern, timestamp FROM mega_moves WHERE peak_gain_pct >= ?"
            params: List[Any] = [min_gain]
            if move_type:
                query += " AND move_type = ?"
                params.append(move_type)
            lim_val = int(limit) if limit and limit > 0 else 150
            query += f" ORDER BY peak_gain_pct DESC LIMIT {lim_val}"
            cursor.execute(query, params)
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to retrieve mega moves: {e}")
        return []

def get_mega_move_detail(move_id: int) -> Optional[Dict[str, Any]]:
    """Retrieves full mega move event with pre-pump candles."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM mega_moves WHERE id = ?", (move_id,))
            row = cursor.fetchone()
            conn.close()
            if row:
                d = dict(row)
                d["pre_pump_data"] = json.loads(d["pre_pump_data_json"] or "[]")
                return d
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to load mega move detail #{move_id}: {e}")
    return None

def clear_mega_moves():
    """Wipes mined mega moves cache."""
    with _db_lock:
        conn = get_connection()
        conn.execute("DELETE FROM mega_moves;")
        conn.commit()
        conn.close()

# =====================================================================
# LIVE BOLLINGER SQUEEZE BREAKOUT SIGNALS (10% TO 20% MOVES)
# =====================================================================

def save_live_breakout_signal(
    symbol: str,
    direction: str,
    entry_price: float,
    stop_loss: float,
    target_10pct: float,
    target_20pct: float,
    bandwidth: float,
    vol_surge: float,
    timeframe: str,
    signal_reason: str,
    strategy_name: str = "Bollinger Squeeze & Vol Surge",
    target_mega: float = 0.0,
    risk_reward_ratio: float = 3.0,
    expected_move_pct: float = 20.0,
    ai_confidence_pct: float = 90.0,
    is_mega_move: bool = False,
    target_rr_2: float = 0.0,
    target_rr_5: float = 0.0,
    target_rr_10: float = 0.0,
    target_rr_50: float = 0.0,
    target_rr_100: float = 0.0,
    target_rr_500: float = 0.0
) -> Optional[int]:
    """
    Saves a newly triggered real-time multi-strategy / mega-move signal.
    Prevents duplicate active signals for the same pair.
    """
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    with _db_lock:
        conn = get_connection()
        cursor = conn.cursor()
        
        # Check if active signal already exists for this symbol
        cursor.execute(
            "SELECT id FROM live_breakout_signals WHERE symbol = ? AND status = 'ACTIVE' LIMIT 1",
            (symbol,)
        )
        if cursor.fetchone():
            conn.close()
            return None  # Already tracking this active setup

        cursor.execute("""
            INSERT INTO live_breakout_signals (
                timestamp, symbol, direction, entry_price, current_price,
                stop_loss, target_10pct, target_20pct, target_mega, risk_reward_ratio,
                expected_move_pct, ai_confidence_pct, strategy_name, is_mega_move,
                max_favorable_pct, status, bandwidth, vol_surge, timeframe, signal_reason, updated_at,
                target_rr_2, target_rr_5, target_rr_10, target_rr_50, target_rr_100, target_rr_500, rr_hit_tier
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            now_iso, symbol, direction.upper(), round(entry_price, 6), round(entry_price, 6),
            round(stop_loss, 6), round(target_10pct, 6), round(target_20pct, 6),
            round(target_mega, 6), round(risk_reward_ratio, 1), round(expected_move_pct, 1),
            round(ai_confidence_pct, 1), strategy_name, 1 if is_mega_move else 0,
            0.0, "ACTIVE", round(bandwidth, 4), round(vol_surge, 2), timeframe, signal_reason, now_iso,
            round(target_rr_2, 6), round(target_rr_5, 6), round(target_rr_10, 6),
            round(target_rr_50, 6), round(target_rr_100, 6), round(target_rr_500, 6), "NONE"
        ))
        row_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return row_id

def get_active_breakout_signals() -> List[Dict[str, Any]]:
    """Returns all currently active breakout signals being monitored."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM live_breakout_signals WHERE status = 'ACTIVE' ORDER BY id DESC")
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to get active breakout signals: {e}")
        return []

def get_all_breakout_signals(
    limit: int = 150,
    direction: Optional[str] = None,
    status: Optional[str] = None,
    min_confidence: Optional[float] = None
) -> List[Dict[str, Any]]:
    """Fetches all logged breakout signals with optional filtering."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            query = "SELECT * FROM live_breakout_signals WHERE 1=1"
            params = []
            if direction and direction != "ALL":
                query += " AND direction = ?"
                params.append(direction.upper())
            if status and status != "ALL":
                query += " AND status = ?"
                params.append(status.upper())
            if min_confidence is not None:
                query += " AND ai_confidence_pct >= ?"
                params.append(min_confidence)
            query += " ORDER BY id DESC LIMIT ?"
            params.append(limit)
            cursor.execute(query, params)
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to get all breakout signals: {e}")
        return []

def update_breakout_signal(
    signal_id: int,
    current_price: float,
    max_favorable_pct: float,
    new_status: str,
    rr_hit_tier: str = "NONE"
):
    """Updates live mark price, peak favorable move %, status, and highest R:R tier hit."""
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    with _db_lock:
        conn = get_connection()
        if rr_hit_tier != "NONE":
            conn.execute("""
                UPDATE live_breakout_signals
                SET current_price = ?, max_favorable_pct = ?, status = ?, rr_hit_tier = ?, updated_at = ?
                WHERE id = ?
            """, (round(current_price, 6), round(max_favorable_pct, 2), new_status, rr_hit_tier, now_iso, signal_id))
        else:
            conn.execute("""
                UPDATE live_breakout_signals
                SET current_price = ?, max_favorable_pct = ?, status = ?, updated_at = ?
                WHERE id = ?
            """, (round(current_price, 6), round(max_favorable_pct, 2), new_status, now_iso, signal_id))
        conn.commit()
        conn.close()

def get_breakout_accuracy_stats() -> Dict[str, Any]:
    """Computes live accuracy, win rate, and performance metrics across all strategies."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals")
            total = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE status = 'ACTIVE'")
            active = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE status IN ('HIT_10PCT', 'HIT_20PCT', 'MEGA_HIT')")
            hits = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE status = 'HIT_20PCT'")
            hits_20 = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE status = 'MEGA_HIT' OR max_favorable_pct >= 50.0")
            mega_hits = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE status = 'STOPPED_OUT'")
            stopped = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE direction = 'LONG'")
            longs = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE direction = 'SHORT'")
            shorts = cursor.fetchone()[0]

            cursor.execute("SELECT COUNT(*) FROM live_breakout_signals WHERE ai_confidence_pct >= 90.0")
            high_conf = cursor.fetchone()[0]

            cursor.execute("SELECT AVG(max_favorable_pct) FROM live_breakout_signals")
            avg_gain = cursor.fetchone()[0] or 0.0

            cursor.execute("SELECT AVG(risk_reward_ratio) FROM live_breakout_signals")
            avg_rr = cursor.fetchone()[0] or 3.5

            conn.close()

            resolved = hits + stopped
            live_accuracy = (hits / resolved * 100) if resolved > 0 else 0.0

            return {
                "total_signals": total,
                "active_signals": active,
                "hit_10pct": hits,
                "hit_20pct": hits_20,
                "mega_hits": mega_hits,
                "stopped_out": stopped,
                "high_accuracy_count": high_conf,
                "live_accuracy_pct": round(live_accuracy, 1),
                "resolved_trades": resolved,
                "long_signals": longs,
                "short_signals": shorts,
                "avg_max_gain_pct": round(float(avg_gain), 2),
                "avg_risk_reward": round(float(avg_rr), 1)
            }
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to compute accuracy stats: {e}")
        return {
            "total_signals": 0, "active_signals": 0, "hit_10pct": 0,
            "hit_20pct": 0, "mega_hits": 0, "stopped_out": 0,
            "high_accuracy_count": 0, "live_accuracy_pct": 0.0,
            "resolved_trades": 0, "long_signals": 0, "short_signals": 0,
            "avg_max_gain_pct": 0.0, "avg_risk_reward": 3.0
        }

def clear_breakout_signals():
    """Clears all live breakout signal records."""
    with _db_lock:
        conn = get_connection()
        conn.execute("DELETE FROM live_breakout_signals;")
        conn.commit()
        conn.close()

def get_storage_stats() -> Dict[str, Any]:
    """Returns database size, record count, and memory buffer usage for live telemetry."""
    db_size_bytes = os.path.getsize(DB_PATH) if DB_PATH.exists() else 0
    db_size_mb = round(db_size_bytes / (1024 * 1024), 2)
    
    total_logs = 0
    total_bt = 0
    total_mega = 0
    total_signals = 0
    try:
        with _db_lock:
            conn = get_connection()
            cur = conn.cursor()
            cur.execute("SELECT COUNT(*) FROM system_logs")
            total_logs = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM backtest_results")
            total_bt = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM mega_moves")
            total_mega = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM live_breakout_signals")
            total_signals = cur.fetchone()[0]
            conn.close()
    except Exception:
        pass

    with _log_lock:
        ram_logs = len(_log_buffer)

    return {
        "db_size_mb": db_size_mb,
        "total_log_records": total_logs,
        "ram_buffer_logs": ram_logs,
        "backtest_records": total_bt,
        "mega_move_records": total_mega,
        "breakout_signals_count": total_signals
    }

# =====================================================================
# MANUAL TRADE ANALYSIS & ACTIVE POSITION RISK MANAGEMENT
# =====================================================================

def save_manual_trade_analysis(data: Dict[str, Any]) -> int:
    """Saves a user's manual trade analysis and AI exit/re-entry recommendations."""
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    with _db_lock:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO manual_analyzed_trades (
                timestamp, symbol, direction, entry_price, current_price,
                stop_loss, take_profit, position_size_usd, current_pnl_usd,
                current_pnl_pct, risk_status, exit_recommendation, exit_price,
                reentry_recommendation, reentry_price_min, reentry_price_max, analysis_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            now_iso,
            data.get("symbol", "").upper(),
            data.get("direction", "LONG").upper(),
            float(data.get("entry_price", 0.0)),
            float(data.get("current_price", 0.0)),
            float(data.get("stop_loss", 0.0)) if data.get("stop_loss") else None,
            float(data.get("take_profit", 0.0)) if data.get("take_profit") else None,
            float(data.get("position_size_usd", 1.0)),
            float(data.get("current_pnl_usd", 0.0)),
            float(data.get("current_pnl_pct", 0.0)),
            str(data.get("risk_status", "NORMAL")),
            str(data.get("exit_recommendation", "")),
            float(data.get("exit_price", 0.0)) if data.get("exit_price") else None,
            str(data.get("reentry_recommendation", "")),
            float(data.get("reentry_price_min", 0.0)) if data.get("reentry_price_min") else None,
            float(data.get("reentry_price_max", 0.0)) if data.get("reentry_price_max") else None,
            json.dumps(data)
        ))
        row_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return row_id

def get_manual_trade_analyses(limit: int = 50) -> List[Dict[str, Any]]:
    """Retrieves saved manual trade risk evaluations."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM manual_analyzed_trades ORDER BY id DESC LIMIT ?", (limit,))
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to get manual trade analyses: {e}")
        return []

def get_completed_batch_evaluations(limit: int = 20) -> List[Dict[str, Any]]:
    """Retrieves historical completed batch evaluations."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM batch_evaluations ORDER BY batch_number DESC LIMIT ?", (limit,))
            rows = cursor.fetchall()
            conn.close()
            results = []
            for r in rows:
                d = dict(r)
                if d.get("strategy_stats_json"):
                    try:
                        d["strategy_stats"] = json.loads(d["strategy_stats_json"])
                    except Exception:
                        d["strategy_stats"] = {}
                if d.get("rr_tier_stats_json"):
                    try:
                        d["rr_tier_stats"] = json.loads(d["rr_tier_stats_json"])
                    except Exception:
                        d["rr_tier_stats"] = {}
                results.append(d)
            return results
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to get batch evaluations: {e}")
        return []

def save_smart_alert(
    symbol: str,
    direction: str,
    alert_type: str,
    message: str,
    price: float,
    pnl_pct: float
) -> int:
    """Saves a smart reversal or early exit alert."""
    try:
        now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("""
                INSERT INTO smart_alerts (symbol, direction, alert_type, message, price, pnl_pct, timestamp, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            """, (symbol, direction, alert_type, message, float(price), float(pnl_pct), now_str))
            row_id = cursor.lastrowid
            conn.commit()
            conn.close()
            return row_id
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to save smart alert: {e}")
        return 0

def get_active_smart_alerts(limit: int = 15) -> List[Dict[str, Any]]:
    """Retrieves active unacknowledged smart reversal / early exit alerts."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM smart_alerts WHERE is_active = 1 ORDER BY id DESC LIMIT ?", (limit,))
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to get smart alerts: {e}")
        return []

def dismiss_smart_alert(alert_id: int):
    """Dismisses an active smart alert."""
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()
            cursor.execute("UPDATE smart_alerts SET is_active = 0 WHERE id = ?", (alert_id,))
            conn.commit()
            conn.close()
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to dismiss smart alert: {e}")

def search_historical_trades(
    query: str = "",
    outcome_filter: str = "ALL",
    strategy_filter: str = "ALL",
    sort_by: str = "NEWEST",
    limit: int = 150
) -> List[Dict[str, Any]]:
    """
    Historical Trade Search Engine:
    Searches, filters, and ranks past executed trades by pair, keyword, outcome, strategy, and date/PnL.
    """
    try:
        with _db_lock:
            conn = get_connection()
            cursor = conn.cursor()

            conditions = []
            params = []

            # Query filter (matches symbol, strategy, reason, or side)
            if query and query.strip():
                q = f"%{query.strip().upper()}%"
                conditions.append("(UPPER(symbol) LIKE ? OR UPPER(strategy) LIKE ? OR UPPER(reason) LIKE ? OR UPPER(side) LIKE ?)")
                params.extend([q, q, q, q])

            # Outcome filter
            if outcome_filter == "WINS":
                conditions.append("pnl > 0")
            elif outcome_filter == "LOSSES":
                conditions.append("pnl <= 0")
            elif outcome_filter == "EARLY_EXIT":
                conditions.append("(UPPER(reason) LIKE '%EARLY%' OR UPPER(reason) LIKE '%REVERSAL%' OR UPPER(reason) LIKE '%SECURE%')")

            # Strategy filter
            if strategy_filter and strategy_filter != "ALL":
                conditions.append("strategy = ?")
                params.append(strategy_filter)

            where_clause = ("WHERE " + " AND ".join(conditions)) if conditions else ""

            # Sorting
            if sort_by == "HIGHEST_PNL":
                order_clause = "ORDER BY pnl_pct DESC"
            elif sort_by == "LOWEST_PNL":
                order_clause = "ORDER BY pnl_pct ASC"
            elif sort_by == "LARGEST_DOLLAR":
                order_clause = "ORDER BY pnl DESC"
            elif sort_by == "OLDEST":
                order_clause = "ORDER BY id ASC"
            else:
                order_clause = "ORDER BY id DESC"

            sql = f"SELECT * FROM paper_trades {where_clause} {order_clause} LIMIT ?"
            params.append(limit)

            cursor.execute(sql, tuple(params))
            rows = cursor.fetchall()
            conn.close()
            return [dict(r) for r in rows]
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to search historical trades: {e}")
        return []

def get_system_setting(key: str, default: str = "") -> str:
    """Retrieves a persistent system configuration setting from SQLite."""
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT setting_value FROM system_settings WHERE setting_key = ?;", (key,))
        row = cursor.fetchone()
        conn.close()
        if row and row[0]:
            return str(row[0])
    except Exception as e:
        log_event("WARNING", "DB", f"Failed to get system setting '{key}': {e}")
    return default

def set_system_setting(key: str, value: str) -> bool:
    """Saves or dynamically updates a persistent system setting in SQLite."""
    try:
        conn = get_connection()
        cursor = conn.cursor()
        now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
        cursor.execute("""
            INSERT INTO system_settings (setting_key, setting_value, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(setting_key) DO UPDATE SET
                setting_value = excluded.setting_value,
                updated_at = excluded.updated_at;
        """, (key, value, now_iso))
        conn.commit()
        conn.close()
        log_event("INFO", "SECURITY", f"System setting '{key}' updated successfully.")
        return True
    except Exception as e:
        log_event("ERROR", "DB", f"Failed to set system setting '{key}': {e}")
        return False

def get_recent_ai_trade_diagnoses(limit: int = 15) -> List[Dict[str, Any]]:
    """Retrieves recent completed trades with AI SL/TP root cause analysis and self-correction logs."""
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM paper_trades ORDER BY id DESC LIMIT ?;", (limit,))
        rows = [dict(r) for r in cursor.fetchall()]
        conn.close()
        return rows
    except Exception as e:
        log_event("WARNING", "DB", f"Failed to get recent AI trade diagnoses: {e}")
        return []

# Auto-initialize database on import
init_db()
