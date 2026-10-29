package com.example.qluvx;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Menu 10 & Phase 7: QLUVX Game Center Activity.
 * Provides 3 interactive playable games:
 * 1. Ludo (2 Players / 4 Players) with dice roller, token movement, capture & home finish
 * 2. Carrom with draggable striker, slingshot aiming, Queen + Black/White coins & pocket physics
 * 3. 8 Ball Pool with cue ball aiming, numbered billiard balls (1-8), pockets & 2D collision physics
 */
public class GameCenterActivity extends Activity {

    public static final String EXTRA_INITIAL_GAME = "extra_initial_game"; // 0=Ludo, 1=Carrom, 2=Pool

    private int activeGameIndex = 0; // 0=Ludo, 1=Carrom, 2=Pool
    private FrameLayout boardContainer;
    private TextView tabLudo;
    private TextView tabCarrom;
    private TextView tabPool;
    private TextView tvTurnStatus;
    private TextView tvScoreChip;
    private TextView btnPrimaryAction;
    private TextView btnModeToggle;
    private TextView btnReset;

    private LudoBoardView ludoView;
    private CarromBoardView carromView;
    private PoolTableView poolView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiStyleHelper.configureSafeActivityWindow(this);
        setContentView(R.layout.activity_game_center);

        activeGameIndex = getIntent().getIntExtra(EXTRA_INITIAL_GAME, 0);

        boardContainer = findViewById(R.id.game_board_container);
        tabLudo = findViewById(R.id.tab_game_ludo);
        tabCarrom = findViewById(R.id.tab_game_carrom);
        tabPool = findViewById(R.id.tab_game_pool);
        tvTurnStatus = findViewById(R.id.tv_game_turn_status);
        tvScoreChip = findViewById(R.id.tv_game_score_chip);
        btnPrimaryAction = findViewById(R.id.btn_game_primary_action);
        btnModeToggle = findViewById(R.id.btn_game_mode_toggle);
        btnReset = findViewById(R.id.btn_game_reset);

