package pvz.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.VasebreakerSession;
import pvz.PvzApplication;
import pvz.ui.PlantArtResolver;
import pvz.ui.VasebreakerBoardActor;

import java.util.List;

/** Dedicated, fixed-grid Vasebreaker play page. */
public final class VasebreakerScreen extends MiniGamePlayScreen {
    private final VasebreakerSession vaseSession;
    private final VasebreakerBoardActor board;
    private final Table packetTray;
    private final ScrollPane packetScroll;
    private final Label progress;
    private int selectedPacket = -1;

    public VasebreakerScreen(PvzApplication app) {
        super(app);
        vaseSession = (VasebreakerSession) session;
        board = new VasebreakerBoardActor(app, this::handleCell);
        packetTray = new Table();
        packetScroll = new ScrollPane(packetTray, theme.skin());
        packetScroll.setFadeScrollBars(false);
        packetScroll.setOverscroll(false, false);
        progress = theme.settingsLabel("");
        buildUi();
        refreshFromSession();
    }

    private void buildUi() {
        Table screen = new Table();
        screen.top().pad(20f, 30f, 14f, 30f);
        screen.add(titleBar("VASEBREAKER  •  LEVEL " + session.getLevel()))
            .colspan(2).width(1220f).height(54f).padBottom(10f);
        screen.row();
        screen.add(buildSidePanel()).width(286f).height(550f).padRight(14f);
        screen.add(board).width(900f).height(500f).top();
        screen.row();
        message.setAlignment(Align.center);
        screen.add(message).colspan(2).width(1120f).height(30f).padTop(6f);
        root.add(screen).grow();
    }

    private Table buildSidePanel() {
        Table panel = theme.settingsCardPanel(12f);
        panel.top();
        Label heading = theme.heading("PLANT PACKETS");
        heading.setAlignment(Align.center);
        panel.add(heading).width(258f).height(38f).padBottom(4f);
        panel.row();
        Label hint = theme.bodyLabel(
            "Break a vase to reveal a plant packet or zombie. "
                + "Select a packet, then click any cleared tile to plant it."
        );
        hint.setAlignment(Align.left);
        hint.setWrap(true);
        hint.setFontScale(0.58f);
        panel.add(hint).width(258f).height(72f).left();
        panel.row().padTop(5f);
        panel.add(packetScroll).width(262f).height(300f).top();
        panel.row().padTop(6f);
        progress.setAlignment(Align.center);
        progress.setWrap(true);
        panel.add(progress).width(262f).height(40f);
        panel.row().padTop(6f);
        TextButton back = theme.secondaryButton("BACK TO MINI GAMES");
        UiActions.onClick(back, app::returnToMiniGames);
        panel.add(back).width(252f).height(48f);
        return panel;
    }

    private void handleCell(VasebreakerBoardActor.Cell cell) {
        if (session.isFinished()) {
            return;
        }
        int x = cell.column() + 1;
        int y = cell.row() + 1;
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
        board.setState(vaseSession.getVaseViews(), vaseSession.getPlantViews(),
            vaseSession.getZombieViews());
        rebuildPackets(vaseSession.getPacketViews());
        progress.setText(
            "Vases broken: " + vaseSession.getBrokenVases() + " / " + session.getTarget()
                + "\nZombies defeated: " + vaseSession.getKilledZombies()
        );
    }

    private void rebuildPackets(List<VasebreakerSession.PacketView> packets) {
        packetTray.clearChildren();
        if (selectedPacket > 0 && packets.stream().noneMatch(packet -> packet.id() == selectedPacket)) {
            selectedPacket = -1;
        }
        if (packets.isEmpty()) {
            Label empty = theme.bodyLabel("No plant packets revealed yet.");
            empty.setFontScale(0.70f);
            packetTray.add(empty).width(248f).padTop(10f);
            return;
        }
        for (VasebreakerSession.PacketView packet : packets) {
            Table row = theme.settingsBadgePanel(5f);
            Image plant = packetImage(packet.plantType());
            if (plant != null) {
                plant.setScaling(Scaling.fit);
                row.add(plant).size(48f).padRight(4f);
            }
            TextButton select = packet.id() == selectedPacket
                ? theme.tertiaryButton(packetLabel(packet, true))
                : theme.primaryButton(packetLabel(packet, false));
            select.getLabel().setFontScale(0.66f);
            UiActions.onClick(select, () -> selectPacket(packet.id()));
            row.add(select).width(184f).height(43f);
            packetTray.add(row).width(250f).height(54f).padBottom(4f);
            packetTray.row();
        }
    }

    private String packetLabel(VasebreakerSession.PacketView packet, boolean selected) {
        return "#" + packet.id() + " " + (selected ? "SELECTED" : packet.plantType());
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
