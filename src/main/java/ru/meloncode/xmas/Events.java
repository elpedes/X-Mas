package ru.meloncode.xmas;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.meloncode.xmas.utils.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Events implements Listener {
    private final Map<UUID, Long> destroyers = new HashMap<>();

    public void registerListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onPlayerOpenPresent(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.SKULL) {
                XMas.processPresent(block, event.getPlayer());
            }
        }
    }

    // Prevent bonemeal on magic tree
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerUseBonemeal(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
            if (MagicTree.isBlockBelongs(event.getClickedBlock()))
                if (event.getItem() != null)
                    if (event.getItem().getType() == Material.INK_SACK && event.getItem().getData().getData() == 15)
                        event.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerClickBlock(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return; //Event firing for both hands
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (MagicTree.isBlockBelongs(block)) {
                event.setCancelled(true);
                MagicTree tree = MagicTree.getTreeByBlock(block);
                if (Main.EVENT_IN_PROGRESS) {
                    if (tree.getLevel().hasNext()) {
                        if (tree.canLevelUp()) {
                            if (!tree.tryLevelUp()) {
                                TextUtils.sendMessage(player, LocaleManager.GROW_NOT_ENOUGH_PLACE);
                            }
                        } else {
                            if (event.getItem() != null) {
                                ItemStack is = event.getItem();
                                if (tree.grow(is.getType())) {
                                    TextUtils.sendMessage(player, LocaleManager.GROW_LVL_PROGRESS);
                                    if (player.getGameMode() != GameMode.CREATIVE) {
                                        if (is.getAmount() > 1) {
                                            is.setAmount(is.getAmount() - 1);
                                        } else {
                                            event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
                                        }
                                    }
                                }
                            }
                            if (tree.level.nextLevel != null) {
                                TextUtils.sendMessage(player, LocaleManager.GROW_LVL_PROGRESS);
                                for (String line : TextUtils.generateChatReqList(tree)) {
                                    TextUtils.sendMessage(player, line);
                                }

                                if (tree.getLevelupRequirements().size() == 0) {
                                    TextUtils.sendMessage(player, LocaleManager.GROW_LVL_READY);
                                }
                            }
                        }
                    } else {
                        TextUtils.sendMessage(player, LocaleManager.GROW_LEVEL_MAX);
                    }
                } else {
                    if (player.getUniqueId().equals(tree.getOwner())) {
                        tree.end();
                        TextUtils.sendMessage(player, LocaleManager.TIMEOUT);
                    } else {
                        TextUtils.sendMessage(player, LocaleManager.DESTROY_FAIL_OWNER);
                    }
                }
            } else {
                if (block.getType() == Material.SAPLING) {
                    if (block.getData() == (byte) 1) {
                        ItemStack is = event.getItem();
                        if (is != null)
                            if (!(Main.AUTO_END && System.currentTimeMillis() > Main.END_TIME)) {
                                if (XMas.getTreesPlayerOwn(player).size() < Main.MAX_TREE_COUNT) {
                                    if (is.getType() == XMas.XMAS_CRYSTAL.getType() && is.hasItemMeta() && is.getItemMeta().hasLore()) {
                                        ItemMeta im = is.getItemMeta();
                                        if (im.getLore().equals(XMas.XMAS_CRYSTAL.getItemMeta().getLore())) {
                                            if (player.getGameMode() != GameMode.CREATIVE) {
                                                if (is.getAmount() > 1) {
                                                    is.setAmount(is.getAmount() - 1);
                                                } else {
                                                    event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
                                                }
                                            }
                                            XMas.createMagicTree(player, block.getLocation());
                                        }
                                    }
                                } else {
                                    TextUtils.sendMessage(player, LocaleManager.TREE_LIMIT);
                                }
                            } else {
                                TextUtils.sendMessage(player, LocaleManager.TIMEOUT);
                            }
                    }

                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (MagicTree.isBlockBelongs(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onBlockBreakByExplosion(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (MagicTree.isBlockBelongs(block))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        if (item.getType() == Material.SKULL_ITEM) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getOwner() != null && Main.getHeads().contains(meta.getOwner())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (MagicTree.isBlockBelongs(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (MagicTree.isBlockBelongs(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (MagicTree.isBlockBelongs(event.getBlock())) {
            event.setCancelled(true);
        }
        for (BlockFace face : BlockFace.values()) {
            if (MagicTree.isBlockBelongs(event.getBlock().getRelative(face))) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (MagicTree.isBlockBelongs(block)) {
            event.setCancelled(true);
            MagicTree tree = MagicTree.getTreeByBlock(block);
            switch (block.getType()) {
                case LOG:
                    if (player.getUniqueId().equals(tree.getOwner())) {
                        if (Main.EVENT_IN_PROGRESS)
                            if (destroyers.containsKey(player.getUniqueId()) && System.currentTimeMillis() - destroyers.get(player.getUniqueId()) <= 10000) {
                                XMas.removeTree(tree);
                                if (Main.EVENT_IN_PROGRESS)
                                    TextUtils.sendMessage(player, ChatColor.DARK_RED + LocaleManager.MONSTER);
                            } else {
                                destroyers.put(player.getUniqueId(), System.currentTimeMillis());
                                if (Main.EVENT_IN_PROGRESS)
                                    TextUtils.sendMessage(player, ChatColor.RED + LocaleManager.DESTROY_WARNING);
                                TextUtils.sendMessage(player, ChatColor.DARK_RED + LocaleManager.DESTROY_TUT);
                            }
                        else {
                            tree.end();
                        }
                    } else {
                        TextUtils.sendMessage(player, LocaleManager.DESTROY_FAIL_OWNER);
                    }
                    break;
                case LEAVES:
                case GLOWSTONE:
                    if (Main.EVENT_IN_PROGRESS)
                        TextUtils.sendMessage(player, ChatColor.DARK_GREEN + LocaleManager.DESTROY_LEAVES_SANTA);
                    if (player.getUniqueId().equals(tree.getOwner())) {
                        TextUtils.sendMessage(player, ChatColor.RED + LocaleManager.DESTROY_LEAVES_TUT);
                    } else {
                        TextUtils.sendMessage(player, LocaleManager.DESTROY_FAIL_OWNER);
                    }
                    break;
                case SAPLING:
                    if (player.getUniqueId().equals(tree.getOwner())) {
                        if (Main.EVENT_IN_PROGRESS) {
                            if (destroyers.containsKey(player.getUniqueId()) && System.currentTimeMillis() - destroyers.get(player.getUniqueId()) <= 10000) {
                                XMas.removeTree(tree);
                                TextUtils.sendMessage(player, ChatColor.RED + LocaleManager.MONSTER);
                            } else {
                                destroyers.put(player.getUniqueId(), System.currentTimeMillis());
                                if (Main.EVENT_IN_PROGRESS)
                                    TextUtils.sendMessage(player, ChatColor.RED + LocaleManager.DESTROY_SAPLING);
                                TextUtils.sendMessage(player, ChatColor.DARK_RED + LocaleManager.DESTROY_TUT);
                            }
                        } else {
                            tree.end();
                        }
                    } else {
                        TextUtils.sendMessage(player, LocaleManager.DESTROY_FAIL_OWNER);
                    }

                    break;
                case SKULL:
                    XMas.processPresent(block, player);
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onSaplingGrow(StructureGrowEvent event) {
        // Prevent magic tree to grow as normal
        if (event.getSpecies() == TreeType.REDWOOD) {
            if (MagicTree.isBlockBelongs(event.getLocation().getBlock())) {
                event.setCancelled(true);
            }
        }
    }

}
