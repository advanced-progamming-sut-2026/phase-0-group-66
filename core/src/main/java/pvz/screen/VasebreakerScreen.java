package pvz.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.MiniGamePlantSnapshot;
import model.VasebreakerSession;
import pvz.PvzApplication;
import pvz.ui.MiniGameUnitLayer;
import pvz.ui.PlantArtResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VasebreakerScreen extends MiniGamePlayScreen {
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final float BOARD_WIDTH = 835f;
    private static final float BOARD_HEIGHT = 470f;
    private static final String NORMAL_VASE =
        "IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150";
    private static final String PLANT_VASE =
        "IMAGE_VASEBREAKER_VASE_GREEN_VASE_GREEN_115X150";
    private static final String GIANT_VASE =
        "IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_115X150";

    private final VasebreakerSession vaseSession;
    private final MiniGameUnitLayer units;
    private final Group vaseLayer;
    private final Table packetTray;
    private final Label progress;
    private final Map<Integer, TextButton> packetButtons = new HashMap<>();
    private int selectedPacket = -1;

    public VasebreakerScreen(PvzApplication app) {
        super(app);
        vaseSession = (VasebreakerSession) session;
        units = new MiniGameUnitLayer(app);
        vaseLayer = new Group();
        packetTray = new Table();
        progress = theme.settingsLabel("");
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(32f, 38f, 18f, 38f);
        screen.add(titleBar("VASEBREAKER - LEVEL " + session.getLevel()))
            .colspan(2).width(1190f).height(52f).padBottom(10f);
        screen.row();

        screen.add(buildSidePanel()).width(300f).height(545f).padRight(12f);
        screen.add(buildBoard()).width(BOARD_WIDTH).height(BOARD_HEIGHT).top();
        screen.row();

        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1040f).height(28f).padTop(5f);
        root.add(screen).grow();
    }

    private Table buildSidePanel() {
        Table panel = theme.settingsCardPanel(13f);
        panel.top();
        panel.add(theme.heading("PLANT PACKETS")).padBottom(8f);
        panel.row();
        Label hint = theme.bodyLabel(
            "Break a vase by clicking it. Select a temporary plant packet, then click "
                + "an empty tile in columns 1 to 6 to plant it."
        );
        hint.setAlignment(Align.left);
        hint.setWrap(true);
        hint.setFontScale(0.72f);
        panel.add(hint).width(266f).height(94f).left();
        panel.row().padTop(8f);
        panel.add(packetTray).width(270f).expandY().top();
        panel.row().padTop(6f);
        progress.setAlignment(Align.center);
        panel.add(progress).width(270f).height(44f);
        panel.row().padTop(8f);
        TextButton back = theme.secondaryButton("Back to Mini Games");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(250f).height(52f);
        return panel;
    }

    private Stack buildBoard() {
        Stack board = new Stack();
        Image background = theme.image("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
        if (background != null) {
            background.setScaling(Scaling.stretch);
            board.add(background);
        }
        board.add(units);
        board.add(vaseLayer);
        board.add(interactionGrid());
        return board;
    }

    private Table interactionGrid() {
        Table grid = new Table();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Button cell = new Button(new Button.ButtonStyle());
                int x = col + 1;
                int y = row + 1;
                UiActions.onClick(cell, () -> handleCell(x, y));
                grid.add(cell).width(BOARD_WIDTH / COLS).height(BOARD_HEIGHT / ROWS);
            }
            grid.row();
        }
        return grid;
    }

    private void handleCell(int x, int y) {
        if (session.isFinished()) {
            return;
        }
        if (selectedPacket > 0) {
            int packet = selectedPacket;
            execute("plant " + packet + " " + x + " " + y);
            if (vaseSession.getPacketViews().stream().noneMatch(view -> view.id() == packet)) {
                selectedPacket = -1;
            }
        } else {
            execute("break " + x + " " + y);
        }
    }

    @Override
    protected void refreshFromSession() {
        units.setPlants(vaseSession.getPlantViews());
        units.setZombies(vaseSession.getZombieViews());
        rebuildVases(vaseSession.getVaseViews());
        rebuildPackets(vaseSession.getPacketViews());
        progress.setText(
            "Vases " + vaseSession.getBrokenVases() + " / " + session.getTarget()
                + "   |   Kills " + vaseSession.getKilledZombies()
        );
    }

    private void rebuildVases(List<VasebreakerSession.VaseView> vases) {
        vaseLayer.clearChildren();
        float cellWidth = BOARD_WIDTH / COLS;
        float cellHeight = BOARD_HEIGHT / ROWS;
        for (VasebreakerSession.VaseView vase : vases) {
            Image image = theme.image(vaseImage(vase.kind()));
            if (image == null) {
                continue;
            }
            image.setScaling(Scaling.fit);
            float width = cellWidth * 0.80f;
            float height = cellHeight * 0.94f;
            float x = vase.column() * cellWidth + (cellWidth - width) * 0.5f;
            float y = (ROWS - 1 - vase.row()) * cellHeight + (cellHeight - height) * 0.5f;
            image.setBounds(x, y, width, height);
            vaseLayer.addActor(image);
        }
    }

    private String vaseImage(String kind) {
        return switch (kind) {
            case "PLANT" -> PLANT_VASE;
            case "GIANT" -> GIANT_VASE;
            default -> NORMAL_VASE;
        };
    }

    private void rebuildPackets(List<VasebreakerSession.PacketView> packets) {
        packetTray.clearChildren();
        packetButtons.clear();
        if (packets.isEmpty()) {
            Label empty = theme.bodyLabel("No temporary packets available.");
            empty.setFontScale(0.72f);
            packetTray.add(empty).width(250f).padTop(12f);
            return;
        }
        for (VasebreakerSession.PacketView packet : packets) {
            Table row = theme.settingsBadgePanel(5f);
            Image plant = packetImage(packet.plantType());
            if (plant != null) {
                row.add(plant).size(52f).padRight(5f);
            }
            TextButton select = packet.id() == selectedPacket
                ? theme.tertiaryButton("#" + packet.id() + " SELECTED")
                : theme.primaryButton("#" + packet.id() + " " + packet.plantType());
            select.getLabel().setFontScale(0.72f);
            UiActions.onClick(select, () -> selectPacket(packet.id()));
            row.add(select).width(188f).height(45f);
            packetTray.add(row).width(260f).height(58f).padBottom(5f);
            packetTray.row();
            packetButtons.put(packet.id(), select);
        }
    }

    private Image packetImage(String type) {
        return app.services().gameData().getPlantFactory().findDefinition(type)
            .map(definition -> PlantArtResolver.packetImage(theme, definition))
            .orElse(null);
    }

    private void selectPacket(int packetId) {
        selectedPacket = selectedPacket == packetId ? -1 : packetId;
        rebuildPackets(vaseSession.getPacketViews());
        if (selectedPacket > 0) {
            theme.showSuccess(message, "Packet #" + selectedPacket + " selected. Choose a tile.");
        } else {
            message.setText(shortStatus());
        }
    }
}
