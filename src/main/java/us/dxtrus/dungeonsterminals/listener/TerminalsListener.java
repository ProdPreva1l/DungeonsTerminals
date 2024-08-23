package us.dxtrus.dungeonsterminals.listener;

import net.playavalon.mythicdungeons.MythicDungeons;
import net.playavalon.mythicdungeons.api.events.dungeon.RemoteTriggerEvent;
import net.playavalon.mythicdungeons.dungeons.triggers.TriggerRemote;
import net.playavalon.mythicdungeons.player.MythicPlayer;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.dxtrus.commons.cooldowns.Cooldown;
import us.dxtrus.commons.cooldowns.CooldownReponse;
import us.dxtrus.commons.utils.StringUtils;
import us.dxtrus.dungeonsterminals.api.TerminalCompleteEvent;
import us.dxtrus.dungeonsterminals.data.CacheManager;
import us.dxtrus.dungeonsterminals.guis.MemorizeGUI;
import us.dxtrus.dungeonsterminals.guis.SwitchGUI;

public class TerminalsListener implements Listener {
    private final JavaPlugin plugin;

    public TerminalsListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent e) {
        CooldownReponse cdR = Cooldown.localFromClass(TerminalsListener.class, e.getPlayer().getUniqueId(), 5L).execute();
        if (!cdR.shouldContinue()) return;

        Block block = e.getClickedBlock();
        if (block == null) return;
        MythicPlayer player = MythicDungeons.inst().getMythicPlayer(e.getPlayer());
        if (player.getInstance() == null || !player.getInstance().isStarted()) return;
        CacheManager.getInstance().get(block.getLocation()).ifPresent(terminal -> {
            if (!player.getInstance().getDungeon().getFolder().getName().equals(terminal.getAssociatedDungeon())) return;
            Cooldown cd = Cooldown.local("terminal_fail", player.getPlayer().getUniqueId(), 600L);
            if (cd.isActive()) {
                player.getPlayer().sendMessage(StringUtils.modernMessage("&cTerminal on cooldown! &7(Wait: %s seconds)"
                        .formatted(cd.remainingTime() / 1000D)));
                return;
            }
            switch (terminal.getType()) {
                case MEMORIZE -> new MemorizeGUI(terminal, player.getPlayer(), plugin).open(player.getPlayer());
                case SWITCHES -> new SwitchGUI(terminal, player.getPlayer(), plugin).open(player.getPlayer());
                default -> throw new NotImplementedException();
            }
        });
    }

    @EventHandler
    public void onTerminalComplete(TerminalCompleteEvent event) {
        MythicPlayer mythicPlayer = MythicDungeons.inst().getMythicPlayer(event.getPlayer());

        if (mythicPlayer.getInstance() == null) return;
        if (!event.getTerminal().getAssociatedDungeon().equals(mythicPlayer.getInstance().getDungeon().getFolder().getName())) return;

        TriggerRemote remoteTrig = new TriggerRemote();
        remoteTrig.setTriggerName(event.getTerminal().getId());
        new RemoteTriggerEvent(remoteTrig.getTriggerName(), remoteTrig, mythicPlayer.getInstance()).callEvent();
//        TerminalTrigger terminalTrigger = new TerminalTrigger();
//        terminalTrigger.setTerminalId(event.getTerminal().getId());
//        terminalTrigger.trigger(mythicPlayer);
    }
}
