package top.theillusivec4.polymorph.common.network;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import top.theillusivec4.polymorph.api.PolymorphComponents;
import top.theillusivec4.polymorph.common.PolymorphMod;
import top.theillusivec4.polymorph.common.integration.fastbench.FastBenchModule;
import top.theillusivec4.polymorph.common.util.CraftingPlayers;

public class PolymorphNetwork {

  public static void setup() {
    ServerPlayNetworking.registerGlobalReceiver(PolymorphPackets.GET_RECIPES,
        (minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> minecraftServer.execute(
            () -> {
              World world = serverPlayerEntity.getEntityWorld();
              ScreenHandler screenHandler = serverPlayerEntity.currentScreenHandler;
              Inventory inventory = screenHandler.slots.get(0).inventory;

              if (inventory instanceof BlockEntity) {
                PolymorphComponents.BLOCK_ENTITY_RECIPE_SELECTOR.maybeGet((BlockEntity) inventory)
                    .ifPresent(selector -> {
                      @SuppressWarnings("unchecked") List<? extends Recipe<?>> recipes =
                          world.getRecipeManager().values().stream()
                              .filter((val) -> val.getType() == selector.getRecipeType())
                              .flatMap((val) -> Util.stream(selector.getRecipeType()
                                  .get((Recipe<Inventory>) val, world,
                                      (Inventory) selector.getParent())))
                              .sorted(Comparator
                                  .comparing((recipe) -> recipe.getOutput().getTranslationKey()))
                              .collect(Collectors.toList());
                      PacketByteBuf buf = PacketByteBufs.create();

                      if (!recipes.isEmpty()) {
                        buf.writeIdentifier(selector.getSelectedRecipe().map(Recipe::getId)
                            .orElse(recipes.get(0).getId()));

                        for (Recipe<?> recipe : recipes) {
                          buf.writeString(recipe.getId().toString());
                        }
                      }
                      ServerPlayNetworking
                          .send(serverPlayerEntity, PolymorphPackets.SEND_RECIPES, buf);
                    });
              }
            }));
    ServerPlayNetworking.registerGlobalReceiver(PolymorphPackets.SELECT_CRAFT,
        (minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
          Identifier id = packetByteBuf.readIdentifier();
          minecraftServer.execute(() -> {
            CraftingPlayers.add(serverPlayerEntity, id);

            if (PolymorphMod.isFastBenchLoaded) {
              minecraftServer.getRecipeManager().get(id)
                  .ifPresent(recipe -> FastBenchModule.setLastRecipe(serverPlayerEntity, recipe));
            }
            serverPlayerEntity.currentScreenHandler.onContentChanged(
                serverPlayerEntity.currentScreenHandler.slots.get(0).inventory);
          });
        });
    ServerPlayNetworking.registerGlobalReceiver(PolymorphPackets.SELECT_PERSIST,
        (minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
          Identifier id = packetByteBuf.readIdentifier();
          minecraftServer.execute(() -> {
            World world = serverPlayerEntity.getEntityWorld();
            Optional<? extends Recipe<?>> recipe = world.getRecipeManager().get(id);
            recipe.ifPresent(res -> {
              ScreenHandler screenHandler = serverPlayerEntity.currentScreenHandler;
              Inventory inventory = screenHandler.slots.get(0).inventory;

              if (inventory instanceof BlockEntity) {
                PolymorphComponents.BLOCK_ENTITY_RECIPE_SELECTOR.maybeGet((BlockEntity) inventory)
                    .ifPresent(selector -> selector.setSelectedRecipe(res));
              }
            });
          });
        });
  }
}
