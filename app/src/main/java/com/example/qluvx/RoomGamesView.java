package com.example.qluvx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Random;

public class RoomGamesView extends LinearLayout {

    public interface GameSyncBridge {
        boolean sendGameActionOverAgora(JSONObject actionJson);
        boolean isAgoraChannelConnected();
        void onGameLogEvent(String message);
    }

    public enum GameType {
        LUDO,
        CARROM,
        POOL_8BALL
    }

    private final GameSyncBridge syncBridge;
    private GameType activeGame = GameType.LUDO;
    private boolean ludoFourPlayerMode = true; // true = 4P, false = 2P

    private TextView syncStatusBanner;
    private TextView turnStatusView;
    private TextView primaryActionBtn;
    private TextView modeToggleBtn;
    private GameBoardCanvas boardCanvas;

    // --- Ludo State ---
    private final int[][] ludoTokens = new int[4][4]; // [player 0..3][token 0..3], 0 = base, 1..56 = track, 57 = home
    private final int[] ludoScores = new int[4];
    private int ludoTurn = 0;
    private int lastDiceValue = 0;
    private boolean waitingForTokenMove = false;
    private int moveSeq = 0;
    private final Random random = new Random();

    // --- Carrom State ---
    private static class Disc {
        float x, y, vx, vy, radius;
        int color;
        int points;
        boolean pocketed;
        String label;
    }

    private final Disc[] carromDiscs = new Disc[10]; // 0 = Striker, 1 = Queen, 2..5 = White, 6..9 = Black
    private final int[] carromScores = new int[2];
    private int carromTurn = 0;
    private float aimX = 0.5f, aimY = 0.25f;
    private boolean carromMoving = false;

    // --- 8 Ball Pool State ---
    private final Disc[] poolBalls = new Disc[10]; // 0 = Cue Ball, 1..4 = Solids, 5..8 = Stripes, 9 = 8-Ball
    private final int[] poolScores = new int[2];
    private int poolTurn = 0;
    private float poolAimX = 0.72f, poolAimY = 0.5f;
    private boolean poolMoving = false;

    public RoomGamesView(Context context, GameSyncBridge syncBridge) {
        super(context);
        this.syncBridge = syncBridge;
        setOrientation(VERTICAL);
        int pad = UiStyleHelper.dp(context, 10);
        setPadding(pad, pad, pad, pad);
        setBackground(UiStyleHelper.roundedBox(context, Color.rgb(14, 14, 26), 16f, UiStyleHelper.COLOR_ELECTRIC_VIOLET, 1.2f));

        resetLudoState();
        resetCarromState();
        resetPoolState();
        buildLayout(context);
    }

    private void buildLayout(Context context) {
        // 1. Game Selector Tabs (Ludo | Carrom | 8 Ball Pool)
        LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(HORIZONTAL);
        tabsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView ludoTab = UiStyleHelper.createPillButton(context, "🎲 Ludo", UiStyleHelper.COLOR_NEON_PINK, Color.WHITE, v -> switchGame(GameType.LUDO));
        TextView carromTab = UiStyleHelper.createPillButton(context, "🎯 Carrom", UiStyleHelper.COLOR_CARD_ELEVATED, Color.WHITE, v -> switchGame(GameType.CARROM));
        TextView poolTab = UiStyleHelper.createPillButton(context, "🎱 8 Ball Pool", UiStyleHelper.COLOR_CARD_ELEVATED, Color.WHITE, v -> switchGame(GameType.POOL_8BALL));

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tabLp.setMargins(UiStyleHelper.dp(context, 3), 0, UiStyleHelper.dp(context, 3), 0);
        tabsRow.addView(ludoTab, tabLp);
        tabsRow.addView(carromTab, tabLp);
        tabsRow.addView(poolTab, tabLp);
        addView(tabsRow);

        // 2. Sync & Validation Status Banner
        syncStatusBanner = new TextView(context);
        syncStatusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        syncStatusBanner.setPadding(0, UiStyleHelper.dp(context, 4), 0, UiStyleHelper.dp(context, 2));
        addView(syncStatusBanner);

        // 3. Turn & Score Indicator
        turnStatusView = new TextView(context);
        turnStatusView.setTextColor(Color.WHITE);
        turnStatusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        turnStatusView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        turnStatusView.setPadding(0, UiStyleHelper.dp(context, 2), 0, UiStyleHelper.dp(context, 6));
        addView(turnStatusView);

        // 4. Interactive Game Board Canvas
        boardCanvas = new GameBoardCanvas(context);
        LinearLayout.LayoutParams boardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiStyleHelper.dp(context, 250)
        );
        addView(boardCanvas, boardLp);

