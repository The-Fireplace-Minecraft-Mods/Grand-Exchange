package the_fireplace.grandexchange.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import the_fireplace.grandeconomy.api.GrandEconomyApi;
import the_fireplace.grandexchange.market.BuyOffer;
import the_fireplace.grandexchange.market.Offer;
import the_fireplace.grandexchange.market.SellOffer;

import java.io.*;
import java.util.*;

public final class TransactionDatabase implements Serializable {
	private static final long serialVersionUID = 0x42069;

	private static TransactionDatabase instance = null;
	private static final String dataFileName = "grandexchange.dat";
	private static File saveDir = DimensionManager.getCurrentSaveRootDirectory();

	public static TransactionDatabase getInstance() {
		if(instance == null)
			readFromFile();
		return instance;
	}

	public static HashMap<Pair<String, Integer>, List<BuyOffer>> getBuyOffers() {
		getInstance().sortOffers();
		return getInstance().buyOffers;
	}

	public static HashMap<Pair<String, Integer>, List<SellOffer>> getSellOffers() {
		getInstance().sortOffers();
		return getInstance().sellOffers;
	}

	@SuppressWarnings("WeakerAccess")
	public static HashMap<UUID, List<String>> getPayouts() {
		return getInstance().payouts;
	}

	private HashMap<Pair<String, Integer>, List<BuyOffer>> buyOffers = Maps.newHashMap();
	private HashMap<Pair<String, Integer>, List<SellOffer>> sellOffers = Maps.newHashMap();
	private HashMap<UUID, List<String>> payouts = Maps.newHashMap();

	public static boolean hasPayout(UUID player){
		return getPayouts().containsKey(player) && !getPayouts().get(player).isEmpty();
	}

	@SuppressWarnings("WeakerAccess")
	public void addPayout(UUID player, ItemStack payout){
		if(!payouts.containsKey(player))
			payouts.put(player, Lists.newArrayList());
		payouts.get(player).add(SerializationUtils.stackToString(payout));
		saveToFile();
	}

	public static List<String> getPayout(UUID player){
		return hasPayout(player) ? getInstance().payouts.get(player) : Lists.newArrayList();
	}

	public void removePayouts(UUID player, Collection<String> toRemove){
		payouts.get(player).removeAll(toRemove);
	}

