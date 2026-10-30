package vn.autobuild;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * AutoBuild single-source prototype for Fabric 1.21.11.
 * This file provides the keybind and compact configuration screen.
 * Schematic parsing, pathfinding and actual block placement are not implemented yet.
 */
public final class AutoBuildClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuild.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AutoBuildScreen());
                }
            }
        });
    }

    /** The menu is nested here so the project needs only this one Java source file. */
    public static final class AutoBuildScreen extends Screen {
        private TextFieldWidget schematicField;
        private TextFieldWidget xField;
        private TextFieldWidget yField;
        private TextFieldWidget zField;
        private TextFieldWidget layerField;
        private int rotation = 0;
        private String status = "Chua chon ban thiet ke";

        public AutoBuildScreen() {
            super(Text.literal("AutoBuild"));
        }

        @Override
        protected void init() {
            int panelW = 286;
            int left = (this.width - panelW) / 2;
            int top = Math.max(12, (this.height - 260) / 2);

            schematicField = new TextFieldWidget(textRenderer, left + 12, top + 48,
                    panelW - 24, 20, Text.literal("Duong dan schematic"));
            schematicField.setMaxLength(240);
            schematicField.setPlaceholder(Text.literal("Chon file .litematic..."));
            addDrawableChild(schematicField);

            addDrawableChild(ButtonWidget.builder(Text.literal("CHON FILE .LITEMATIC"), b -> chooseSchematic())
                    .dimensions(left + 12, top + 76, 262, 20).build());

            xField = numberField(left + 12, top + 112, "X");
            yField = numberField(left + 102, top + 112, "Y");
            zField = numberField(left + 192, top + 112, "Z");

            layerField = new TextFieldWidget(textRenderer, left + 70, top + 151,
                    70, 20, Text.literal("Tang"));
            layerField.setText("0");
            layerField.setMaxLength(6);
            addDrawableChild(layerField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Xoay: " + rotation + "°"), b -> {
                rotation = (rotation + 90) % 360;
                b.setMessage(Text.literal("Xoay: " + rotation + "°"));
            }).dimensions(left + 154, top + 151, 120, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("DAT VI TRI"), b -> {
                if (validCoordinates()) status = "Vi tri: " + xField.getText() + ", " + yField.getText() + ", " + zField.getText();
                else status = "Toa do phai la so nguyen.";
            }).dimensions(left + 12, top + 183, 126, 22).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("XEM TRUOC"), b ->
                    status = "Preview can bo doc .litematic (chua tich hop)."
            ).dimensions(left + 148, top + 183, 126, 22).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("START BUILD"), b ->
                    status = "Chua xay: bo doc schematic va dat block chua duoc cai dat."
            ).dimensions(left + 12, top + 213, 126, 22).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("DONG"), b -> close())
                    .dimensions(left + 148, top + 213, 126, 22).build());
        }

        private void chooseSchematic() {
            // Swing file picker is opened on the AWT event thread.
            SwingUtilities.invokeLater(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Chon ban thiet ke Litematica");
                chooser.setFileFilter(new FileNameExtensionFilter("Litematica schematic (*.litematic)", "litematic"));
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();
                    // Screen widgets must be updated on Minecraft's client thread.
                    net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                        if (schematicField != null) schematicField.setText(selected.getAbsolutePath());
                        status = "Da chon: " + selected.getName() + " (chua doc du lieu).";
                    });
                }
            });
        }

        private TextFieldWidget numberField(int x, int y, String label) {
            TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 82, 20, Text.literal(label));
            field.setText("0");
            field.setMaxLength(11);
            addDrawableChild(field);
            return field;
        }

        private boolean validCoordinates() {
            try {
                Integer.parseInt(xField.getText());
                Integer.parseInt(yField.getText());
                Integer.parseInt(zField.getText());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            int panelW = 286;
            int panelH = 260;
            int left = (this.width - panelW) / 2;
            int top = Math.max(12, (this.height - panelH) / 2);
            context.fill(left, top, left + panelW, top + panelH, 0xEE171C22);
            context.fill(left, top, left + panelW, top + 3, 0xFF35C6A3);
            context.drawText(textRenderer, "AUTOBUILD", left + 12, top + 12, 0xFF48D7B2, false);
            context.drawText(textRenderer, "SCHEMATIC (.LITEMATIC)", left + 12, top + 36, 0xFFE5E9EF, false);
            context.drawText(textRenderer, "TOA DO X / Y / Z", left + 12, top + 101, 0xFFE5E9EF, false);
            context.drawText(textRenderer, "Tang:", left + 12, top + 157, 0xFFE5E9EF, false);
            context.drawText(textRenderer, trimStatus(status, 42), left + 12, top + 244, 0xFFFFD479, false);
            super.render(context, mouseX, mouseY, delta);
        }

        private String trimStatus(String value, int max) {
            return value.length() <= max ? value : value.substring(0, max - 3) + "...";
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
