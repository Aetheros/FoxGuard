/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015. gravityfox - https://gravityfox.net/ and contributors.
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

package net.gravityfox.foxguard.commands;

import net.gravityfox.foxguard.FGManager;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.handlers.GlobalHandler;
import net.gravityfox.foxguard.handlers.IHandler;
import net.gravityfox.foxguard.regions.GlobalRegion;
import net.gravityfox.foxguard.regions.IRegion;
import net.gravityfox.foxguard.util.FGHelper;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandCallable;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.gravityfox.foxguard.util.Aliases.*;

/**
 * Created by Fox on 11/29/2015.
 * Project: SpongeForge
 */
public class CommandEnableDisable implements CommandCallable {

    private final boolean enableState;

    public CommandEnableDisable(boolean enableState) {
        this.enableState = enableState;
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!testPermission(source)) {
            source.sendMessage(Texts.of(TextColors.RED, "You don't have permission to use this command!"));
            return CommandResult.empty();
        }
        String[] args = {};
        if (!arguments.isEmpty()) args = arguments.split(" +");
        if (source instanceof Player) {
            Player player = (Player) source;
            if (args.length == 0) {
                source.sendMessage(Texts.builder()
                        .append(Texts.of(TextColors.GREEN, "Usage: "))
                        .append(getUsage(source))
                        .build());
                return CommandResult.empty();
            } else if (isAlias(REGIONS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int flag = 0;
                Optional<World> optWorld = FGHelper.parseWorld(args[1], FoxGuardMain.getInstance().getGame().getServer());
                World world;
                if (optWorld != null && optWorld.isPresent()) {
                    world = optWorld.get();
                    flag = 1;
                } else world = player.getWorld();
                if (args.length < 2 + flag) throw new CommandException(Texts.of("Must specify a name!"));
                int successes = 0;
                int failures = 0;
                for (String name : Arrays.copyOfRange(args, 1 + flag, args.length)) {
                    IRegion region = FGManager.getInstance().getRegion(world, name);
                    if (region == null) failures++;
                    else if (region instanceof GlobalRegion || region.isEnabled() == this.enableState) failures++;
                    else {
                        region.setIsEnabled(this.enableState);
                        successes++;
                    }
                }
                if (successes > 0) {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " regions with "
                            + successes + " successes and " + failures + " failures!"));
                    return CommandResult.builder().successCount(successes).build();
                } else {
                    throw new CommandException(Texts.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                            " regions. Check to make sure you spelled their names correctly and that they are not already "
                            + (this.enableState ? "enabled." : "disabled.")));
                }

            } else if (isAlias(HANDLERS_ALIASES, args[0])) {
                if (args.length < 2) throw new CommandException(Texts.of("Must specify a name!"));
                int successes = 0;
                int failures = 0;
                for (String name : Arrays.copyOfRange(args, 1, args.length)) {
                    IHandler handler = FGManager.getInstance().gethandler(name);
                    if (handler == null) failures++;
                    else if (handler instanceof GlobalHandler || handler.isEnabled() == this.enableState) {
                        failures++;
                    } else {
                        handler.setIsEnabled(this.enableState);
                        successes++;
                    }
                }
                if (successes > 0) {
                    source.sendMessage(Texts.of(TextColors.GREEN, "Successfully " + (this.enableState ? "enabled" : "disabled") + " handlers with "
                            + successes + " successes and " + failures + " failures!"));
                    return CommandResult.builder().successCount(successes).build();
                } else {
                    throw new CommandException(Texts.of(failures + " failures while trying to " + (this.enableState ? "enable" : "disable") +
                            " handlers. Check to make sure you spelled their names correctly and that they are not already "
                            + (this.enableState ? "enabled." : "disabled.")));
                }
            } else throw new ArgumentParseException(Texts.of("Not a valid category!"), args[0], 0);
        } else {

        }
        return CommandResult.empty();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) throws CommandException {
        return null;
    }

    @Override
    public boolean testPermission(CommandSource source) {
        return source.hasPermission("foxguard.command.modify.objects.enabledisable");
    }

    @Override
    public Optional<? extends Text> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<? extends Text> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Text getUsage(CommandSource source) {
        if (source instanceof Player)
            return Texts.of((this.enableState ? "enable" : "disable") + " <region [w:<worldname>] | handler> [names]...");
        else return Texts.of((this.enableState ? "enable" : "disable") + " <region <worldname> | handler> [names]...");
    }
}