"""
Sharyar Ultimate Trading Masterclass & Quantitative Academy
Modular package initialization.
"""

from academy.curriculum import get_all_modules, get_module_by_id, CURRICULUM_MODULES
from academy.simulator import (
    init_simulator_state,
    reset_simulator_wallet,
    open_virtual_trade,
    close_virtual_trade,
    get_positions_with_floating_pnl,
    SIMULATOR_SUPPORTED_ASSETS
)
from academy.quizzes import get_all_quizzes, calculate_student_badges, QUIZ_TOPICS
from academy.ai_strategy_feed import get_synced_ai_academy_strategies
from academy.ui import render_academy_main_interface

__all__ = [
    "get_all_modules",
    "get_module_by_id",
    "CURRICULUM_MODULES",
    "init_simulator_state",
    "reset_simulator_wallet",
    "open_virtual_trade",
    "close_virtual_trade",
    "get_positions_with_floating_pnl",
    "SIMULATOR_SUPPORTED_ASSETS",
    "get_all_quizzes",
    "calculate_student_badges",
    "QUIZ_TOPICS",
    "get_synced_ai_academy_strategies",
    "render_academy_main_interface"
]
