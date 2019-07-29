# Grand Exchange - A server-side marketplace mod

Grand Exchange adds a command-based marketplace to Forge servers. It is a server-side only mod, so players do not have to install the mod to join your server and use it.

Almost anything can be traded on the marketplace. The only things that can't are Enchanted items and other NBT tagged items, such as Written Books.

This mod requires [Grand Economy](https://minecraft.curseforge.com/projects/grand-economy) to work.

## Commands
/ge buy <item> <meta> <amount> <price>
  
  Alias: /ge b <item> <meta> <amount> <price>
  
  Put out a buy offer for an item. When someone sells something matching the criteria you've specified, the transaction will be completed. You must have enough money to complete the transaction in your wallet to make the offer, as the money will be taken out when you make the offer.
  
/ge sell <item> <meta> <amount> <price>
  
  Alias: /ge s <item> <meta> <amount> <price>
  
  Put out a sell offer for an item. When someone buys something matching the criteria you've specified, the transaction will be completed. You must have enough of the item you specified in your inventory to make the offer, as the items will be removed from your inventory when you make the offer.
  
/ge identify

  Alias: /ge i
  
  Identifies the item held in your main hand. This gives you the <item> and <meta> for the above commands.
  
/ge collect

  Alias: /ge c
  
  Collects items recieved from completed buy offers or cancelled sell offers. If there are more items than your inventory can hold, it will give you as many as you can take and let you collect the rest once you've made room.
  
/ge buyoffers

  Alias: /ge bo [page]
  
  Displays current buy offers, so players know what people want and at what price.
  
/ge selloffers

  Alias: /ge so [page]
  
  Displays current sell offers, so players know what people are selling and at what price.
  
/ge myoffers [page]

  Alias: /ge m
  
  Alias: /ge mo
  
  Displays all offers you currently have on the market, with offer numbers next to them so you can choose which you want to cancel.
  
/ge canceloffer <offer number>
  
  Alias: /ge co <offer number>
  
  Cancels an offer you've made. Items from cancelled sell offers can be retrieved with /ge collect. Money from cancelled buy offers is automatically added to your wallet.
  
/ge help

  Alias: /ge h
  
  Displays the list of Grand Exchange commands.
  

## Installation
1. Install Minecraft forge.
2. Put the mod's .jar file you downloaded into the mods directory.

## Permissions:
This mod is available under GPL2.
You are allowed to include it into mod packs of any kind, without asking for permission.
