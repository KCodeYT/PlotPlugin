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

package ms.kevi.plotplugin.provider;

import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.PlotId;
import ms.kevi.plotplugin.util.PlotLevelSettings;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public interface Provider {

    CompletableFuture<PlotManager> loadOrCreateManager(String levelName, PlotLevelSettings levelSettings);

    CompletableFuture<List<PlotManager>> getAllPlotManagers();

    CompletableFuture<Boolean> existSchematic(String name);

    CompletableFuture<Schematic> loadSchematic(String name);

    CompletableFuture<Boolean> saveSchematic(String name, Schematic schematic);

    CompletableFuture<Boolean> deleteSchematic(String name);

    CompletableFuture<List<Plot>> getPlots(String levelName);

    CompletableFuture<Boolean> savePlots(String levelName, List<Plot> plots);

    CompletableFuture<Void> addPlot(String levelName, Plot plot);

    CompletableFuture<Void> removePlot(String levelName, Plot plot);

    CompletableFuture<Plot> getPlot(String levelName, PlotId id);

    CompletableFuture<Boolean> hasPlot(String levelName, PlotId id);

    CompletableFuture<Plot> searchNextFreePlot(String levelName);

    CompletableFuture<List<Plot>> getPlotsByOwner(String levelName, UUID owner);

}
