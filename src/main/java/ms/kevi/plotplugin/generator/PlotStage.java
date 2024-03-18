package ms.kevi.plotplugin.generator;

import cn.nukkit.level.Level;
import cn.nukkit.level.format.ChunkState;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.generator.ChunkGenerateContext;
import cn.nukkit.level.generator.GenerateStage;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;
import ms.kevi.plotplugin.util.ShapeType;

import static ms.kevi.plotplugin.generator.PlotGenerator.*;

public class PlotStage extends GenerateStage {
    public static final String NAME = "plot_gen";

    @Override
    public void apply(ChunkGenerateContext chunkGenerateContext) {
        final IChunk chunk = chunkGenerateContext.getChunk();
        LevelProvider provider = chunk.getProvider();
        Level level = provider.getLevel();
        if (level == null) return;
        final PlotManager plotManager = PlotPlugin.INSTANCE.getPlotManager(level);
        if (plotManager == null) return;
        final ShapeType[] shapes = plotManager.getShapes(chunk.getX() << 4, chunk.getZ() << 4);

        preGenerateChunk(plotManager, chunk, shapes, GENERATE_ALLOWED, true, null, null, null, null);
        final Schematic schematic = plotManager.getPlotSchematic().getSchematic();
        if (schematic != null)
            placeChunkSchematic(plotManager, schematic, chunk, shapes, GENERATE_ALLOWED, null, null, null, null);
        chunk.setChunkState(ChunkState.POPULATED);
    }

    @Override
    public String name() {
        return NAME;
    }
}
