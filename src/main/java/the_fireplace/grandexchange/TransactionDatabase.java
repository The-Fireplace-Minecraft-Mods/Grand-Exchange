package the_fireplace.grandexchange;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;

import java.util.*;

public final class TransactionDatabase {
	public static HashMap<Pair<String, Integer>, List<BuyOffer>> getBuyOffers() {
		sortOffers();
		return buyOffers;
	}

	public static HashMap<Pair<String, Integer>, List<SellOffer>> getSellOffers() {
		sortOffers();
		return sellOffers;
	}

	public static HashMap<UUID, List<ItemStack>> getPayouts() {
		return payouts;
	}

	private static HashMap<Pair<String, Integer>, List<BuyOffer>> buyOffers = Maps.newHashMap();
	private static HashMap<Pair<String, Integer>, List<SellOffer>> sellOffers = Maps.newHashMap();
	private static HashMap<UUID, List<ItemStack>> payouts = Maps.newHashMap();

	public static boolean hasPayout(UUID player){
		return payouts.containsKey(player) && !payouts.get(player).isEmpty();
	}

	public static List<ItemStack> getPayout(UUID player){
		return hasPayout(player) ? payouts.get(player) : Lists.newArrayList();
	}

	public static void removePayouts(UUID player, Collection<ItemStack> toRemove){
		payouts.get(player).removeAll(toRemove);
	}

	public static boolean canTransactItem(ItemStack item){
		return !item.isEmpty() && !item.hasTagCompound() && !item.isItemEnchanted();
	}

	public static boolean makeOffer(Offer offer){
		boolean offerFulfilled = tryFulfillOffer(offer);
		if(!offerFulfilled){
			if(offer instanceof BuyOffer){
				if(!buyOffers.containsKey(offer.getItemPair()))
					buyOffers.put(offer.getItemPair(), Lists.newArrayList());
				buyOffers.get(offer.getItemPair()).add((BuyOffer)offer);
			} else {
				if(!sellOffers.containsKey(offer.getItemPair()))
					sellOffers.put(offer.getItemPair(), Lists.newArrayList());
				sellOffers.get(offer.getItemPair()).add((SellOffer)offer);
			}
		}
		return offerFulfilled;
	}

	@SuppressWarnings("Duplicates")
	private static boolean tryFulfillOffer(Offer offer) {
		if(!payouts.containsKey(offer.getOwner()))
			payouts.put(offer.getOwner(), Lists.newArrayList());
		ResourceLocation offerResource = new ResourceLocation(offer.getItemResourceName());
		boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerResource);
		@SuppressWarnings("ConstantConditions")
		ItemStack sizeCheckStack = isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 1, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 1, offer.getItemMeta());
		int maxStackSize = sizeCheckStack.getMaxStackSize();
		sortOffers();
		if(offer instanceof BuyOffer){
			if(sellOffers.containsKey(offer.getItemPair()) && !sellOffers.get(offer.getItemPair()).isEmpty()){
				List<SellOffer> possibleSellOffers = sellOffers.get(offer.getItemPair());
				possibleSellOffers.removeIf(o1 -> o1.getPrice() > offer.getPrice());
				List<SellOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(SellOffer sellOffer: possibleSellOffers){
					if(offer.getAmount() > sellOffer.getAmount()){
						int givingAmount = sellOffer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice());
						while(givingAmount > maxStackSize) {
							//noinspection ConstantConditions
							payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), maxStackSize, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, offer.getItemMeta()));
							givingAmount -= maxStackSize;
						}
						//noinspection ConstantConditions
						payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, offer.getItemMeta()));
						offer.decrementAmount(sellOffer.getAmount());
						removeOffers.add(sellOffer);
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice());
						while(givingAmount > maxStackSize) {
							//noinspection ConstantConditions
							payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), maxStackSize, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, offer.getItemMeta()));
							givingAmount -= maxStackSize;
						}
						//noinspection ConstantConditions
						payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, offer.getItemMeta()));
						if(offer.getAmount() == sellOffer.getAmount())
							removeOffers.add(sellOffer);
						else
							sellOffer.decrementAmount(offer.getAmount());
						offerComplete = true;
						break;
					}
				}
				sellOffers.get(offer.getItemPair()).removeAll(removeOffers);
				return offerComplete;
			}
		} else {
			if(buyOffers.containsKey(offer.getItemPair()) && !buyOffers.get(offer.getItemPair()).isEmpty()){
				List<BuyOffer> possibleBuyOffers = buyOffers.get(offer.getItemPair());
				possibleBuyOffers.removeIf(o1 -> o1.getPrice() < offer.getPrice());
				List<BuyOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(BuyOffer buyOffer: possibleBuyOffers){
					if(!payouts.containsKey(buyOffer.getOwner()))
						payouts.put(buyOffer.getOwner(), Lists.newArrayList());
					if(offer.getAmount() > buyOffer.getAmount()){
						int givingAmount = buyOffer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice());
						while(givingAmount > maxStackSize) {
							//noinspection ConstantConditions
							payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), maxStackSize, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, offer.getItemMeta()));
							givingAmount -= maxStackSize;
						}
						//noinspection ConstantConditions
						payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, offer.getItemMeta()));
						offer.decrementAmount(buyOffer.getAmount());
						removeOffers.add(buyOffer);
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice());
						while(givingAmount > maxStackSize) {
							//noinspection ConstantConditions
							payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), maxStackSize, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), maxStackSize, offer.getItemMeta()));
							givingAmount -= maxStackSize;
						}
						//noinspection ConstantConditions
						payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount, offer.getItemMeta()));
						if(offer.getAmount() == buyOffer.getAmount())
							removeOffers.add(buyOffer);
						else
							buyOffer.decrementAmount(offer.getAmount());
						offerComplete = true;
						break;
					}
				}
				buyOffers.get(offer.getItemPair()).removeAll(removeOffers);
				return offerComplete;
			}
		}
		return false;
	}

	private static void sortOffers(){
		for(List<BuyOffer> buyOffer : buyOffers.values())
			buyOffer.sort(Comparator.comparing(BuyOffer::getPrice).thenComparing(BuyOffer::getTimestamp));
		for(List<SellOffer> sellOffer : sellOffers.values())
			sellOffer.sort(Comparator.comparing(SellOffer::getTimestamp));
	}
}
