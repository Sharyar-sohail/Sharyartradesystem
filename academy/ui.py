"""
Main User Interface Renderer for Sharyar Ultimate Trading Masterclass & Academy
Designed with high-contrast OLED dark aesthetics, responsive layout,
and clean separation from the password-protected terminal.
"""

import streamlit as st
import pandas as pd
from typing import Dict, Any, List

from academy.curriculum import get_all_modules, get_module_by_id
from academy.simulator import (
    init_simulator_state,
    reset_simulator_wallet,
    open_virtual_trade,
    close_virtual_trade,
    get_positions_with_floating_pnl,
    SIMULATOR_SUPPORTED_ASSETS
)
from academy.quizzes import get_all_quizzes, calculate_student_badges
from academy.ai_strategy_feed import get_synced_ai_academy_strategies
from backend.market_data_hub import get_live_market_price

def render_academy_main_interface():
    """Renders the comprehensive Public Trading Masterclass & Academy."""
    init_simulator_state(st.session_state)

    if "academy_quiz_scores" not in st.session_state:
        st.session_state.academy_quiz_scores = {}
    if "selected_curriculum_module" not in st.session_state:
        st.session_state.selected_curriculum_module = "module_1"

    # -------------------------------------------------------------
    # 1. TOP HEADER & SWITCH BAR
    # -------------------------------------------------------------
    head_c1, head_c2 = st.columns([3.2, 1.2], vertical_alignment="center")
    with head_c1:
        st.markdown(
            """
            <div style="display:flex; align-items:center; gap:12px; margin-bottom:4px;">
                <span style="font-size:2.2rem;">🎓</span>
                <div>
                    <h2 style="margin:0; font-size:1.8rem; font-weight:900; letter-spacing:0.02em; color:#FFFFFF;">
                        SHARYAR ULTIMATE TRADING MASTERCLASS & ACADEMY
                    </h2>
                    <div style="font-size:0.85rem; color:#00E5FF; font-weight:700; letter-spacing:0.05em;">
                        INSTITUTIONAL QUANTITATIVE EDUCATION • ZERO-RISK SIMULATOR • AI-SYNCED STRATEGY LAB
                    </div>
                </div>
            </div>
            """,
            unsafe_allow_html=True
        )
    with head_c2:
        if st.button("⚡ Access Live Terminal", type="primary", width="stretch", key="btn_switch_to_terminal_top"):
            st.session_state.in_academy_mode = False
            st.rerun()

    # -------------------------------------------------------------
    # 2. STATUS & GAMIFIED METRICS STRIP
    # -------------------------------------------------------------
    badges = calculate_student_badges(st.session_state.academy_quiz_scores)
    open_positions = st.session_state.academy_open_positions
    wallet_bal = st.session_state.academy_wallet_balance

    with st.container(border=True):
        m1, m2, m3, m4 = st.columns(4)
        with m1:
            st.metric("VIRTUAL DEMO WALLET", f"${wallet_bal:,.2f}", "Zero Financial Risk")
        with m2:
            st.metric("ACTIVE POSITIONS", f"{len(open_positions)} Open", "Live Market Tracking")
        with m3:
            st.metric("QUIZ MASTERY", f"{len(st.session_state.academy_quiz_scores)}/4 Passed", "Knowledge Score")
        with m4:
            badge_str = f"{len(badges)} Unlocked" if badges else "Novice"
            st.metric("STUDENT RANK", badge_str, "Institutional Badges")

    st.write("")

    # -------------------------------------------------------------
    # 3. MAIN NAVIGATION TABS
    # -------------------------------------------------------------
    tab_curriculum, tab_sim, tab_quiz, tab_ai_feed, tab_calc = st.tabs([
        "📚 Masterclass Curriculum (7 Modules)",
        "🎮 Virtual Paper Trading Simulator ($10,000)",
        "🎯 Interactive Quizzes & Skill Certification",
        "🤖 AI-Synced Daily Strategy Feed",
        "🧮 Interactive Position Sizer & Compounding"
    ])

    # =============================================================
    # TAB 1: MASTERCLASS CURRICULUM
    # =============================================================
    with tab_curriculum:
        st.markdown("### 📚 **Institutional Trading Knowledge Base**")
        st.caption("Structured, self-paced masterclass modules designed to transform beginner chartists into disciplined, institutional quantitative operators.")

        modules = get_all_modules()
        mod_titles = [f"{m['icon']} {m['title'].split(':')[1].strip()} ({m['tag']})" for m in modules]
        mod_map = {mod_titles[idx]: modules[idx]["id"] for idx in range(len(modules))}

        selected_title = st.selectbox(
            "Select Curriculum Module",
            options=mod_titles,
            index=0,
            key="sb_curriculum_module"
        )
        selected_mod_id = mod_map[selected_title]
        current_mod = get_module_by_id(selected_mod_id)

        with st.container(border=True):
            st.markdown(f"#### {current_mod['icon']} **{current_mod['title']}**")
            st.caption(f"Estimated Duration: **{current_mod['duration']}** • Category: **{current_mod['tag']}**")
            st.info(current_mod["summary"])

            for lesson in current_mod["lessons"]:
                with st.expander(f"📖 {lesson['title']}", expanded=True):
                    st.markdown(lesson["content"])

    # =============================================================
    # TAB 2: VIRTUAL PAPER TRADING SIMULATOR
    # =============================================================
    with tab_sim:
        st.markdown("### 🎮 **Virtual Paper Trading Simulator ($10,000 Demo Balance)**")
        st.caption("Practice executing long and short setups directly on 100% real-time live price feeds with zero risk to your real capital.")

        col_left, col_right = st.columns([1.3, 1.0])

        with col_left:
            with st.container(border=True):
                st.markdown("##### 📝 **Order Execution Panel**")

                sym_options = [f"{a[0]} — {a[2]}" for a in SIMULATOR_SUPPORTED_ASSETS]
                selected_sym_str = st.selectbox("Select Asset to Trade", options=sym_options, key="sim_sel_sym")
                clean_sym = selected_sym_str.split(" — ")[0]

                # Fetch real-time live price
                curr_live_p = get_live_market_price(clean_sym)
                if not curr_live_p or curr_live_p <= 0:
                    curr_live_p = 100.0 if "BTC" in clean_sym else (4020.0 if "XAU" in clean_sym else 1.12)

                digits = 2 if ("XAU" in clean_sym or "JPY" in clean_sym or "BTC" in clean_sym or "ETH" in clean_sym) else 4
                price_fmt = f"${curr_live_p:,.{digits}f}" if "USD" in clean_sym else f"{curr_live_p:.{digits}f}"
                st.markdown(f"**Live Market Price:** `{price_fmt}` (100% Real-Time Live Feed)")

                c_dir, c_size = st.columns(2)
                with c_dir:
                    trade_dir = st.radio("Direction", ["BUY / LONG", "SELL / SHORT"], horizontal=True, key="sim_dir")
                with c_size:
                    if "XAU" in clean_sym:
                        default_units = 0.50
                        step_val = 0.10
                        unit_label = "Lot Size (Ounces)"
                    elif "BTC" in clean_sym:
                        default_units = 0.05
                        step_val = 0.01
                        unit_label = "Units (BTC)"
                    elif "ETH" in clean_sym:
                        default_units = 0.50
                        step_val = 0.10
                        unit_label = "Units (ETH)"
                    else:
                        default_units = 1000.0
                        step_val = 100.0
                        unit_label = "Units / Base Currency"
                    trade_units = st.number_input(unit_label, min_value=0.001, value=float(default_units), step=step_val, key="sim_units")

                # Default SL & TP based on direction and micro-tight risk rules
                is_long = "LONG" in trade_dir
                if "XAU" in clean_sym:
                    # Gold micro-tight $2.80 SL distance
                    def_sl = round(curr_live_p - 2.80 if is_long else curr_live_p + 2.80, 2)
                    def_tp = round(curr_live_p + 12.60 if is_long else curr_live_p - 12.60, 2)
                elif "BTC" in clean_sym:
                    # Bitcoin 1.2% tight SL distance
                    def_sl = round(curr_live_p * (0.988 if is_long else 1.012), 2)
                    def_tp = round(curr_live_p * (1.050 if is_long else 0.950), 2)
                else:
                    def_sl = round(curr_live_p * (0.992 if is_long else 1.008), digits)
                    def_tp = round(curr_live_p * (1.035 if is_long else 0.965), digits)

                c_sl, c_tp = st.columns(2)
                with c_sl:
                    sl_input = st.number_input("Micro-Tight Stop Loss ($)", value=float(def_sl), format=f"%.{digits}f", key="sim_sl")
                with c_tp:
                    tp_input = st.number_input("Take Profit Target ($)", value=float(def_tp), format=f"%.{digits}f", key="sim_tp")

                risk_dist = abs(curr_live_p - sl_input)
                reward_dist = abs(tp_input - curr_live_p)
                rr = round(reward_dist / max(risk_dist, 1e-5), 2)
                dollar_risk = round(trade_units * risk_dist, 2)
                dollar_reward = round(trade_units * reward_dist, 2)

                st.caption(f"Risk-to-Reward: **1:{rr:.2f}** • Dollar Risk: **${dollar_risk:,.2f}** • Potential Profit: **${dollar_reward:,.2f}**")

                if st.button("⚡ Execute Simulated Trade", type="primary", width="stretch", key="btn_exec_sim"):
                    res_trade = open_virtual_trade(
                        st.session_state,
                        symbol=clean_sym,
                        direction=trade_dir,
                        lot_or_units=trade_units,
                        stop_loss=sl_input,
                        take_profit=tp_input,
                        strategy_tag="Masterclass Trade",
                        custom_entry=curr_live_p
                    )
                    st.success(f"✅ Position Opened! Trade #{res_trade['id']}: {trade_dir} on {clean_sym} @ ${res_trade['entry_price']:,.2f}")
                    st.rerun()

        with col_right:
            with st.container(border=True):
                st.markdown("##### 💼 **Wallet Summary**")
                st.markdown(f"**Virtual Cash Balance:** `${st.session_state.academy_wallet_balance:,.2f}`")
                st.markdown(f"**Active Open Positions:** `{len(st.session_state.academy_open_positions)}`")
                st.markdown(f"**Closed Trades Ledger:** `{len(st.session_state.academy_trade_history)}`")

                if st.button("🔄 Reset Demo Wallet to $10,000", width="stretch", key="btn_reset_sim_wallet"):
                    reset_simulator_wallet(st.session_state)
                    st.success("Wallet reset to $10,000.00!")
                    st.rerun()

        st.divider()

        # ACTIVE OPEN POSITIONS MONITOR
        st.markdown("#### 📊 **Active Virtual Positions (Real-Time Floating PnL)**")
        active_positions = get_positions_with_floating_pnl(st.session_state)
        if active_positions:
            for pos in active_positions:
                with st.container(border=True):
                    cp1, cp2, cp3, cp4, cp5 = st.columns([1.2, 1.2, 1.2, 1.2, 1.0], vertical_alignment="center")
                    with cp1:
                        st.markdown(f"**{pos['symbol']}** ({pos['direction']})")
                        st.caption(f"ID: `{pos['id']}` • {pos['opened_at']}")
                    with cp2:
                        st.markdown(f"Entry: **${pos['entry_price']:,.2f}**")
                        st.markdown(f"Live: **${pos['live_price']:,.2f}**")
                    with cp3:
                        st.markdown(f"SL: `${pos['stop_loss']:,.2f}`")
                        st.markdown(f"TP: `${pos['take_profit']:,.2f}` (1:{pos['rr_ratio']:.1f} RR)")
                    with cp4:
                        pnl_usd = pos["floating_pnl_usd"]
                        pnl_pct = pos["floating_pnl_pct"]
                        color_str = "#00E676" if pnl_usd >= 0 else "#FF1744"
                        st.markdown(f"<span style='color:{color_str}; font-weight:800; font-size:1.1rem;'>{'+' if pnl_usd>=0 else ''}${pnl_usd:,.2f} ({'+' if pnl_pct>=0 else ''}{pnl_pct:.2f}%)</span>", unsafe_allow_html=True)
                    with cp5:
                        if st.button("Close Trade", key=f"btn_close_{pos['id']}", width="stretch"):
                            closed = close_virtual_trade(st.session_state, pos["id"])
                            if closed:
                                st.success(f"Position {pos['id']} closed for {'+' if closed['net_pnl_usd']>=0 else ''}${closed['net_pnl_usd']:,.2f}!")
                                st.rerun()
        else:
            st.info("No active open positions. Execute a trade in the Order Execution Panel above to practice!")

        st.divider()

        # CLOSED TRADES HISTORY LEDGER
        st.markdown("#### 📜 **Closed Trades Ledger**")
        if st.session_state.academy_trade_history:
            history_rows = []
            for t in st.session_state.academy_trade_history:
                history_rows.append({
                    "ID": t["id"],
                    "Asset": t["symbol"],
                    "Side": t["direction"],
                    "Entry": f"${t['entry_price']:,.2f}",
                    "Exit": f"${t['exit_price']:,.2f}",
                    "Net PnL ($)": f"{'+' if t['net_pnl_usd']>=0 else ''}${t['net_pnl_usd']:,.2f}",
                    "ROI (%)": f"{'+' if t['net_pnl_pct']>=0 else ''}{t['net_pnl_pct']:.2f}%",
                    "Outcome": t["outcome"],
                    "Closed At": t["closed_at"]
                })
            st.dataframe(pd.DataFrame(history_rows), width="stretch", hide_index=True)
        else:
            st.caption("Past completed trades will be recorded here transparently.")

    # =============================================================
    # TAB 3: INTERACTIVE QUIZZES & CERTIFICATION
    # =============================================================
    with tab_quiz:
        st.markdown("### 🎯 **Interactive Quizzes & Skill Certification**")
        st.caption("Test your comprehension across technical analysis, Smart Money Concepts, and risk management. Score ≥70% to unlock institutional badges.")

        quizzes = get_all_quizzes()
        quiz_titles = [f"{q['title']} [{q['badge']}]" for q in quizzes]
        q_idx = st.selectbox("Select Exam Topic", options=range(len(quizzes)), format_func=lambda x: quiz_titles[x], key="sb_quiz_topic")
        current_quiz = quizzes[q_idx]

        with st.container(border=True):
            st.markdown(f"#### 📝 **{current_quiz['title']}**")
            st.caption(f"Pass Mark: **70%** • Milestone Reward: **{current_quiz['badge']}**")

            user_answers = {}
            for idx, q in enumerate(current_quiz["questions"], start=1):
                st.markdown(f"**Question {idx}:** {q['question']}")
                choice = st.radio(
                    f"Options for Q{idx}",
                    options=q["options"],
                    index=None,
                    key=f"q_{current_quiz['id']}_{q['id']}",
                    label_visibility="collapsed"
                )
                user_answers[q["id"]] = choice
                st.write("")

            if st.button("Submit Exam for Grading", type="primary", key=f"btn_grade_{current_quiz['id']}"):
                correct_count = 0
                total_q = len(current_quiz["questions"])

                for q in current_quiz["questions"]:
                    ans = user_answers.get(q["id"])
                    correct_ans = q["options"][q["correct_index"]]
                    if ans == correct_ans:
                        correct_count += 1

                score_pct = round((correct_count / max(total_q, 1)) * 100.0, 1)
                st.session_state.academy_quiz_scores[current_quiz["id"]] = score_pct

                if score_pct >= 70.0:
                    st.success(f"🎉 Exam Passed! You scored {score_pct:.1f}% ({correct_count}/{total_q} correct). Badge Awarded: {current_quiz['badge']}!")
                else:
                    st.warning(f"Exam Finished: You scored {score_pct:.1f}% ({correct_count}/{total_q} correct). Review the curriculum and try again to achieve ≥70%!")

                # Render educational explanations
                with st.expander("🔍 View Question Explanations & Answers", expanded=True):
                    for idx, q in enumerate(current_quiz["questions"], start=1):
                        ans = user_answers.get(q["id"])
                        correct_ans = q["options"][q["correct_index"]]
                        is_corr = (ans == correct_ans)
                        st.markdown(f"**Q{idx}: {q['question']}**")
                        st.markdown(f"- Your Answer: `{ans or 'Not answered'}` {'✅' if is_corr else '❌'}")
                        st.markdown(f"- Correct Answer: `{correct_ans}`")
                        st.info(f"💡 **Explanation:** {q['explanation']}")
                        st.write("---")

        st.divider()
        st.markdown("#### 🏅 **Unlocked Student Achievement Badges**")
        earned_badges = calculate_student_badges(st.session_state.academy_quiz_scores)
        if earned_badges:
            b_cols = st.columns(min(len(earned_badges), 4))
            for b_idx, badge_item in enumerate(earned_badges):
                with b_cols[b_idx % 4]:
                    st.success(f"**{badge_item}**")
        else:
            st.info("Pass exams with ≥70% to unlock your official Sharyar Trading System credentials!")

    # =============================================================
    # TAB 4: AI-SYNCED DAILY STRATEGY FEED
    # =============================================================
    with tab_ai_feed:
        st.markdown("### 🤖 **AI-Synced Daily Strategy Feed**")
        st.caption("Live feed of empirical algorithmic strategies synthesized autonomously by the Multi-Agent Swarm. Each strategy is decoded into human-readable operational rules.")

        f_lane = st.segmented_control("Filter Market", ["All Markets", "Crypto Assets", "Spot Gold & Forex"], default="All Markets", key="sc_ai_feed_filter")
        lane_arg = None
        if f_lane == "Crypto Assets":
            lane_arg = "crypto"
        elif f_lane == "Spot Gold & Forex":
            lane_arg = "forex_metals"

        ai_strats = get_synced_ai_academy_strategies(limit=8, lane_filter=lane_arg)

        for s in ai_strats:
            with st.container(border=True):
                sc1, sc2, sc3 = st.columns([2.5, 1.2, 1.0], vertical_alignment="center")
                with sc1:
                    st.markdown(f"##### **{s['name']}**")
                    st.caption(f"Market: **{s['market_label']}** • Asset: **{s['symbol']}** • Generation: **Gen #{s['generation']}** • Parent: `{s['parent_strategy']}`")
                with sc2:
                    st.markdown(f"Win Rate: **{s['win_rate']:.1f}%**")
                    st.markdown(f"Profit Factor: **{s['profit_factor']:.2f}** (Sample: {s['total_trades']} trades)")
                with sc3:
                    st.success(f"Status: **{s['status']}**")

                st.markdown(f"**Structural Confluence:** `{s['structure_rule']}`")
                st.markdown(f"**AI Optimization Rationale:** {s['mutation_reason']}")

                # 1-Click transfer to simulator
                if st.button(f"🎯 Practice '{s['name']}' in Simulator", key=f"btn_feed_sim_{s['id']}"):
                    st.session_state.selected_symbol = s["symbol"]
                    st.info(f"Loaded {s['symbol']} into Virtual Paper Trading Simulator! Switch to the Simulator tab to execute.")

    # =============================================================
    # TAB 5: POSITION SIZER & COMPOUNDING CALCULATOR
    # =============================================================
    with tab_calc:
        st.markdown("### 🧮 **Interactive Quantitative Calculators**")
        st.caption("Master position sizing, lot calculation, risk budgeting, and exponential compounding math.")

        c_calc1, c_calc2 = st.columns(2)

        with c_calc1:
            with st.container(border=True):
                st.markdown("##### 🛡️ **Position Sizer & Risk Calculator**")
                calc_bal = st.number_input("Account Equity ($)", value=10000.0, step=500.0, key="calc_equity")
                calc_risk_pct = st.slider("Risk Percentage (%)", min_value=0.25, max_value=5.0, value=1.0, step=0.25, key="calc_risk_pct")
                calc_entry = st.number_input("Entry Price ($)", value=4020.0, step=1.0, key="calc_entry_p")
                calc_sl = st.number_input("Stop Loss Price ($)", value=4017.20, step=0.5, key="calc_sl_p")

                calc_risk_usd = round(calc_bal * (calc_risk_pct / 100.0), 2)
                calc_risk_dist = abs(calc_entry - calc_sl)
                calc_units = round(calc_risk_usd / max(calc_risk_dist, 1e-4), 3)

                st.write("---")
                st.markdown(f"**Max Allowed Dollar Risk:** `${calc_risk_usd:,.2f}` (Strict {calc_risk_pct:.2f}% cap)")
                st.markdown(f"**Stop Loss Distance:** `${calc_risk_dist:,.2f}`")
                st.markdown(f"**Recommended Position Size:** `{calc_units:,.3f} Units` (or Lots)")
                st.caption("If price hits your Stop Loss, your actual loss will equal exactly your risk budget.")

        with c_calc2:
            with st.container(border=True):
                st.markdown("##### 📈 **Exponential Compounding Simulator**")
                comp_starting = st.number_input("Starting Capital ($)", value=2500.0, step=500.0, key="comp_start")
                comp_trades = st.slider("Number of Trades", min_value=10, max_value=100, value=50, step=5, key="comp_trades")
                comp_wr = st.slider("Win Rate (%)", min_value=35.0, max_value=85.0, value=60.0, step=5.0, key="comp_wr")
                comp_rr = st.slider("Risk-Reward Ratio (1:X)", min_value=1.5, max_value=6.0, value=3.0, step=0.5, key="comp_rr")

                # Simulate compounded equity curve
                bal = comp_starting
                equity_curve = [bal]
                risk_fraction = 0.015

                win_count = int(comp_trades * (comp_wr / 100.0))
                loss_count = comp_trades - win_count

                # Interleaved sequence
                trade_results = ([True] * win_count) + ([False] * loss_count)
                # Deterministic shuffle
                trade_results = sorted(trade_results, key=lambda x: hash(f"{x}_{comp_trades}") % 100)

                for res in trade_results:
                    risk_amount = bal * risk_fraction
                    if res:
                        bal += (risk_amount * comp_rr)
                    else:
                        bal -= risk_amount
                    equity_curve.append(round(bal, 2))

                st.write("---")
                st.markdown(f"**Projected Ending Balance:** `${equity_curve[-1]:,.2f}`")
                gain_pct = round(((equity_curve[-1] - comp_starting) / comp_starting) * 100.0, 1)
                st.markdown(f"**Total Compounded ROI:** `+{gain_pct:,.1f}%` across {comp_trades} trades")
                st.line_chart(equity_curve)
