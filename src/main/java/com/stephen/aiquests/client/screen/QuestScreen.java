package com.stephen.aiquests.client.screen;

import com.stephen.aiquests.network.NetworkHandler;
import com.stephen.aiquests.network.QuestScreenClosedC2SPacket;
import com.stephen.aiquests.network.RerollQuestsC2SPacket;
import com.stephen.aiquests.network.SelectQuestC2SPacket;
import com.stephen.aiquests.quest.ItemFetchQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class QuestScreen extends Screen {

    private final int entityId;
    private List<ItemFetchQuest> options;

    public QuestScreen(int entityId, List<ItemFetchQuest> options) {
        super(Component.literal("Quests"));
        this.entityId = entityId;
        this.options = options;
    }

    @Override
    protected void init() {
        super.init();

        int imageWidth = 300;
        int imageHeight = 180;
        int left = (this.width - imageWidth) / 2;
        int top = (this.height - imageHeight) / 2;

        int buttonWidth = 90;
        int buttonHeight = 20;
        int spacing = 4;

        // Place pick buttons near the bottom of the panel so they don't overlap text
        int buttonsTop = top + 70;
        for (int i = 0; i < options.size(); i++) {
            int y = buttonsTop + i * (buttonHeight + spacing);
            int index = i;
            this.addRenderableWidget(Button.builder(
                            Component.literal("Pick quest " + (i + 1)),
                            btn -> {
                                NetworkHandler.CHANNEL.sendToServer(new SelectQuestC2SPacket(entityId, index));
                                Minecraft.getInstance().setScreen(null);
                            })
                    .bounds(left + 10, y, buttonWidth, buttonHeight)
                    .build());
        }

        // Reroll button at the very bottom, under the pick buttons
        this.addRenderableWidget(Button.builder(
                        Component.literal("Reroll quests (1 diamond)"),
                        btn -> NetworkHandler.CHANNEL.sendToServer(new RerollQuestsC2SPacket(entityId)))
                .bounds(left + 10, top + imageHeight - 30, imageWidth - 20, buttonHeight)
                .build());
    }

    public void updateOptions(List<ItemFetchQuest> newOptions) {
        this.options = newOptions;
        this.clearWidgets();
        this.init();
    }

    @Override
    public void onClose() {
        super.onClose();
        NetworkHandler.CHANNEL.sendToServer(new QuestScreenClosedC2SPacket(entityId));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int imageWidth = 300;
        int imageHeight = 180;
        int left = (this.width - imageWidth) / 2;
        int top = (this.height - imageHeight) / 2;

        // Draw a clean white/gray panel with a darker border
        int backgroundColor = 0xF0F0F0F0; // ARGB light gray/white
        int borderColor = 0xFF404040;     // dark gray

        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, backgroundColor);
        guiGraphics.fill(left, top, left + imageWidth, top + 1, borderColor);
        guiGraphics.fill(left, top + imageHeight - 1, left + imageWidth, top + imageHeight, borderColor);
        guiGraphics.fill(left, top, left + 1, top + imageHeight, borderColor);
        guiGraphics.fill(left + imageWidth - 1, top, left + imageWidth, top + imageHeight, borderColor);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, left + imageWidth / 2, top + 6, 0x404040);

        // Draw each quest's full text on the left so it doesn't scroll/clamp
        int textX = left + 10;
        int textY = top + 28;
        int lineHeight = 14;

        for (int i = 0; i < options.size(); i++) {
            String text = (i + 1) + ") " + options.get(i).getDisplayText();
            guiGraphics.drawString(this.font, text, textX, textY + i * lineHeight, 0x202020, false);
        }
    }
}

