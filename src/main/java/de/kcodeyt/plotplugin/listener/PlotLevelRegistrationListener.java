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

package de.kcodeyt.plotplugin.listener;

import cn.nukkit.Player;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.util.PlotLevelRegistration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlotLevelRegistrationListener implements Listener {

    private final PlotPlugin plugin;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(PlayerChatEvent event) {
        final Player player = event.getPlayer();

        if(this.plugin.getLevelRegistrationMap().containsKey(player)) {
            event.setCancelled(true);

            final PlotLevelRegistration registration = this.plugin.getLevelRegistrationMap().get(player);

            switch(registration.getState()) {
                case DIMENSION: {
                    final int dimension;
                    try {
                        dimension = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setDimension(dimension);
                    break;
                }
                case PLOT_BIOME: {
                    final int biome;
                    try {
                        biome = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setPlotBiome(biome);
                    break;
                }
                case ROAD_BIOME: {
                    final int biome;
                    try {
                        biome = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setRoadBiome(biome);
                    break;
                }
                case FIRST_LAYER: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setFirstLayerBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setFirstLayerBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case MIDDLE_LAYER: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setMiddleLayerBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setMiddleLayerBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case LAST_LAYER: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setLastLayerBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setLastLayerBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case ROAD: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setRoadBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setRoadBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case ROAD_FILLING: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setRoadFillingBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setRoadFillingBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case WALL_UNOWNED: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setWallPlotBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setWallPlotBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case WALL_CLAIMED: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setClaimPlotBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setClaimPlotBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case WALL_FILLING: {
                    final BlockState blockState = Item.fromString(event.getMessage()).getBlock().getCurrentState();
                    registration.getLevelSettings().setWallFillingBlockId(blockState.getBlockId());
                    registration.getLevelSettings().setWallFillingBlockMeta(blockState.getHugeDamage().intValue());
                    break;
                }
                case PLOT_SIZE: {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setPlotSize(number);
                    break;
                }
                case ROAD_SIZE: {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setRoadSize(number);
                    break;
                }
                case GROUND_HEIGHT: {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch(NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setGroundHeight(number);
                    break;
                }
            }

            final PlotLevelRegistration.RegistrationState[] states = PlotLevelRegistration.RegistrationState.values();
            final int nextStage = registration.getState().ordinal() + 1;
            if(nextStage >= states.length) {
                this.plugin.getLevelRegistrationMap().remove(player);
                Level level;
                if((level = this.plugin.createLevel(registration.getLevelName(), registration.isDefaultLevel(), registration.getLevelSettings())) != null) {
                    player.sendMessage(this.plugin.getLanguage().translate("generate-success" + (registration.isDefaultLevel() ? "-default" : ""), registration.getLevelName()));
                    player.teleport(level.getSafeSpawn(level.getGenerator().getSpawn()));
                } else {
                    player.sendMessage(this.plugin.getLanguage().translate("generate-failure", registration.getLevelName()));
                }
                return;
            }

            registration.setState(states[nextStage]);
            switch(registration.getState()) {
                case PLOT_BIOME:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-plot-biome", registration.getLevelSettings().getPlotBiome()));
                    break;
                case ROAD_BIOME:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-road-biome", registration.getLevelSettings().getRoadBiome()));
                    break;

                case FIRST_LAYER:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-first-layer",
                            registration.getLevelSettings().getFirstLayerBlockId() + ":" +
                                    registration.getLevelSettings().getFirstLayerBlockMeta()));
                    break;
                case MIDDLE_LAYER:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-middle-layer",
                            registration.getLevelSettings().getMiddleLayerBlockId() + ":" +
                                    registration.getLevelSettings().getMiddleLayerBlockMeta()));
                    break;
                case LAST_LAYER:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-last-layer",
                            registration.getLevelSettings().getLastLayerBlockId() + ":" +
                                    registration.getLevelSettings().getLastLayerBlockMeta()));
                    break;

                case ROAD:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-road",
                            registration.getLevelSettings().getRoadBlockId() + ":" +
                                    registration.getLevelSettings().getRoadBlockMeta()));
                    break;
                case ROAD_FILLING:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-road-filling",
                            registration.getLevelSettings().getRoadFillingBlockId() + ":" +
                                    registration.getLevelSettings().getRoadFillingBlockMeta()));
                    break;

                case WALL_UNOWNED:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-wall-unowned",
                            registration.getLevelSettings().getWallPlotBlockId() + ":" +
                                    registration.getLevelSettings().getWallPlotBlockMeta()));
                    break;
                case WALL_CLAIMED:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-wall-claimed",
                            registration.getLevelSettings().getClaimPlotBlockId() + ":" +
                                    registration.getLevelSettings().getClaimPlotBlockMeta()));
                    break;
                case WALL_FILLING:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-wall-filling",
                            registration.getLevelSettings().getWallFillingBlockId() + ":" +
                                    registration.getLevelSettings().getWallFillingBlockMeta()));
                    break;

                case PLOT_SIZE:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-plot-size", registration.getLevelSettings().getPlotSize()));
                    break;
                case ROAD_SIZE:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-road-size", registration.getLevelSettings().getRoadSize()));
                    break;
                case GROUND_HEIGHT:
                    player.sendMessage(this.plugin.getLanguage().translate("generate-ground-height", registration.getLevelSettings().getGroundHeight()));
                    break;
            }
        }
    }

}
