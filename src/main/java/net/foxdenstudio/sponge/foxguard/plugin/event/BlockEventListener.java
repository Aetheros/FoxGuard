/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxguard.plugin.event;

import com.flowpowered.math.vector.Vector3i;
import net.foxdenstudio.sponge.foxguard.plugin.FGManager;
import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.handler.util.Flag;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockEventListener implements EventListener<ChangeBlockEvent> {

    @Override
    public void handle(ChangeBlockEvent event) throws Exception {
        if (event.isCancelled()) return;
        if (event.getTransactions().isEmpty()) return;
        for (Transaction<BlockSnapshot> tr : event.getTransactions()) {
            if (tr.getOriginal().getState().getType().equals(BlockTypes.DIRT)
                    && tr.getFinal().getState().getType().equals(BlockTypes.GRASS)
                    || tr.getOriginal().getState().getType().equals(BlockTypes.GRASS)
                    && tr.getFinal().getState().getType().equals(BlockTypes.DIRT)) return;
        }
        User user;
        if (event.getCause().containsType(Player.class)) {
            user = event.getCause().first(Player.class).get();
        } else if (event.getCause().containsType(User.class)) {
            user = event.getCause().first(User.class).get();
        } else {
            user = null;
        }
        //DebugHelper.printBlockEvent(event);
        Flag typeFlag;
        if (event instanceof ChangeBlockEvent.Modify) typeFlag = Flag.BLOCK_MODIFY;
        else if (event instanceof ChangeBlockEvent.Break) typeFlag = Flag.BLOCK_BREAK;
        else if (event instanceof ChangeBlockEvent.Place) typeFlag = Flag.BLOCK_PLACE;
        else return;


        //FoxGuardMain.instance().getLogger().info(player.getName());

        List<IHandler> handlerList = new ArrayList<>();
        handlerList.add(FGManager.getInstance().getGlobalHandler());
        World world = event.getTargetWorld();

        for (Transaction<BlockSnapshot> trans : event.getTransactions()) {
            Vector3i loc = trans.getOriginal().getLocation().get().getBlockPosition();
            FGManager.getInstance().getRegionListAsStream(world).filter(region -> region.isInRegion(loc))
                    .forEach(region -> region.getHandlersCopy().stream()
                            .filter(handler -> !handlerList.contains(handler))
                            .forEach(handlerList::add));
        }
        Collections.sort(handlerList);
        int currPriority = handlerList.get(0).getPriority();
        Tristate flagState = Tristate.UNDEFINED;
        for (IHandler handler : handlerList) {
            if (handler.getPriority() < currPriority && flagState != Tristate.UNDEFINED) {
                break;
            }
            flagState = flagState.and(handler.handle(user, typeFlag, event));
            currPriority = handler.getPriority();
        }
        flagState = typeFlag.resolve(flagState);
        if (flagState == Tristate.FALSE) {
            if (user instanceof Player)
                ((Player) user).sendMessage(Texts.of("You don't have permission!"));
            event.setCancelled(true);
        } else {
            //makes sure that handlers are unable to cancel the event directly.
            event.setCancelled(false);
        }
    }


}
