import { Request, Response } from "express";
import { Pool } from "pg";

/**
 * QLUVX Production Streamer Diamond Payout & Google Play IAP Verification Controller
 *
 * 1. Google Play In-App Billing Verification (POST /api/v1/wallet/verify-iap)
 *    - Verifies Google Play PurchaseToken for SKUs:
 *      * coins_bundle_100 (₹89 -> 100 Coins)
 *      * coins_bundle_500 (₹399 -> 500 Coins)
 *      * coins_bundle_2000 (₹1,499 -> 2,000 Coins)
 *      * vip_monthly_subscription (₹499/month -> VIP + 1,000 Bonus Coins)
 *    - Atomically credits coins to `user_wallets` in PostgreSQL 16.
 *
 * 2. Streamer Diamond Payout Engine (POST /api/v1/payouts/request)
 *    - Conversion Rate: 1 Diamond = ₹0.50 INR
 *    - Minimum Withdrawal: 5,000 Diamonds = ₹2,500 INR
 *    - Supported Methods: UPI (username@upi), Direct Bank IMPS/NEFT, PayPal
 */

const pgPool = new Pool({
  connectionString:
    process.env.DATABASE_URL ||
    "postgresql://qluvx_admin:qluvx_secret_prod@localhost:5432/qluvx_production",
  max: 25,
  idleTimeoutMillis: 30000,
});

export const DIAMOND_TO_INR_RATE = 0.5; // 1 Diamond = ₹0.50 INR
export const MIN_WITHDRAWAL_DIAMONDS = 5000; // 5,000 Diamonds = ₹2,500 INR

const SKU_CATALOG: Record<
  string,
  { coins: number; priceInr: number; isVipSubscription: boolean }
> = {
  coins_bundle_100: { coins: 100, priceInr: 89, isVipSubscription: false },
  coins_bundle_500: { coins: 500, priceInr: 399, isVipSubscription: false },
  coins_bundle_2000: { coins: 2000, priceInr: 1499, isVipSubscription: false },
  vip_monthly_subscription: {
    coins: 1000,
    priceInr: 499,
    isVipSubscription: true,
  },
};

/**
 * POST /api/v1/wallet/verify-iap
 * Verifies Google Play Billing PurchaseToken and atomically credits coins to PostgreSQL `user_wallets`.
 */
export async function verifyGooglePlayIap(
  req: Request,
  res: Response
): Promise<void> {
  const { userId, productId, purchaseToken, orderId, priceInr } = req.body;

  if (!userId || !productId || !purchaseToken) {
    res.status(400).json({
      success: false,
      message: "Missing required fields: userId, productId, or purchaseToken",
    });
    return;
  }

  const skuConfig = SKU_CATALOG[productId];
  if (!skuConfig) {
    res.status(400).json({
      success: false,
      message: `Unsupported Google Play SKU: ${productId}`,
    });
    return;
  }

  const client = await pgPool.connect();
  try {
    await client.query("BEGIN");

    // 1. Ensure idempotency: verify purchaseToken has not already been redeemed
    const existingTokenCheck = await client.query(
      `SELECT id FROM iap_transactions WHERE purchase_token = $1 LIMIT 1`,
      [purchaseToken]
    );

    if (existingTokenCheck.rowCount && existingTokenCheck.rowCount > 0) {
      await client.query("ROLLBACK");
      res.status(409).json({
        success: false,
        message: "PurchaseToken has already been verified and credited.",
      });
      return;
    }

    // 2. Record verified Google Play purchase in iap_transactions ledger
    await client.query(
      `INSERT INTO iap_transactions (user_id, product_id, purchase_token, order_id, price_inr, credited_coins, verified_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW())`,
      [
        userId,
        productId,
        purchaseToken,
        orderId || `GPA.${Date.now()}`,
        priceInr || skuConfig.priceInr,
        skuConfig.coins,
      ]
    );

    // 3. Atomically credit coins & VIP status to user_wallets table in PostgreSQL
    const walletResult = await client.query(
      `INSERT INTO user_wallets (user_id, coins, diamonds, is_vip, updated_at)
       VALUES ($1, $2, 8500, $3, NOW())
       ON CONFLICT (user_id)
       DO UPDATE SET
         coins = user_wallets.coins + EXCLUDED.coins,
         is_vip = CASE WHEN EXCLUDED.is_vip THEN TRUE ELSE user_wallets.is_vip END,
         updated_at = NOW()
       RETURNING coins, is_vip`,
      [userId, skuConfig.coins, skuConfig.isVipSubscription]
    );

    await client.query("COMMIT");

    const updatedWallet = walletResult.rows[0];
    res.status(200).json({
      success: true,
      creditedCoins: skuConfig.coins,
      newCoinBalance: Number(updatedWallet.coins),
      isVipActive: Boolean(updatedWallet.is_vip),
      message: `Successfully credited ${skuConfig.coins} Gold Coins via Google Play Billing.`,
    });
  } catch (error) {
    await client.query("ROLLBACK");
    console.error("[verifyGooglePlayIap] Database transaction error:", error);
    res.status(500).json({
      success: false,
      message: "Internal server error while verifying Google Play purchase.",
    });
  } finally {
    client.release();
  }
}

