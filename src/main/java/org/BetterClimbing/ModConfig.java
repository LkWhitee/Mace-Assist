package org.BetterClimbing;

import net.minecraft.client.util.InputUtil;

public class ModConfig {
    // Stato delle funzioni
    public static boolean isAttributeSwapEnabled = true;
    public static boolean isShieldBreakEnabled = true;
    public static boolean isAimAssistEnabled = true;

    // Percentuali di errore (Fail chance - anti sgamo) da 0 a 100
    public static int attributeSwapFailChance = 8;
    public static int shieldBreakFailChance = 8;

    // Tasti per il toggle e le macro
    public static InputUtil.Key toggleAttributeSwapKey = InputUtil.UNKNOWN_KEY;
    public static InputUtil.Key toggleShieldBreakKey = InputUtil.UNKNOWN_KEY;
    public static InputUtil.Key toggleAimAssistKey = InputUtil.UNKNOWN_KEY;

    // NUOVI TASTI AGGIUNTI
    public static InputUtil.Key enderpearlKey = InputUtil.UNKNOWN_KEY;
    public static InputUtil.Key windchargeKey = InputUtil.UNKNOWN_KEY;
}