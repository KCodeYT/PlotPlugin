/*
 * Copyright 2022 KCodeYT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ms.kevi.plotplugin.listener;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.*;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.level.StructureGrowEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import lombok.RequiredArgsConstructor;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.event.PlotEnterEvent;
import ms.kevi.plotplugin.event.PlotLeaveEvent;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.PlotConfig;
import ms.kevi.plotplugin.util.ShapeType;
import ms.kevi.plotplugin.util.Utils;

import java.util.ArrayList;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@RequiredArgsConstructor
public class PlotListener implements Listener {

    private final PlotPlugin plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.registerPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.place")) {
            final int x = event.getBlock().getFloorX();
            final int z = event.getBlock().getFloorZ();
            final Plot plot = plotManager.getMergedPlot(x, z);

            if(plot != null) {
                if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                    event.setCancelled(true);

                if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlock()) < 5) {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.TOO_CLOSE_TO_HOME));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.break")) {
            final int x = event.getBlock().getFloorX();
            final int z = event.getBlock().getFloorZ();
            final Plot plot = plotManager.getMergedPlot(x, z);

            if(plot != null) {
                if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                    event.setCancelled(true);

                if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlock()) < 5) {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.TOO_CLOSE_TO_HOME));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.bucket.emtpy")) {
            final int x = event.getBlockClicked().getFloorX();
            final int z = event.getBlockClicked().getFloorZ();
            final Plot plot = plotManager.getMergedPlot(x, z);

            if(plot != null) {
                if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                    event.setCancelled(true);

                if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlockClicked()) < 5) {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.TOO_CLOSE_TO_HOME));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.bucket.fill")) {
            final int x = event.getBlockClicked().getFloorX();
            final int z = event.getBlockClicked().getFloorZ();
            final Plot plot = plotManager.getMergedPlot(x, z);

            if(plot != null) {
                if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                    event.setCancelled(true);

                if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlockClicked()) < 5) {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.TOO_CLOSE_TO_HOME));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.interact")) {
            final Block block = event.getBlock();
            final Item item = event.getItem();

            if(event.getAction() == PlayerInteractEvent.Action.PHYSICAL && block != null) {
                final Plot plot = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());

                if(plot != null) {
                    if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                        event.setCancelled(true);
                }

                return;
            }

            if((block != null && ((!player.isSneaking() || item == null || item.isNull()) && block.canBeActivated())) || (item != null && item.canBeActivated())) {
                final int x = (block == null || block.isAir() ? player : block).getFloorX();
                final int z = (block == null || block.isAir() ? player : block).getFloorZ();
                final Plot plot = plotManager.getMergedPlot(x, z);

                if(plot != null) {
                    if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                        event.setCancelled(true);

                    if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlock()) < 5) {
                        event.setCancelled(true);
                        player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.TOO_CLOSE_TO_HOME));
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInteractWithEntity(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && !player.hasPermission("plot.admin.interact")) {
            final Entity entity = event.getEntity();
            final Plot plot = plotManager.getMergedPlot(entity.getFloorX(), entity.getFloorZ());

            if(plot != null) {
                if(!plot.isOwner(player.getUniqueId()) && !plot.isHelper(player.getUniqueId()) && !plot.isHelper(Utils.UUID_EVERYONE))
                    event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());

        if(plotManager != null && event.getFrom() != null) {
            final Plot plotFrom = plotManager.getMergedPlot(event.getFrom().getFloorX(), event.getFrom().getFloorZ());
            final Plot plotTo = plotManager.getMergedPlot(event.getTo().getFloorX(), event.getTo().getFloorZ());

            if(plotTo != null) {
                if((plotTo.isDenied(player.getUniqueId()) || plotTo.isDenied(Utils.UUID_EVERYONE)) && !player.hasPermission("plot.admin.bypass.deny")) {
                    event.setCancelled(true);
                    return;
                }

                if(plotFrom == null) {
                    final PlotEnterEvent plotEnterEvent = new PlotEnterEvent(player, plotTo);
                    this.plugin.getServer().getPluginManager().callEvent(plotEnterEvent);
                    if(plotEnterEvent.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }

                    if(!plotTo.hasOwner())
                        player.sendActionBar(this.plugin.getLanguage().translate(player, TranslationKey.PLOT_POPUP_NO_OWNER));
                    else
                        player.sendActionBar(this.plugin.getLanguage().translate(player, TranslationKey.PLOT_POPUP_OWNER, this.plugin.getCorrectName(plotTo.getOwner())));
                }
            } else if(plotFrom != null) {
                final PlotLeaveEvent plotLeaveEvent = new PlotLeaveEvent(player, plotFrom);
                this.plugin.getServer().getPluginManager().callEvent(plotLeaveEvent);
                if(plotLeaveEvent.isCancelled())
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        final PlotManager plotManager = this.plugin.getPlotManager(entity.getLevel());

        if(event instanceof EntityDamageByEntityEvent)
            return;

        if(plotManager != null) {
            Plot plot = plotManager.getMergedPlot(entity.getFloorX(), entity.getFloorZ());
            if(plot != null) {
                if(!((boolean) PlotConfig.ConfigEnum.DAMAGE.getConfig().get(plot)))
                    event.setCancelled(true);
            } else
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        final PlotManager plotManager = this.plugin.getPlotManager(entity.getLevel());

        if(plotManager != null) {
            final Plot plot = plotManager.getMergedPlot(entity.getFloorX(), entity.getFloorZ());
            damager = damager instanceof EntityProjectile && ((EntityProjectile) damager).shootingEntity != null ? ((EntityProjectile) damager).shootingEntity : damager;

            if(plot != null) {
                if(!((damager instanceof Player && (((Player) damager).hasPermission("plot.admin.damage")) || (entity instanceof Player ? ((boolean) PlotConfig.ConfigEnum.PVP.getConfig().get(plot)) : ((boolean) PlotConfig.ConfigEnum.PVE.getConfig().get(plot))) || (!(entity instanceof Player) && damager instanceof Player && plot.isOwner(damager.getUniqueId())))))
                    event.setCancelled(true);
            } else if(!(damager instanceof Player) || !((Player) damager).hasPermission("plot.admin.damage"))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.setBlockList(new ArrayList<>());
    }

    @EventHandler
    public void onFlow(LiquidFlowEvent event) {
        final Block blockSource = event.getSource();
        final PlotManager plotManager = this.plugin.getPlotManager(blockSource.getLevel());
        if(plotManager != null) {
            final Block blockTo = event.getTo();
            final Plot plotFrom = plotManager.getMergedPlot(blockSource.getFloorX(), blockSource.getFloorZ());
            final Plot plotTo = plotManager.getMergedPlot(blockTo.getFloorX(), blockTo.getFloorZ());

            if(plotFrom != null && plotTo == null) event.setCancelled(true);
            if(plotTo != null && plotFrom == null) event.setCancelled(true);

            if(plotFrom != null && plotFrom.getHomePosition() != null && plotFrom.getHomePosition().distance(event.getBlock()) < 5)
                event.setCancelled(true);
            if(plotTo != null && plotTo.getHomePosition() != null && plotTo.getHomePosition().distance(event.getBlock()) < 5)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        final Block blockSource = event.getSource();
        if(blockSource == null) return;

        final PlotManager plotManager = this.plugin.getPlotManager(blockSource.getLevel());
        if(plotManager != null) {
            final Block block = event.getBlock();
            final Plot plotFrom = plotManager.getMergedPlot(blockSource.getFloorX(), blockSource.getFloorZ());
            final Plot plotTo = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());

            if(plotFrom != null && plotTo == null) event.setCancelled(true);
            if(plotTo != null && plotFrom == null) event.setCancelled(true);

            if(plotFrom != null && plotFrom.getHomePosition() != null && plotFrom.getHomePosition().distance(event.getBlock()) < 5)
                event.setCancelled(true);
            if(plotTo != null && plotTo.getHomePosition() != null && plotTo.getHomePosition().distance(event.getBlock()) < 5)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDecay(LeavesDecayEvent event) {
        final Block block = event.getBlock();
        final PlotManager plotManager = this.plugin.getPlotManager(block.getLevel());
        if(plotManager == null) return;

        final Plot plot = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());
        if(plot == null) event.setCancelled(true);
        else if(plot.getHomePosition() != null && plot.getHomePosition().distance(event.getBlock()) < 5)
            event.setCancelled(true);
    }

    @EventHandler
    public void onUpdate(BlockUpdateEvent event) {
        final Block block = event.getBlock();
        final PlotManager plotManager = this.plugin.getPlotManager(block.getLevel());
        if(plotManager == null) return;

        final Plot plot = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());
        if(plot == null) event.setCancelled(true);
    }

    @EventHandler
    public void onPiston(BlockPistonEvent event) {
        final Block block = event.getBlock();
        final PlotManager plotManager = this.plugin.getPlotManager(block.getLevel());
        if(plotManager != null) {
            final Plot blockPlot = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());

            for(Block movingBlock : event.getBlocks()) {
                final Plot movingBlockPlot = plotManager.getMergedPlot(movingBlock.getFloorX(), movingBlock.getFloorZ());

                if(blockPlot != null && movingBlockPlot == null) {
                    event.setCancelled(true);
                    return;
                }

                if(blockPlot == null && movingBlockPlot != null) {
                    event.setCancelled(true);
                    return;
                }

                final ShapeType[] shapes = plotManager.getShapes(movingBlock.getChunkX() << 4, movingBlock.getChunkZ() << 4);
                if(shapes[((movingBlock.getFloorZ() & 15) << 4) | (movingBlock.getFloorX() & 15)] == ShapeType.WALL) {
                    event.setCancelled(true);
                    return;
                }
            }

            for(Block movingBlock : event.getDestroyedBlocks()) {
                final Plot movingBlockPlot = plotManager.getMergedPlot(movingBlock.getFloorX(), movingBlock.getFloorZ());

                if(blockPlot != null && movingBlockPlot == null) {
                    event.setCancelled(true);
                    return;
                }

                if(blockPlot == null && movingBlockPlot != null) {
                    event.setCancelled(true);
                    return;
                }

                final ShapeType[] shapes = plotManager.getShapes(movingBlock.getChunkX() << 4, movingBlock.getChunkZ() << 4);
                if(shapes[((movingBlock.getFloorZ() & 15) << 4) | (movingBlock.getFloorX() & 15)] == ShapeType.WALL) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onGrow(StructureGrowEvent event) {
        final Block block = event.getBlock();
        final PlotManager plotManager = this.plugin.getPlotManager(block.getLevel());
        if(plotManager != null) {
            final Plot blockPlot = plotManager.getMergedPlot(block.getFloorX(), block.getFloorZ());
            for(Block movingBlock : event.getBlockList()) {
                final Plot movingBlockPlot = plotManager.getMergedPlot(movingBlock.getFloorX(), movingBlock.getFloorZ());
                if(blockPlot != null && movingBlockPlot == null) {
                    event.setCancelled(true);
                    break;
                }

                if(blockPlot == null && movingBlockPlot != null) {
                    event.setCancelled(true);
                    break;
                }

                final ShapeType[] shapes = plotManager.getShapes(movingBlock.getChunkX() << 4, movingBlock.getChunkZ() << 4);
                if(shapes[((movingBlock.getFloorZ() & 15) << 4) | (movingBlock.getFloorX() & 15)] == ShapeType.WALL) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

}