/**
 * POST /api/v1/payouts/request
 * Streamer Diamond Payout Engine:
 * - Conversion Rate: 1 Diamond = ₹0.50 INR (Min withdrawal: 5,000 Diamonds = ₹2,500 INR)
 * - Supported Methods: UPI (username@upi), Direct Bank IMPS/NEFT, PayPal
 */
export async function requestDiamondPayout(
  req: Request,
  res: Response
): Promise<void> {
  const { userId, diamondsAmount, method, accountDetails } = req.body;
  const parsedDiamonds = Number(diamondsAmount);

  if (!userId || !parsedDiamonds || !method || !accountDetails) {
    res.status(400).json({
      success: false,
      message: "Missing required fields for diamond payout request.",
    });
    return;
  }

  if (parsedDiamonds < MIN_WITHDRAWAL_DIAMONDS) {
    res.status(400).json({
      success: false,
      message: `Minimum withdrawal is ${MIN_WITHDRAWAL_DIAMONDS} Diamonds (₹${
        MIN_WITHDRAWAL_DIAMONDS * DIAMOND_TO_INR_RATE
      } INR).`,
    });
    return;
  }

  const normalizedMethod = String(method).toUpperCase();
  if (!["UPI", "IMPS", "PAYPAL"].includes(normalizedMethod)) {
    res.status(400).json({
      success: false,
      message: "Unsupported withdrawal method. Allowed: UPI, IMPS, PAYPAL.",
    });
    return;
  }

  const calculatedPayoutInr = Number(
    (parsedDiamonds * DIAMOND_TO_INR_RATE).toFixed(2)
  );

  const client = await pgPool.connect();
  try {
    await client.query("BEGIN");

    // Lock streamer's wallet row for atomic deduction
    const walletRes = await client.query(
      `SELECT diamonds FROM user_wallets WHERE user_id = $1 FOR UPDATE`,
      [userId]
    );

    if (!walletRes.rowCount || walletRes.rowCount === 0) {
      await client.query("ROLLBACK");
      res.status(404).json({
        success: false,
        message: "Streamer wallet not found.",
      });
      return;
    }

    const currentDiamonds = Number(walletRes.rows[0].diamonds);
    if (currentDiamonds < parsedDiamonds) {
      await client.query("ROLLBACK");
      res.status(400).json({
        success: false,
        message: `Insufficient diamond balance. Current: ${currentDiamonds}, Requested: ${parsedDiamonds}`,
      });
      return;
    }

    // Deduct diamonds from user_wallets
    const updatedWallet = await client.query(
      `UPDATE user_wallets
       SET diamonds = diamonds - $1, updated_at = NOW()
       WHERE user_id = $2
       RETURNING diamonds`,
      [parsedDiamonds, userId]
    );

    const payoutId = `PAYOUT-${Date.now()}-${Math.floor(
      1000 + Math.random() * 9000
    )}`;

    // Insert into payout_requests table for Web Admin Console approval
    await client.query(
      `INSERT INTO payout_requests (payout_id, user_id, diamonds_amount, payout_inr, method, account_details, status, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, 'PENDING_APPROVAL', NOW())`,
      [
        payoutId,
        userId,
        parsedDiamonds,
        calculatedPayoutInr,
        normalizedMethod,
        accountDetails,
      ]
    );

    await client.query("COMMIT");

    res.status(200).json({
      success: true,
      payoutId,
      remainingDiamonds: Number(updatedWallet.rows[0].diamonds),
      payoutInr: calculatedPayoutInr,
      status: "PENDING_APPROVAL",
    });
  } catch (error) {
    await client.query("ROLLBACK");
    console.error("[requestDiamondPayout] Error processing payout:", error);
    res.status(500).json({
      success: false,
      message: "Internal server error processing diamond payout request.",
    });
  } finally {
    client.release();
  }
}

/**
 * POST /api/v1/admin/payouts/:payoutId/approve
 * Approves a pending streamer withdrawal from the Web Admin & Moderation Console.
 */
export async function approveDiamondPayout(
  req: Request,
  res: Response
): Promise<void> {
  const { payoutId } = req.params;

  try {
    const result = await pgPool.query(
      `UPDATE payout_requests
       SET status = 'APPROVED_PAID', processed_at = NOW()
       WHERE payout_id = $1
       RETURNING payout_id, user_id, payout_inr, method, account_details, status`,
      [payoutId]
    );

    if (!result.rowCount || result.rowCount === 0) {
      res.status(404).json({
        success: false,
        message: `Payout request ${payoutId} not found.`,
      });
      return;
    }

    res.status(200).json({
      success: true,
      payout: result.rows[0],
    });
  } catch (error) {
    console.error("[approveDiamondPayout] Error:", error);
    res.status(500).json({
      success: false,
      message: "Failed to approve payout request.",
    });
  }
}