	public void cancelOffer(Offer offer){
		if(offer instanceof BuyOffer && buyOffers.get(offer.getItemPair()).remove(offer)) {
			GrandEconomyApi.addToBalance(offer.getOwner(), offer.getPrice()*offer.getAmount(), true);
		} else if(offer instanceof SellOffer && sellOffers.get(offer.getItemPair()).remove(offer)) {
			ResourceLocation offerRes = new ResourceLocation(offer.getItemResourceName());
			boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerRes);
			addPayout(offer.getOwner(), isOfferBlock ? new ItemStack(Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(offerRes)), offer.getAmount(), offer.getItemMeta()) : new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(offerRes)), offer.getAmount(), offer.getItemMeta()));
		}
		saveToFile();
	}

	public static boolean canTransactItem(ItemStack item){
		return !item.isEmpty();
	}

	public boolean makeOffer(Offer offer){
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
		saveToFile();
		return offerFulfilled;
	}

	@SuppressWarnings("Duplicates")
	private boolean tryFulfillOffer(Offer offer) {
		if(!payouts.containsKey(offer.getOwner()))
			payouts.put(offer.getOwner(), Lists.newArrayList());
		ResourceLocation offerResource = new ResourceLocation(offer.getItemResourceName());
		boolean isOfferBlock = ForgeRegistries.BLOCKS.containsKey(offerResource);
		@SuppressWarnings("ConstantConditions")
		ItemStack sizeCheckStack = isOfferBlock ? new ItemStack(ForgeRegistries.BLOCKS.getValue(offerResource), 1, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), 1, offer.getItemMeta());
		int maxStackSize = sizeCheckStack.getMaxStackSize();
		sortOffers();
		if(offer instanceof BuyOffer){
			return tryFulfillBuyOffer((BuyOffer)offer, offerResource, isOfferBlock, maxStackSize);
		} else if(offer instanceof SellOffer) {
			return tryFulfillSellOffer((SellOffer)offer, offerResource, isOfferBlock, maxStackSize);
		}
		return false;
	}

	private boolean tryFulfillSellOffer(SellOffer offer, ResourceLocation offerResource, boolean isOfferBlock, int maxStackSize) {
		if(buyOffers.containsKey(offer.getItemPair()) && !buyOffers.get(offer.getItemPair()).isEmpty()){
			List<BuyOffer> possibleBuyOffers = buyOffers.get(offer.getItemPair());
			possibleBuyOffers.removeIf(o1 -> o1.getPrice() < offer.getPrice());
			List<BuyOffer> removeOffers = Lists.newArrayList();
			boolean offerComplete = false;
			for(BuyOffer buyOffer: possibleBuyOffers){
				Entity buyer = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(buyOffer.getOwner());
				if(!payouts.containsKey(buyOffer.getOwner()))
					payouts.put(buyOffer.getOwner(), Lists.newArrayList());
				if(offer.getAmount() > buyOffer.getAmount()){
					int givingAmount = buyOffer.getAmount();
					GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), true);
					while(givingAmount > maxStackSize) {
						payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, maxStackSize, offer)));
						givingAmount -= maxStackSize;
					}
					payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, givingAmount, offer)));
					offer.decrementAmount(buyOffer.getAmount());
					removeOffers.add(buyOffer);
					if(buyer != null)
						buyer.sendMessage(new TextComponentTranslation("Buy Offer fulfilled: %s %s %s at %s each.", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
				} else {
					int givingAmount = offer.getAmount();
					GrandEconomyApi.addToBalance(offer.getOwner(), givingAmount*buyOffer.getPrice(), true);
					while(givingAmount > maxStackSize) {
						payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, maxStackSize, offer)));
						givingAmount -= maxStackSize;
					}
					payouts.get(buyOffer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, givingAmount, offer)));
					if(offer.getAmount() == buyOffer.getAmount()) {
						removeOffers.add(buyOffer);
						if(buyer != null)
							buyer.sendMessage(new TextComponentTranslation("Buy Offer fulfilled: %s %s %s at %s each.", buyOffer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
					} else {
						buyOffer.decrementAmount(offer.getAmount());
						if(buyer != null)
							buyer.sendMessage(new TextComponentTranslation("Buy Offer partially fulfilled: %s %s %s at %s each.", offer.getAmount(), buyOffer.getItemResourceName(), buyOffer.getItemMeta() + (buyOffer.getNbt() != null ? " with NBT "+buyOffer.getNbt() : ""), buyOffer.getPrice()).setStyle(TextStyles.BLUE));
					}
					offerComplete = true;
					break;
				}
			}
			buyOffers.get(offer.getItemPair()).removeAll(removeOffers);
			saveToFile();
			return offerComplete;
		}
		return false;
	}

	private boolean tryFulfillBuyOffer(BuyOffer offer, ResourceLocation offerResource, boolean isOfferBlock, int maxStackSize) {
		if(sellOffers.containsKey(offer.getItemPair()) && !sellOffers.get(offer.getItemPair()).isEmpty()){
			List<SellOffer> possibleSellOffers = sellOffers.get(offer.getItemPair());
			possibleSellOffers.removeIf(o1 -> o1.getPrice() > offer.getPrice());
			List<SellOffer> removeOffers = Lists.newArrayList();
			boolean offerComplete = false;
			for(SellOffer sellOffer: possibleSellOffers){
				Entity seller = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(sellOffer.getOwner());
				if(offer.getAmount() > sellOffer.getAmount()){
					int givingAmount = sellOffer.getAmount();
					GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
					if(seller != null)
						seller.sendMessage(new TextComponentTranslation("Sell Offer fulfilled: %s %s %s at %s each.", givingAmount, offer.getItemResourceName(), offer.getItemMeta(), offer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
					while(givingAmount > maxStackSize) {
						payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, maxStackSize, offer)));
						givingAmount -= maxStackSize;
					}
					payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, givingAmount, offer)));
					offer.decrementAmount(sellOffer.getAmount());
					removeOffers.add(sellOffer);
				} else {
					int givingAmount = offer.getAmount();
					GrandEconomyApi.addToBalance(sellOffer.getOwner(), givingAmount*sellOffer.getPrice(), true);
					while(givingAmount > maxStackSize) {
						payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, maxStackSize, offer)));
						givingAmount -= maxStackSize;
					}
					payouts.get(offer.getOwner()).add(SerializationUtils.stackToString(getStack(offerResource, isOfferBlock, givingAmount, offer)));
					if(offer.getAmount() == sellOffer.getAmount()) {
						removeOffers.add(sellOffer);
						if(seller != null)
							seller.sendMessage(new TextComponentTranslation("Sell Offer fulfilled: %s %s %s at %s each.", offer.getAmount(), offer.getItemResourceName(), offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : ""), sellOffer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
					} else {
						sellOffer.decrementAmount(offer.getAmount());
						if(seller != null)
							seller.sendMessage(new TextComponentTranslation("Sell Offer partially fulfilled: %s %s %s at %s each.", offer.getAmount(), offer.getItemResourceName(), offer.getItemMeta() + (offer.getNbt() != null ? " with NBT "+offer.getNbt() : ""), sellOffer.getPrice()).setStyle(TextStyles.DARK_PURPLE));
					}
					offerComplete = true;
					break;
				}
			}
			sellOffers.get(offer.getItemPair()).removeAll(removeOffers);
			saveToFile();
			return offerComplete;
		}
		return false;
	}

	private static ItemStack getStack(ResourceLocation offerResource, boolean isOfferBlock, int amount, Offer offer) {
		//noinspection ConstantConditions
		ItemStack stack =  isOfferBlock ? new ItemStack(Item.getItemFromBlock(ForgeRegistries.BLOCKS.getValue(offerResource)), amount, offer.getItemMeta()) : new ItemStack(ForgeRegistries.ITEMS.getValue(offerResource), amount, offer.getItemMeta());
		stack.setTagCompound(SerializationUtils.getNbt(offer.getNbt()));
		return stack;
	}

	private void sortOffers(){
		for(List<BuyOffer> buyOffer : buyOffers.values())
			buyOffer.sort(Comparator.comparing(BuyOffer::getPrice).thenComparing(BuyOffer::getTimestamp));
		for(List<SellOffer> sellOffer : sellOffers.values())
			sellOffer.sort(Comparator.comparing(SellOffer::getTimestamp));
	}

	private static void readFromFile() {
		if (saveDir == null)
			saveDir = DimensionManager.getCurrentSaveRootDirectory();
		if (saveDir == null) {
			instance = new TransactionDatabase();
			return;
		}
		File f = new File(saveDir, dataFileName);
		if (f.exists()) {
			try {
				ObjectInputStream stream = new ObjectInputStream(new FileInputStream(f));
				instance = (TransactionDatabase) stream.readObject();
				stream.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				instance = new TransactionDatabase();
				f.delete();
			}
		}
		if (instance == null)
			instance = new TransactionDatabase();
	}

	private static void saveToFile() {
		try {
			if (saveDir == null)
				saveDir = DimensionManager.getCurrentSaveRootDirectory();
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(saveDir, dataFileName)));
			out.writeObject(instance);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
