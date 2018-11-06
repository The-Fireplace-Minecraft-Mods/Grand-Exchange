package the_fireplace.grandexchange;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TransactionDatabase {
	private static HashMap<String, List<BuyOffer>> buyOffers = Maps.newHashMap();
	private static HashMap<String, List<SellOffer>> sellOffers = Maps.newHashMap();
	private static HashMap<UUID, List<ItemStack>> payouts = Maps.newHashMap();

	public static boolean makeOffer(Offer offer){
		boolean offerFulfilled = tryFulfillOffer(offer);
		if(!offerFulfilled){
			if(offer instanceof BuyOffer){
				if(!buyOffers.containsKey(offer.getItem()))
					buyOffers.put(offer.getItem(), Lists.newArrayList());
				buyOffers.get(offer.getItem()).add((BuyOffer)offer);
			} else {
				if(!sellOffers.containsKey(offer.getItem()))
					sellOffers.put(offer.getItem(), Lists.newArrayList());
				sellOffers.get(offer.getItem()).add((SellOffer)offer);
			}
		}
		return offerFulfilled;
	}

	@SuppressWarnings("Duplicates")
	private static boolean tryFulfillOffer(Offer offer) {
		if(!payouts.containsKey(offer.getOwner()))
			payouts.put(offer.getOwner(), Lists.newArrayList());
		ResourceLocation offerResource = new ResourceLocation(offer.getItem());
		boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerResource);
		sortOffers();
		if(offer instanceof BuyOffer){
			if(sellOffers.containsKey(offer.getItem()) && !sellOffers.get(offer.getItem()).isEmpty()){
				List<SellOffer> possibleSellOffers = sellOffers.get(offer.getItem());
				possibleSellOffers.removeIf(o1 -> o1.getPrice() > offer.getPrice());
				List<SellOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(SellOffer sellOffer: possibleSellOffers){
					if(offer.getAmount() > sellOffer.getAmount()){
						int givingAmount = sellOffer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice());
						while(givingAmount > 64) {
							//noinspection ConstantConditions
							payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 64) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 64));
							givingAmount -= 64;
						}
						//noinspection ConstantConditions
						payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount));
						offer.decrementAmount(sellOffer.getAmount());
						removeOffers.add(sellOffer);
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice());
						while(givingAmount > 64) {
							//noinspection ConstantConditions
							payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 64) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 64));
							givingAmount -= 64;
						}
						//noinspection ConstantConditions
						payouts.get(offer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount));
						if(offer.getAmount() == sellOffer.getAmount())
							removeOffers.add(sellOffer);
						else
							sellOffer.decrementAmount(offer.getAmount());
						offerComplete = true;
						break;
					}
				}
				sellOffers.get(offer.getItem()).removeAll(removeOffers);
				return offerComplete;
			}
		} else {
			if(buyOffers.containsKey(offer.getItem()) && !buyOffers.get(offer.getItem()).isEmpty()){
				List<BuyOffer> possibleBuyOffers = buyOffers.get(offer.getItem());
				possibleBuyOffers.removeIf(o1 -> o1.getPrice() < offer.getPrice());
				List<BuyOffer> removeOffers = Lists.newArrayList();
				boolean offerComplete = false;
				for(BuyOffer buyOffer: possibleBuyOffers){
					if(!payouts.containsKey(buyOffer.getOwner()))
						payouts.put(buyOffer.getOwner(), Lists.newArrayList());
					if(offer.getAmount() > buyOffer.getAmount()){
						int givingAmount = buyOffer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice());
						while(givingAmount > 64) {
							//noinspection ConstantConditions
							payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 64) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 64));
							givingAmount -= 64;
						}
						//noinspection ConstantConditions
						payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount));
						offer.decrementAmount(buyOffer.getAmount());
						removeOffers.add(buyOffer);
					} else {
						int givingAmount = offer.getAmount();
						GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice());
						while(givingAmount > 64) {
							//noinspection ConstantConditions
							payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 64) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 64));
							givingAmount -= 64;
						}
						//noinspection ConstantConditions
						payouts.get(buyOffer.getOwner()).add(isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), givingAmount) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), givingAmount));
						if(offer.getAmount() == buyOffer.getAmount())
							removeOffers.add(buyOffer);
						else
							buyOffer.decrementAmount(offer.getAmount());
						offerComplete = true;
						break;
					}
				}
				buyOffers.get(offer.getItem()).removeAll(removeOffers);
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
