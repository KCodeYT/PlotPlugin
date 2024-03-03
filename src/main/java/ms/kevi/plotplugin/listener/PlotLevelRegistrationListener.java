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
import cn.nukkit.block.BlockState;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import lombok.RequiredArgsConstructor;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.PlotLevelRegistration;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@RequiredArgsConstructor
public class PlotLevelRegistrationListener implements Listener {

    private final PlotPlugin plugin;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(PlayerChatEvent event) {
        final Player player = event.getPlayer();

        if (this.plugin.getLevelRegistrationMap().containsKey(player)) {
            event.setCancelled(true);

            final PlotLevelRegistration registration = this.plugin.getLevelRegistrationMap().get(player);

            switch (registration.getState()) {
                case DIMENSION -> {
                    final int dimension;
                    try {
                        dimension = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setDimension(dimension);
                }
                case PLOT_BIOME -> {
                    final int biome;
                    try {
                        biome = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setPlotBiome(biome);
                }
                case ROAD_BIOME -> {
                    final int biome;
                    try {
                        biome = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setRoadBiome(biome);
                }
                case FIRST_LAYER -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setFirstLayerBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setFirstLayerBlockMeta(blockState.specialValue());
                }
                case MIDDLE_LAYER -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setMiddleLayerBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setMiddleLayerBlockMeta(blockState.specialValue());
                }
                case LAST_LAYER -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setLastLayerBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setLastLayerBlockMeta(blockState.specialValue());
                }
                case ROAD -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setRoadBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setRoadBlockMeta(blockState.specialValue());
                }
                case ROAD_FILLING -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setRoadFillingBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setRoadFillingBlockMeta(blockState.specialValue());
                }
                case WALL_UNOWNED -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setWallPlotBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setWallPlotBlockMeta(blockState.specialValue());
                }
                case WALL_CLAIMED -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setClaimPlotBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setClaimPlotBlockMeta(blockState.specialValue());
                }
                case WALL_FILLING -> {
                    if (!event.getMessage().contains(":")) break;
                    final BlockState blockState = Item.get(event.getMessage()).getBlock().getBlockState();
                    registration.getLevelSettings().setWallFillingBlockId(blockState.getIdentifier());
                    registration.getLevelSettings().setWallFillingBlockMeta(blockState.specialValue());
                }
                case PLOT_SIZE -> {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setPlotSize(number);
                }
                case ROAD_SIZE -> {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setRoadSize(number);
                }
                case GROUND_HEIGHT -> {
                    final int number;
                    try {
                        number = Integer.parseInt(event.getMessage());
                    } catch (NumberFormatException e) {
                        break;
                    }

                    registration.getLevelSettings().setGroundHeight(number);
                }
            }

            final PlotLevelRegistration.RegistrationState[] states = PlotLevelRegistration.RegistrationState.values();
            final int nextStage = registration.getState().ordinal() + 1;
            if (nextStage >= states.length) {
                this.plugin.getLevelRegistrationMap().remove(player);
                Level level;
                if ((level = this.plugin.createLevel(registration.getLevelName(), registration.isDefaultLevel(), registration.getLevelSettings())) != null) {
                    player.sendMessage(this.plugin.getLanguage().translate(
                            player,
                            registration.isDefaultLevel() ? TranslationKey.GENERATE_SUCCESS_DEFAULT : TranslationKey.GENERATE_SUCCESS,
                            registration.getLevelName())
                    );
                    player.teleport(level.getSafeSpawn(level.getSpawnLocation()));
                } else {
                    player.sendMessage(this.plugin.getLanguage().translate(player, TranslationKey.GENERATE_FAILURE, registration.getLevelName()));
                }
                return;
            }

            registration.setState(states[nextStage]);
            switch (registration.getState()) {
                case PLOT_BIOME -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_PLOT_BIOME,
                        registration.getLevelSettings().getPlotBiome()
                ));
                case ROAD_BIOME -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_ROAD_BIOME,
                        registration.getLevelSettings().getRoadBiome()));
                case FIRST_LAYER -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_FIRST_LAYER,
                        registration.getLevelSettings().getFirstLayerBlockId() + ":" +
                                registration.getLevelSettings().getFirstLayerBlockMeta()
                ));
                case MIDDLE_LAYER -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_MIDDLE_LAYER,
                        registration.getLevelSettings().getMiddleLayerBlockId() + ":" +
                                registration.getLevelSettings().getMiddleLayerBlockMeta()
                ));
                case LAST_LAYER -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_LAST_LAYER,
                        registration.getLevelSettings().getLastLayerBlockId() + ":" +
                                registration.getLevelSettings().getLastLayerBlockMeta()
                ));
                case ROAD -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_ROAD,
                        registration.getLevelSettings().getRoadBlockId() + ":" +
                                registration.getLevelSettings().getRoadBlockMeta()
                ));
                case ROAD_FILLING -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_ROAD_FILLING,
                        registration.getLevelSettings().getRoadFillingBlockId() + ":" +
                                registration.getLevelSettings().getRoadFillingBlockMeta()
                ));
                case WALL_UNOWNED -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_WALL_UNOWNED,
                        registration.getLevelSettings().getWallPlotBlockId() + ":" +
                                registration.getLevelSettings().getWallPlotBlockMeta()
                ));
                case WALL_CLAIMED -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_WALL_CLAIMED,
                        registration.getLevelSettings().getClaimPlotBlockId() + ":" +
                                registration.getLevelSettings().getClaimPlotBlockMeta()
                ));
                case WALL_FILLING -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_WALL_FILLING,
                        registration.getLevelSettings().getWallFillingBlockId() + ":" +
                                registration.getLevelSettings().getWallFillingBlockMeta()
                ));
                case PLOT_SIZE -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_PLOT_SIZE,
                        registration.getLevelSettings().getPlotSize()
                ));
                case ROAD_SIZE -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_ROAD_SIZE,
                        registration.getLevelSettings().getRoadSize()
                ));
                case GROUND_HEIGHT -> player.sendMessage(this.plugin.getLanguage().translate(player,
                        TranslationKey.GENERATE_GROUND_HEIGHT,
                        registration.getLevelSettings().getGroundHeight()
                ));
            }
        }
    }

}