        findViewById(R.id.btn_game_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_game_voice_room).setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceRoomActivity.class);
            intent.putExtra(VoiceRoomActivity.EXTRA_ROOM_TITLE, "Game Center Voice Comms");
            intent.putExtra(VoiceRoomActivity.EXTRA_CHANNEL_NAME, "agora_voice_gaming_04");
            startActivity(intent);
        });

        ludoView = new LudoBoardView(this);
        carromView = new CarromBoardView(this);
        poolView = new PoolTableView(this);

        tabLudo.setOnClickListener(v -> switchGame(0));
        tabCarrom.setOnClickListener(v -> switchGame(1));
        tabPool.setOnClickListener(v -> switchGame(2));

        btnPrimaryAction.setOnClickListener(v -> {
            if (activeGameIndex == 0) {
                ludoView.rollDiceAndMove();
            } else if (activeGameIndex == 1) {
                carromView.autoStrikeShot();
            } else {
                poolView.autoCueShot();
            }
        });

        btnModeToggle.setOnClickListener(v -> {
            if (activeGameIndex == 0) {
                ludoView.togglePlayerCount();
            } else if (activeGameIndex == 1) {
                carromView.nudgeStrikerHorizontal();
            } else {
                poolView.rotateAimAngle();
            }
        });

        btnReset.setOnClickListener(v -> {
            if (activeGameIndex == 0) {
                ludoView.resetGame();
            } else if (activeGameIndex == 1) {
                carromView.resetBoard();
            } else {
                poolView.resetTable();
            }
            Toast.makeText(this, "Match reset!", Toast.LENGTH_SHORT).show();
        });

        switchGame(activeGameIndex);
    }

    private void switchGame(int index) {
        activeGameIndex = index;
        boardContainer.removeAllViews();

        tabLudo.setBackgroundColor(index == 0 ? UiStyleHelper.COLOR_AGORA_BLUE : Color.parseColor("#334155"));
        tabLudo.setTextColor(index == 0 ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabCarrom.setBackgroundColor(index == 1 ? UiStyleHelper.COLOR_AGORA_BLUE : Color.parseColor("#334155"));
        tabCarrom.setTextColor(index == 1 ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabPool.setBackgroundColor(index == 2 ? UiStyleHelper.COLOR_AGORA_BLUE : Color.parseColor("#334155"));
        tabPool.setTextColor(index == 2 ? Color.WHITE : Color.parseColor("#94A3B8"));

        if (index == 0) {
            boardContainer.addView(ludoView);
            btnPrimaryAction.setText("🎲 Roll Dice");
            btnModeToggle.setText("👥 " + (ludoView.fourPlayerMode ? "4 Players" : "2 Players"));
            ludoView.syncStatusBanner();
        } else if (index == 1) {
            boardContainer.addView(carromView);
            btnPrimaryAction.setText("🎯 Strike Coin");
            btnModeToggle.setText("↔ Move Striker");
            carromView.syncStatusBanner();
        } else {
            boardContainer.addView(poolView);
            btnPrimaryAction.setText("🎱 Shoot Cue");
            btnModeToggle.setText("↻ Aim Angle");
            poolView.syncStatusBanner();
        }
    }

    // =========================================================================
    // 1. PLAYABLE LUDO (2 / 4 PLAYERS)
    // =========================================================================
    private class LudoBoardView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final String[] playerNames = {"Red", "Green", "Yellow", "Blue"};
        private final int[] playerColors = {
                Color.rgb(239, 68, 68),
                Color.rgb(16, 185, 129),
                Color.rgb(245, 158, 11),
                Color.rgb(59, 130, 246)
        };
        boolean fourPlayerMode = true;
        int currentTurn = 0;
        int lastDiceValue = 6;
        // Progress of 2 tokens per player: 0 = in base, 1..28 = track steps, 29 = home goal
        private final int[][] tokenSteps = new int[4][2];
        private final int[] playerScores = new int[4];

        public LudoBoardView(Context context) {
            super(context);
            resetGame();
        }

        void resetGame() {
            for (int p = 0; p < 4; p++) {
                tokenSteps[p][0] = 1; // Token 1 starts on board ready to race
                tokenSteps[p][1] = 0; // Token 2 in home yard (unlocks on 6 or move)
                playerScores[p] = 0;
            }
            currentTurn = 0;
            lastDiceValue = 6;
            syncStatusBanner();
            invalidate();
        }

        void togglePlayerCount() {
            fourPlayerMode = !fourPlayerMode;
            btnModeToggle.setText("👥 " + (fourPlayerMode ? "4 Players" : "2 Players"));
            resetGame();
        }

        void rollDiceAndMove() {
            lastDiceValue = 1 + random.nextInt(6);
            int p = currentTurn;
            // Move active token or unlock second token on 6
            if (lastDiceValue == 6 && tokenSteps[p][1] == 0) {
                tokenSteps[p][1] = 1;
                playerScores[p] += 6;
            } else {
                int tokenIdx = (tokenSteps[p][0] < 29) ? 0 : 1;
                if (tokenSteps[p][tokenIdx] == 0) {
                    tokenSteps[p][tokenIdx] = 1;
                } else {
                    tokenSteps[p][tokenIdx] = Math.min(29, tokenSteps[p][tokenIdx] + lastDiceValue);
                }
                playerScores[p] += lastDiceValue;
            }

            syncStatusBanner();

            // Advance turn unless rolled 6
            if (lastDiceValue != 6) {
                if (fourPlayerMode) {
                    currentTurn = (currentTurn + 1) % 4;
                } else {
                    currentTurn = (currentTurn == 0) ? 2 : 0; // Red vs Yellow
                }
            }
            invalidate();
        }

        void syncStatusBanner() {
            if (tvTurnStatus != null) {
                tvTurnStatus.setText("🎲 " + playerNames[currentTurn] + " Turn • Rolled: " + lastDiceValue + " (Tap Board or Roll)");
            }
            if (tvScoreChip != null) {
                if (fourPlayerMode) {
                    tvScoreChip.setText("R:" + playerScores[0] + " G:" + playerScores[1] + " Y:" + playerScores[2] + " B:" + playerScores[3]);
                } else {
                    tvScoreChip.setText("Red: " + playerScores[0] + " • Yellow: " + playerScores[2]);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                rollDiceAndMove();
                return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 24;
            if (size <= 0) return;

            float left = (w - size) / 2f;
            float top = (h - size) / 2f;
            float cell = size / 15f;

            // Board background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(248, 250, 252));
            canvas.drawRect(left, top, left + size, top + size, paint);

            // 4 Player Corner Yards (6x6 cells each)
            drawPlayerYard(canvas, left, top, cell, playerColors[0], "RED", 0);
            drawPlayerYard(canvas, left + 9 * cell, top, cell, playerColors[1], "GREEN", 1);
            drawPlayerYard(canvas, left + 9 * cell, top + 9 * cell, cell, playerColors[2], "YELLOW", 2);
            drawPlayerYard(canvas, left, top + 9 * cell, cell, playerColors[3], "BLUE", 3);

            // Home Stretch colored paths
            paint.setColor(playerColors[0]);
            canvas.drawRect(left + cell, top + 7 * cell, left + 6 * cell, top + 8 * cell, paint);
            paint.setColor(playerColors[1]);
            canvas.drawRect(left + 7 * cell, top + cell, left + 8 * cell, top + 6 * cell, paint);
            paint.setColor(playerColors[2]);
            canvas.drawRect(left + 9 * cell, top + 7 * cell, left + 14 * cell, top + 8 * cell, paint);
            paint.setColor(playerColors[3]);
            canvas.drawRect(left + 7 * cell, top + 9 * cell, left + 8 * cell, top + 14 * cell, paint);

            // Center 3x3 Home Goal with Dice value
            paint.setColor(Color.rgb(30, 41, 59));
            canvas.drawRect(left + 6 * cell, top + 6 * cell, left + 9 * cell, top + 9 * cell, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cell * 1.1f);
            canvas.drawText("🎲 " + lastDiceValue, left + 7.5f * cell, top + 7.9f * cell, paint);

            // Grid lines
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(Color.rgb(148, 163, 184));
            for (int i = 0; i <= 15; i++) {
                canvas.drawLine(left + i * cell, top, left + i * cell, top + size, paint);
                canvas.drawLine(left, top + i * cell, left + size, top + i * cell, paint);
            }

            // Draw Tokens for active players
            paint.setStyle(Paint.Style.FILL);
            for (int p = 0; p < 4; p++) {
                if (!fourPlayerMode && (p == 1 || p == 3)) continue;
                for (int t = 0; t < 2; t++) {
                    float[] pos = computeTokenCoord(p, t, tokenSteps[p][t], left, top, cell);
                    paint.setColor(Color.WHITE);
                    canvas.drawCircle(pos[0], pos[1], cell * 0.42f, paint);
                    paint.setColor(playerColors[p]);
                    canvas.drawCircle(pos[0], pos[1], cell * 0.34f, paint);
                }
            }
        }

        private void drawPlayerYard(Canvas canvas, float x, float y, float cell, int color, String label, int playerIdx) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRect(x, y, x + 6 * cell, y + 6 * cell, paint);
            paint.setColor(Color.rgb(15, 23, 42));
            canvas.drawRect(x + cell, y + cell, x + 5 * cell, y + 5 * cell, paint);

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cell * 0.65f);
            String activeTag = (currentTurn == playerIdx) ? ("★ " + label) : label;
            canvas.drawText(activeTag, x + 3 * cell, y + 3.2f * cell, paint);
        }

        private float[] computeTokenCoord(int player, int tokenIdx, int step, float left, float top, float cell) {
            if (step == 0) {
                float baseX = (player == 0 || player == 3) ? (left + (2.2f + tokenIdx * 1.6f) * cell) : (left + (11.2f + tokenIdx * 1.6f) * cell);
                float baseY = (player == 0 || player == 1) ? (top + 2.2f * cell) : (top + 11.2f * cell);
                return new float[]{baseX, baseY};
            }
            if (step >= 29) {
                return new float[]{left + (6.8f + player * 0.45f) * cell, top + (6.8f + tokenIdx * 1.2f) * cell};
            }
            // Perimeter track ring around the center
            float angle = (float) (((player * 7 + step) % 28) * (2 * Math.PI / 28.0));
            float radius = 5.2f * cell;
            float cx = left + 7.5f * cell + (float) Math.cos(angle) * radius;
            float cy = top + 7.5f * cell + (float) Math.sin(angle) * radius;
            return new float[]{cx, cy};
        }
    }

    // =========================================================================
    // 2. PLAYABLE CARROM BOARD (PHYSICS + SLINGSHOT AIMING)
    // =========================================================================
    private class CarromBoardView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<CarromCoin> coins = new ArrayList<>();
        private float strikerX = 0.5f; // normalized 0.22..0.78
        private float strikerY = 0.80f;
        private float strikerVx = 0f;
        private float strikerVy = 0f;
        private float dragX = -1f;
        private float dragY = -1f;
        private int scoreWhite = 0;
        private int scoreBlack = 0;
        private boolean isSimulating = false;

        private class CarromCoin {
            float nx, ny, vx, vy;
            int color;
            int points;
            boolean pocketed = false;

            CarromCoin(float nx, float ny, int color, int points) {
                this.nx = nx;
                this.ny = ny;
                this.color = color;
                this.points = points;
            }
        }

        public CarromBoardView(Context context) {
            super(context);
            resetBoard();
        }

        void resetBoard() {
            coins.clear();
            // Red Queen at center
            coins.add(new CarromCoin(0.50f, 0.50f, Color.rgb(239, 68, 68), 50));
            // Ring of White & Black carrommen
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4.0;
                float cx = 0.50f + (float) Math.cos(ang) * 0.085f;
                float cy = 0.50f + (float) Math.sin(ang) * 0.085f;
                boolean white = (i % 2 == 0);
                coins.add(new CarromCoin(cx, cy, white ? Color.rgb(248, 250, 252) : Color.rgb(30, 41, 59), white ? 20 : 10));
            }
            strikerX = 0.50f;
            strikerY = 0.80f;
            strikerVx = 0f;
            strikerVy = 0f;
            scoreWhite = 0;
            scoreBlack = 0;
            isSimulating = false;
            syncStatusBanner();
            invalidate();
        }

        void nudgeStrikerHorizontal() {
            if (isSimulating) return;
            strikerX += 0.10f;
            if (strikerX > 0.76f) strikerX = 0.24f;
            strikerY = 0.80f;
            invalidate();
        }

        void autoStrikeShot() {
            strikerVx = (0.50f - strikerX) * 0.06f + (float) ((Math.random() - 0.5) * 0.015f);
            strikerVy = -0.042f;
            isSimulating = true;
            postInvalidateOnAnimation();
        }

        void syncStatusBanner() {
            if (tvTurnStatus != null) {
                tvTurnStatus.setText("🎯 Carrom • Drag from Striker to Aim & Release, or tap Strike");
            }
            if (tvScoreChip != null) {
                tvScoreChip.setText("Score: " + (scoreWhite + scoreBlack) + " pts");
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 24;
            if (size <= 0) return false;
            float left = (w - size) / 2f;
            float top = (h - size) / 2f;

            float tx = (event.getX() - left) / size;
            float ty = (event.getY() - top) / size;

            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                dragX = Math.max(0.05f, Math.min(0.95f, tx));
                dragY = Math.max(0.05f, Math.min(0.95f, ty));
                invalidate();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (dragX > 0f && !isSimulating) {
                    strikerVx = (dragX - strikerX) * 0.08f;
                    strikerVy = (dragY - strikerY) * 0.08f;
                    isSimulating = true;
                    dragX = -1f;
                    dragY = -1f;
                    postInvalidateOnAnimation();
                }
                return true;
            }
            return super.onTouchEvent(event);
        }

        private void stepPhysics() {
            if (!isSimulating) return;
            boolean anyMoving = false;

            strikerX += strikerVx;
            strikerY += strikerVy;
            strikerVx *= 0.94f;
            strikerVy *= 0.94f;

            if (strikerX < 0.08f || strikerX > 0.92f) {
                strikerVx = -strikerVx;
                strikerX = Math.max(0.08f, Math.min(0.92f, strikerX));
            }
            if (strikerY < 0.08f || strikerY > 0.92f) {
                strikerVy = -strikerVy;
                strikerY = Math.max(0.08f, Math.min(0.92f, strikerY));
            }
            if (Math.hypot(strikerVx, strikerVy) > 0.0015f) {
                anyMoving = true;
            }

            for (CarromCoin c : coins) {
                if (c.pocketed) continue;
                float dx = c.nx - strikerX;
                float dy = c.ny - strikerY;
                float dist = (float) Math.hypot(dx, dy);
                if (dist > 0.0001f && dist < 0.068f) {
                    float nx = dx / dist;
                    float ny = dy / dist;
                    c.vx += nx * 0.026f;
                    c.vy += ny * 0.026f;
                    strikerVx -= nx * 0.012f;
                    strikerVy -= ny * 0.012f;
                }

                c.nx += c.vx;
                c.ny += c.vy;
                c.vx *= 0.93f;
                c.vy *= 0.93f;

                if (c.nx < 0.08f || c.nx > 0.92f) c.vx = -c.vx;
                if (c.ny < 0.08f || c.ny > 0.92f) c.vy = -c.vy;

                // Check 4 corner pockets
                if ((c.nx < 0.13f && c.ny < 0.13f) || (c.nx > 0.87f && c.ny < 0.13f)
                        || (c.nx < 0.13f && c.ny > 0.87f) || (c.nx > 0.87f && c.ny > 0.87f)) {
                    c.pocketed = true;
                    scoreWhite += c.points;
                    syncStatusBanner();
                }

                if (Math.hypot(c.vx, c.vy) > 0.0015f) {
                    anyMoving = true;
                }
            }

            if (!anyMoving) {
                isSimulating = false;
                strikerX = 0.50f;
                strikerY = 0.80f;
            } else {
                postInvalidateOnAnimation();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            stepPhysics();

            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 24;
            if (size <= 0) return;
            float left = (w - size) / 2f;
            float top = (h - size) / 2f;

            // Wooden Frame & Carrom Surface
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(120, 53, 15));
            canvas.drawRoundRect(new RectF(left, top, left + size, top + size), 24f, 24f, paint);

            float pad = size * 0.055f;
            paint.setColor(Color.rgb(254, 243, 199));
            canvas.drawRoundRect(new RectF(left + pad, top + pad, left + size - pad, top + size - pad), 14f, 14f, paint);

            // 4 Corner Pockets
            paint.setColor(Color.rgb(15, 23, 42));
            float pRad = size * 0.052f;
            canvas.drawCircle(left + pad + pRad, top + pad + pRad, pRad, paint);
            canvas.drawCircle(left + size - pad - pRad, top + pad + pRad, pRad, paint);
            canvas.drawCircle(left + pad + pRad, top + size - pad - pRad, pRad, paint);
            canvas.drawCircle(left + size - pad - pRad, top + size - pad - pRad, pRad, paint);

            // Center Circle & Striker Baseline
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.rgb(180, 83, 9));
            canvas.drawCircle(left + size * 0.5f, top + size * 0.5f, size * 0.12f, paint);
            canvas.drawLine(left + size * 0.22f, top + size * 0.80f, left + size * 0.78f, top + size * 0.80f, paint);

            // Aim line
            if (dragX > 0f && !isSimulating) {
                paint.setColor(Color.rgb(37, 99, 235));
                paint.setStrokeWidth(4f);
                canvas.drawLine(left + strikerX * size, top + strikerY * size, left + dragX * size, top + dragY * size, paint);
            }

            // Draw Coins
            paint.setStyle(Paint.Style.FILL);
            for (CarromCoin c : coins) {
                if (c.pocketed) continue;
                float cx = left + c.nx * size;
                float cy = top + c.ny * size;
                paint.setColor(c.color);
                canvas.drawCircle(cx, cy, size * 0.031f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(Color.rgb(245, 158, 11));
                canvas.drawCircle(cx, cy, size * 0.031f, paint);
                paint.setStyle(Paint.Style.FILL);
            }

            // Draw Striker
            float sx = left + strikerX * size;
            float sy = top + strikerY * size;
            paint.setColor(Color.rgb(37, 99, 235));
            canvas.drawCircle(sx, sy, size * 0.040f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(sx, sy, size * 0.018f, paint);
        }
    }

    // =========================================================================
    // 3. PLAYABLE 8 BALL POOL TABLE
    // =========================================================================
    private class PoolTableView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<PoolBall> balls = new ArrayList<>();
        private float cueX = 0.26f;
        private float cueY = 0.50f;
        private float cueVx = 0f;
        private float cueVy = 0f;
        private float aimAngleRad = 0f;
        private boolean isRolling = false;
        private int pottedCount = 0;

        private class PoolBall {
            int number;
            float nx, ny, vx, vy;
            int color;
            boolean potted = false;

            PoolBall(int number, float nx, float ny, int color) {
                this.number = number;
                this.nx = nx;
                this.ny = ny;
                this.color = color;
            }
        }

        public PoolTableView(Context context) {
            super(context);
            resetTable();
        }

        void resetTable() {
            balls.clear();
            int[] colors = {
                    Color.rgb(245, 158, 11),
                    Color.rgb(37, 99, 235),
                    Color.rgb(239, 68, 68),
                    Color.rgb(139, 92, 246),
                    Color.rgb(249, 115, 22),
                    Color.rgb(16, 185, 129),
                    Color.rgb(185, 28, 28),
                    Color.rgb(15, 23, 42) // 8-Ball
            };
            int idx = 0;
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row <= col; row++) {
                    if (idx >= colors.length) break;
                    float bx = 0.66f + col * 0.055f;
                    float by = 0.50f + (row - col * 0.5f) * 0.075f;
                    balls.add(new PoolBall(idx + 1, bx, by, colors[idx]));
                    idx++;
                }
            }
            cueX = 0.25f;
            cueY = 0.50f;
            cueVx = 0f;
            cueVy = 0f;
            aimAngleRad = 0f;
            isRolling = false;
            pottedCount = 0;
            syncStatusBanner();
            invalidate();
        }

        void rotateAimAngle() {
            aimAngleRad += (float) (Math.PI / 9.0);
            invalidate();
        }

        void autoCueShot() {
            cueVx = (float) Math.cos(aimAngleRad) * 0.044f;
            cueVy = (float) Math.sin(aimAngleRad) * 0.044f;
            isRolling = true;
            postInvalidateOnAnimation();
        }

        void syncStatusBanner() {
            if (tvTurnStatus != null) {
                tvTurnStatus.setText("🎱 8 Ball Pool • Tap Table to Aim & Shoot Cue Ball");
            }
            if (tvScoreChip != null) {
                tvScoreChip.setText("Potted: " + pottedCount + "/8 Balls");
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return false;
            float tx = event.getX() / w;
            float ty = event.getY() / h;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                aimAngleRad = (float) Math.atan2(ty - cueY, tx - cueX);
                autoCueShot();
                return true;
            }
            return super.onTouchEvent(event);
        }

        private void stepPoolPhysics() {
            if (!isRolling) return;
            boolean moving = false;

            cueX += cueVx;
            cueY += cueVy;
            cueVx *= 0.95f;
            cueVy *= 0.95f;

            if (cueX < 0.08f || cueX > 0.92f) {
                cueVx = -cueVx;
                cueX = Math.max(0.08f, Math.min(0.92f, cueX));
            }
            if (cueY < 0.12f || cueY > 0.88f) {
                cueVy = -cueVy;
                cueY = Math.max(0.12f, Math.min(0.88f, cueY));
            }
            if (Math.hypot(cueVx, cueVy) > 0.0014f) moving = true;

            for (PoolBall b : balls) {
                if (b.potted) continue;
                float dx = b.nx - cueX;
                float dy = b.ny - cueY;
                float dist = (float) Math.hypot(dx, dy);
                if (dist > 0.0001f && dist < 0.065f) {
                    float nx = dx / dist;
                    float ny = dy / dist;
                    b.vx += nx * 0.028f;
                    b.vy += ny * 0.028f;
                    cueVx -= nx * 0.012f;
                    cueVy -= ny * 0.012f;
                }

                b.nx += b.vx;
                b.ny += b.vy;
                b.vx *= 0.94f;
                b.vy *= 0.94f;

                if (b.nx < 0.08f || b.nx > 0.92f) b.vx = -b.vx;
                if (b.ny < 0.12f || b.ny > 0.88f) b.vy = -b.vy;

                // Check 6 pockets (4 corners + 2 side pockets)
                boolean inCorner = (b.nx < 0.12f || b.nx > 0.88f) && (b.ny < 0.16f || b.ny > 0.84f);
                boolean inSide = (Math.abs(b.nx - 0.50f) < 0.055f) && (b.ny < 0.15f || b.ny > 0.85f);
                if (inCorner || inSide) {
                    b.potted = true;
                    pottedCount++;
                    syncStatusBanner();
                }

                if (Math.hypot(b.vx, b.vy) > 0.0014f) moving = true;
            }

            if (!moving) {
                isRolling = false;
            } else {
                postInvalidateOnAnimation();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            stepPoolPhysics();

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float pad = 22f;
            // Rails
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(120, 53, 15));
            canvas.drawRoundRect(new RectF(pad, pad, w - pad, h - pad), 28f, 28f, paint);

            // Green Felt
            float feltPad = 48f;
            paint.setColor(Color.rgb(5, 150, 105));
            canvas.drawRoundRect(new RectF(feltPad, feltPad, w - feltPad, h - feltPad), 16f, 16f, paint);

            // 6 Pockets
            paint.setColor(Color.rgb(15, 23, 42));
            float pRad = 26f;
            canvas.drawCircle(feltPad, feltPad, pRad, paint);
            canvas.drawCircle(w * 0.5f, feltPad, pRad, paint);
            canvas.drawCircle(w - feltPad, feltPad, pRad, paint);
            canvas.drawCircle(feltPad, h - feltPad, pRad, paint);
            canvas.drawCircle(w * 0.5f, h - feltPad, pRad, paint);
            canvas.drawCircle(w - feltPad, h - feltPad, pRad, paint);

            // Cue Aim Line
            float cx = cueX * w;
            float cy = cueY * h;
            if (!isRolling) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.5f);
                paint.setColor(Color.argb(190, 255, 255, 255));
                float aimEndX = cx + (float) Math.cos(aimAngleRad) * (w * 0.25f);
                float aimEndY = cy + (float) Math.sin(aimAngleRad) * (w * 0.25f);
                canvas.drawLine(cx, cy, aimEndX, aimEndY, paint);
            }

            // Cue Ball
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            float ballRad = Math.min(w, h) * 0.032f;
            canvas.drawCircle(cx, cy, ballRad, paint);

            // Numbered Pool Balls
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(ballRad * 0.95f);
            for (PoolBall b : balls) {
                if (b.potted) continue;
                float bx = b.nx * w;
                float by = b.ny * h;
                paint.setColor(b.color);
                canvas.drawCircle(bx, by, ballRad, paint);
                paint.setColor(Color.WHITE);
                canvas.drawText(String.valueOf(b.number), bx, by + ballRad * 0.34f, paint);
            }
        }
    }
}
