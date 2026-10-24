"""
AI & Machine Learning Strategy Optimizer (Fine-Tuned for Mega-Pumps)
Uses XGBoost, Random Forest, and Advanced Feature Engineering to cross-analyze
winning pre-pump patterns (50% to 500% moves) and output the Master Mega-Pump Strategy.
"""

import math
import pandas as pd
import numpy as np
from typing import Dict, Any, Tuple, List, Optional
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, HistGradientBoostingClassifier, VotingClassifier
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.inspection import permutation_importance
from sklearn.metrics import accuracy_score, precision_score, recall_score, roc_auc_score, confusion_matrix

try:
    import xgboost as xgb
    XGB_AVAILABLE = True
except ImportError:
    XGB_AVAILABLE = False

from backend.strategies import (
    compute_atr,
    compute_rsi,
    compute_macd,
    compute_bollinger_bands,
    compute_ema
)
from backend.db import log_event

def extract_ml_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Extracts deep quantitative, volume anomaly, and structural price-action features.
    """
    data = df.copy()

    # 1. Price Momentum & Returns
    data["ret_1"] = data["close"].pct_change(1)
    data["ret_3"] = data["close"].pct_change(3)
    data["ret_5"] = data["close"].pct_change(5)
    data["ret_10"] = data["close"].pct_change(10)
    data["ret_20"] = data["close"].pct_change(20)

    # 2. Oscillators & Indicators
    data["rsi_14"] = compute_rsi(data["close"], 14)
    data["rsi_7"] = compute_rsi(data["close"], 7)
    data["rsi_slope_3"] = data["rsi_14"] - data["rsi_14"].shift(3)
    
    macd_line, signal_line, hist = compute_macd(data["close"])
    data["macd_hist"] = hist
    data["macd_diff"] = macd_line - signal_line

    upper, mid, lower, bw = compute_bollinger_bands(data["close"], 20, 2.0)
    data["bb_pct_b"] = (data["close"] - lower) / (upper - lower + 1e-10)
    data["bb_bandwidth"] = bw

    # 3. Volatility & Base Compression
    atr = compute_atr(data, 14)
    data["atr_pct"] = atr / (data["close"] + 1e-10)

    rolling_high15 = data["high"].shift(1).rolling(15).max()
    rolling_low15 = data["low"].shift(1).rolling(15).min()
    data["compression_ratio"] = (rolling_high15 - rolling_low15) / (rolling_low15 + 1e-10)

    # 4. Volume Dynamics & Anomalies
    vol_sma20 = data["volume"].rolling(20).mean()
    data["vol_surge"] = data["volume"] / (vol_sma20 + 1e-10)
    data["vol_surge_max3"] = data["vol_surge"].rolling(3).max()

    # 5. Candlestick Geometry
    candle_range = data["high"] - data["low"] + 1e-10
    body_range = (data["close"] - data["open"]).abs()
    data["body_ratio"] = body_range / candle_range
    data["upper_wick_ratio"] = (data["high"] - data[["open", "close"]].max(axis=1)) / candle_range
    data["lower_wick_ratio"] = (data[["open", "close"]].min(axis=1) - data["low"]) / candle_range
    data["is_bullish_bar"] = (data["close"] > data["open"]).astype(int)

    # 6. Smart Money Concepts Footprints
    # FVG displacement flag
    data["fvg_displacement"] = ((data["low"] > data["high"].shift(2)) & (data["close"] > data["open"])).astype(int)
    # Break of Structure (BOS)
    data["bos_breakout"] = (data["close"] > rolling_high15).astype(int)

    # 7. Moving Average Alignments
    ema9 = compute_ema(data["close"], 9)
    ema21 = compute_ema(data["close"], 21)
    ema55 = compute_ema(data["close"], 55)
    data["ema9_ema21_diff"] = (ema9 - ema21) / (data["close"] + 1e-10)
    data["ema21_ema55_diff"] = (ema21 - ema55) / (data["close"] + 1e-10)

    return data

def generate_forward_labels(
    df: pd.DataFrame,
    forward_bars: int = 18,
    profit_target_pct: float = 30.0,
    stop_loss_pct: float = 6.0
) -> pd.Series:
    """
    Creates target labels using the Triple Barrier Method:
    Label = 1 if price reaches +profit_target_pct before dropping -stop_loss_pct within forward_bars.
    Label = 0 otherwise.
    """
    n = len(df)
    labels = np.zeros(n, dtype=int)
    highs = df["high"].values
    lows = df["low"].values
    closes = df["close"].values

    for i in range(n - forward_bars):
        entry_price = closes[i]
        tp_price = entry_price * (1.0 + (profit_target_pct / 100.0))
        sl_price = entry_price * (1.0 - (stop_loss_pct / 100.0))

        hit_tp = False
        hit_sl = False

        for f in range(1, forward_bars + 1):
            bar_high = highs[i + f]
            bar_low = lows[i + f]

            if bar_high >= tp_price:
                hit_tp = True
                break
            if bar_low <= sl_price:
                hit_sl = True
                break

        if hit_tp and not hit_sl:
            labels[i] = 1

    return pd.Series(labels, index=df.index)

class StrategyOptimizer:
    """Trains and manages ML models for Mega-Pump detection and win-rate elevation."""

    FEATURE_COLS = [
        "ret_1", "ret_3", "ret_5", "ret_10", "ret_20",
        "rsi_14", "rsi_slope_3", "macd_hist", "macd_diff",
        "bb_pct_b", "bb_bandwidth", "atr_pct", "compression_ratio",
        "vol_surge", "vol_surge_max3", "body_ratio", "upper_wick_ratio",
        "lower_wick_ratio", "is_bullish_bar", "fvg_displacement",
        "bos_breakout", "ema9_ema21_diff", "ema21_ema55_diff"
    ]

    def __init__(self, model_type: str = "xgboost"):
        normalized = model_type.lower().replace(" ", "_").replace("-", "_")
        if normalized in ["xgboost", "xgb"]:
            self.model_type = "xgboost" if XGB_AVAILABLE else "gradient_boosting"
        elif normalized in ["random_forest", "rf"]:
            self.model_type = "random_forest"
        elif normalized in ["gradient_boosting", "gbm", "hist_gradient_boosting"]:
            self.model_type = "gradient_boosting"
        elif normalized in ["neural_net", "mlp", "deep_learning"]:
            self.model_type = "neural_net"
        elif normalized in ["ensemble", "consensus", "voting"]:
            self.model_type = "ensemble"
        else:
            self.model_type = "random_forest"

        self.model = None
        self.feature_importance: Dict[str, float] = {}
        self.metrics: Dict[str, Any] = {}
        self.is_trained = False
        self.scaler = None

    def train(
        self,
        df: pd.DataFrame,
        forward_bars: int = 18,
        profit_target_pct: float = 25.0,
        stop_loss_pct: float = 6.0,
        test_size: float = 0.25,
        strategy_weights: Optional[Dict[str, float]] = None
    ) -> Dict[str, Any]:
        """
        Extracts features, labels mega-pump trades, trains ML model, and returns performance metrics.
        Applies multi-strategy dynamic feature weighting matrix if provided.
        """
        featured_df = extract_ml_features(df)
        labels = generate_forward_labels(
            featured_df,
            forward_bars=forward_bars,
            profit_target_pct=profit_target_pct,
            stop_loss_pct=stop_loss_pct
        )

        dataset = featured_df[self.FEATURE_COLS].copy()

        # Multi-strategy dynamic weighting matrix
        if strategy_weights:
            # Map strategy multipliers to feature subsets
            if "smc_ob" in strategy_weights:
                dataset["fvg_displacement"] *= float(strategy_weights["smc_ob"])
                dataset["bos_breakout"] *= float(strategy_weights["smc_ob"])
            if "volume_surge" in strategy_weights:
                dataset["vol_surge"] *= float(strategy_weights["volume_surge"])
                dataset["vol_surge_max3"] *= float(strategy_weights["volume_surge"])
            if "bollinger_coil" in strategy_weights:
                dataset["bb_bandwidth"] *= float(strategy_weights["bollinger_coil"])
                dataset["compression_ratio"] *= float(strategy_weights["bollinger_coil"])
            if "rsi_divergence" in strategy_weights:
                dataset["rsi_slope_3"] *= float(strategy_weights["rsi_divergence"])
                dataset["macd_diff"] *= float(strategy_weights["rsi_divergence"])

        dataset["target"] = labels
        dataset = dataset.dropna()

        if len(dataset) < 60:
            raise ValueError(f"Insufficient historical data ({len(dataset)} rows) to train ML model.")

        X = dataset[self.FEATURE_COLS]
        y = dataset["target"]

        # Chronological train-test split (avoid lookahead bias)
        split_idx = int(len(X) * (1.0 - test_size))
        X_train, X_test = X.iloc[:split_idx], X.iloc[split_idx:]
        y_train, y_test = y.iloc[:split_idx], y.iloc[split_idx:]

        # Handle class balance
        pos_ratio = max(float(y_train.mean()), 0.01)
        scale_pos_weight = min((1.0 - pos_ratio) / pos_ratio, 10.0)

        # Initialize Model based on model_type
        if self.model_type == "xgboost" and XGB_AVAILABLE:
            self.model = xgb.XGBClassifier(
                n_estimators=150,
                max_depth=4,
                learning_rate=0.03,
                scale_pos_weight=scale_pos_weight,
                subsample=0.85,
                colsample_bytree=0.85,
                eval_metric="logloss",
                random_state=42
            )
            self.model.fit(X_train, y_train)

        elif self.model_type == "gradient_boosting":
            self.model = HistGradientBoostingClassifier(
                max_iter=150,
                max_depth=5,
                learning_rate=0.03,
                class_weight="balanced",
                random_state=42
            )
            self.model.fit(X_train, y_train)

        elif self.model_type == "neural_net":
            self.scaler = StandardScaler()
            X_train_scaled = self.scaler.fit_transform(X_train)
            X_test_scaled = self.scaler.transform(X_test)
            self.model = MLPClassifier(
                hidden_layer_sizes=(64, 32),
                activation="relu",
                solver="adam",
                max_iter=300,
                alpha=0.001,
                random_state=42
            )
            self.model.fit(X_train_scaled, y_train)

        elif self.model_type == "ensemble":
            rf = RandomForestClassifier(n_estimators=100, max_depth=5, class_weight="balanced", random_state=42, n_jobs=-1)
            gbm = HistGradientBoostingClassifier(max_iter=100, max_depth=5, learning_rate=0.03, class_weight="balanced", random_state=42)
            mlp_pipe = Pipeline([
                ("scaler", StandardScaler()),
                ("mlp", MLPClassifier(hidden_layer_sizes=(32, 16), max_iter=250, random_state=42))
            ])
            self.model = VotingClassifier(
                estimators=[("rf", rf), ("gbm", gbm), ("mlp", mlp_pipe)],
                voting="soft",
                n_jobs=-1
            )
            self.model.fit(X_train, y_train)

        else: # default random_forest
            self.model = RandomForestClassifier(
                n_estimators=150,
                max_depth=5,
                class_weight="balanced",
                random_state=42,
                n_jobs=-1
            )
            self.model.fit(X_train, y_train)

        # Predict & Evaluate
        if self.model_type == "neural_net":
            X_eval = X_test_scaled
        else:
            X_eval = X_test

        y_pred = self.model.predict(X_eval)
        y_prob = self.model.predict_proba(X_eval)[:, 1] if hasattr(self.model, "predict_proba") else y_pred

        accuracy = float(accuracy_score(y_test, y_pred))
        precision = float(precision_score(y_test, y_pred, zero_division=0))
        recall = float(recall_score(y_test, y_pred, zero_division=0))
        try:
            roc_auc = float(roc_auc_score(y_test, y_prob))
        except Exception:
            roc_auc = 0.50

        cm = confusion_matrix(y_test, y_pred).tolist()

        # Feature Importance Calculation
        if hasattr(self.model, "feature_importances_"):
            importances = self.model.feature_importances_
            feat_imp = {feat: float(round(imp, 4)) for feat, imp in zip(self.FEATURE_COLS, importances)}
            self.feature_importance = dict(sorted(feat_imp.items(), key=lambda item: item[1], reverse=True))
        else:
            try:
                perm = permutation_importance(self.model, X_eval, y_test, n_repeats=5, random_state=42, scoring="roc_auc")
                importances = perm.importances_mean
                # Normalize
                imp_sum = sum(max(0, x) for x in importances) or 1.0
                feat_imp = {feat: float(round(max(0, imp) / imp_sum, 4)) for feat, imp in zip(self.FEATURE_COLS, importances)}
                self.feature_importance = dict(sorted(feat_imp.items(), key=lambda item: item[1], reverse=True))
            except Exception:
                # Fallback even feature importance
                self.feature_importance = {feat: round(1.0 / len(self.FEATURE_COLS), 4) for feat in self.FEATURE_COLS}

        self.metrics = {
            "model_type": self.model_type.upper(),
            "accuracy": round(accuracy * 100, 1),
            "precision": round(precision * 100, 1),
            "recall": round(recall * 100, 1),
            "roc_auc": round(roc_auc, 3),
            "train_samples": len(X_train),
            "test_samples": len(X_test),
            "positive_events": int(y.sum()),
            "confusion_matrix": cm,
            "feature_importance": self.feature_importance
        }
        self.is_trained = True
        log_event("SUCCESS", "ML_ENGINE", f"Trained {self.model_type.upper()} Mega-Pump model. ROC-AUC: {roc_auc:.3f}, Accuracy: {accuracy*100:.1f}%")
        return self.metrics

    def predict_signals(self, df: pd.DataFrame, probability_threshold: float = 0.70) -> Tuple[pd.Series, pd.Series]:
        """
        Generates ML win-probability confidence scores and triggers buy signals when confidence >= threshold.
        """
        if not self.is_trained or self.model is None:
            return pd.Series(0, index=df.index), pd.Series(0.0, index=df.index)

        featured_df = extract_ml_features(df)
        X = featured_df[self.FEATURE_COLS].fillna(0)
        if self.model_type == "neural_net" and self.scaler is not None:
            X_eval = self.scaler.transform(X)
        else:
            X_eval = X

        probs = self.model.predict_proba(X_eval)[:, 1]

        signals = pd.Series(0, index=df.index)
        signals[probs >= probability_threshold] = 1
        probs = self.model.predict_proba(X)[:, 1]

        signals = pd.Series(0, index=df.index)
        signals[probs >= probability_threshold] = 1
        return signals, pd.Series(probs, index=df.index)

# Singleton Optimizer Instance
strategy_optimizer = StrategyOptimizer()