        // 5. Game Controls Row
        LinearLayout ctrlRow = new LinearLayout(context);
        ctrlRow.setOrientation(HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER_VERTICAL);
        ctrlRow.setPadding(0, UiStyleHelper.dp(context, 8), 0, 0);

        primaryActionBtn = UiStyleHelper.createPillButton(
                context,
                "🎲 Roll Dice",
                UiStyleHelper.COLOR_LIVE_GREEN,
                Color.BLACK,
                v -> onPrimaryGameActionClicked()
        );

        modeToggleBtn = UiStyleHelper.createPillButton(
                context,
                "👥 Mode: 4 Players",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                UiStyleHelper.COLOR_CYBER_CYAN,
                v -> onSecondaryGameModeClicked()
        );

        TextView resetBtn = UiStyleHelper.createPillButton(
                context,
                "↺ Reset",
                UiStyleHelper.COLOR_CARD_ELEVATED,
                Color.WHITE,
                v -> {
                    if (activeGame == GameType.LUDO) resetLudoState();
                    else if (activeGame == GameType.CARROM) resetCarromState();
                    else resetPoolState();
                    refreshGameHeaderUI();
                    boardCanvas.invalidate();
                }
        );

        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f);
        pLp.setMargins(0, 0, UiStyleHelper.dp(context, 6), 0);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        mLp.setMargins(0, 0, UiStyleHelper.dp(context, 6), 0);

        ctrlRow.addView(primaryActionBtn, pLp);
        ctrlRow.addView(modeToggleBtn, mLp);
        ctrlRow.addView(resetBtn);
        addView(ctrlRow);

        refreshGameHeaderUI();
    }

    private void switchGame(GameType next) {
        this.activeGame = next;
        refreshGameHeaderUI();
        boardCanvas.invalidate();
    }

    public void stepPhysicsAndSyncUI() {
        boolean needsRedraw = false;
        if (activeGame == GameType.CARROM && carromMoving) {
            carromMoving = stepDiscPhysics(carromDiscs, true);
            needsRedraw = true;
            if (!carromMoving) {
                refreshGameHeaderUI();
            }
        } else if (activeGame == GameType.POOL_8BALL && poolMoving) {
            poolMoving = stepDiscPhysics(poolBalls, false);
            needsRedraw = true;
            if (!poolMoving) {
                refreshGameHeaderUI();
            }
        }
        if (needsRedraw && boardCanvas != null) {
            boardCanvas.invalidate();
        }
    }

    private void refreshGameHeaderUI() {
        boolean rtcConnected = syncBridge != null && syncBridge.isAgoraChannelConnected();
        boolean backendConfigured = EconomyManager.isBackendConfigured();

        String syncLabel = (rtcConnected ? "🟢 Agora DataStream Sync: Active" : "🟡 Agora DataStream: Offline (Local Table)")
                + "  •  " + (backendConfigured ? "Server State Validation: Enabled" : "Local Rule Engine (Set QLUVX_BACKEND_API_URL for Server Validation)");
        syncStatusBanner.setText(syncLabel);
        syncStatusBanner.setTextColor(rtcConnected ? UiStyleHelper.COLOR_LIVE_GREEN : Color.rgb(255, 215, 64));

        if (activeGame == GameType.LUDO) {
            String[] pNames = new String[]{"Red (P1)", "Green (P2)", "Yellow (P3)", "Blue (P4)"};
            String diceStr = lastDiceValue > 0 ? (" • Last Roll: 🎲 " + lastDiceValue) : "";
            turnStatusView.setText("🎲 Ludo (" + (ludoFourPlayerMode ? "4P" : "2P") + ") • Turn: " + pNames[ludoTurn]
                    + diceStr + (waitingForTokenMove ? " (Tap a token or Move!)" : ""));
            primaryActionBtn.setText(waitingForTokenMove ? ("Move Token (+" + lastDiceValue + ")") : "🎲 Roll Dice");
            modeToggleBtn.setText(ludoFourPlayerMode ? "👥 4 Players" : "👥 2 Players");
        } else if (activeGame == GameType.CARROM) {
            turnStatusView.setText("🎯 Carrom • Turn: P" + (carromTurn + 1)
                    + " • Score P1: " + carromScores[0] + " | P2: " + carromScores[1]
                    + " (Tap board to aim & strike)");
            primaryActionBtn.setText("🎯 Strike Carrom");
            modeToggleBtn.setText("Queen: 50 pts");
        } else {
            turnStatusView.setText("🎱 8 Ball Pool • Turn: P" + (poolTurn + 1)
                    + " • Potted P1: " + poolScores[0] + " | P2: " + poolScores[1]
                    + " (Tap table to aim cue)");
            primaryActionBtn.setText("🎱 Shoot Cue Ball");
            modeToggleBtn.setText("Rack: 8-Ball");
        }
    }

    private void onPrimaryGameActionClicked() {
        if (activeGame == GameType.LUDO) {
            if (!waitingForTokenMove) {
                lastDiceValue = 1 + random.nextInt(6);
                waitingForTokenMove = true;
                refreshGameHeaderUI();
                boardCanvas.invalidate();
            } else {
                advanceLudoTokenForCurrentPlayer(0);
            }
        } else if (activeGame == GameType.CARROM) {
            fireCarromStriker(aimX, aimY);
        } else {
            firePoolCueBall(poolAimX, poolAimY);
        }
    }

    private void onSecondaryGameModeClicked() {
        if (activeGame == GameType.LUDO) {
            ludoFourPlayerMode = !ludoFourPlayerMode;
            resetLudoState();
            refreshGameHeaderUI();
            boardCanvas.invalidate();
        } else if (activeGame == GameType.CARROM) {
            resetCarromState();
            refreshGameHeaderUI();
            boardCanvas.invalidate();
        } else {
            resetPoolState();
            refreshGameHeaderUI();
            boardCanvas.invalidate();
        }
    }

    private void advanceLudoTokenForCurrentPlayer(int preferredTokenIndex) {
        if (!waitingForTokenMove) return;
        int chosen = -1;
        for (int i = 0; i < 4; i++) {
            int idx = (preferredTokenIndex + i) % 4;
            int pos = ludoTokens[ludoTurn][idx];
            if (pos == 0 && (lastDiceValue == 6 || lastDiceValue == 1)) {
                chosen = idx;
                break;
            } else if (pos > 0 && pos + lastDiceValue <= 57) {
                chosen = idx;
                break;
            }
        }
        if (chosen >= 0) {
            int oldPos = ludoTokens[ludoTurn][chosen];
            int newPos = (oldPos == 0) ? 1 : (oldPos + lastDiceValue);
            ludoTokens[ludoTurn][chosen] = newPos;
            ludoScores[ludoTurn] += lastDiceValue;
            broadcastGameAction("LUDO_MOVE", ludoTurn, chosen, lastDiceValue, 0f, 0f);
        }
        waitingForTokenMove = false;
        if (lastDiceValue != 6) {
            if (ludoFourPlayerMode) {
                ludoTurn = (ludoTurn + 1) % 4;
            } else {
                ludoTurn = (ludoTurn == 0) ? 2 : 0;
            }
        }
        refreshGameHeaderUI();
        boardCanvas.invalidate();
    }

    private void fireCarromStriker(float targetNormX, float targetNormY) {
        if (carromMoving) return;
        Disc striker = carromDiscs[0];
        striker.pocketed = false;
        striker.x = 0.5f;
        striker.y = 0.82f;
        float dx = targetNormX - striker.x;
        float dy = targetNormY - striker.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.01f) {
            dx = 0f;
            dy = -1f;
            len = 1f;
        }
        striker.vx = (dx / len) * 0.036f;
        striker.vy = (dy / len) * 0.036f;
        carromMoving = true;
        broadcastGameAction("CARROM_STRIKE", carromTurn, 0, 0, targetNormX, targetNormY);
        carromTurn = (carromTurn + 1) % 2;
        refreshGameHeaderUI();
    }

    private void firePoolCueBall(float targetNormX, float targetNormY) {
        if (poolMoving) return;
        Disc cue = poolBalls[0];
        if (cue.pocketed) {
            cue.pocketed = false;
            cue.x = 0.25f;
            cue.y = 0.5f;
        }
        float dx = targetNormX - cue.x;
        float dy = targetNormY - cue.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.01f) {
            dx = 1f;
            dy = 0f;
            len = 1f;
        }
        cue.vx = (dx / len) * 0.038f;
        cue.vy = (dy / len) * 0.038f;
        poolMoving = true;
        broadcastGameAction("POOL_SHOT", poolTurn, 0, 0, targetNormX, targetNormY);
        poolTurn = (poolTurn + 1) % 2;
        refreshGameHeaderUI();
    }

    private void broadcastGameAction(String actionType, int playerTurn, int tokenIdx, int dice, float tx, float ty) {
        moveSeq++;
        if (syncBridge == null) return;
        try {
            JSONObject packet = new JSONObject();
            packet.put("type", "GAME_ACTION");
            packet.put("action", actionType);
            packet.put("seq", moveSeq);
            packet.put("turn", playerTurn);
            packet.put("tokenIdx", tokenIdx);
            packet.put("dice", dice);
            packet.put("tx", tx);
            packet.put("ty", ty);
            boolean sent = syncBridge.sendGameActionOverAgora(packet);
            syncBridge.onGameLogEvent("Game Action #" + moveSeq + " (" + actionType + ") "
                    + (sent ? "synced via Agora RTC DataStream." : "executed locally."));
        } catch (Throwable ignored) {
        }
    }

    public void applyRemoteGameAction(JSONObject json) {
        if (json == null) return;
        String action = json.optString("action", "");
        int turn = json.optInt("turn", 0);
        int tokenIdx = json.optInt("tokenIdx", 0);
        int dice = json.optInt("dice", 1);
        float tx = (float) json.optDouble("tx", 0.5);
        float ty = (float) json.optDouble("ty", 0.5);

        if ("LUDO_MOVE".equals(action)) {
            this.activeGame = GameType.LUDO;
            this.ludoTurn = turn % 4;
            this.lastDiceValue = dice;
            this.waitingForTokenMove = true;
            advanceLudoTokenForCurrentPlayer(tokenIdx);
        } else if ("CARROM_STRIKE".equals(action)) {
            this.activeGame = GameType.CARROM;
            this.aimX = tx;
            this.aimY = ty;
            fireCarromStriker(tx, ty);
        } else if ("POOL_SHOT".equals(action)) {
            this.activeGame = GameType.POOL_8BALL;
            this.poolAimX = tx;
            this.poolAimY = ty;
            firePoolCueBall(tx, ty);
        }
    }

    private void resetLudoState() {
        for (int p = 0; p < 4; p++) {
            ludoScores[p] = 0;
            ludoTokens[p][0] = 1; // 1 token already on starting square for immediate action
            ludoTokens[p][1] = 0;
            ludoTokens[p][2] = 0;
            ludoTokens[p][3] = 0;
        }
        ludoTurn = 0;
        lastDiceValue = 0;
        waitingForTokenMove = false;
    }

    private void resetCarromState() {
        carromScores[0] = 0;
        carromScores[1] = 0;
        carromTurn = 0;
        carromMoving = false;

        // Striker
        carromDiscs[0] = makeDisc(0.5f, 0.82f, 0.042f, Color.rgb(0, 229, 255), 0, "S");
        // Red Queen at center
        carromDiscs[1] = makeDisc(0.5f, 0.46f, 0.034f, Color.rgb(255, 50, 90), 50, "Q");
        // Circle of White and Black pucks around Queen
        for (int i = 0; i < 8; i++) {
            double ang = (Math.PI * 2 * i) / 8.0;
            float cx = 0.5f + (float) Math.cos(ang) * 0.085f;
            float cy = 0.46f + (float) Math.sin(ang) * 0.085f;
            boolean white = (i % 2 == 0);
            carromDiscs[2 + i] = makeDisc(
                    cx, cy, 0.032f,
                    white ? Color.rgb(245, 240, 225) : Color.rgb(45, 48, 68),
                    white ? 20 : 10,
                    white ? "W" : "B"
            );
        }
    }

    private void resetPoolState() {
        poolScores[0] = 0;
        poolScores[1] = 0;
        poolTurn = 0;
        poolMoving = false;

        // Cue ball
        poolBalls[0] = makeDisc(0.24f, 0.5f, 0.033f, Color.WHITE, 0, "C");
        // Triangle rack on right side
        int[] colors = new int[]{
                Color.rgb(255, 215, 64), Color.rgb(0, 176, 255), Color.rgb(255, 64, 96),
                Color.rgb(124, 77, 255), Color.rgb(255, 145, 0), Color.rgb(0, 230, 118),
                Color.rgb(255, 55, 180), Color.rgb(22, 22, 28), Color.rgb(0, 229, 255)
        };
        int idx = 1;
        float startX = 0.68f;
        for (int col = 0; col < 4 && idx < poolBalls.length; col++) {
            int countInCol = col + 1;
            for (int row = 0; row < countInCol && idx < poolBalls.length; row++) {
                float bx = startX + col * 0.058f;
                float by = 0.5f + (row - (countInCol - 1) / 2f) * 0.068f;
                String lbl = (idx == 5) ? "8" : String.valueOf(idx);
                int c = (idx == 5) ? Color.rgb(20, 20, 26) : colors[(idx - 1) % colors.length];
                poolBalls[idx] = makeDisc(bx, by, 0.031f, c, 1, lbl);
                idx++;
            }
        }
    }

    private Disc makeDisc(float x, float y, float r, int color, int pts, String label) {
        Disc d = new Disc();
        d.x = x;
        d.y = y;
        d.radius = r;
        d.color = color;
        d.points = pts;
        d.label = label;
        d.pocketed = false;
        return d;
    }

    private boolean stepDiscPhysics(Disc[] discs, boolean isCarrom) {
        boolean anyMoving = false;
        float minBound = 0.07f;
        float maxBound = 0.93f;

        for (int i = 0; i < discs.length; i++) {
            Disc d = discs[i];
            if (d == null || d.pocketed) continue;
            d.x += d.vx;
            d.y += d.vy;
            d.vx *= 0.94f;
            d.vy *= 0.94f;

            if (Math.hypot(d.vx, d.vy) < 0.0008f) {
                d.vx = 0f;
                d.vy = 0f;
            } else {
                anyMoving = true;
            }

            // Wall bounce
            if (d.x - d.radius < minBound) {
                d.x = minBound + d.radius;
                d.vx = -d.vx * 0.85f;
            } else if (d.x + d.radius > maxBound) {
                d.x = maxBound - d.radius;
                d.vx = -d.vx * 0.85f;
            }
            if (d.y - d.radius < minBound) {
                d.y = minBound + d.radius;
                d.vy = -d.vy * 0.85f;
            } else if (d.y + d.radius > maxBound) {
                d.y = maxBound - d.radius;
                d.vy = -d.vy * 0.85f;
            }

            // Pocket detection
            if (isNearPocket(d.x, d.y, isCarrom)) {
                if (i == 0) {
                    // Striker / Cue ball scratch -> reset position
                    d.vx = 0f;
                    d.vy = 0f;
                    d.x = isCarrom ? 0.5f : 0.24f;
                    d.y = isCarrom ? 0.82f : 0.5f;
                } else {
                    d.pocketed = true;
                    d.vx = 0f;
                    d.vy = 0f;
                    if (isCarrom) {
                        carromScores[carromTurn] += d.points;
                    } else {
                        poolScores[poolTurn] += d.points;
                    }
                }
            }
        }

        // Pairwise elastic collisions
        for (int i = 0; i < discs.length; i++) {
            for (int j = i + 1; j < discs.length; j++) {
                Disc a = discs[i];
                Disc b = discs[j];
                if (a == null || b == null || a.pocketed || b.pocketed) continue;
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float dist = (float) Math.hypot(dx, dy);
                float minDist = a.radius + b.radius;
                if (dist > 0.0001f && dist < minDist) {
                    float nx = dx / dist;
                    float ny = dy / dist;
                    float overlap = (minDist - dist) * 0.5f;
                    a.x -= nx * overlap;
                    a.y -= ny * overlap;
                    b.x += nx * overlap;
                    b.y += ny * overlap;

                    float kx = a.vx - b.vx;
                    float ky = a.vy - b.vy;
                    float p = nx * kx + ny * ky;
                    if (p > 0) {
                        a.vx -= p * nx * 0.9f;
                        a.vy -= p * ny * 0.9f;
                        b.vx += p * nx * 0.9f;
                        b.vy += p * ny * 0.9f;
                        anyMoving = true;
                    }
                }
            }
        }
        return anyMoving;
    }

    private boolean isNearPocket(float x, float y, boolean isCarrom) {
        float thresh = 0.075f;
        // 4 corners
        if (Math.hypot(x - 0.08f, y - 0.08f) < thresh) return true;
        if (Math.hypot(x - 0.92f, y - 0.08f) < thresh) return true;
        if (Math.hypot(x - 0.08f, y - 0.92f) < thresh) return true;
        if (Math.hypot(x - 0.92f, y - 0.92f) < thresh) return true;
        // Side pockets for 8-Ball Pool
        if (!isCarrom) {
            if (Math.hypot(x - 0.5f, y - 0.07f) < thresh) return true;
            if (Math.hypot(x - 0.5f, y - 0.93f) < thresh) return true;
        }
        return false;
    }

    private class GameBoardCanvas extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        public GameBoardCanvas(Context context) {
            super(context);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                float nx = Math.max(0.08f, Math.min(0.92f, event.getX() / Math.max(1f, getWidth())));
                float ny = Math.max(0.08f, Math.min(0.92f, event.getY() / Math.max(1f, getHeight())));
                if (activeGame == GameType.CARROM) {
                    aimX = nx;
                    aimY = ny;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        fireCarromStriker(aimX, aimY);
                    }
                    invalidate();
                    return true;
                } else if (activeGame == GameType.POOL_8BALL) {
                    poolAimX = nx;
                    poolAimY = ny;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        firePoolCueBall(poolAimX, poolAimY);
                    }
                    invalidate();
                    return true;
                } else if (activeGame == GameType.LUDO && event.getAction() == MotionEvent.ACTION_DOWN) {
                    onPrimaryGameActionClicked();
                    return true;
                }
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (activeGame == GameType.LUDO) {
                drawLudoBoard(canvas, w, h);
            } else if (activeGame == GameType.CARROM) {
                drawCarromOrPoolBoard(canvas, w, h, true);
            } else {
                drawCarromOrPoolBoard(canvas, w, h, false);
            }
        }

        private void drawLudoBoard(Canvas canvas, float w, float h) {
            float size = Math.min(w, h) * 0.94f;
            float left = (w - size) / 2f;
            float top = (h - size) / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(22, 22, 38));
            rect.set(left, top, left + size, top + size);
            canvas.drawRoundRect(rect, 20f, 20f, paint);

            int[] pColors = new int[]{
                    Color.rgb(255, 64, 96),   // Red (P1)
                    Color.rgb(0, 230, 118),   // Green (P2)
                    Color.rgb(255, 215, 64),  // Yellow (P3)
                    Color.rgb(0, 176, 255)    // Blue (P4)
            };

            float quad = size * 0.38f;
            float[][] baseCoords = new float[][]{
                    {left + 8f, top + 8f},
                    {left + size - quad - 8f, top + 8f},
                    {left + size - quad - 8f, top + size - quad - 8f},
                    {left + 8f, top + size - quad - 8f}
            };

            for (int p = 0; p < 4; p++) {
                if (!ludoFourPlayerMode && (p == 1 || p == 3)) {
                    paint.setColor(Color.argb(60, 80, 80, 100));
                } else {
                    paint.setColor(pColors[p]);
                }
                float bx = baseCoords[p][0];
                float by = baseCoords[p][1];
                rect.set(bx, by, bx + quad, by + quad);
                canvas.drawRoundRect(rect, 14f, 14f, paint);

                // Highlight active turn border
                if (ludoTurn == p) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5f);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(rect, 14f, 14f, paint);
                    paint.setStyle(Paint.Style.FILL);
                }

                // Draw tokens & progress inside each player zone
                textPaint.setColor(Color.BLACK);
                textPaint.setTextSize(quad * 0.18f);
                canvas.drawText("P" + (p + 1) + " (" + ludoScores[p] + " steps)", bx + quad / 2f, by + quad * 0.28f, textPaint);

                for (int t = 0; t < 4; t++) {
                    float tx = bx + quad * (t % 2 == 0 ? 0.32f : 0.68f);
                    float ty = by + quad * (t < 2 ? 0.55f : 0.80f);
                    paint.setColor(Color.rgb(16, 16, 28));
                    canvas.drawCircle(tx, ty, quad * 0.11f, paint);
                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(quad * 0.12f);
                    int step = ludoTokens[p][t];
                    canvas.drawText(step == 0 ? "B" : String.valueOf(step), tx, ty + quad * 0.04f, textPaint);
                }
            }

            // Center Home & Dice Box
            float cx = left + size / 2f;
            float cy = top + size / 2f;
            float centerBox = size * 0.20f;
            rect.set(cx - centerBox / 2f, cy - centerBox / 2f, cx + centerBox / 2f, cy + centerBox / 2f);
            paint.setColor(Color.rgb(36, 28, 64));
            canvas.drawRoundRect(rect, 12f, 12f, paint);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(centerBox * 0.34f);
            String dText = lastDiceValue > 0 ? ("🎲 " + lastDiceValue) : "ROLL";
            canvas.drawText(dText, cx, cy + centerBox * 0.12f, textPaint);
        }

        private void drawCarromOrPoolBoard(Canvas canvas, float w, float h, boolean isCarrom) {
            float boardW = w * 0.94f;
            float boardH = h * 0.94f;
            float left = (w - boardW) / 2f;
            float top = (h - boardH) / 2f;

            // Outer frame
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isCarrom ? Color.rgb(42, 28, 22) : Color.rgb(32, 18, 48));
            rect.set(left, top, left + boardW, top + boardH);
            canvas.drawRoundRect(rect, 22f, 22f, paint);

            // Inner play surface
            float pad = Math.min(boardW, boardH) * 0.06f;
            rect.set(left + pad, top + pad, left + boardW - pad, top + boardH - pad);
            paint.setColor(isCarrom ? Color.rgb(26, 24, 38) : Color.rgb(10, 46, 38));
            canvas.drawRoundRect(rect, 14f, 14f, paint);

            // Center circle / markings
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(Color.argb(90, 255, 255, 255));
            canvas.drawCircle(left + boardW * 0.5f, top + boardH * (isCarrom ? 0.46f : 0.5f), Math.min(boardW, boardH) * 0.14f, paint);

            // Pockets
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(6, 6, 10));
            float pr = Math.min(boardW, boardH) * 0.055f;
            canvas.drawCircle(left + boardW * 0.08f, top + boardH * 0.08f, pr, paint);
            canvas.drawCircle(left + boardW * 0.92f, top + boardH * 0.08f, pr, paint);
            canvas.drawCircle(left + boardW * 0.08f, top + boardH * 0.92f, pr, paint);
            canvas.drawCircle(left + boardW * 0.92f, top + boardH * 0.92f, pr, paint);
            if (!isCarrom) {
                canvas.drawCircle(left + boardW * 0.5f, top + boardH * 0.07f, pr, paint);
                canvas.drawCircle(left + boardW * 0.5f, top + boardH * 0.93f, pr, paint);
            }

            Disc[] discs = isCarrom ? carromDiscs : poolBalls;
            Disc shooter = discs[0];

            // Aim line when stationary
            boolean moving = isCarrom ? carromMoving : poolMoving;
            if (!moving && shooter != null && !shooter.pocketed) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setColor(Color.argb(170, 0, 229, 255));
                float sx = left + shooter.x * boardW;
                float sy = top + shooter.y * boardH;
                float tx = left + (isCarrom ? aimX : poolAimX) * boardW;
                float ty = top + (isCarrom ? aimY : poolAimY) * boardH;
                canvas.drawLine(sx, sy, tx, ty, paint);
            }

            // Draw discs/balls
            paint.setStyle(Paint.Style.FILL);
            for (Disc d : discs) {
                if (d == null || d.pocketed) continue;
                float cx = left + d.x * boardW;
                float cy = top + d.y * boardH;
                float r = d.radius * Math.min(boardW, boardH);
                paint.setColor(d.color);
                canvas.drawCircle(cx, cy, r, paint);

                textPaint.setColor(d.color == Color.WHITE || d.color == Color.rgb(245, 240, 225) || d.color == Color.rgb(255, 215, 64)
                        ? Color.BLACK : Color.WHITE);
                textPaint.setTextSize(r * 0.95f);
                canvas.drawText(d.label, cx, cy + r * 0.34f, textPaint);
            }
        }
    }
}
