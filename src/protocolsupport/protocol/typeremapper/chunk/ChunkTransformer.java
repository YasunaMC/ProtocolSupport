package protocolsupport.protocol.typeremapper.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.storage.netcache.TileDataCache;
import protocolsupport.protocol.typeremapper.basic.TileNBTRemapper;
import protocolsupport.protocol.typeremapper.utils.RemappingTable.ArrayBasedIdRemappingTable;
import protocolsupport.protocol.utils.minecraftdata.MinecraftData;
import protocolsupport.protocol.utils.types.ChunkCoord;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.protocol.utils.types.nbt.NBTCompound;

public abstract class ChunkTransformer {

	protected ChunkCoord chunk;
	protected int columnsCount;
	protected boolean hasSkyLight;
	protected boolean hasBiomeData;
	protected final ChunkSection[] sections = new ChunkSection[16];
	protected final int[] biomeData = new int[256];
	protected List<NBTCompound> tiles;

	protected final ArrayBasedIdRemappingTable blockTypeRemappingTable;
	protected final TileDataCache tilecache;
	protected final TileNBTRemapper tileremapper;
	public ChunkTransformer(ArrayBasedIdRemappingTable blockRemappingTable, TileDataCache tilecache) {
		this.blockTypeRemappingTable = blockRemappingTable;
		this.tilecache = tilecache;
		this.tileremapper = tilecache.getTileRemapper();
	}

	public void loadData(ChunkCoord chunk, ByteBuf chunkdata, int bitmap, boolean hasSkyLight, boolean hasBiomeData, NBTCompound[] tiles) {
		this.chunk = chunk;
		this.columnsCount = Integer.bitCount(bitmap);
		this.hasSkyLight = hasSkyLight;
		this.hasBiomeData = hasBiomeData;
		for (int i = 0; i < sections.length; i++) {
			if ((bitmap & (1 << i)) != 0) {
				sections[i] = new ChunkSection(chunkdata, hasSkyLight);
			} else {
				sections[i] = null;
			}
		}
		if (hasBiomeData) {
			for (int i = 0; i < biomeData.length; i++) {
				biomeData[i] = chunkdata.readInt();
			}
		}
		this.tiles = new ArrayList<>(Arrays.asList(tiles));
	}

	public NBTCompound[] remapAndGetTiles() {
		return tiles.stream().map(tileremapper::remap).toArray(NBTCompound[]::new);
	}

	protected int getBlockState(int section, BlockStorageReader blockstorage, int blockindex) {
		int blockstate = blockstorage.getBlockState(blockindex);
		if (tileremapper.tileThatNeedsBlockstate(blockstate)) {
			tilecache.setCachedTileBlockstate(getGlobalPositionFromSectionIndex(section, blockindex), blockstate);
		}
		if (tileremapper.usedToBeTile(blockstate)) {
			NBTCompound tile = tileremapper.getLegacyTileFromBlock(getGlobalPositionFromSectionIndex(section, blockindex), blockstate);
			if (tile != null) {
				tiles.add(tile);
			}
		}
		return blockstate;
	}

	protected Position getGlobalPositionFromSectionIndex(int section, int blockindex) {
		return new Position((chunk.getX() * 16) + (blockindex & 0xF), 
				(section * 16) + ((blockindex >> 8) & 0xF),
				(chunk.getZ() * 16) + ((blockindex >> 4) & 0xF));
	}

	protected static final int blocksInSection = 16 * 16 * 16;

	protected static class ChunkSection {

		protected static final int globalPaletteBitsPerBlock = 14;
		protected static final int[] globalPaletteData = new int[MinecraftData.BLOCKDATA_COUNT];
		static {
			for (int i = 0; i < globalPaletteData.length; i++) {
				globalPaletteData[i] = i;
			}
		}

		protected final BlockStorageReader blockdata;
		protected final byte[] blocklight = new byte[2048];
		protected final byte[] skylight = new byte[2048];

		public ChunkSection(ByteBuf datastream, boolean hasSkyLight) {
			byte bitsPerBlock = datastream.readByte();
			int[] palette = globalPaletteData;
			if (bitsPerBlock != globalPaletteBitsPerBlock) {
				palette = ArraySerializer.readVarIntVarIntArray(datastream);
			}
			this.blockdata = new BlockStorageReader(palette, bitsPerBlock, VarNumberSerializer.readVarInt(datastream));
			this.blockdata.readFromStream(datastream);
			datastream.readBytes(blocklight);
			if (hasSkyLight) {
				datastream.readBytes(skylight);
			}
		}

	}

}
