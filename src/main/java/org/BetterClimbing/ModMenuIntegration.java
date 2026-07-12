package org.BetterClimbing;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Better Climbing Settings"));

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("Generale"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Slider: Possibilità di fallire lo Shield Break (0-100%)
            general.addEntry(entryBuilder.startIntSlider(Text.literal("Fail Chance % (Shield Break)"), ModConfig.shieldBreakFailChance, 0, 100)
                    .setDefaultValue(8)
                    .setSaveConsumer(newValue -> ModConfig.shieldBreakFailChance = newValue)
                    .build());

            // Slider: Possibilità di fallire l'Attribute Swap (0-100%)
            general.addEntry(entryBuilder.startIntSlider(Text.literal("Fail Chance % (Attribute Swap)"), ModConfig.attributeSwapFailChance, 0, 100)
                    .setDefaultValue(8)
                    .setSaveConsumer(newValue -> ModConfig.attributeSwapFailChance = newValue)
                    .build());

            // Tasto: Attribute Swap (Mace)
            general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Tasto: Attribute Swap (Mace)"), ModConfig.toggleAttributeSwapKey)
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(newValue -> ModConfig.toggleAttributeSwapKey = newValue)
                    .build());

            // Tasto: Shield Break (Ascia)
            general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Tasto: Auto Shield Break (Ascia)"), ModConfig.toggleShieldBreakKey)
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(newValue -> ModConfig.toggleShieldBreakKey = newValue)
                    .build());

            // Tasto: Aim Assist
            general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Tasto: Attiva/Disattiva Aim Assist"), ModConfig.toggleAimAssistKey)
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(newValue -> ModConfig.toggleAimAssistKey = newValue)
                    .build());

            // --- NUOVI CAMPI PER ENDERPEARL E WIND CHARGE ---

            // Tasto: Macro Ender Pearl
            general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Tasto: Macro Ender Pearl"), ModConfig.enderpearlKey)
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(newValue -> ModConfig.enderpearlKey = newValue)
                    .build());

            // Tasto: Wind Charge
            general.addEntry(entryBuilder.startKeyCodeField(Text.literal("Tasto: Wind Charge"), ModConfig.windchargeKey)
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(newValue -> ModConfig.windchargeKey = newValue)
                    .build());

            return builder.build();
        };
    }
}