package org.BetterClimbing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class Better_climbing implements ClientModInitializer {

    private int lastSelectedSlot = -1;
    private boolean isAutoAttacking = false;

    // Debounce per i tasti di configurazione
    private boolean wasAttributeSwapKeyPressed = false;
    private boolean wasShieldBreakKeyPressed = false;

    // Variabili per il delay dello swap-back (0.2s = 4 tick)
    private int swapBackDelay = -1;
    private int originalSlotToReturn = -1;

    @Override
    public void onInitializeClient() {
        System.out.println("[Better Climbing] Avvio (Versione Pulita)...");
        registerEvents();
    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player != null) {

                // --- GESTIONE DEL DELAY DI RITORNO ALLO SLOT ORIGINALE (0.2 SECONDI = 4 TICKS) ---
                if (swapBackDelay > 0) {
                    swapBackDelay--;
                    if (swapBackDelay == 0 && originalSlotToReturn != -1) {
                        changeSlot(player, originalSlotToReturn);
                        originalSlotToReturn = -1;
                    }
                }

                // --- GESTIONE DEI KEYBINDS NASCOSTI ---
                long windowHandle = client.getWindow().getHandle();

                boolean attrPressed = isKeyActive(windowHandle, ModConfig.toggleAttributeSwapKey);
                if (attrPressed && !wasAttributeSwapKeyPressed) {
                    ModConfig.isAttributeSwapEnabled = !ModConfig.isAttributeSwapEnabled;
                    player.sendMessage(Text.literal("Attribute Swap (Mace): " + (ModConfig.isAttributeSwapEnabled ? "§aON" : "§cOFF")), true);
                }
                wasAttributeSwapKeyPressed = attrPressed;

                boolean shieldPressed = isKeyActive(windowHandle, ModConfig.toggleShieldBreakKey);
                if (shieldPressed && !wasShieldBreakKeyPressed) {
                    ModConfig.isShieldBreakEnabled = !ModConfig.isShieldBreakEnabled;
                    player.sendMessage(Text.literal("Auto Shield Break: " + (ModConfig.isShieldBreakEnabled ? "§aON" : "§cOFF")), true);
                }
                wasShieldBreakKeyPressed = shieldPressed;

                // --- DETECT HOTBAR ---
                int currentSlot = player.getInventory().getSelectedSlot();
                if (currentSlot != lastSelectedSlot) {
                    lastSelectedSlot = currentSlot;
                }
            }
        });

        // --- GESTIONE ATTACCHI DIRETTI SULLE ENTITA ---
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() || isAutoAttacking) {
                return ActionResult.PASS;
            }

            ClientPlayerEntity clientPlayer = (ClientPlayerEntity) player;
            ItemStack mainHand = clientPlayer.getMainHandStack();

            // --- LOGICA STANDARD MACRO (Spade, Asce, Mazze) ---
            boolean isSword = mainHand.isIn(ItemTags.SWORDS);
            boolean isAxe = mainHand.isIn(ItemTags.AXES);
            boolean isMace = mainHand.getItem() instanceof MaceItem;

            if (!isSword && !isAxe && !isMace) {
                return ActionResult.PASS;
            }

            if (!ModConfig.isAttributeSwapEnabled && !ModConfig.isShieldBreakEnabled) {
                return ActionResult.PASS;
            }

            int axeSlot = -1, maceDensitySlot = -1, maceBreachSlot = -1, anyMaceSlot = -1;
            int originalSlot = clientPlayer.getInventory().getSelectedSlot();

            for (int i = 0; i < 9; i++) {
                ItemStack stack = clientPlayer.getInventory().getStack(i);
                if (stack.isEmpty()) continue;

                if (stack.isIn(ItemTags.AXES) && axeSlot == -1) {
                    axeSlot = i;
                }
                else if (stack.getItem() instanceof MaceItem) {
                    if (anyMaceSlot == -1) anyMaceSlot = i;
                    ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
                    if (enchants != null && !enchants.isEmpty()) {
                        for (var entry : enchants.getEnchantmentEntries()) {
                            String enchantName = entry.getKey().getIdAsString().toLowerCase();
                            if (enchantName.contains("density") && maceDensitySlot == -1) maceDensitySlot = i;
                            if (enchantName.contains("breach") && maceBreachSlot == -1) maceBreachSlot = i;
                        }
                    }
                }
            }

            double highestPoint = clientPlayer.getY() + clientPlayer.fallDistance;
            double effectiveFallDist = highestPoint - entity.getY();

            boolean isShieldUp = entity instanceof LivingEntity living && living.isBlocking();
            int targetMaceSlot = -1;

            if (effectiveFallDist > 8.0f) {
                targetMaceSlot = (maceDensitySlot != -1) ? maceDensitySlot : anyMaceSlot;
            } else {
                targetMaceSlot = (maceBreachSlot != -1) ? maceBreachSlot : anyMaceSlot;
            }

            // --- FIX TERRA / SALTO: Accetta lo swap solo se la mace ha Breach ---
            if (clientPlayer.isOnGround() || clientPlayer.fallDistance <= 0.0f) {
                targetMaceSlot = maceBreachSlot;
            }

            boolean attackOverridden = false;
            isAutoAttacking = true;

            boolean failShieldBreak = (Math.random() * 100.0) < ModConfig.shieldBreakFailChance;
            boolean failAttributeSwap = (Math.random() * 100.0) < ModConfig.attributeSwapFailChance;

            // SCUDO ALZATO: Arma -> Axe (Break) -> Arma -> Mace (Danno) -> Arma (Delay)
            if (isShieldUp && ModConfig.isShieldBreakEnabled && !failShieldBreak) {
                int breakerSlot = (axeSlot != -1) ? axeSlot : targetMaceSlot;
                if (breakerSlot == -1 && isMace) {
                    breakerSlot = originalSlot;
                }

                if (breakerSlot != -1) {
                    unequipElytra(clientPlayer);

                    changeSlot(clientPlayer, breakerSlot);
                    attackTarget(entity);

                    if (breakerSlot == axeSlot && targetMaceSlot != -1 && ModConfig.isAttributeSwapEnabled && !failAttributeSwap) {
                        changeSlot(clientPlayer, originalSlot);
                        changeSlot(clientPlayer, targetMaceSlot);
                        attackTarget(entity);
                    }

                    swapBackDelay = 4;
                    originalSlotToReturn = originalSlot;
                    attackOverridden = true;
                }
            }
            // SCUDO ABBASSATO: Arma -> Mace (Danno) -> Arma (Delay)
            else if (ModConfig.isAttributeSwapEnabled && !failAttributeSwap) {
                if (targetMaceSlot != -1) {
                    unequipElytra(clientPlayer);

                    changeSlot(clientPlayer, targetMaceSlot);
                    attackTarget(entity);

                    swapBackDelay = 4;
                    originalSlotToReturn = originalSlot;
                    attackOverridden = true;
                } else if (isMace || isAxe) {
                    unequipElytra(clientPlayer);
                    attackTarget(entity);
                    attackOverridden = true;
                }
            }

            if (!attackOverridden && (isMace || isAxe)) {
                unequipElytra(clientPlayer);
            }

            isAutoAttacking = false;
            if (attackOverridden) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }

    private void unequipElytra(ClientPlayerEntity player) {
        ItemStack currentChest = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (currentChest.isOf(Items.ELYTRA)) {
            int chestplateSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.isIn(net.minecraft.registry.tag.ItemTags.CHEST_ARMOR)) {
                    chestplateSlot = i;
                    break;
                }
            }
            if (chestplateSlot != -1) {
                MinecraftClient client = MinecraftClient.getInstance();
                changeSlot(player, chestplateSlot);
                if (client.interactionManager != null) {
                    client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                }
            }
        }
    }

    private boolean isKeyActive(long windowHandle, InputUtil.Key key) {
        if (key == InputUtil.UNKNOWN_KEY) return false;
        if (key.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(windowHandle, key.getCode()) == GLFW.GLFW_PRESS;
        } else if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    public static void changeSlot(PlayerEntity player, int slot) {
        if (slot >= 0 && slot <= 8) {
            player.getInventory().setSelectedSlot(slot);
        }
    }

    public static void attackTarget(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && target != null && client.interactionManager != null) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }
}