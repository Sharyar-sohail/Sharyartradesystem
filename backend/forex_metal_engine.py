"""
Forex & Metals Engine - Modular Interface
Re-exports the core ForexEngine implementation from backend.forex_engine
for complete backward compatibility across all modules and tests.
"""

from backend.forex_engine import (
    ForexEngine,
    forex_engine,
    ForexMetalEngine,
    forex_metal_engine,
    FOREX_MAJOR_PAIRS,
    YAHOO_FX_MAP,
    SYNTHETIC_FX_PRICES,
    _fetch_yahoo_direct_rest
)

__all__ = [
    "ForexEngine",
    "forex_engine",
    "ForexMetalEngine",
    "forex_metal_engine",
    "FOREX_MAJOR_PAIRS",
    "YAHOO_FX_MAP",
    "SYNTHETIC_FX_PRICES",
    "_fetch_yahoo_direct_rest"
]
